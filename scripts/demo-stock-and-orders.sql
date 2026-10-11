-- Демо-поставка на склад и заказы напрямую в БД — по тем же правилам, что у бэкенда:
--   * поставка: на каждый ингредиент две партии (короткий срок 8–48 ч и свежая 3–10 дней) + движение RECEIPT;
--   * заказ: потребность по рецептам; если чего-то не хватает — отказ и запись в stock_deficits,
--     иначе списание по FEFO (сначала ближайший срок) с блокировкой партий и движениями ORDER_CONSUMPTION.
-- Каждый запуск добавляет новую поставку и новые заказы.
--
-- Запуск:
--   docker exec -i UmirHack psql -U admin -d UmirFlagman -v ON_ERROR_STOP=1 < scripts/demo-stock-and-orders.sql
-- Параметры (все необязательные):
--   docker exec -i UmirHack psql -U admin -d UmirFlagman -v ON_ERROR_STOP=1 \
--     -v portions=60 -v orders=100 -v short_share=0.3 -v max_lines=3 -v max_portions=3 < scripts/demo-stock-and-orders.sql
--   portions=0 — без поставки, orders=0 — без заказов.
--
-- В обход API не публикуются события (SSE) и не пересчитывается момент попадания блюд в стоп-лист:
-- сам стоп-лист считается на лету и сразу актуален, а время попадания обновится при следующей
-- операции со складом или перезапуске бэкенда.

\if :{?portions}
\else
\set portions 30
\endif
\if :{?orders}
\else
\set orders 30
\endif
\if :{?short_share}
\else
\set short_share 0.3
\endif
\if :{?max_lines}
\else
\set max_lines 3
\endif
\if :{?max_portions}
\else
\set max_portions 3
\endif

SELECT set_config('flagman.portions', :'portions', false),
       set_config('flagman.orders', :'orders', false),
       set_config('flagman.short_share', :'short_share', false),
       set_config('flagman.max_lines', :'max_lines', false),
       set_config('flagman.max_portions', :'max_portions', false) \gset

DO $$
DECLARE
    v_portions     numeric := current_setting('flagman.portions')::numeric;
    v_orders       int     := current_setting('flagman.orders')::int;
    v_short_share  numeric := current_setting('flagman.short_share')::numeric;
    v_max_lines    int     := current_setting('flagman.max_lines')::int;
    v_max_portions int     := current_setting('flagman.max_portions')::int;
    v_now          timestamptz := now();

    v_delivery uuid;
    v_number   text;
    v_batches  int := 0;
    ing        record;
    part       record;
    v_total    numeric;
    v_short    numeric;
    v_batch    uuid;

    v_dishes  uuid[];
    v_qty     int[];
    v_order   uuid;
    v_ext     text;
    r_need    record;
    b         record;
    v_left    numeric;
    v_take    numeric;
    v_created int := 0;
    v_refused int := 0;
BEGIN
    IF v_short_share < 0 OR v_short_share > 1 THEN
        RAISE EXCEPTION 'short_share должен быть от 0 до 1';
    END IF;

    -- Поставка
    IF v_portions > 0 THEN
        v_delivery := gen_random_uuid();
        v_number := 'SQL-' || to_char(clock_timestamp() AT TIME ZONE 'UTC', 'YYYYMMDD-HH24MISS-MS');
        INSERT INTO deliveries (id, delivery_number, received_at, created_at, status)
        VALUES (v_delivery, v_number, v_now, v_now, 'ACCEPTED');

        -- Каждого ингредиента хватает примерно на `portions` порций самого «тяжелого» блюда с ним
        FOR ing IN
            SELECT i.id, i.unit, max(di.quantity) AS per_portion
            FROM ingredients i
            LEFT JOIN dish_ingredients di ON di.ingredient_id = i.id
            GROUP BY i.id, i.unit
        LOOP
            v_total := coalesce(ing.per_portion * v_portions * (0.6 + random() * 0.8),
                                CASE ing.unit WHEN 'g' THEN 3000 WHEN 'ml' THEN 2000 ELSE 30 END);
            IF ing.unit = 'pcs' THEN
                v_total := greatest(1, round(v_total));
                v_short := round(v_total * v_short_share);
            ELSE
                v_total := round(v_total, 1);
                v_short := round(v_total * v_short_share, 1);
            END IF;

            FOR part IN
                SELECT * FROM (VALUES
                    (v_short,           8 + floor(random() * 41)::int),
                    (v_total - v_short, 72 + floor(random() * 169)::int)
                ) AS t (qty, hours)
            LOOP
                CONTINUE WHEN part.qty <= 0;
                v_batch := gen_random_uuid();
                INSERT INTO stock_batches (id, delivery_id, ingredient_id, received_quantity, remaining_quantity,
                                           expires_at, created_at, version, expired_processed, expiring_notified)
                VALUES (v_batch, v_delivery, ing.id, part.qty, part.qty,
                        v_now + make_interval(hours => part.hours), v_now, 0, false, false);
                INSERT INTO stock_movements (id, batch_id, ingredient_id, type, quantity, order_id, created_at)
                VALUES (gen_random_uuid(), v_batch, ing.id, 'RECEIPT', part.qty, NULL, v_now);
                v_batches := v_batches + 1;
            END LOOP;
        END LOOP;
        RAISE NOTICE 'Поставка %: % партий', v_number, v_batches;
    END IF;

    -- Заказы
    CREATE TEMP TABLE IF NOT EXISTS tmp_order_need (
        ingredient_id uuid PRIMARY KEY,
        need          numeric NOT NULL,
        available     numeric NOT NULL
    ) ON COMMIT DROP;

    FOR n IN 1..v_orders LOOP
        SELECT array_agg(id) INTO v_dishes
        FROM (
            SELECT d.id FROM dishes d
            WHERE d.active AND NOT d.manual_stop
              AND EXISTS (SELECT 1 FROM dish_ingredients di WHERE di.dish_id = d.id)
            ORDER BY random()
            LIMIT 1 + floor(random() * v_max_lines)::int
        ) s;
        IF v_dishes IS NULL THEN
            RAISE NOTICE 'Нет активных блюд с рецептом — заказы не созданы';
            EXIT;
        END IF;
        SELECT array_agg(1 + floor(random() * v_max_portions)::int) INTO v_qty FROM unnest(v_dishes);
        v_ext := 'SQL-' || substr(md5(random()::text || clock_timestamp()::text), 1, 10);

        -- Суммарная потребность и доступный (непросроченный) остаток с блокировкой партий
        DELETE FROM tmp_order_need;
        INSERT INTO tmp_order_need (ingredient_id, need, available)
        SELECT di.ingredient_id, sum(di.quantity * l.qty), 0
        FROM unnest(v_dishes, v_qty) AS l (dish_id, qty)
        JOIN dish_ingredients di ON di.dish_id = l.dish_id
        GROUP BY di.ingredient_id;

        PERFORM 1 FROM stock_batches sb
        WHERE sb.ingredient_id IN (SELECT ingredient_id FROM tmp_order_need)
          AND sb.expires_at > v_now AND sb.remaining_quantity > 0
        ORDER BY sb.expires_at, sb.id
        FOR UPDATE;

        UPDATE tmp_order_need t
        SET available = coalesce((
            SELECT sum(sb.remaining_quantity) FROM stock_batches sb
            WHERE sb.ingredient_id = t.ingredient_id AND sb.expires_at > v_now AND sb.remaining_quantity > 0
        ), 0);

        IF EXISTS (SELECT 1 FROM tmp_order_need WHERE available < need) THEN
            INSERT INTO stock_deficits (id, ingredient_id, required_quantity, available_quantity, external_order_id, created_at)
            SELECT gen_random_uuid(), ingredient_id, need, available, v_ext, v_now
            FROM tmp_order_need WHERE available < need;
            v_refused := v_refused + 1;
            CONTINUE;
        END IF;

        v_order := gen_random_uuid();
        INSERT INTO orders (id, external_order_id, status, created_at)
        VALUES (v_order, v_ext, 'CONFIRMED', v_now);
        INSERT INTO order_items (id, order_id, dish_id, quantity)
        SELECT gen_random_uuid(), v_order, l.dish_id, l.qty
        FROM unnest(v_dishes, v_qty) AS l (dish_id, qty);

        -- FEFO: сначала партии с ближайшим сроком
        FOR r_need IN SELECT ingredient_id, need FROM tmp_order_need LOOP
            v_left := r_need.need;
            FOR b IN
                SELECT id, remaining_quantity FROM stock_batches
                WHERE ingredient_id = r_need.ingredient_id AND expires_at > v_now AND remaining_quantity > 0
                ORDER BY expires_at, id
            LOOP
                EXIT WHEN v_left <= 0;
                v_take := least(b.remaining_quantity, v_left);
                UPDATE stock_batches
                SET remaining_quantity = remaining_quantity - v_take, version = version + 1
                WHERE id = b.id;
                INSERT INTO stock_movements (id, batch_id, ingredient_id, type, quantity, order_id, created_at)
                VALUES (gen_random_uuid(), b.id, r_need.ingredient_id, 'ORDER_CONSUMPTION', v_take, v_order, v_now);
                v_left := v_left - v_take;
            END LOOP;
        END LOOP;
        v_created := v_created + 1;
    END LOOP;

    IF v_orders > 0 THEN
        RAISE NOTICE 'Заказы: создано %, отказ из-за нехватки %', v_created, v_refused;
    END IF;
END $$;

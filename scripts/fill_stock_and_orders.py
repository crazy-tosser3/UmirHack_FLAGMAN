#!/usr/bin/env python3
"""
Наполняет склад поставками и создает заказы через API бэкенда.

Работает через обычные эндпоинты, поэтому срабатывает вся логика: партии со сроками годности,
списание по FEFO, стоп-лист, журнал движений, аналитика и SSE-события.

Нужна учетная запись с ролью manager или admin. Внешние зависимости не нужны.

Примеры:
    python3 scripts/fill_stock_and_orders.py --email admin@yandex.ru --password adminadmin1234
    python3 scripts/fill_stock_and_orders.py --orders 50 --portions 40
    python3 scripts/fill_stock_and_orders.py --no-delivery --orders 20      # только заказы
    python3 scripts/fill_stock_and_orders.py --orders 0                     # только поставка

Логин и пароль можно задать переменными FLAGMAN_EMAIL и FLAGMAN_PASSWORD.
"""

import argparse
import json
import os
import random
import sys
import time
import urllib.error
import urllib.request
import uuid
from datetime import datetime, timedelta, timezone

UTC = timezone.utc  # datetime.UTC есть только с Python 3.11

# Сколько штук/граммов завозить, если ингредиент не используется ни в одном рецепте
DEFAULT_QUANTITY = {"g": 3000, "ml": 2000, "pcs": 30}


class Api:
    def __init__(self, base_url):
        self.base = base_url.rstrip("/") + "/api/v1"
        self.token = None

    def call(self, method, path, body=None, headers=None):
        data = json.dumps(body).encode() if body is not None else None
        request = urllib.request.Request(self.base + path, data=data, method=method)
        request.add_header("Content-Type", "application/json")
        if self.token:
            request.add_header("Authorization", "Bearer " + self.token)
        for key, value in (headers or {}).items():
            request.add_header(key, value)
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                raw = response.read()
                return response.status, json.loads(raw) if raw else None
        except urllib.error.HTTPError as e:
            raw = e.read()
            try:
                return e.code, json.loads(raw)
            except ValueError:
                return e.code, {
                    "error": {
                        "code": str(e.code),
                        "message": raw.decode(errors="replace"),
                    }
                }
        except urllib.error.URLError as e:
            sys.exit("Бэкенд недоступен по адресу %s: %s" % (self.base, e.reason))

    def get_all(self, path):
        items, page = [], 1
        while True:
            sep = "&" if "?" in path else "?"
            status, data = self.call("GET", "%s%spage=%d&limit=100" % (path, sep, page))
            if status != 200:
                sys.exit("GET %s → %d: %s" % (path, status, error_text(data)))
            items += data["items"]
            if len(items) >= data["total"] or not data["items"]:
                return items
            page += 1


def error_text(data):
    if isinstance(data, dict) and "error" in data:
        return "%s — %s" % (data["error"].get("code"), data["error"].get("message"))
    return str(data)


def iso(dt):
    return dt.strftime("%Y-%m-%dT%H:%M:%SZ")


def login(api, email, password):
    status, data = api.call(
        "POST", "/auth/login", {"email": email, "password": password}
    )
    if status != 200:
        sys.exit("Не удалось войти как %s: %s" % (email, error_text(data)))
    role = data["user"]["role"]
    if role not in ("manager", "admin"):
        sys.exit("Нужна роль manager или admin, а у %s — %s" % (email, role))
    api.token = data["accessToken"]
    print("Вход: %s (%s)" % (email, role))


def plan_quantities(ingredients, dishes, portions):
    """Количество на поставку: хватает примерно на `portions` порций самого «тяжелого» блюда с ингредиентом."""
    per_portion = {}
    for dish in dishes:
        for di in dish["ingredients"]:
            per_portion[di["ingredientId"]] = max(
                per_portion.get(di["ingredientId"], 0), float(di["quantity"])
            )

    plan = {}
    for ingredient in ingredients:
        need = per_portion.get(ingredient["id"])
        if need is None:
            quantity = DEFAULT_QUANTITY.get(ingredient["unit"], 1000)
        else:
            quantity = need * portions * random.uniform(0.6, 1.4)
        plan[ingredient["id"]] = (
            max(1, round(quantity))
            if ingredient["unit"] == "pcs"
            else round(quantity, 1)
        )
    return plan


def make_delivery(api, ingredients, plan, short_share):
    """Одна поставка: на каждый ингредиент две партии — часть с коротким сроком, остальное со свежим.
    Так видно и FEFO-списание, и горячие предложения, и предупреждения об истекающих партиях."""
    now = datetime.now(UTC)
    items = []
    for ingredient in ingredients:
        total = plan[ingredient["id"]]
        # Сервер принимает не больше 3 знаков после запятой
        if ingredient["unit"] == "pcs":
            short = int(round(total * short_share))
            fresh = int(total) - short
        else:
            short = round(total * short_share, 1)
            fresh = round(total - short, 1)
        for quantity, hours in (
            (short, random.randint(8, 48)),
            (fresh, random.randint(72, 240)),
        ):
            if quantity > 0:
                items.append(
                    {
                        "ingredientId": ingredient["id"],
                        "quantity": quantity,
                        "expiresAt": iso(now + timedelta(hours=hours)),
                    }
                )

    number = "SCRIPT-" + now.strftime("%Y%m%d-%H%M%S")
    # Не больше 500 позиций в одной поставке — режем на части
    created = 0
    for part, start in enumerate(range(0, len(items), 500), 1):
        chunk = items[start : start + 500]
        body = {
            "deliveryNumber": number + ("-%d" % part if len(items) > 500 else ""),
            "receivedAt": iso(now),
            "items": chunk,
        }
        status, data = api.call(
            "POST", "/deliveries", body, {"Idempotency-Key": body["deliveryNumber"]}
        )
        if status != 201:
            sys.exit("Поставка не создана: %s" % error_text(data))
        created += len(data["items"])
        print("Поставка %s: %d партий" % (data["deliveryNumber"], len(data["items"])))
    return created


def make_orders(api, dishes, count, max_lines, max_portions, delay):
    active = [d for d in dishes if d.get("isActive") and d["ingredients"]]
    if not active:
        print("Нет активных блюд с рецептом — заказы не созданы")
        return
    stats = {"created": 0, "insufficient": 0, "unavailable": 0, "other": 0}
    for i in range(count):
        lines = random.sample(active, min(len(active), random.randint(1, max_lines)))
        body = {
            "externalOrderId": "SCRIPT-" + uuid.uuid4().hex[:10],
            "items": [
                {"dishId": d["id"], "quantity": random.randint(1, max_portions)}
                for d in lines
            ],
        }
        status, data = api.call("POST", "/orders", body)
        if status == 201:
            stats["created"] += 1
        elif status == 409 and data["error"]["code"] == "INSUFFICIENT_STOCK":
            stats["insufficient"] += 1
        elif status == 409 and data["error"]["code"] == "DISH_UNAVAILABLE":
            stats["unavailable"] += 1
        else:
            stats["other"] += 1
            print("Заказ %d: %d %s" % (i + 1, status, error_text(data)))
        if delay:
            time.sleep(delay)
    print(
        "Заказы: создано %(created)d, отказ из-за нехватки %(insufficient)d, блюдо недоступно %(unavailable)d, "
        "другие ошибки %(other)d" % stats
    )


def main():
    parser = argparse.ArgumentParser(description="Поставки на склад и заказы через API")
    parser.add_argument(
        "--base-url", default=os.environ.get("FLAGMAN_URL", "http://127.0.0.1:8080")
    )
    parser.add_argument(
        "--email", default=os.environ.get("FLAGMAN_EMAIL", "admin@yandex.ru")
    )
    parser.add_argument(
        "--password", default=os.environ.get("FLAGMAN_PASSWORD", "adminadmin1234")
    )
    parser.add_argument(
        "--no-delivery", action="store_true", help="не создавать поставку"
    )
    parser.add_argument(
        "--portions",
        type=int,
        default=30,
        help="на сколько порций самого тяжелого блюда хватит поставки (по умолчанию 30)",
    )
    parser.add_argument(
        "--short-share",
        type=float,
        default=0.3,
        help="доля партии с коротким сроком 8–48 ч (по умолчанию 0.3)",
    )
    parser.add_argument(
        "--orders",
        type=int,
        default=30,
        help="сколько заказов создать (по умолчанию 30)",
    )
    parser.add_argument(
        "--max-lines", type=int, default=3, help="максимум разных блюд в заказе"
    )
    parser.add_argument(
        "--max-portions", type=int, default=3, help="максимум порций одного блюда"
    )
    parser.add_argument(
        "--delay", type=float, default=0, help="пауза между заказами, сек"
    )
    parser.add_argument("--seed", type=int, help="seed для воспроизводимых данных")
    args = parser.parse_args()

    if not 0 <= args.short_share <= 1:
        parser.error("--short-share должен быть от 0 до 1")
    if args.seed is not None:
        random.seed(args.seed)

    api = Api(args.base_url)
    login(api, "crazytosser4@gmail.com", "111111111111")

    ingredients = api.get_all("/ingredients")
    dishes = api.get_all("/dishes")
    print("Каталог: %d ингредиентов, %d блюд" % (len(ingredients), len(dishes)))

    if not args.no_delivery:
        if not ingredients:
            sys.exit(
                "Каталог пуст — сначала добавьте ингредиенты (scripts/demo-recipes.sql)"
            )
        make_delivery(
            api,
            ingredients,
            plan_quantities(ingredients, dishes, args.portions),
            args.short_share,
        )

    if args.orders > 0:
        make_orders(
            api, dishes, args.orders, args.max_lines, args.max_portions, args.delay
        )

    status, stop_list = api.call("GET", "/stop-list?limit=1")
    if status == 200:
        print("В стоп-листе сейчас: %d блюд" % stop_list["total"])


if __name__ == "__main__":
    main()

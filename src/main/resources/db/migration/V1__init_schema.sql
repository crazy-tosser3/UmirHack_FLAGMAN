-- Начальная схема: снята с БД, которую раньше строил Hibernate (ddl-auto=update).
-- Имена ограничений сохранены как у Hibernate, чтобы новые и существующие базы совпадали.

CREATE TABLE users (
    id                uuid         NOT NULL,
    created_at        timestamp(6) with time zone NOT NULL,
    email             varchar(255) NOT NULL,
    name              varchar(100) NOT NULL,
    password_hash     varchar(255),
    role              varchar(32)  NOT NULL,
    status            varchar(32)  NOT NULL,
    telegram_id       bigint,
    telegram_username varchar(64),
    updated_at        timestamp(6) with time zone,
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email),
    CONSTRAINT ukdus03vmwyluiy7k2p2gcrqbms UNIQUE (telegram_id),
    CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'MANAGER', 'EMPLOYEE')),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'PENDING_VERIFICATION', 'DISABLED'))
);

CREATE TABLE ingredients (
    id         uuid         NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    name       varchar(100) NOT NULL,
    unit       varchar(8)   NOT NULL,
    CONSTRAINT ingredients_pkey PRIMARY KEY (id),
    CONSTRAINT ukj6tsl15xx76y4kv41yxr4uxab UNIQUE (name)
);

CREATE TABLE dishes (
    id            uuid         NOT NULL,
    active        boolean      NOT NULL,
    manual_stop   boolean      NOT NULL,
    name          varchar(100) NOT NULL,
    stopped_since timestamp(6) with time zone,
    CONSTRAINT dishes_pkey PRIMARY KEY (id),
    CONSTRAINT ukg9v3f8f18je2t2ou8fvwse3kq UNIQUE (name)
);

CREATE TABLE dish_ingredients (
    id            uuid           NOT NULL,
    quantity      numeric(14, 3) NOT NULL,
    dish_id       uuid           NOT NULL,
    ingredient_id uuid           NOT NULL,
    CONSTRAINT dish_ingredients_pkey PRIMARY KEY (id),
    CONSTRAINT uk6sje12n9u5ryib88fmiuflw6g UNIQUE (dish_id, ingredient_id),
    CONSTRAINT fkbhivhxrdibnjx2unpalmvc0e8 FOREIGN KEY (dish_id) REFERENCES dishes (id),
    CONSTRAINT fkcu67of27o6282rbfi19w1sa88 FOREIGN KEY (ingredient_id) REFERENCES ingredients (id)
);

CREATE TABLE deliveries (
    id              uuid         NOT NULL,
    created_at      timestamp(6) with time zone NOT NULL,
    delivery_number varchar(100) NOT NULL,
    received_at     timestamp(6) with time zone NOT NULL,
    status          varchar(32)  NOT NULL,
    CONSTRAINT deliveries_pkey PRIMARY KEY (id),
    CONSTRAINT uk667lysacjsrsb1r6ppswku1rx UNIQUE (delivery_number),
    CONSTRAINT deliveries_status_check CHECK (status IN ('ACCEPTED'))
);

CREATE TABLE stock_batches (
    id                 uuid           NOT NULL,
    created_at         timestamp(6) with time zone NOT NULL,
    expired_processed  boolean        NOT NULL,
    expires_at         timestamp(6) with time zone NOT NULL,
    expiring_notified  boolean        NOT NULL,
    received_quantity  numeric(14, 3) NOT NULL,
    remaining_quantity numeric(14, 3) NOT NULL,
    version            bigint         NOT NULL,
    delivery_id        uuid           NOT NULL,
    ingredient_id      uuid           NOT NULL,
    CONSTRAINT stock_batches_pkey PRIMARY KEY (id),
    CONSTRAINT fkfw1maew32uua1pj3rsmwygk7q FOREIGN KEY (delivery_id) REFERENCES deliveries (id),
    CONSTRAINT fkino68d1837ageuktiilbto4cm FOREIGN KEY (ingredient_id) REFERENCES ingredients (id)
);

CREATE INDEX idx_batches_ingredient_expires ON stock_batches (ingredient_id, expires_at);

CREATE TABLE orders (
    id                uuid         NOT NULL,
    created_at        timestamp(6) with time zone NOT NULL,
    external_order_id varchar(100),
    status            varchar(32)  NOT NULL,
    CONSTRAINT orders_pkey PRIMARY KEY (id),
    CONSTRAINT ukl3gv3ksl0ru29cvo8xsoow6eo UNIQUE (external_order_id),
    CONSTRAINT orders_status_check CHECK (status IN ('CONFIRMED', 'CANCELLED'))
);

CREATE TABLE order_items (
    id       uuid    NOT NULL,
    quantity integer NOT NULL,
    dish_id  uuid    NOT NULL,
    order_id uuid    NOT NULL,
    CONSTRAINT order_items_pkey PRIMARY KEY (id),
    CONSTRAINT fkn06bdypik73hotpxvefsrtn77 FOREIGN KEY (dish_id) REFERENCES dishes (id),
    CONSTRAINT fkbioxgbv59vetrxe0ejfubep1w FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE TABLE stock_movements (
    id            uuid           NOT NULL,
    created_at    timestamp(6) with time zone NOT NULL,
    quantity      numeric(14, 3) NOT NULL,
    type          varchar(32)    NOT NULL,
    batch_id      uuid           NOT NULL,
    ingredient_id uuid           NOT NULL,
    order_id      uuid,
    CONSTRAINT stock_movements_pkey PRIMARY KEY (id),
    CONSTRAINT fksh2y34gx1h83ayngihwbxr3at FOREIGN KEY (batch_id) REFERENCES stock_batches (id),
    CONSTRAINT fko4lrt8rm4ackq86x4qfdj72vv FOREIGN KEY (ingredient_id) REFERENCES ingredients (id),
    CONSTRAINT fk82mrlg9h36kaw5kn90fliqu0b FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT stock_movements_type_check CHECK (type IN ('RECEIPT', 'ORDER_CONSUMPTION', 'EXPIRED_WRITE_OFF', 'ADJUSTMENT'))
);

CREATE INDEX idx_movements_type_created ON stock_movements (type, created_at);

CREATE TABLE stock_deficits (
    id                 uuid           NOT NULL,
    available_quantity numeric(14, 3) NOT NULL,
    created_at         timestamp(6) with time zone NOT NULL,
    external_order_id  varchar(100),
    required_quantity  numeric(14, 3) NOT NULL,
    ingredient_id      uuid           NOT NULL,
    CONSTRAINT stock_deficits_pkey PRIMARY KEY (id),
    CONSTRAINT fkalvx96vaycfvo4l9d7obf6l7i FOREIGN KEY (ingredient_id) REFERENCES ingredients (id)
);

CREATE TABLE idempotency_keys (
    id           varchar(300) NOT NULL,
    created_at   timestamp(6) with time zone NOT NULL,
    request_hash varchar(64)  NOT NULL,
    resource_id  uuid         NOT NULL,
    CONSTRAINT idempotency_keys_pkey PRIMARY KEY (id)
);

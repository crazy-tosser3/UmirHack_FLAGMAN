CREATE TABLE order_item_excluded_ingredients (
    order_item_id uuid NOT NULL,
    ingredient_id uuid NOT NULL,
    CONSTRAINT order_item_excluded_ingredients_pkey PRIMARY KEY (order_item_id, ingredient_id),
    CONSTRAINT fk_order_item_excluded_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_excluded_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id)
);

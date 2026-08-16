CREATE TABLE item_favorites (
    item_favorite_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_item_favorites_user_item UNIQUE (user_id, item_id),
    CONSTRAINT fk_item_favorites_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_favorites_item
        FOREIGN KEY (item_id) REFERENCES items (item_id) ON DELETE CASCADE
);

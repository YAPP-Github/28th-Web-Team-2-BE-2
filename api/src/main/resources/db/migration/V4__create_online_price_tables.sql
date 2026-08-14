CREATE TABLE online_channels (
    channel_id SERIAL PRIMARY KEY,
    channel_name VARCHAR(50) NOT NULL
);

INSERT INTO online_channels (channel_id, channel_name)
VALUES
    (1, '오아시스'),
    (2, '컬리'),
    (3, '11번가'),
    (4, 'GS SHOP');

SELECT setval(
    pg_get_serial_sequence('online_channels', 'channel_id'),
    (SELECT MAX(channel_id) FROM online_channels)
);

CREATE TABLE online_prices (
    online_price_id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES items(item_id) ON DELETE CASCADE,
    channel_id INT NOT NULL REFERENCES online_channels(channel_id) ON DELETE CASCADE,
    item_name VARCHAR(255),
    product_name VARCHAR(255) NOT NULL,
    price INT NOT NULL,
    unit INT NOT NULL,
    product_url VARCHAR(500),
    delivery_note VARCHAR(50),
    created_at DATE NOT NULL DEFAULT CURRENT_DATE
);

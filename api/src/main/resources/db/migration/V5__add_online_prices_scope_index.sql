CREATE INDEX idx_online_prices_item_channel_date
    ON online_prices (item_id, channel_id, created_at);

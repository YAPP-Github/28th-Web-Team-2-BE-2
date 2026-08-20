-- 채널 성격. 배송 조건이 다른 가격을 같은 상품처럼 오해하는 것을 막기 위해 금액과 함께 노출한다.
-- 값은 프론트의 OnlineChannelKind 와 맞춘다 (app/_lib/types.ts).
ALTER TABLE online_channels ADD COLUMN channel_kind VARCHAR(20);

UPDATE online_channels SET channel_kind = '새벽배송' WHERE channel_name IN ('오아시스', '컬리');
UPDATE online_channels SET channel_kind = '오픈마켓' WHERE channel_name IN ('11번가', 'GS SHOP');

ALTER TABLE online_channels ALTER COLUMN channel_kind SET NOT NULL;

ALTER TABLE online_channels
    ADD CONSTRAINT ck_online_channels_kind
        CHECK (channel_kind IN ('새벽배송', '당일배송', '오픈마켓', '즉시배송'));

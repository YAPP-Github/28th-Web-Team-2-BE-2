package com.example.demo.kamis.infrastructure;

import com.example.demo.kamis.application.port.PublicPriceCommandPort;
import java.sql.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
class PublicPriceCommandAdapter implements PublicPriceCommandPort {

    private static final String UPSERT = """
            WITH lock AS (
                SELECT pg_advisory_xact_lock(734920193)
            ), updated AS (
                UPDATE public_prices
                SET price = ?
                FROM lock
                WHERE item_id = ? AND region_id = ? AND price_date = ?
                RETURNING public_price_id
            )
            INSERT INTO public_prices (item_id, region_id, price, price_date)
            SELECT ?, ?, ?, ?
            FROM lock
            WHERE NOT EXISTS (SELECT 1 FROM updated)
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public int upsertAll(final List<PublicPriceCommand> prices) {
        if (prices.isEmpty()) {
            return 0;
        }
        jdbcTemplate.batchUpdate(UPSERT, prices, prices.size(), (statement, price) -> {
            final Date priceDate = Date.valueOf(price.priceDate());
            statement.setInt(1, price.price());
            statement.setLong(2, price.itemId());
            statement.setString(3, price.regionId());
            statement.setDate(4, priceDate);
            statement.setLong(5, price.itemId());
            statement.setString(6, price.regionId());
            statement.setInt(7, price.price());
            statement.setDate(8, priceDate);
        });
        return prices.size();
    }
}

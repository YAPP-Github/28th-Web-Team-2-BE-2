package com.example.demo.item.application.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.item.application.command.CrawlOnlineItemCommand;
import com.example.demo.item.application.result.CrawlOnlineItemResult;
import com.example.demo.item.application.result.OnlineItemCrawlStatus;
import java.net.URI;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class OnlineItemCrawlerPortContractTest {

    private static final URI TARGET_URL = URI.create("https://example.com/items?query=apple");
    private static final OffsetDateTime COLLECTED_AT = OffsetDateTime.parse("2026-08-09T12:00:00+09:00");

    @Test
    void requiresAbsoluteTargetUrl() {
        assertThatThrownBy(() -> new CrawlOnlineItemCommand(URI.create("/items")))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(ErrorType.INVALID_PARAMETER_ERROR);
                    assertThat(exception.httpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void createsSuccessfulCrawlOnlineItemResult() {
        final CrawlOnlineItemResult result = CrawlOnlineItemResult.success(
                TARGET_URL, "<html>items</html>", COLLECTED_AT);

        assertThat(result.sourceUrl()).isEqualTo(TARGET_URL);
        assertThat(result.html()).isEqualTo("<html>items</html>");
        assertThat(result.collectedAt()).isEqualTo(COLLECTED_AT);
        assertThat(result.status()).isEqualTo(OnlineItemCrawlStatus.SUCCESS);
        assertThat(result.failureReason()).isNull();
    }

    @Test
    void createsTemporaryFailureWithoutCrawledHtml() {
        final CrawlOnlineItemResult result = CrawlOnlineItemResult.temporaryFailure(
                TARGET_URL, COLLECTED_AT, "page load timeout");

        assertThat(result.sourceUrl()).isEqualTo(TARGET_URL);
        assertThat(result.html()).isEmpty();
        assertThat(result.status()).isEqualTo(OnlineItemCrawlStatus.TEMPORARY_FAILURE);
        assertThat(result.failureReason()).isEqualTo("page load timeout");
    }
}

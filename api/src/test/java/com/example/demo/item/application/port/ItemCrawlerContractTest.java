package com.example.demo.item.application.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.example.demo.item.application.command.CrawlRequest;
import com.example.demo.item.application.result.CrawledPage;
import com.example.demo.item.application.result.CrawledPage.CrawlStatus;
import java.net.URI;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class ItemCrawlerContractTest {

    private static final URI TARGET_URL = URI.create("https://example.com/items?query=apple");
    private static final OffsetDateTime COLLECTED_AT = OffsetDateTime.parse("2026-08-09T12:00:00+09:00");

    @Test
    void requiresAbsoluteTargetUrl() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new CrawlRequest(URI.create("/items")));
    }

    @Test
    void createsSuccessfulCrawledPage() {
        final CrawledPage page = CrawledPage.success(TARGET_URL, "<html>items</html>", COLLECTED_AT);

        assertThat(page.sourceUrl()).isEqualTo(TARGET_URL);
        assertThat(page.html()).isEqualTo("<html>items</html>");
        assertThat(page.collectedAt()).isEqualTo(COLLECTED_AT);
        assertThat(page.status()).isEqualTo(CrawlStatus.SUCCESS);
        assertThat(page.failureReason()).isNull();
    }

    @Test
    void createsTemporaryFailureWithoutPageSource() {
        final CrawledPage page = CrawledPage.temporaryFailure(
                TARGET_URL, COLLECTED_AT, "page load timeout");

        assertThat(page.sourceUrl()).isEqualTo(TARGET_URL);
        assertThat(page.html()).isEmpty();
        assertThat(page.status()).isEqualTo(CrawlStatus.TEMPORARY_FAILURE);
        assertThat(page.failureReason()).isEqualTo("page load timeout");
    }
}

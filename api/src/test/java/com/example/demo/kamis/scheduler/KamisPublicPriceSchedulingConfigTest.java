package com.example.demo.kamis.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.example.demo.kamis.application.usecase.CollectKamisPublicPriceUseCase;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class KamisPublicPriceSchedulingConfigTest {

    @Test
    void 유효한_지역과_KAMIS_코드가_있을_때만_scheduler_빈을_생성한다() {
        try (AnnotationConfigApplicationContext context = context("1144010200", "1101")) {
            context.refresh();

            assertThat(context.getBeansOfType(KamisPublicPriceScheduler.class)).hasSize(1);
        }
    }

    @Test
    void 수집이_활성화됐는데_지역이_비어_있으면_기동을_실패한다() {
        try (AnnotationConfigApplicationContext context = context("", "1101")) {
            assertThatThrownBy(context::refresh)
                    .isInstanceOf(BeanCreationException.class)
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage("kamis.public-price.collection.region-id must be a 10-digit region ID");
        }
    }

    private AnnotationConfigApplicationContext context(final String regionId, final String countryCode) {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "test",
                Map.of(
                        "kamis.public-price.collection.enabled", "true",
                        "kamis.public-price.collection.region-id", regionId,
                        "kamis.public-price.collection.country-code", countryCode)));
        context.registerBean(CollectKamisPublicPriceUseCase.class, () -> mock(CollectKamisPublicPriceUseCase.class));
        context.register(KamisPublicPriceSchedulingConfig.class);
        return context;
    }
}

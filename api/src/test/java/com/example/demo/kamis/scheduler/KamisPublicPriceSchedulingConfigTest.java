package com.example.demo.kamis.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.demo.kamis.application.usecase.CollectKamisPublicPriceUseCase;
import com.example.demo.kamis.application.usecase.KamisHistoricalPublicPriceBackfillUseCase;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

class KamisPublicPriceSchedulingConfigTest {

    private static final List<String> MAPO_REGION_IDS = List.of(
            "1144010100", "1144010200", "1144010300", "1144010400", "1144010500", "1144010600",
            "1144010700", "1144010800", "1144010900", "1144011000", "1144011100", "1144011200",
            "1144011300", "1144011400", "1144011500", "1144011600", "1144011700", "1144011800",
            "1144012000", "1144012100", "1144012200", "1144012300", "1144012400", "1144012500",
            "1144012600", "1144012700");

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

    @Test
    void 백필이_활성화되면_일회성_실행기_빈을_생성한다() {
        try (AnnotationConfigApplicationContext context = backfillContext("1144010200", "1101")) {
            context.refresh();

            assertThat(context.getBeansOfType(ApplicationRunner.class)).hasSize(1);
        }
    }

    @Test
    void 기존_단일_백필_리전_환경변수는_실제_YAML_경로로_해석된다() throws IOException {
        final PropertySource<?> kamisProperties = loadKamisProperties();
        try (AnnotationConfigApplicationContext context = backfillContextFromKamisYaml(kamisProperties)) {
            final String legacyRegionId = (String) kamisProperties.getProperty("kamis.public-price.backfill.region-id");

            assertThat(context.getEnvironment().resolvePlaceholders(legacyRegionId)).isEqualTo("1144010200");
        }
    }

    @Test
    void 복수_백필_리전이_없으면_기존_단일_리전을_사용한다() {
        try (AnnotationConfigApplicationContext context = legacyBackfillContext("1144010200", "1101")) {
            context.refresh();

            assertThat(context.getBeansOfType(ApplicationRunner.class)).hasSize(1);
        }
    }

    @Test
    void 백필은_마포구_모든_동에_대해_각각_실행한다() throws Exception {
        try (AnnotationConfigApplicationContext context = backfillContext(String.join(",", MAPO_REGION_IDS), "1101")) {
            context.refresh();

            context.getBean(ApplicationRunner.class).run(null);

            final KamisHistoricalPublicPriceBackfillUseCase useCase =
                    context.getBean(KamisHistoricalPublicPriceBackfillUseCase.class);
            final ArgumentCaptor<List<String>> regionIdsCaptor = ArgumentCaptor.forClass(List.class);
            verify(useCase, times(1)).execute(
                    regionIdsCaptor.capture(), eq("1101"), any(LocalDate.class), any(LocalDate.class));
            assertThat(regionIdsCaptor.getValue()).containsExactlyElementsOf(MAPO_REGION_IDS);
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

    private AnnotationConfigApplicationContext backfillContext(
            final String regionId, final String countryCode) {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "test",
                Map.of(
                        "kamis.public-price.backfill.enabled", "true",
                        "kamis.public-price.backfill.region-ids", regionId,
                        "kamis.public-price.backfill.country-code", countryCode)));
        context.registerBean(
                KamisHistoricalPublicPriceBackfillUseCase.class,
                () -> mock(KamisHistoricalPublicPriceBackfillUseCase.class));
        context.register(KamisPublicPriceSchedulingConfig.class);
        return context;
    }

    private AnnotationConfigApplicationContext legacyBackfillContext(
            final String regionId, final String countryCode) {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "test",
                Map.of(
                        "kamis.public-price.backfill.enabled", "true",
                        "kamis.public-price.backfill.region-id", regionId,
                        "kamis.public-price.backfill.country-code", countryCode)));
        context.registerBean(
                KamisHistoricalPublicPriceBackfillUseCase.class,
                () -> mock(KamisHistoricalPublicPriceBackfillUseCase.class));
        context.register(KamisPublicPriceSchedulingConfig.class);
        return context;
    }

    private AnnotationConfigApplicationContext backfillContextFromKamisYaml(
            final PropertySource<?> kamisProperties) {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "environment",
                Map.of(
                        "KAMIS_PUBLIC_PRICE_BACKFILL_ENABLED", "true",
                        "KAMIS_PUBLIC_PRICE_BACKFILL_REGION_ID", "1144010200")));
        context.getEnvironment().getPropertySources().addLast(kamisProperties);
        return context;
    }

    private PropertySource<?> loadKamisProperties() throws IOException {
        return new YamlPropertySourceLoader().load(
                "application-kamis-client.yml", new FileSystemResource("src/main/resources/application-kamis-client.yml")).getFirst();
    }
}

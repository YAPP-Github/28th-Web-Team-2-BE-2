package com.example.demo.report.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.report.application.contract.ExtractedPriceTag;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PriceTagResponseParserTest {

    private final PriceTagResponseParser parser = new PriceTagResponseParser(new ObjectMapper());

    @Test
    void 스키마를_만족하는_JSON을_내부_타입으로_바꾼다() {
        final ExtractedPriceTag result = parser.parse("""
                {"itemName":"오이","itemConfidence":0.96,"price":250,
                 "amount":1,"amountConfidence":0.72,"otherNumberCount":2}""");

        assertThat(result.itemName()).isEqualTo("오이");
        assertThat(result.price()).isEqualTo(250);
        assertThat(result.amount()).isEqualByComparingTo("1");
        assertThat(result.otherNumberCount()).isEqualTo(2);
    }

    // stripCodeFence 를 지운 뒤의 계약: 코드펜스가 붙어 오면 조용히 넘기지 않고 해석 실패로 끝낸다.
    // response_format 이 동작함을 실호출로 확인했으므로(ADR 0002) 펜스는 오지 않아야 한다.
    @Test
    void 코드펜스가_붙어_오면_해석_실패로_끝낸다() {
        assertThatThrownBy(() -> parser.parse("""
                ```json
                {"price":250}
                ```"""))
                .isInstanceOf(ApiException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.IMAGE_ANALYSIS_INVALID_RESPONSE);
    }

    // 이전에는 응답에 priceConfidence 가 없어 품목 신뢰도를 가격 신뢰도로 내보냈다.
    @Test
    void 가격_신뢰도를_품목_신뢰도와_별도로_읽는다() {
        final ExtractedPriceTag result = parser.parse("""
                {"itemName":"오이","itemConfidence":0.96,"price":250,
                 "priceConfidence":0.40,"otherNumberCount":1}""");

        assertThat(result.itemConfidence().value()).isEqualByComparingTo("0.96");
        assertThat(result.priceConfidence().value()).isEqualByComparingTo("0.40");
    }

    @Test
    void 필드가_빠지면_그_필드만_비운다() {
        final ExtractedPriceTag result = parser.parse("{\"price\":250,\"otherNumberCount\":1}");

        assertThat(result.itemName()).isNull();
        assertThat(result.itemConfidence()).isNull();
        assertThat(result.amount()).isNull();
        assertThat(result.price()).isEqualTo(250);
    }

    @Test
    void 명시적_null도_비운_것으로_다룬다() {
        final ExtractedPriceTag result = parser.parse("""
                {"itemName":null,"itemConfidence":null,"price":null,"amount":null,"otherNumberCount":0}""");

        assertThat(result.hasItemName()).isFalse();
        assertThat(result.price()).isNull();
    }

    // 모델이 타입을 틀리는 경우가 있다. 전체를 버리기보다 해당 필드만 버린다.
    @Test
    void 타입이_다른_필드는_그_필드만_버린다() {
        final ExtractedPriceTag result = parser.parse("""
                {"itemName":"오이","price":"250원","amount":"한 개","otherNumberCount":"많음"}""");

        assertThat(result.itemName()).isEqualTo("오이");
        assertThat(result.price()).isNull();
        assertThat(result.amount()).isNull();
        assertThat(result.otherNumberCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-100"})
    void 사진에서_나올_수_없는_가격은_버린다(final String price) {
        final ExtractedPriceTag result = parser.parse(
                "{\"price\":" + price + ",\"otherNumberCount\":1}");

        assertThat(result.price()).isNull();
    }

    @Test
    void 음수_수량은_버린다() {
        final ExtractedPriceTag result = parser.parse("{\"amount\":-1,\"otherNumberCount\":1}");

        assertThat(result.amount()).isNull();
    }

    // 0~100 스케일로 답하는 이탈이 흔하다. clamp 하면 90 이 "최대 확신"으로 승격된다.
    @Test
    void 범위를_벗어난_신뢰도는_버린다() {
        final ExtractedPriceTag result = parser.parse("""
                {"itemName":"오이","itemConfidence":90,"amount":1,"amountConfidence":-0.5,"otherNumberCount":1}""");

        assertThat(result.itemConfidence()).isNull();
        assertThat(result.amountConfidence()).isNull();
    }

    // asInt() 는 int 범위를 넘는 값을 조용히 잘라낸다.
    @Test
    void int_범위를_넘는_가격은_버린다() {
        assertThat(parser.parse("{\"price\":99999999999,\"otherNumberCount\":0}").price()).isNull();
        assertThat(parser.parse("{\"price\":1e300,\"otherNumberCount\":0}").price()).isNull();
    }

    // 저장 API 가 @Digits(integer = 7, fraction = 3) 이다.
    @Test
    void 저장_API_범위를_넘는_수량은_버린다() {
        assertThat(parser.parse("{\"amount\":0.00001,\"otherNumberCount\":0}").amount()).isNull();
        assertThat(parser.parse("{\"amount\":99999999,\"otherNumberCount\":0}").amount()).isNull();
        assertThat(parser.parse("{\"amount\":1.5,\"otherNumberCount\":0}").amount()).isEqualByComparingTo("1.5");
    }

    @Test
    void 가격_기준_수량을_읽는다() {
        final ExtractedPriceTag result = parser.parse("""
                {"price":9900,"priceBasis":"3kg","otherNumberCount":0}""");

        assertThat(result.priceBasis()).isEqualTo("3kg");
    }

    // 본문이 JSON이 아니면 프롬프트나 모델 설정이 잘못된 신호다. 조용히 빈 결과를 주면 원인이 묻힌다.
    @ParameterizedTest
    @ValueSource(strings = {
        "죄송합니다. 사진을 읽을 수 없습니다.",
        "[1,2,3]",
        "\"문자열\"",
        "{unclosed",
        "42"
    })
    void JSON_객체가_아닌_응답은_해석_실패로_끝낸다(final String content) {
        assertThatThrownBy(() -> parser.parse(content))
                .isInstanceOf(ApiException.class)
                .extracting("errorType")
                // 재시도해도 같은 답이 오므로 upstream 장애와 구분한다.
                .isEqualTo(ErrorType.IMAGE_ANALYSIS_INVALID_RESPONSE);
    }

    @Test
    void 음수_otherNumberCount는_0으로_다룬다() {
        assertThat(parser.parse("{\"otherNumberCount\":-3}").otherNumberCount()).isZero();
    }

    @Test
    void 공백만_있는_품목명은_비운_것으로_다룬다() {
        assertThat(parser.parse("{\"itemName\":\"   \",\"otherNumberCount\":0}").hasItemName()).isFalse();
    }
}

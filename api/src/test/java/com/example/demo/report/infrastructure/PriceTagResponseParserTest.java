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
                 "amount":1,"amountConfidence":0.72,"numberCount":2}""");

        assertThat(result.itemName()).isEqualTo("오이");
        assertThat(result.price()).isEqualTo(250);
        assertThat(result.amount()).isEqualByComparingTo("1");
        assertThat(result.numberCount()).isEqualTo(2);
    }

    // 프롬프트로 금지해도 코드펜스를 붙여 오는 모델이 있다.
    @Test
    void 코드펜스로_감싼_응답도_파싱한다() {
        final ExtractedPriceTag result = parser.parse("""
                ```json
                {"itemName":"오이","price":250,"numberCount":1}
                ```""");

        assertThat(result.itemName()).isEqualTo("오이");
        assertThat(result.price()).isEqualTo(250);
    }

    @Test
    void 필드가_빠지면_그_필드만_비운다() {
        final ExtractedPriceTag result = parser.parse("{\"price\":250,\"numberCount\":1}");

        assertThat(result.itemName()).isNull();
        assertThat(result.itemConfidence()).isNull();
        assertThat(result.amount()).isNull();
        assertThat(result.price()).isEqualTo(250);
    }

    @Test
    void 명시적_null도_비운_것으로_다룬다() {
        final ExtractedPriceTag result = parser.parse("""
                {"itemName":null,"itemConfidence":null,"price":null,"amount":null,"numberCount":0}""");

        assertThat(result.hasItemName()).isFalse();
        assertThat(result.price()).isNull();
    }

    // 모델이 타입을 틀리는 경우가 있다. 전체를 버리기보다 해당 필드만 버린다.
    @Test
    void 타입이_다른_필드는_그_필드만_버린다() {
        final ExtractedPriceTag result = parser.parse("""
                {"itemName":"오이","price":"250원","amount":"한 개","numberCount":"많음"}""");

        assertThat(result.itemName()).isEqualTo("오이");
        assertThat(result.price()).isNull();
        assertThat(result.amount()).isNull();
        assertThat(result.numberCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-100"})
    void 사진에서_나올_수_없는_가격은_버린다(final String price) {
        final ExtractedPriceTag result = parser.parse(
                "{\"price\":" + price + ",\"numberCount\":1}");

        assertThat(result.price()).isNull();
    }

    @Test
    void 음수_수량은_버린다() {
        final ExtractedPriceTag result = parser.parse("{\"amount\":-1,\"numberCount\":1}");

        assertThat(result.amount()).isNull();
    }

    @Test
    void 범위를_벗어난_신뢰도는_범위로_끌어당긴다() {
        final ExtractedPriceTag result = parser.parse("""
                {"itemName":"오이","itemConfidence":1.8,"amount":1,"amountConfidence":-0.5,"numberCount":1}""");

        assertThat(result.itemConfidence().value()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(result.amountConfidence().value()).isEqualByComparingTo(BigDecimal.ZERO);
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
    void JSON_객체가_아닌_응답은_인식_실패로_끝낸다(final String content) {
        assertThatThrownBy(() -> parser.parse(content))
                .isInstanceOf(ApiException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.IMAGE_ANALYSIS_UNAVAILABLE);
    }

    @Test
    void 음수_numberCount는_0으로_다룬다() {
        assertThat(parser.parse("{\"numberCount\":-3}").numberCount()).isZero();
    }

    @Test
    void 공백만_있는_품목명은_비운_것으로_다룬다() {
        assertThat(parser.parse("{\"itemName\":\"   \",\"numberCount\":0}").hasItemName()).isFalse();
    }
}

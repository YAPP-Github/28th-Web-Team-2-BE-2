package com.example.demo.report.infrastructure;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.report.application.contract.ExtractedPriceTag;
import com.example.demo.report.domain.AnalysisConfidence;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 모델이 돌려준 JSON 문자열을 검증해 내부 타입으로 바꾼다.
 *
 * <p>Qwen JSON을 presentation까지 흘리지 않는 경계다. 필드가 빠지거나 타입이 다르면 그 필드만
 * {@code null}로 두고, 본문 자체가 JSON이 아니면 인식 실패로 끝낸다 — 후자는 프롬프트나 모델
 * 설정이 잘못된 신호이므로 조용히 빈 결과를 주면 원인이 드러나지 않는다.
 *
 * <p>모델이 코드펜스를 붙여 오는 경우가 흔해 앞뒤 펜스를 걷어낸 뒤 파싱한다. 프롬프트로 금지해도
 * 지키지 않는 일이 있어 방어한다.
 */
@Component
@RequiredArgsConstructor
public class PriceTagResponseParser {

    private static final int AMOUNT_SCALE = 3;
    private static final int AMOUNT_INTEGER_DIGITS = 7;

    private final ObjectMapper objectMapper;

    public ExtractedPriceTag parse(final String content) {
        final JsonNode root = readTree(stripCodeFence(content));
        return new ExtractedPriceTag(
                text(root, "itemName"),
                confidence(root, "itemConfidence"),
                integer(root, "price"),
                confidence(root, "priceConfidence"),
                text(root, "priceBasis"),
                decimal(root, "amount"),
                confidence(root, "amountConfidence"),
                otherNumberCount(root));
    }

    private JsonNode readTree(final String content) {
        try {
            final JsonNode root = objectMapper.readTree(content);
            if (root == null || !root.isObject()) {
                throw invalidPayload(null);
            }
            return root;
        } catch (final com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw invalidPayload(exception);
        }
    }

    /** ```json ... ``` 형태를 걷어낸다. */
    private String stripCodeFence(final String content) {
        final String trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        final int firstNewline = trimmed.indexOf('\n');
        final int closing = trimmed.lastIndexOf("```");
        if (firstNewline < 0 || closing <= firstNewline) {
            return trimmed;
        }
        return trimmed.substring(firstNewline + 1, closing).trim();
    }

    private String text(final JsonNode root, final String field) {
        final JsonNode node = root.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            return null;
        }
        return node.asText().trim();
    }

    private Integer integer(final JsonNode root, final String field) {
        final JsonNode node = root.get(field);
        if (node == null || !node.isNumber()) {
            return null;
        }
        // asInt() 는 int 범위를 넘는 값을 조용히 잘라낸다 — 99999999999 가 1215752191 이 된다.
        if (!node.canConvertToInt()) {
            return null;
        }
        // 음수 가격은 사진에서 나올 수 없다. 파싱 오류로 보고 버린다.
        if (node.asInt() <= 0) {
            return null;
        }
        return node.asInt();
    }

    private BigDecimal decimal(final JsonNode root, final String field) {
        final JsonNode node = root.get(field);
        if (node == null || !node.isNumber()) {
            return null;
        }
        final BigDecimal value = node.decimalValue();
        if (value.signum() <= 0) {
            return null;
        }
        // 저장 API 가 @Digits(integer = 7, fraction = 3) 이다. 넘는 값을 실어 보내면 400 이 된다.
        if (value.scale() > AMOUNT_SCALE || value.precision() - value.scale() > AMOUNT_INTEGER_DIGITS) {
            return null;
        }
        return value;
    }

    /**
     * 신뢰도는 0~1 밖이면 버린다.
     *
     * <p>clamp 하지 않는 이유가 있다. 모델이 0~100 스케일로 답하면(프롬프트로 금지해도 흔한 이탈)
     * 90 이 1.00 으로 올라가 "확신 90%"가 "최대 확신"으로 승격된다. 모르는 값은 모른다고 두는 편이
     * 낫다 — 신뢰도 하나를 비우는 건 인식 결과를 버리는 게 아니다.
     */
    private AnalysisConfidence confidence(final JsonNode root, final String field) {
        final JsonNode node = root.get(field);
        if (node == null || !node.isNumber()) {
            return null;
        }
        final BigDecimal value = node.decimalValue();
        if (value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            return null;
        }
        return new AnalysisConfidence(value);
    }

    private int otherNumberCount(final JsonNode root) {
        final JsonNode node = root.get("otherNumberCount");
        if (node == null || !node.isNumber() || !node.canConvertToInt() || node.asInt() < 0) {
            return 0;
        }
        return node.asInt();
    }

    private ApiException invalidPayload(final Throwable cause) {
        return new ApiException(
                ErrorType.IMAGE_ANALYSIS_UNAVAILABLE.description(),
                ErrorType.IMAGE_ANALYSIS_UNAVAILABLE,
                HttpStatus.BAD_GATEWAY,
                cause);
    }
}

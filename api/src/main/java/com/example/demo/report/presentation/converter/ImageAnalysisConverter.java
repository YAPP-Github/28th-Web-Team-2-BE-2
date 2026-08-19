package com.example.demo.report.presentation.converter;

import com.example.demo.report.application.command.AnalyzeReportImageCommand;
import com.example.demo.report.application.contract.ItemCandidate;
import com.example.demo.report.application.result.ImageAnalysisResult;
import com.example.demo.report.domain.AnalysisConfidence;
import com.example.demo.report.presentation.dto.ImageAnalysisRequest;
import com.example.demo.report.presentation.dto.ImageAnalysisResponse;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** 인식 요청·결과 변환. */
@Component
public class ImageAnalysisConverter {

    public AnalyzeReportImageCommand toCommand(final ImageAnalysisRequest request) {
        return new AnalyzeReportImageCommand(request.imageUrl(), request.itemId());
    }

    public ImageAnalysisResponse toResponse(final ImageAnalysisResult result) {
        return new ImageAnalysisResponse(
                toItem(result),
                toPrice(result),
                toAmount(result));
    }

    private ImageAnalysisResponse.AnalyzedItem toItem(final ImageAnalysisResult result) {
        final ItemCandidate item = result.item();
        if (item == null) {
            return null;
        }
        return new ImageAnalysisResponse.AnalyzedItem(
                item.itemId(), item.name(), result.unit(), valueOf(result.itemConfidence()));
    }

    private ImageAnalysisResponse.AnalyzedPrice toPrice(final ImageAnalysisResult result) {
        if (result.price() == null) {
            return null;
        }
        return new ImageAnalysisResponse.AnalyzedPrice(
                result.price(),
                ImageAnalysisResponse.AnalyzedPrice.KRW,
                valueOf(result.priceConfidence()),
                result.priceBasis(),
                unitMatched(result));
    }

    private ImageAnalysisResponse.AnalyzedAmount toAmount(final ImageAnalysisResult result) {
        if (result.amount() == null) {
            return null;
        }
        return new ImageAnalysisResponse.AnalyzedAmount(
                result.amount(), valueOf(result.amountConfidence()));
    }

    /**
     * 사진의 가격 기준과 품목 기본 단위가 같은지.
     *
     * <p>둘 중 하나라도 모르면 판단할 수 없으므로 {@code null}이다. 억지로 true 로 두면 클라이언트가
     * 환산 없이 저장해 공시가 대비 차이가 왜곡된다.
     */
    private Boolean unitMatched(final ImageAnalysisResult result) {
        if (result.item() == null || result.priceBasis() == null) {
            return null;
        }
        return result.item().matchesUnit(result.priceBasis());
    }

    private BigDecimal valueOf(final AnalysisConfidence confidence) {
        if (confidence == null) {
            return null;
        }
        return confidence.value();
    }
}

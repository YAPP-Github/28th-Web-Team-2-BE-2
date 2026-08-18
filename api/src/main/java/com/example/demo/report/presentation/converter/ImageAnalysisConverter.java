package com.example.demo.report.presentation.converter;

import com.example.demo.report.application.command.AnalyzeReportImageCommand;
import com.example.demo.report.application.contract.ItemCandidate;
import com.example.demo.report.application.result.ImageAnalysisResult;
import com.example.demo.report.domain.AnalysisConfidence;
import com.example.demo.report.presentation.dto.ImageAnalysisRequest;
import com.example.demo.report.presentation.dto.ImageAnalysisResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/** 인식 요청·결과 변환. */
@Component
public class ImageAnalysisConverter {

    public AnalyzeReportImageCommand toCommand(final ImageAnalysisRequest request) {
        return new AnalyzeReportImageCommand(request.imageUrl(), request.itemId());
    }

    public ImageAnalysisResponse toResponse(final ImageAnalysisResult result) {
        return new ImageAnalysisResponse(
                toItem(result.item(), result.unit(), result.itemConfidence()),
                toCandidates(result.candidates()),
                toPrice(result),
                toAmount(result));
    }

    private ImageAnalysisResponse.AnalyzedItem toItem(
            final ItemCandidate candidate, final String unit, final AnalysisConfidence confidence) {
        if (candidate == null) {
            return null;
        }
        return new ImageAnalysisResponse.AnalyzedItem(
                candidate.itemId(), candidate.name(), unit, valueOf(confidence));
    }

    /**
     * 후보 목록에도 단위를 담는다. 사용자가 후보를 고른 순간 화면이 저장 요청의 {@code unit}을
     * 채울 수 있어야 하고, 그 값은 품목마다 다르다.
     */
    private List<ImageAnalysisResponse.AnalyzedItem> toCandidates(final List<ItemCandidate> candidates) {
        return candidates.stream()
                .map(candidate -> new ImageAnalysisResponse.AnalyzedItem(
                        candidate.itemId(), candidate.name(), candidate.defaultUnit(), null))
                .toList();
    }

    private ImageAnalysisResponse.AnalyzedPrice toPrice(final ImageAnalysisResult result) {
        if (result.price() == null) {
            return null;
        }
        return new ImageAnalysisResponse.AnalyzedPrice(
                result.price(),
                ImageAnalysisResponse.AnalyzedPrice.KRW,
                valueOf(result.priceConfidence()));
    }

    private ImageAnalysisResponse.AnalyzedAmount toAmount(final ImageAnalysisResult result) {
        if (result.amount() == null) {
            return null;
        }
        return new ImageAnalysisResponse.AnalyzedAmount(
                result.amount(), valueOf(result.amountConfidence()));
    }

    private BigDecimal valueOf(final AnalysisConfidence confidence) {
        if (confidence == null) {
            return null;
        }
        return confidence.value();
    }
}

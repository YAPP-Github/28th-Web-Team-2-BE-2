package com.example.demo.report.presentation.converter;

import com.example.demo.report.application.contract.ItemCandidate;
import com.example.demo.report.application.result.CreateUserReportResult;
import com.example.demo.report.application.result.ImageAnalysisResult;
import com.example.demo.report.application.result.StoreReportResult;
import com.example.demo.report.application.result.StoreReportsResult;
import com.example.demo.report.domain.AnalysisConfidence;
import com.example.demo.report.presentation.dto.CreateUserReportResponse;
import com.example.demo.report.presentation.dto.ImageAnalysisResponse;
import com.example.demo.report.presentation.dto.StoreReportResponse;
import com.example.demo.report.presentation.dto.StoreReportsResponse;
import com.example.demo.report.presentation.dto.StoreReportsSummaryResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UserReportResultConverter {
    public CreateUserReportResponse toResponse(final CreateUserReportResult result) {
        return new CreateUserReportResponse(result.reportId(), result.itemId(), result.storeId(), result.reportedAt());
    }

    public StoreReportsResponse toStoreReportsResponse(final StoreReportsResult result) {
        final List<StoreReportResponse> reports = result.reports().stream()
                .map(this::toStoreReportResponse)
                .toList();
        return new StoreReportsResponse(
                result.storeId(),
                new StoreReportsSummaryResponse(result.cheapCount(), result.expensiveCount()),
                reports,
                result.page(),
                result.size(),
                result.hasNext());
    }

    private StoreReportResponse toStoreReportResponse(final StoreReportResult result) {
        return new StoreReportResponse(
                result.reportId(), result.itemId(), result.itemName(), result.itemImageUrl(),
                result.price(), result.unit(), result.reportedDate(), result.publicPriceDiff(),
                result.priceDiffRate(), result.priceClassification());
    }

    /** 인식하지 못한 항목은 {@code null}로 내려 보낸다. 빈 껍데기를 주면 값이 있는 것처럼 보인다. */
    public ImageAnalysisResponse toImageAnalysisResponse(final ImageAnalysisResult result) {
        return new ImageAnalysisResponse(
                toAnalyzedItem(result), toAnalyzedPrice(result), toAnalyzedAmount(result));
    }

    private ImageAnalysisResponse.AnalyzedItem toAnalyzedItem(final ImageAnalysisResult result) {
        final ItemCandidate item = result.item();
        if (item == null) {
            return null;
        }
        return new ImageAnalysisResponse.AnalyzedItem(
                item.itemId(), item.name(), result.unit(), valueOf(result.itemConfidence()));
    }

    private ImageAnalysisResponse.AnalyzedPrice toAnalyzedPrice(final ImageAnalysisResult result) {
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

    private ImageAnalysisResponse.AnalyzedAmount toAnalyzedAmount(final ImageAnalysisResult result) {
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

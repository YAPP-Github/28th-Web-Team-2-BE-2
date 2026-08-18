package com.example.demo.report.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.report.application.command.AnalyzeReportImageCommand;
import com.example.demo.report.application.contract.ExtractedPriceTag;
import com.example.demo.report.application.contract.ItemCandidate;
import com.example.demo.report.application.port.ImageAnalysisPort;
import com.example.demo.report.application.port.ItemCandidateQueryPort;
import com.example.demo.report.application.result.ImageAnalysisResult;
import com.example.demo.report.domain.AnalysisConfidence;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnalyzeReportImageUseCaseTest {

    private static final String IMAGE_URL = "https://cdn.example.com/images/abc.jpg";
    private static final ItemCandidate CUCUMBER = new ItemCandidate(12L, "오이", "1개");
    private static final ItemCandidate ZUCCHINI = new ItemCandidate(10L, "애호박", "1개");

    private ImageAnalysisPort imageAnalysisPort;
    private ItemCandidateQueryPort itemCandidateQueryPort;
    private AnalyzeReportImageUseCase useCase;

    @BeforeEach
    void setUp() {
        imageAnalysisPort = mock(ImageAnalysisPort.class);
        itemCandidateQueryPort = mock(ItemCandidateQueryPort.class);
        useCase = new AnalyzeReportImageUseCase(imageAnalysisPort, itemCandidateQueryPort);
    }

    @Test
    void 품목이_하나로_좁혀지면_확정하고_품목의_기본_단위를_돌려준다() {
        given(extracted("오이", "0.96", 250, null, null, 1));
        when(itemCandidateQueryPort.findCandidatesByName("오이")).thenReturn(List.of(CUCUMBER));

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.item()).isEqualTo(CUCUMBER);
        assertThat(result.price()).isEqualTo(250);
        assertThat(result.unit()).isEqualTo("1개");
    }

    // 저장 API가 unit을 items.default_unit과 문자열 일치로 검증한다. 모델이 준 단위를 쓰면 400이다.
    @Test
    void 단위는_모델_응답이_아니라_품목에서_가져온다() {
        given(extracted("오이", "0.96", 250, null, null, 1));
        when(itemCandidateQueryPort.findCandidatesByName("오이")).thenReturn(List.of(CUCUMBER));

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.unit()).isEqualTo(CUCUMBER.defaultUnit());
    }

    @Test
    void 후보가_여럿이면_확정하지_않고_목록을_준다() {
        given(extracted("호박", "0.90", 3000, null, null, 1));
        when(itemCandidateQueryPort.findCandidatesByName("호박"))
                .thenReturn(List.of(ZUCCHINI, new ItemCandidate(11L, "쥬키니", "1개")));

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.item()).isNull();
        assertThat(result.unit()).isNull();
        assertThat(result.candidates()).hasSize(2);
    }

    // 우리 목록에서 갈렸다는 사실 자체가 근거 부족이다.
    @Test
    void 후보가_여럿이면_모델_점수를_그대로_쓰지_않고_낮춘다() {
        given(extracted("호박", "0.99", 3000, null, null, 1));
        when(itemCandidateQueryPort.findCandidatesByName("호박"))
                .thenReturn(List.of(ZUCCHINI, new ItemCandidate(11L, "쥬키니", "1개")));

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.itemConfidence()).isEqualTo(AnalysisConfidence.low());
    }

    @Test
    void 품목_매칭에_실패하면_품목을_비우고_가격은_유지한다() {
        given(extracted("알수없는채소", "0.40", 1200, null, null, 1));
        when(itemCandidateQueryPort.findCandidatesByName("알수없는채소")).thenReturn(List.of());

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.item()).isNull();
        assertThat(result.candidates()).isEmpty();
        assertThat(result.price()).isEqualTo(1200);
    }

    @Test
    void 사용자가_고른_품목이_모델_판단보다_우선한다() {
        given(extracted("애호박", "0.95", 3000, null, null, 1));
        when(itemCandidateQueryPort.findById(12L)).thenReturn(Optional.of(CUCUMBER));

        final ImageAnalysisResult result = useCase.execute(command(12L));

        assertThat(result.item()).isEqualTo(CUCUMBER);
        verify(itemCandidateQueryPort, never()).findCandidatesByName(any());
    }

    @Test
    void 품목명을_인식하지_못하면_조회를_시도하지_않는다() {
        given(extracted(null, null, 250, null, null, 1));

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.item()).isNull();
        verify(itemCandidateQueryPort, never()).findCandidatesByName(any());
    }

    // 사진에 근거가 없으면 억지로 추정하지 않는다는 요구사항.
    @Test
    void 수량을_인식하지_못하면_비워_두고_신뢰도도_주지_않는다() {
        given(extracted("오이", "0.96", 250, null, null, 1));
        when(itemCandidateQueryPort.findCandidatesByName("오이")).thenReturn(List.of(CUCUMBER));

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.amount()).isNull();
        assertThat(result.amountConfidence()).isNull();
    }

    // "250원 / 1인 10개 제한"처럼 숫자가 여럿이면 어느 값이 판매 가격인지 확정할 근거가 약하다.
    @Test
    void 가격표에_숫자가_여럿이면_가격_신뢰도를_낮춘다() {
        given(extracted("오이", "0.95", 250, null, null, 3));
        when(itemCandidateQueryPort.findCandidatesByName("오이")).thenReturn(List.of(CUCUMBER));

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.price()).isEqualTo(250);
        assertThat(result.priceConfidence()).isEqualTo(AnalysisConfidence.low());
    }

    @Test
    void 숫자가_하나면_가격_신뢰도를_그대로_둔다() {
        given(extracted("오이", "0.95", 250, null, null, 1));
        when(itemCandidateQueryPort.findCandidatesByName("오이")).thenReturn(List.of(CUCUMBER));

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.priceConfidence()).isEqualTo(new AnalysisConfidence(new BigDecimal("0.95")));
    }

    @Test
    void 숫자가_여럿이면_수량_신뢰도도_함께_낮춘다() {
        given(extracted("오이", "0.95", 250, new BigDecimal("1"), new BigDecimal("0.80"), 3));
        when(itemCandidateQueryPort.findCandidatesByName("오이")).thenReturn(List.of(CUCUMBER));

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.amountConfidence()).isEqualTo(AnalysisConfidence.low());
    }

    @Test
    void 아무것도_읽지_못하면_빈_결과를_준다() {
        when(imageAnalysisPort.analyze(IMAGE_URL)).thenReturn(ExtractedPriceTag.empty());

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.item()).isNull();
        assertThat(result.price()).isNull();
        assertThat(result.amount()).isNull();
        assertThat(result.candidates()).isEmpty();
    }

    @Test
    void 사용자가_고른_품목이_존재하지_않으면_후보를_비운다() {
        given(extracted("오이", "0.96", 250, null, null, 1));
        when(itemCandidateQueryPort.findById(999L)).thenReturn(Optional.empty());

        final ImageAnalysisResult result = useCase.execute(command(999L));

        assertThat(result.item()).isNull();
        assertThat(result.candidates()).isEmpty();
    }

    private void given(final ExtractedPriceTag extracted) {
        when(imageAnalysisPort.analyze(IMAGE_URL)).thenReturn(extracted);
    }

    private AnalyzeReportImageCommand command(final Long itemId) {
        return new AnalyzeReportImageCommand(IMAGE_URL, itemId);
    }

    private ExtractedPriceTag extracted(
            final String itemName,
            final String itemConfidence,
            final Integer price,
            final BigDecimal amount,
            final BigDecimal amountConfidence,
            final int numberCount) {
        return new ExtractedPriceTag(
                itemName,
                itemConfidence == null ? null : new AnalysisConfidence(new BigDecimal(itemConfidence)),
                price,
                amount,
                amountConfidence == null ? null : new AnalysisConfidence(amountConfidence),
                numberCount);
    }
}

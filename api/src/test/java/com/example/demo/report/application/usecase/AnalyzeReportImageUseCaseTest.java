package com.example.demo.report.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class AnalyzeReportImageUseCaseTest {

    private static final String IMAGE_URL = "https://cdn.example.com/images/abc.jpg";
    private static final ItemCandidate CUCUMBER = new ItemCandidate(12L, "오이", "1개");

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
        given(extracted("오이", "0.96", 250));
        when(itemCandidateQueryPort.findByName("오이")).thenReturn(Optional.of(CUCUMBER));

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.item()).isEqualTo(CUCUMBER);
        assertThat(result.price()).isEqualTo(250);
        assertThat(result.unit()).isEqualTo("1개");
    }

    // 저장 API가 unit을 items.default_unit과 문자열 일치로 검증한다. 모델이 준 단위를 쓰면 400이다.
    @Test
    void 단위는_모델_응답이_아니라_품목에서_가져온다() {
        given(extracted("오이", "0.96", 250));
        when(itemCandidateQueryPort.findByName("오이")).thenReturn(Optional.of(CUCUMBER));

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.unit()).isEqualTo(CUCUMBER.defaultUnit());
    }

    @Test
    void 품목_매칭에_실패하면_품목을_비우고_가격은_유지한다() {
        given(extracted("알수없는채소", "0.40", 1200));
        when(itemCandidateQueryPort.findByName("알수없는채소")).thenReturn(Optional.empty());

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.item()).isNull();
        assertThat(result.unit()).isNull();
        assertThat(result.price()).isEqualTo(1200);
    }

    @Test
    void 사용자가_고른_품목이_모델_판단보다_우선한다() {
        given(extracted("애호박", "0.95", 3000));
        when(itemCandidateQueryPort.findById(12L)).thenReturn(Optional.of(CUCUMBER));

        final ImageAnalysisResult result = useCase.execute(command(12L));

        assertThat(result.item()).isEqualTo(CUCUMBER);
        verify(itemCandidateQueryPort, never()).findByName(any());
    }

    @Test
    void 사진_분석_성공_로그에_품목_추적값을_남긴다(final CapturedOutput output) {
        given(extracted("애호박", "0.95", 3000));
        when(itemCandidateQueryPort.findById(12L)).thenReturn(Optional.of(CUCUMBER));

        useCase.execute(command(12L));

        assertThat(output)
                .contains("report image analysis completed")
                .contains("selectedItemId=12")
                .contains("matchedItemId=12");
    }

    @Test
    void 품목명을_인식하지_못하면_조회를_시도하지_않는다() {
        given(extracted(null, null, 250));

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.item()).isNull();
        verify(itemCandidateQueryPort, never()).findByName(any());
    }

    // 사진에 근거가 없으면 억지로 추정하지 않는다는 요구사항.
    @Test
    void 수량을_인식하지_못하면_비워_두고_신뢰도도_주지_않는다() {
        given(extracted("오이", "0.96", 250));
        when(itemCandidateQueryPort.findByName("오이")).thenReturn(Optional.of(CUCUMBER));

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.amount()).isNull();
        assertThat(result.amountConfidence()).isNull();
    }

    // "250원 / 1인 10개 제한"처럼 숫자가 여럿이면 어느 값이 판매 가격인지 확정할 근거가 약하다.
    @Test
    void 가격표에_숫자가_여럿이면_가격_신뢰도를_낮춘다() {
        given(ExtractedPriceTag.builder()
                .itemName("오이").itemConfidence(confidence("0.95"))
                .price(250).priceConfidence(confidence("0.95"))
                .otherNumberCount(2).build());
        when(itemCandidateQueryPort.findByName("오이")).thenReturn(Optional.of(CUCUMBER));

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.price()).isEqualTo(250);
        assertThat(result.priceConfidence()).isEqualTo(AnalysisConfidence.low());
    }

    @Test
    void 숫자가_하나면_가격_신뢰도를_그대로_둔다() {
        given(extracted("오이", "0.95", 250));
        when(itemCandidateQueryPort.findByName("오이")).thenReturn(Optional.of(CUCUMBER));

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.priceConfidence()).isEqualTo(new AnalysisConfidence(new BigDecimal("0.95")));
    }

    // 품목은 확실하지만 가격 숫자가 흐린 사진이 있다. 이전에는 품목 신뢰도를 가격 신뢰도로 내보냈다.
    @Test
    void 가격_신뢰도는_품목_신뢰도와_독립이다() {
        when(imageAnalysisPort.analyze(IMAGE_URL)).thenReturn(ExtractedPriceTag.builder()
                .itemName("오이").itemConfidence(confidence("0.99"))
                .price(250).priceConfidence(confidence("0.35"))
                .build());
        when(itemCandidateQueryPort.findByName("오이")).thenReturn(Optional.of(CUCUMBER));

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.itemConfidence().value()).isEqualByComparingTo("0.99");
        assertThat(result.priceConfidence().value()).isEqualByComparingTo("0.35");
    }

    @Test
    void 숫자가_여럿이면_수량_신뢰도도_함께_낮춘다() {
        given(ExtractedPriceTag.builder()
                .itemName("오이").itemConfidence(confidence("0.95"))
                .price(250).priceConfidence(confidence("0.95"))
                .amount(new BigDecimal("1")).amountConfidence(confidence("0.80"))
                .otherNumberCount(2).build());
        when(itemCandidateQueryPort.findByName("오이")).thenReturn(Optional.of(CUCUMBER));

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.amountConfidence()).isEqualTo(AnalysisConfidence.low());
    }

    @Test
    void 아무것도_읽지_못하면_빈_결과를_준다() {
        when(imageAnalysisPort.analyze(IMAGE_URL)).thenReturn(ExtractedPriceTag.builder().build());

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.item()).isNull();
        assertThat(result.price()).isNull();
        assertThat(result.amount()).isNull();
    }

    // 조용히 빈 결과를 주면 클라이언트가 "인식 실패"와 구분할 수 없다. 저장 API 도 404 를 준다.
    @Test
    void 사용자가_고른_품목이_존재하지_않으면_404로_끝낸다() {
        given(extracted("오이", "0.96", 250));
        when(itemCandidateQueryPort.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command(999L)))
                .isInstanceOf(ApiException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NO_RESOURCE_ERROR);
    }

    // 모델이 다른 품목에 매긴 점수를 사용자가 고른 품목의 신뢰도로 내보내면 거짓 표시가 된다.
    @Test
    void 사용자가_품목을_고르면_AI_신뢰도를_주지_않는다() {
        given(extracted("감자", "0.95", 3000));
        when(itemCandidateQueryPort.findById(12L)).thenReturn(Optional.of(CUCUMBER));

        final ImageAnalysisResult result = useCase.execute(command(12L));

        assertThat(result.item()).isEqualTo(CUCUMBER);
        assertThat(result.itemConfidence()).isNull();
    }

    // 정상적인 가격표도 숫자가 둘이다. 해석하지 못한 숫자가 남았을 때만 깎아야 한다.
    @Test
    void 해석하지_못한_숫자가_없으면_신뢰도를_유지한다() {
        given(ExtractedPriceTag.builder()
                .itemName("감자").itemConfidence(confidence("0.95"))
                .price(3900).priceConfidence(confidence("0.95"))
                .priceBasis("1kg").amount(new BigDecimal("1")).amountConfidence(confidence("0.90"))
                .otherNumberCount(0).build());
        when(itemCandidateQueryPort.findByName("감자"))
                .thenReturn(Optional.of(new ItemCandidate(1L, "감자", "1kg")));

        final ImageAnalysisResult result = useCase.execute(command(null));

        assertThat(result.priceConfidence().value()).isEqualByComparingTo("0.95");
        assertThat(result.amountConfidence().value()).isEqualByComparingTo("0.90");
    }

    private void given(final ExtractedPriceTag extracted) {
        when(imageAnalysisPort.analyze(IMAGE_URL)).thenReturn(extracted);
    }

    private AnalyzeReportImageCommand command(final Long itemId) {
        return new AnalyzeReportImageCommand(IMAGE_URL, itemId);
    }

    /**
     * 이전 헬퍼는 itemConfidence 와 priceConfidence 에 같은 값을 넣어 두 신뢰도가 섞이는 버그를
     * 숨겼다. 두 값을 따로 받는다.
     */
    private ExtractedPriceTag extracted(
            final String itemName, final String itemConfidence, final Integer price) {
        return ExtractedPriceTag.builder()
                .itemName(itemName)
                .itemConfidence(confidence(itemConfidence))
                .price(price)
                .priceConfidence(confidence(itemConfidence))
                .build();
    }

    private AnalysisConfidence confidence(final String value) {
        if (value == null) {
            return null;
        }
        return new AnalysisConfidence(new BigDecimal(value));
    }
}

package com.example.demo.report.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.report.application.command.AnalyzeReportImageCommand;
import com.example.demo.report.application.contract.ExtractedPriceTag;
import com.example.demo.report.application.contract.ItemCandidate;
import com.example.demo.report.application.port.ImageAnalysisPort;
import com.example.demo.report.application.port.ItemCandidateQueryPort;
import com.example.demo.report.application.result.ImageAnalysisResult;
import com.example.demo.report.domain.AnalysisConfidence;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 사진에서 제보 입력값 후보를 뽑는다.
 *
 * <p>쓰기가 없고 외부 모델 호출을 트랜잭션 안에 오래 두지 않으려고 {@code @Transactional}을
 * 붙이지 않는다({@code docs/ARCHITECTURE.md} §6). 품목 조회로 DB 를 읽기는 한다. 이 유스케이스는
 * {@code user_reports}를 만들지 않는다 — 저장은 사용자가 확인한 뒤 기존 저장 API가 한다.
 *
 * <p><b>단위는 모델에게 묻지 않는다.</b> 저장 API가 {@code unit}을 {@code items.default_unit}과
 * 문자열까지 일치하도록 요구하기 때문이다({@code CreateUserReportUseCase.validateUnit}). 모델이
 * "1망"처럼 그럴듯한 값을 주더라도 저장 단계에서 400이 된다. 그래서 단위는 매칭된 품목이
 * 결정하고, 품목을 못 찾으면 비워 둔다.
 */
@Service
@RequiredArgsConstructor
public class AnalyzeReportImageUseCase {

    private final ImageAnalysisPort imageAnalysisPort;
    private final ItemCandidateQueryPort itemCandidateQueryPort;

    public ImageAnalysisResult execute(final AnalyzeReportImageCommand command) {
        final ExtractedPriceTag extracted = imageAnalysisPort.analyze(command.imageUrl());
        return toResult(extracted, resolveItem(command, extracted), command.hasSelectedItem());
    }

    private ItemCandidate resolveItem(
            final AnalyzeReportImageCommand command, final ExtractedPriceTag extracted) {
        if (command.hasSelectedItem()) {
            // 사용자가 고른 품목이 정본이다. 모델 판단으로 덮지 않는다. 없는 ID 를 준 건 요청이
            // 잘못된 것이므로 404 로 끝낸다 — 조용히 비우면 "인식 실패"와 구분할 수 없고 저장
            // 단계에서야 404 를 만난다.
            return itemCandidateQueryPort.findById(command.itemId())
                    .orElseThrow(AnalyzeReportImageUseCase::itemNotFound);
        }
        if (!extracted.hasItemName()) {
            return null;
        }
        return itemCandidateQueryPort.findByName(extracted.itemName()).orElse(null);
    }

    private ImageAnalysisResult toResult(
            final ExtractedPriceTag extracted, final ItemCandidate matched, final boolean selected) {
        return new ImageAnalysisResult(
                matched,
                itemConfidence(extracted, matched, selected),
                extracted.price(),
                priceConfidence(extracted),
                extracted.priceBasis(),
                unitOf(matched),
                extracted.amount(),
                amountConfidence(extracted));
    }

    private AnalysisConfidence itemConfidence(
            final ExtractedPriceTag extracted, final ItemCandidate matched, final boolean selected) {
        if (matched == null) {
            return null;
        }
        // 사용자가 고른 품목에는 AI 신뢰도가 없다. 모델이 다른 품목에 매긴 점수를 그 품목의
        // 신뢰도로 내보내면 "오이 96%" 같은 거짓 표시가 된다.
        if (selected) {
            return null;
        }
        return extracted.itemConfidence();
    }

    private String unitOf(final ItemCandidate matched) {
        if (matched == null) {
            return null;
        }
        return matched.defaultUnit();
    }

    /**
     * 가격표에 숫자가 여럿이면 어느 값이 판매 가격인지 확정할 근거가 약하다. 값은 남기고 신뢰도만
     * 낮춘다 — 후보를 보여 주고 사용자가 고치는 편이 빈 칸을 주는 것보다 낫다.
     */
    private AnalysisConfidence priceConfidence(final ExtractedPriceTag extracted) {
        if (extracted.price() == null) {
            return null;
        }
        return AnalysisConfidence.downgradeIfAmbiguous(
                extracted.priceConfidence(), extracted.otherNumberCount());
    }

    private static ApiException itemNotFound() {
        return new ApiException(
                ErrorType.NO_RESOURCE_ERROR.description(),
                ErrorType.NO_RESOURCE_ERROR,
                HttpStatus.NOT_FOUND);
    }

    private AnalysisConfidence amountConfidence(final ExtractedPriceTag extracted) {
        if (extracted.amount() == null) {
            return null;
        }
        return AnalysisConfidence.downgradeIfAmbiguous(
                extracted.amountConfidence(), extracted.otherNumberCount());
    }
}

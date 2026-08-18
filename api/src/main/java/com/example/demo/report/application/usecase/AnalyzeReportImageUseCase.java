package com.example.demo.report.application.usecase;

import com.example.demo.report.application.command.AnalyzeReportImageCommand;
import com.example.demo.report.application.contract.ExtractedPriceTag;
import com.example.demo.report.application.contract.ItemCandidate;
import com.example.demo.report.application.port.ImageAnalysisPort;
import com.example.demo.report.application.port.ItemCandidateQueryPort;
import com.example.demo.report.application.result.ImageAnalysisResult;
import com.example.demo.report.domain.AnalysisConfidence;
import com.example.demo.report.domain.policy.PriceExtractionPolicy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 사진에서 제보 입력값 후보를 뽑는다.
 *
 * <p>DB를 쓰지 않으므로 {@code @Transactional}을 붙이지 않는다. 외부 모델 호출을 트랜잭션 안에
 * 오래 두지 않는다는 원칙({@code docs/ARCHITECTURE.md} §6)과도 맞다. 이 유스케이스는
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
        final List<ItemCandidate> candidates = resolveCandidates(command, extracted);
        return toResult(extracted, candidates);
    }

    private List<ItemCandidate> resolveCandidates(
            final AnalyzeReportImageCommand command, final ExtractedPriceTag extracted) {
        if (command.hasSelectedItem()) {
            // 사용자가 고른 품목이 정본이다. 모델 판단으로 덮지 않는다.
            return itemCandidateQueryPort.findById(command.itemId()).map(List::of).orElseGet(List::of);
        }
        if (!extracted.hasItemName()) {
            return List.of();
        }
        return itemCandidateQueryPort.findCandidatesByName(extracted.itemName());
    }

    private ImageAnalysisResult toResult(
            final ExtractedPriceTag extracted, final List<ItemCandidate> candidates) {
        final ItemCandidate matched = singleMatch(candidates);
        return new ImageAnalysisResult(
                matched,
                itemConfidence(extracted, candidates),
                candidates,
                extracted.price(),
                priceConfidence(extracted),
                unitOf(matched),
                extracted.amount(),
                amountConfidence(extracted));
    }

    /**
     * 후보가 하나일 때만 확정한다. 여럿이면 {@code item}을 비우고 후보 목록만 준다 — 화면이
     * 사용자에게 고르게 해야 한다.
     */
    private ItemCandidate singleMatch(final List<ItemCandidate> candidates) {
        if (candidates.size() == 1) {
            return candidates.getFirst();
        }
        return null;
    }

    private AnalysisConfidence itemConfidence(
            final ExtractedPriceTag extracted, final List<ItemCandidate> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() > 1) {
            // 우리 목록에서 갈렸다는 사실 자체가 근거 부족이다. 모델 점수를 그대로 쓰지 않는다.
            return AnalysisConfidence.low();
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
        return PriceExtractionPolicy.downgradeIfAmbiguous(
                extracted.itemConfidence(), extracted.numberCount());
    }

    private AnalysisConfidence amountConfidence(final ExtractedPriceTag extracted) {
        if (extracted.amount() == null) {
            return null;
        }
        return PriceExtractionPolicy.downgradeIfAmbiguous(
                extracted.amountConfidence(), extracted.numberCount());
    }
}

package com.example.demo.report.infrastructure;

import com.example.demo.item.domain.Item;
import com.example.demo.report.application.contract.ItemCandidate;
import com.example.demo.report.application.port.ItemCandidateQueryPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 품목 후보 조회 어댑터. {@code item} 도메인의 Entity를 report의 Contract로 바꾼다
 * ({@code docs/ARCHITECTURE.md} §7).
 *
 * <p>이름 매칭은 정확 일치만 한다. 품목이 47개 고정 목록이라 유사도 계산이 필요 없고, 느슨하게
 * 매칭하면 {@code 고추}가 {@code 풋고추·꽈리고추·청양고추·오이맛고추·건고추·붉은고추}를 모두
 * 끌어와 사용자에게 도움이 되지 않는다. 모델이 이름을 다르게 말하면 후보가 비고, 화면은 사용자가
 * 직접 고르는 경로로 넘어간다 — 그게 잘못 짚는 것보다 낫다.
 */
@Component
@RequiredArgsConstructor
public class ItemCandidateQueryAdapter implements ItemCandidateQueryPort {

    private final ItemNameJpaRepository itemNameJpaRepository;

    @Override
    public List<ItemCandidate> findCandidatesByName(final String itemName) {
        final String normalized = normalize(itemName);
        if (normalized.isEmpty()) {
            return List.of();
        }
        return itemNameJpaRepository.findByName(normalized).stream().map(this::toCandidate).toList();
    }

    @Override
    public Optional<ItemCandidate> findById(final Long itemId) {
        return itemNameJpaRepository.findById(itemId).map(this::toCandidate);
    }

    private ItemCandidate toCandidate(final Item item) {
        return new ItemCandidate(item.id(), item.name(), item.defaultUnit());
    }

    private String normalize(final String itemName) {
        if (itemName == null) {
            return "";
        }
        return itemName.replaceAll("\\s+", "");
    }
}

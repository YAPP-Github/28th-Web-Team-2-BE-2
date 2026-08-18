package com.example.demo.report.infrastructure;

import com.example.demo.item.application.port.ItemExistencePort;
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
 * <p>이름 매칭은 정확 일치 → 정규화 일치 순으로 좁힌다. 품목이 47개 고정 목록이라 복잡한 유사도
 * 계산이 필요 없고, 오히려 느슨하게 매칭하면 {@code 고추}가 {@code 풋고추·꽈리고추·청양고추·
 * 오이맛고추·건고추·붉은고추}를 모두 끌어와 사용자에게 도움이 되지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ItemCandidateQueryAdapter implements ItemCandidateQueryPort {

    private final ItemNameJpaRepository itemNameJpaRepository;
    private final ItemExistencePort itemExistencePort;

    @Override
    public List<ItemCandidate> findCandidatesByName(final String itemName) {
        final String normalized = normalize(itemName);
        if (normalized.isEmpty()) {
            return List.of();
        }
        final List<Item> exact = itemNameJpaRepository.findByName(normalized);
        if (!exact.isEmpty()) {
            return toCandidates(exact);
        }
        // 모델이 "청상추 (100g)"처럼 군더더기를 붙여 오는 경우를 위해 부분 일치를 한 번 더 본다.
        return toCandidates(itemNameJpaRepository.findByNameContaining(normalized));
    }

    @Override
    public Optional<ItemCandidate> findById(final Long itemId) {
        return itemExistencePort.findById(itemId).map(this::toCandidate);
    }

    private List<ItemCandidate> toCandidates(final List<Item> items) {
        return items.stream().map(this::toCandidate).toList();
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

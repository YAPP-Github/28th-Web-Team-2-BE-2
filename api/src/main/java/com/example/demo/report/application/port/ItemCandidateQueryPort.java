package com.example.demo.report.application.port;

import com.example.demo.report.application.contract.ItemCandidate;
import java.util.Optional;

/**
 * 품목명으로 후보를 찾는 출력 포트.
 *
 * <p>기존 {@code ItemQueryPort}·{@code ItemExistencePort}에는 이름으로 찾는 경로가 없어 새로 둔다.
 */
public interface ItemCandidateQueryPort {

    /**
     * 인식된 이름에 대응하는 품목을 찾는다.
     *
     * <p>{@code items.item_name}이 유일하고 정확 일치만 하므로 결과는 없거나 하나다. 못 찾으면
     * 비어 있고, 화면은 사용자가 직접 고르는 경로로 넘어간다.
     *
     * <p>별칭·기준명 매핑을 붙여 후보를 여러 개 주고 싶어지면 그때 반환형을 넓힌다. 지금 목록으로
     * 열어 두면 만들 수 없는 값을 위한 분기만 남는다.
     */
    Optional<ItemCandidate> findByName(String itemName);

    /** 사용자가 이미 품목을 골라 둔 경우의 검증용 조회다. */
    Optional<ItemCandidate> findById(Long itemId);
}

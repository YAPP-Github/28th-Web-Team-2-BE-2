package com.example.demo.report.application.port;

import com.example.demo.report.application.contract.ItemCandidate;
import java.util.List;
import java.util.Optional;

/**
 * 품목명으로 후보를 찾는 출력 포트.
 *
 * <p>기존 {@code ItemQueryPort}·{@code ItemExistencePort}에는 이름으로 찾는 경로가 없어 새로 둔다.
 */
public interface ItemCandidateQueryPort {

    /**
     * 인식된 이름에 대응할 수 있는 품목을 찾는다.
     *
     * <p>여러 개를 돌려줄 수 있다. 품목 목록에 {@code 쥬키니/애호박}, {@code 적상추/청상추},
     * {@code 방울토마토/대추방울토마토}, {@code 고춧가루-국산/중국산}처럼 사진만으로 가릴 수 없는
     * 쌍이 있어서다. 하나로 좁히지 않고 사용자가 고르게 한다.
     */
    List<ItemCandidate> findCandidatesByName(String itemName);

    /** 사용자가 이미 품목을 골라 둔 경우의 검증용 조회다. */
    Optional<ItemCandidate> findById(Long itemId);
}

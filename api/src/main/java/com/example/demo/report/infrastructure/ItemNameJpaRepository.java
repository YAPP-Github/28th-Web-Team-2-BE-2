package com.example.demo.report.infrastructure;

import com.example.demo.item.domain.Item;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 품목명 조회 전용 repository.
 *
 * <p>{@code item} 도메인의 Entity를 report의 Infrastructure에서 직접 다룬다. {@code ARCHITECTURE.md}
 * §7은 상대 도메인의 Repository·Entity를 직접 가져오지 말라고 하므로 이건 §9가 허용하는 전환
 * 경계다 — 이름으로 품목을 찾는 공개 유스케이스가 {@code item} 쪽에 아직 없다.
 *
 * <p><b>제거 조건</b>: {@code item} 에 이름 조회 유스케이스가 생기면 이 repository 를 지우고
 * {@code ItemCandidateQueryAdapter} 가 그 유스케이스를 호출하게 바꾼다.
 *
 * <p>ArchUnit 규칙으로 이 경계를 강제하려 했다가 되돌렸다. 같은 이탈이 레포에 이미 여럿 있고
 * (예: {@code StoreReportQueryAdapter} 가 QueryDSL 로 {@code item.domain.QItem} 을 조인한다)
 * 위반 목록을 유지하는 비용이 규칙의 값보다 컸다. 레포 전체를 정리할 때 함께 다룰 일이다.
 */
public interface ItemNameJpaRepository extends JpaRepository<Item, Long> {

    Optional<Item> findByName(String name);
}

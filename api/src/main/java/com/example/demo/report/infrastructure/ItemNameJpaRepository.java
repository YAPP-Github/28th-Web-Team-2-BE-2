package com.example.demo.report.infrastructure;

import com.example.demo.item.domain.Item;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 품목명 조회 전용 repository.
 *
 * <p>{@code item} 도메인의 Entity를 report의 Infrastructure에서 직접 다룬다. {@code ARCHITECTURE.md}
 * §7은 상대 도메인의 Repository·Entity를 직접 가져오지 말라고 하므로 이건 §9가 허용하는 전환
 * 경계다 — 이름으로 품목을 찾는 공개 유스케이스가 {@code item} 쪽에 아직 없다. 그쪽에 생기면
 * 이 repository 를 지우고 포트 구현을 그 유스케이스 호출로 바꾼다.
 */
public interface ItemNameJpaRepository extends JpaRepository<Item, Long> {

    Optional<Item> findByName(String name);
}

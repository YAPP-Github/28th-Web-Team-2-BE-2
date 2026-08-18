package com.example.demo.report.infrastructure;

import com.example.demo.item.domain.Item;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 품목명 조회 전용 repository.
 *
 * <p>{@code item} 도메인의 Entity를 읽지만 report의 Infrastructure에 둔다. 도메인 간 협력은
 * 사용하는 쪽이 포트를 소유하고 Adapter가 변환하는 형태를 따른다({@code docs/ARCHITECTURE.md} §7).
 */
public interface ItemNameJpaRepository extends JpaRepository<Item, Long> {

    List<Item> findByName(String name);

    List<Item> findByNameContaining(String name);
}

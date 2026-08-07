package com.example.demo.price.infrastructure;

import com.example.demo.price.domain.ItemEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemJpaRepository extends JpaRepository<ItemEntity, Long> {

    List<ItemEntity> findAllByActiveTrueOrderByIdAsc();

    Optional<ItemEntity> findByItemCodeAndName(Integer itemCode, String name);
}

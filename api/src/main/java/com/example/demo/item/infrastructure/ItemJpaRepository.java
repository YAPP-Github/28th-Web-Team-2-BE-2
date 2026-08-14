package com.example.demo.item.infrastructure;

import com.example.demo.item.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemJpaRepository extends JpaRepository<Item, Long> {}

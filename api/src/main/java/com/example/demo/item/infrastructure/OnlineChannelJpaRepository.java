package com.example.demo.item.infrastructure;

import com.example.demo.item.domain.OnlineChannel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnlineChannelJpaRepository extends JpaRepository<OnlineChannel, Integer> {}

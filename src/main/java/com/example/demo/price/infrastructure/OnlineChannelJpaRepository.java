package com.example.demo.price.infrastructure;

import com.example.demo.price.domain.ChannelCode;
import com.example.demo.price.domain.OnlineChannelEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnlineChannelJpaRepository extends JpaRepository<OnlineChannelEntity, Integer> {

    List<OnlineChannelEntity> findAllByActiveTrueOrderByIdAsc();

    OnlineChannelEntity findByCode(ChannelCode code);
}

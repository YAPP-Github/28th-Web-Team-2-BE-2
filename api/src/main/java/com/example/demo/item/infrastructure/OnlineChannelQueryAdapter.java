package com.example.demo.item.infrastructure;

import com.example.demo.item.application.port.OnlineChannelQueryPort;
import com.example.demo.item.domain.OnlineChannel;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OnlineChannelQueryAdapter implements OnlineChannelQueryPort {

    private final OnlineChannelJpaRepository onlineChannelJpaRepository;

    @Override
    public List<OnlineChannel> findAll() {
        return onlineChannelJpaRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }
}

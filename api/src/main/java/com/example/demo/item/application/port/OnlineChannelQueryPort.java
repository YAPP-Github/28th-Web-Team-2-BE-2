package com.example.demo.item.application.port;

import com.example.demo.item.domain.OnlineChannel;
import java.util.List;

public interface OnlineChannelQueryPort {

    List<OnlineChannel> findAll();
}

package com.example.demo.item.application.port;

import com.example.demo.item.domain.Item;
import java.util.List;

public interface OnlineItemQueryPort {

    List<Item> findAll();
}

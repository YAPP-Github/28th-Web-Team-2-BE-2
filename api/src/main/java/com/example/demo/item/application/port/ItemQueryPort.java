package com.example.demo.item.application.port;

import com.example.demo.item.domain.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemQueryPort {

    Page<Item> findAll(Pageable pageable);
}

package com.example.demo.report.application.port;

import com.example.demo.report.application.command.StoreSnapshot;

public interface StoreCommandPort {
    Long save(StoreSnapshot store);
}

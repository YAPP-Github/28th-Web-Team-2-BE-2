package com.example.demo.report.application.port;

import java.util.Optional;

public interface RegionNameQueryPort {

    Optional<String> findName(String regionId);
}

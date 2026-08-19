package com.example.demo.user.application.port;

import java.util.Collection;
import java.util.Map;

public interface RegionReferenceRepository {

    boolean existsById(String regionId);

    Map<String, String> findNamesByIds(Collection<String> regionIds);
}

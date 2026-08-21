package com.example.demo.user.application.port;

import java.util.Collection;
import java.util.Map;

public interface UserReportCountQueryPort {

    Map<Long, Long> findReportCounts(Collection<Long> userIds);
}

package com.example.demo.report.infrastructure;

import com.example.demo.report.domain.UserReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserReportJpaRepository extends JpaRepository<UserReport, Long> {}

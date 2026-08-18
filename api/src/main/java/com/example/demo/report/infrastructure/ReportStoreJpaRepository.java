package com.example.demo.report.infrastructure;

import com.example.demo.report.domain.Store;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportStoreJpaRepository extends JpaRepository<Store, Long> {

    List<Store> findAllByIdIn(Collection<Long> storeIds);
}

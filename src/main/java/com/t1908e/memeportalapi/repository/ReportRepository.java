package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.Report;
import com.t1908e.memeportalapi.enums.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Integer> {
    Page<Report> findAllByStatusAndType(int status, ReportType reportType, Pageable pageable);
}

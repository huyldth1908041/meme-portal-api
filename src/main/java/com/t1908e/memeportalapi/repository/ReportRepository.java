package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.Post;
import com.t1908e.memeportalapi.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report,Integer> {
    @Query("SELECT report FROM Report report WHERE report.targetId = :targetId")
    List<Report> findAllByTargetId(@Param(value = "targetId") long targetId);
}

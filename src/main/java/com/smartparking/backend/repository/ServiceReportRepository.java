package com.smartparking.backend.repository;

import com.smartparking.backend.model.ServiceReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceReportRepository extends JpaRepository<ServiceReport, Long> {

    List<ServiceReport> findByReporterIdOrderByCreatedAtDesc(Long reporterId);
}

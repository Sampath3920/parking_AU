package com.smartparking.backend.repository;

import com.smartparking.backend.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findTop100ByOrderByCreatedAtDesc();
}

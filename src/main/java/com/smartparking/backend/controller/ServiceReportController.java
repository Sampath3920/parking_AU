package com.smartparking.backend.controller;

import com.smartparking.backend.dto.PageResult;
import com.smartparking.backend.dto.ServiceReportDecisionRequest;
import com.smartparking.backend.dto.ServiceReportRequest;
import com.smartparking.backend.exception.NotFoundException;
import com.smartparking.backend.model.ServiceReport;
import com.smartparking.backend.model.User;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.service.ServiceReportService;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Complaints and damage reports.
 * Users: send a report, list their own, open one of their own.
 * Admin (URL starts with /api/admin, see SecurityConfig): list all, summary, handle a report.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class ServiceReportController {

    private final ServiceReportService reportService;
    private final UserRepository userRepository;

    public ServiceReportController(ServiceReportService reportService, UserRepository userRepository) {
        this.reportService = reportService;
        this.userRepository = userRepository;
    }

    @PostMapping("/service-reports")
    public ServiceReport submit(@Valid @RequestBody ServiceReportRequest request,
                                Authentication authentication) {
        return reportService.submit(currentUser(authentication), request);
    }

    @GetMapping("/service-reports/mine")
    public List<ServiceReport> mine(@RequestParam(required = false) String type,
                                    Authentication authentication) {
        return reportService.mine(currentUser(authentication).getId(), type);
    }

    @GetMapping("/service-reports/{id}")
    public ServiceReport one(@PathVariable Long id, Authentication authentication) {
        return reportService.getFor(id, currentUser(authentication), isAdmin(authentication));
    }

    @GetMapping("/admin/service-reports")
    public PageResult<ServiceReport> search(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return reportService.search(type, status, search, page, size);
    }

    @GetMapping("/admin/service-reports/summary")
    public Map<String, Object> summary() {
        return reportService.summary();
    }

    @PutMapping("/admin/service-reports/{id}")
    public ServiceReport update(@PathVariable Long id,
                                @Valid @RequestBody ServiceReportDecisionRequest request,
                                Authentication authentication) {
        return reportService.update(id, request, currentUser(authentication));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private boolean isAdmin(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}

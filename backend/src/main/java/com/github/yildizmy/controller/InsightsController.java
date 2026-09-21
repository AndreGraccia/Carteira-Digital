package com.github.yildizmy.controller;

import com.github.yildizmy.dto.request.InsightReportRequest;
import com.github.yildizmy.dto.response.InsightReportResponse;
import com.github.yildizmy.security.UserDetailsImpl;
import com.github.yildizmy.service.InsightsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/insights")
@RequiredArgsConstructor
public class InsightsController {
    private final InsightsService insightsService;

    @PostMapping("/reports")
    public ResponseEntity<InsightReportResponse> generateReport(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody InsightReportRequest request
    ) {
        InsightReportResponse response = insightsService.generate(user.getId(), request.months(), request.language());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}

package com.github.yildizmy.controller;

import com.github.yildizmy.dto.response.OpenFinanceStatusResponse;
import com.github.yildizmy.dto.response.OpenFinanceSyncResponse;
import com.github.yildizmy.security.UserDetailsImpl;
import com.github.yildizmy.service.OpenFinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/open-finance")
@RequiredArgsConstructor
public class OpenFinanceController {

    private final OpenFinanceService openFinanceService;

    @GetMapping("/status")
    public ResponseEntity<OpenFinanceStatusResponse> status(@AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(openFinanceService.status(user.getId()));
    }

    @PostMapping("/sync")
    public ResponseEntity<OpenFinanceSyncResponse> sync(@AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(openFinanceService.sync(user.getId()));
    }

    @DeleteMapping("/connection")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal UserDetailsImpl user) {
        openFinanceService.revoke(user.getId());
        return ResponseEntity.noContent().build();
    }
}

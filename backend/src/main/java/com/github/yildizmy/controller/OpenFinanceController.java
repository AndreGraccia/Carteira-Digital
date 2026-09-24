package com.github.yildizmy.controller;

import com.github.yildizmy.dto.request.OpenFinanceCodeRequest;
import com.github.yildizmy.dto.response.OpenFinanceChallengeResponse;
import com.github.yildizmy.dto.response.OpenFinanceStatusResponse;
import com.github.yildizmy.dto.response.OpenFinanceSyncResponse;
import com.github.yildizmy.security.UserDetailsImpl;
import com.github.yildizmy.service.ForecastService;
import com.github.yildizmy.service.OpenFinanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/open-finance")
@RequiredArgsConstructor
public class OpenFinanceController {

    private final OpenFinanceService openFinanceService;
    private final ForecastService forecastService;

    @GetMapping("/status")
    public ResponseEntity<OpenFinanceStatusResponse> status(@AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(openFinanceService.status(user.getId()));
    }

    @PostMapping("/connection/challenge")
    public ResponseEntity<OpenFinanceChallengeResponse> requestConnectionCode(
            @AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(openFinanceService.requestConnectionCode(user.getId()));
    }

    @PostMapping("/connection/confirm")
    public ResponseEntity<OpenFinanceSyncResponse> confirmConnection(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody OpenFinanceCodeRequest request) {
        OpenFinanceSyncResponse response = openFinanceService.confirmConnection(user.getId(), request.code());
        recordForecast(user.getId());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    @PostMapping("/sync")
    public ResponseEntity<OpenFinanceSyncResponse> sync(@AuthenticationPrincipal UserDetailsImpl user) {
        OpenFinanceSyncResponse response = openFinanceService.sync(user.getId());
        recordForecast(user.getId());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    /**
     * After the import has committed, so the forecast reads the bank's fresh balance. A failure here only
     * costs one day of the accuracy score; it must never turn a successful sync into an error.
     */
    // ponytail: recorded on sync only. When sync becomes a daily job, this moves there unchanged.
    private void recordForecast(long userId) {
        try {
            forecastService.record(userId);
        } catch (RuntimeException exception) {
            log.warn("Forecast snapshot skipped: {}", exception.getClass().getSimpleName());
        }
    }

    @DeleteMapping("/connection")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal UserDetailsImpl user) {
        openFinanceService.revoke(user.getId());
        return ResponseEntity.noContent().build();
    }
}

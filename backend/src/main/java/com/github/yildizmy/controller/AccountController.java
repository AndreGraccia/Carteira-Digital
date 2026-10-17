package com.github.yildizmy.controller;

import com.github.yildizmy.security.AuthenticationRateLimiter;
import com.github.yildizmy.security.UserDetailsImpl;
import com.github.yildizmy.service.AccountDeletionService;
import com.github.yildizmy.service.AccountExportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The logged-in person's own account. Always the caller's: there is no id in the path. */
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountDeletionService accountDeletion;
    private final AccountExportService accountExport;
    private final AuthenticationRateLimiter rateLimiter;

    public record DeletionRequest(@NotBlank @Size(max = 100) String password) {
    }

    /** Deletes and anonymises the account (ADR 0012). The password is asked again; tries share the login limit. */
    @PostMapping("/delete")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetailsImpl user,
                                       @Valid @RequestBody DeletionRequest request,
                                       HttpServletRequest servletRequest) {
        rateLimiter.acquire(user.getUsername(), servletRequest.getRemoteAddr());
        accountDeletion.delete(user.getId(), request.password());
        return ResponseEntity.noContent().build();
    }

    /**
     * Everything kept about the caller, as a JSON download (LGPD art. 18, II and V). No login limiter here: it
     * counts login attempts, and five downloads would lock the person out.
     */
    @GetMapping("/export")
    public ResponseEntity<java.util.Map<String, Object>> export(@AuthenticationPrincipal UserDetailsImpl user) {
        String file = "wallia-dados-" + java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")) + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file + "\"")
                .cacheControl(CacheControl.noStore())
                .body(accountExport.export(user.getId()));
    }
}

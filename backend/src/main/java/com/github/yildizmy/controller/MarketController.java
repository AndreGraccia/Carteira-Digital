package com.github.yildizmy.controller;

import com.github.yildizmy.security.UserDetailsImpl;
import com.github.yildizmy.service.market.MarketRatesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Banco Central reference data: market rates (with the user's bank when found) and CDI, IPCA and Selic. */
@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketRatesService marketRates;

    public record Indicator(String code, LocalDate referenceDate, BigDecimal value, Instant fetchedAt) {
    }

    public record Overview(List<MarketRatesService.Quote> rates, List<Indicator> indicators) {
    }

    @GetMapping("/overview")
    public ResponseEntity<Overview> overview(@AuthenticationPrincipal UserDetailsImpl user) {
        List<Indicator> indicators = marketRates.indicators().stream()
                .map(row -> new Indicator(row.getCode(), row.getReferenceDate(), row.getValue(), row.getFetchedAt()))
                .toList();
        // Per user (their bank's rate), so it is not cached by shared caches.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new Overview(marketRates.quotes(user.getId()), indicators));
    }
}

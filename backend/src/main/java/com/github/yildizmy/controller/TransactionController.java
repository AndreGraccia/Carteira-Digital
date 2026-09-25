package com.github.yildizmy.controller;

import com.github.yildizmy.dto.response.TransactionResponse;
import com.github.yildizmy.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Fetches a single transaction by the given id.
     *
     * @param id
     * @return TransactionResponse wrapped by ResponseEntity<T>
     */
    @PreAuthorize("@walletAccess.transaction(#id)")
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> findById(@PathVariable long id) {
        final TransactionResponse response = transactionService.findById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Fetches a single transaction by the given referenceNumber.
     *
     * @param referenceNumber
     * @return TransactionResponse wrapped by ResponseEntity<T>
     */
    @PreAuthorize("@walletAccess.reference(#referenceNumber)")
    @GetMapping("/references/{referenceNumber}")
    public ResponseEntity<TransactionResponse> findByReferenceNumber(@PathVariable UUID referenceNumber) {
        final TransactionResponse response = transactionService.findByReferenceNumber(referenceNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * The user's statement, newest first, paged in the database ({@code ?page=0&size=100}, at most
     * {@value #MAX_PAGE_SIZE} per page). {@code from} (yyyy-MM-dd, Brasília) limits how far back it goes.
     */
    @PreAuthorize("@walletAccess.user(#userId)")
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<TransactionResponse>> findAllByUserId(
            @PathVariable long userId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso =
                    org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(Math.max(0, page),
                Math.min(Math.max(1, size), MAX_PAGE_SIZE),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        java.time.Instant since = from == null ? java.time.Instant.EPOCH
                : from.atStartOfDay(java.time.ZoneId.of("America/Sao_Paulo")).toInstant();
        return ResponseEntity.ok(transactionService.findAllByUserId(userId, since, pageable));
    }

    static final int MAX_PAGE_SIZE = 500;

    /**
     * Fetches all transactions based on the given paging and sorting parameters.
     *
     * @param pageable
     * @return List of TransactionResponse wrapped by ResponseEntity<T>
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> findAll(Pageable pageable) {
        final Page<TransactionResponse> response = transactionService.findAll(pageable);
        return ResponseEntity.ok(response);
    }
}

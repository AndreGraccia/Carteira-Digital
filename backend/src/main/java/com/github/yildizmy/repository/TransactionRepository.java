package com.github.yildizmy.repository;

import com.github.yildizmy.domain.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReferenceNumber(UUID referenceNumber);
    Optional<Transaction> findByRequestId(UUID requestId);
    boolean existsByExternalId(String externalId);

    List<Transaction> findByExternalIdIn(java.util.Collection<String> externalIds);

    List<Transaction> findByToWalletIdAndStatus(Long walletId, com.github.yildizmy.domain.enums.Status status);
    boolean existsByFromWalletIdOrToWalletId(Long fromWalletId, Long toWalletId);


    @Query(value = "SELECT DISTINCT t " +
            "FROM Transaction t " +
            "LEFT JOIN Wallet w ON w.id IN (t.fromWallet.id, t.toWallet.id) " +
            "WHERE w.user.id = :userId " +
            "ORDER BY t.createdAt DESC")
    List<Transaction> findAllByUserId(@Param("userId") Long userId);

    /** Everything imported into the user's Open Finance wallets: used when the consent is revoked. */
    @org.springframework.data.jpa.repository.Modifying
    @Query("delete from Transaction t where t.fromWallet.id in (select w.id from Wallet w where w.user.id = :userId "
            + "and w.source = com.github.yildizmy.domain.enums.WalletSource.OPEN_FINANCE) "
            + "or t.toWallet.id in (select w.id from Wallet w where w.user.id = :userId "
            + "and w.source = com.github.yildizmy.domain.enums.WalletSource.OPEN_FINANCE)")
    int deleteOpenFinanceByUserId(@Param("userId") Long userId);

    /**
     * The user's movements since {@code from}, newest first, paged in the database. Both sides are checked so a
     * transfer received from someone else is listed too. Scheduled entries (PENDING) belong to the projection,
     * not to the statement.
     */
    @Query(value = "SELECT t FROM Transaction t "
            + "WHERE (t.fromWallet.user.id = :userId OR t.toWallet.user.id = :userId) "
            + "AND t.createdAt >= :from AND t.status <> com.github.yildizmy.domain.enums.Status.PENDING",
            countQuery = "SELECT count(t) FROM Transaction t "
                    + "WHERE (t.fromWallet.user.id = :userId OR t.toWallet.user.id = :userId) "
                    + "AND t.createdAt >= :from AND t.status <> com.github.yildizmy.domain.enums.Status.PENDING")
    org.springframework.data.domain.Page<Transaction> findPageByUserId(@Param("userId") Long userId,
                                                                      @Param("from") java.time.Instant from,
                                                                      org.springframework.data.domain.Pageable pageable);

    /** Everything since {@code from}, scheduled entries included: what the projection and the report read. */
    @Query("SELECT t FROM Transaction t WHERE (t.fromWallet.user.id = :userId OR t.toWallet.user.id = :userId) "
            + "AND t.createdAt >= :from ORDER BY t.createdAt DESC")
    List<Transaction> findAllByUserIdSince(@Param("userId") Long userId, @Param("from") java.time.Instant from);
}

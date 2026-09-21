package com.github.yildizmy.repository;

import com.github.yildizmy.domain.entity.Wallet;
import com.github.yildizmy.domain.enums.WalletSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByIban(String iban);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select w from Wallet w where w.iban in :ibans order by w.id")
    List<Wallet> lockByIbans(@org.springframework.data.repository.query.Param("ibans") List<String> ibans);

    boolean existsByIdAndUserId(Long id, Long userId);
    boolean existsByIbanAndUserId(String iban, Long userId);

    List<Wallet> findByUserId(Long userId);

    Optional<Wallet> findByUserIdAndSourceAndExternalAccountId(Long userId, WalletSource source, String externalAccountId);

    long countByUserIdAndSource(Long userId, WalletSource source);

    boolean existsByIbanIgnoreCase(String iban);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    Wallet getReferenceByIban(String iban);
}

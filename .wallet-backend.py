from pathlib import Path
base=Path('backend/src/main/java/com/github/yildizmy')
def change(name,pairs):
    p=base/name;s=p.read_text(encoding='utf-8')
    for old,new in pairs:
        if old not in s: raise ValueError(f'{name}: missing {old[:70]}')
        s=s.replace(old,new)
    p.write_text(s,encoding='utf-8')
(base/'exception/WalletRuleException.java').write_text('''package com.github.yildizmy.exception;

public class WalletRuleException extends RuntimeException {
    public WalletRuleException(String code) { super(code); }
}
''',encoding='utf-8')
change('exception/GlobalExceptionHandler.java', [('public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {','''public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(WalletRuleException.class)
    public ResponseEntity<Object> handleWalletRule(WalletRuleException ex) {
        return ResponseEntity.badRequest().body(java.util.Map.of("code", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of("code", "FORBIDDEN"));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleConflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("code", "CONFLICT"));
    }
''')])
change('dto/request/TransactionRequest.java', [('    private BigDecimal amount;', '''    @jakarta.validation.constraints.Positive
    @jakarta.validation.constraints.Digits(integer = 10, fraction = 2)
    private BigDecimal amount;

    private UUID requestId;''')])
change('dto/request/WalletRequest.java', [('    @NotBlank(message = "{validation.iban.required}")\n',''), ('    private BigDecimal balance;', '''    @jakarta.validation.constraints.PositiveOrZero
    @jakarta.validation.constraints.Digits(integer = 10, fraction = 2)
    private BigDecimal balance;''')])
change('validator/IbanValidator.java', [('        String trimmed = iban.trim();','''        if (iban == null || iban.isBlank()) return true;
        String trimmed = iban.trim();
        if (!trimmed.matches("[A-Z]{2}[0-9]{2}[A-Z0-9]+")) return false;''')])
change('domain/entity/Transaction.java', [('    private UUID referenceNumber;', '''    private UUID referenceNumber;

    @Column(unique = true)
    private UUID requestId;''')])
change('dto/mapper/TransactionRequestMapper.java', [('@Mapping(source = "createdAt", target = "createdAt", defaultExpression = "java(java.time.Instant.now())")','@Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")\n    @Mapping(target = "id", ignore = true)')])
change('dto/mapper/WalletRequestMapper.java', [('    @Mapping(target = "user", ignore = true)','    @Mapping(target = "user", ignore = true)\n    @Mapping(target = "id", ignore = true)')])
change('dto/mapper/WalletTransactionRequestMapper.java',[('constant = "1L"','constant = "4L"')])
change('repository/WalletRepository.java', [('    Optional<Wallet> findByIban(String iban);','''    Optional<Wallet> findByIban(String iban);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select w from Wallet w where w.iban in :ibans order by w.id")
    List<Wallet> lockByIbans(@org.springframework.data.repository.query.Param("ibans") List<String> ibans);

    boolean existsByIdAndUserId(Long id, Long userId);
    boolean existsByIbanAndUserId(String iban, Long userId);''')])
change('repository/TransactionRepository.java', [('    Optional<Transaction> findByReferenceNumber(UUID referenceNumber);','''    Optional<Transaction> findByReferenceNumber(UUID referenceNumber);
    Optional<Transaction> findByRequestId(UUID requestId);
    boolean existsByFromWalletIdOrToWalletId(Long fromWalletId, Long toWalletId);
'''),('SELECT t ','SELECT DISTINCT t ')])
change('service/TransactionService.java', [('        if (transactions.isEmpty())\n            throw new NoSuchElementFoundException(messageConfig.getMessage(ERROR_NO_RECORDS));\n\n        return transactions.stream()', '        return transactions.stream()')])
change('service/TransactionService.java', [('    public CommandResponse create(TransactionRequest request) {','''    public CommandResponse replay(TransactionRequest request) {
        if (request.getRequestId() == null) return null;
        return transactionRepository.findByRequestId(request.getRequestId()).map(existing -> {
            if (existing.getAmount().compareTo(request.getAmount()) != 0 ||
                !existing.getFromWallet().getIban().equals(request.getFromWalletIban()) ||
                !existing.getToWallet().getIban().equals(request.getToWalletIban()) ||
                !existing.getType().getId().equals(request.getTypeId()) ||
                !java.util.Objects.equals(existing.getDescription(), request.getDescription())) {
                throw new com.github.yildizmy.exception.WalletRuleException("IDEMPOTENCY_CONFLICT");
            }
            return CommandResponse.builder().id(existing.getId()).build();
        }).orElse(null);
    }

    public boolean hasHistory(long walletId) {
        return transactionRepository.existsByFromWalletIdOrToWalletId(walletId, walletId);
    }

    public CommandResponse create(TransactionRequest request) {''')])
# Secure every public read and write with the authenticated owner.
(base/'security/WalletAccess.java').write_text('''package com.github.yildizmy.security;

import com.github.yildizmy.repository.WalletRepository;
import com.github.yildizmy.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component("walletAccess")
@RequiredArgsConstructor
public class WalletAccess {
    private final WalletRepository wallets;
    private final TransactionRepository transactions;
    private Long currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl user ? user.getId() : null;
    }
    public boolean user(Long id) { return id != null && id.equals(currentUserId()); }
    public boolean wallet(Long id) { var user = currentUserId(); return user != null && wallets.existsByIdAndUserId(id, user); }
    public boolean iban(String iban) { var user = currentUserId(); return user != null && wallets.existsByIbanAndUserId(iban, user); }
    public boolean transaction(Long id) { return transactions.findById(id).map(t -> user(t.getFromWallet().getUser().getId()) || user(t.getToWallet().getUser().getId())).orElse(false); }
    public boolean reference(UUID reference) { return transactions.findByReferenceNumber(reference).map(t -> user(t.getFromWallet().getUser().getId()) || user(t.getToWallet().getUser().getId())).orElse(false); }
}
''',encoding='utf-8')
for name,guards in [('WalletController.java',{'findById':'@walletAccess.wallet(#id)','findByIban':'@walletAccess.iban(#iban)','findByUserId':'@walletAccess.user(#userId)','findAll':"hasRole('ADMIN')",'create':'@walletAccess.user(#request.userId)','transferFunds':'@walletAccess.iban(#request.fromWalletIban)','addFunds':'@walletAccess.iban(#request.toWalletIban)','withdrawFunds':'@walletAccess.iban(#request.fromWalletIban)','update':'@walletAccess.wallet(#id) and @walletAccess.user(#request.userId)','deleteById':'@walletAccess.wallet(#id)'}),('TransactionController.java',{'findById':'@walletAccess.transaction(#id)','findByReferenceNumber':'@walletAccess.reference(#referenceNumber)','findAllByUserId':'@walletAccess.user(#userId)','findAll':"hasRole('ADMIN')"})]:
    p=base/'controller'/name;s=p.read_text();import re
    for match in list(re.finditer(r'@PreAuthorize\("[^"]+"\)([\s\S]*?public ResponseEntity<[^\n]+? (\w+)\()',s))[::-1]:
        s=s[:match.start()]+f'@PreAuthorize("{guards[match[2]]}")'+match[1]+s[match.end():]
    p.write_text(s,encoding='utf-8')
p=base/'service/WalletService.java';s=p.read_text();s=s.replace('import java.util.List;', 'import java.util.List;\nimport java.math.BigDecimal;\nimport com.github.yildizmy.exception.WalletRuleException;')
s=s.replace('    public CommandResponse create(WalletRequest request) {','''    public CommandResponse create(WalletRequest request) {
        validateAmount(request.getBalance(), true);
        request.setName(request.getName().trim());
        if (request.getIban() == null || request.getIban().isBlank()) {
            String digits = String.format("%025d", new java.math.BigInteger(java.util.UUID.randomUUID().toString().replace("-", ""), 16).mod(java.math.BigInteger.TEN.pow(25)));
            int checksum = 98 - new java.math.BigInteger(digits + "112700").mod(java.math.BigInteger.valueOf(97)).intValue();
            request.setIban("BR" + String.format("%02d", checksum) + digits);
        }
        request.setIban(request.getIban().replaceAll("\\\\s", "").toUpperCase(java.util.Locale.ROOT));
        if (!ibanValidator.isValid(request.getIban(), null)) throw new WalletRuleException("INVALID_WALLET");''')
s=s.replace('        transactionService.create(walletTransactionRequestMapper.toTransactionRequest(request));','''        if (request.getBalance().signum() > 0) {
            transactionService.create(walletTransactionRequestMapper.toTransactionRequest(request));
        }''')
a=s.index('    @Transactional\n    public CommandResponse transferFunds');b=s.index('    /**\n     * Updates wallet',a)
s=s[:a]+'''    private void validateAmount(BigDecimal amount, boolean allowZero) {
        if (amount == null || amount.signum() < 0 || (!allowZero && amount.signum() == 0) ||
            amount.stripTrailingZeros().scale() > 2 || amount.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new WalletRuleException("INVALID_AMOUNT");
        }
    }

    private Wallet lockedWallet(String iban, List<Wallet> wallets) {
        return wallets.stream().filter(w -> w.getIban().equals(iban)).findFirst()
            .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_WALLET_NOT_FOUND)));
    }

    @Transactional
    public CommandResponse transferFunds(TransactionRequest request) {
        validateAmount(request.getAmount(), false);
        if (request.getFromWalletIban().equals(request.getToWalletIban())) throw new WalletRuleException("SAME_WALLET");
        request.setTypeId(1L);
        var locked = walletRepository.lockByIbans(List.of(request.getFromWalletIban(), request.getToWalletIban()));
        var from = lockedWallet(request.getFromWalletIban(), locked);
        var to = lockedWallet(request.getToWalletIban(), locked);
        var replay = transactionService.replay(request);
        if (replay != null) return replay;
        if (from.getBalance().compareTo(request.getAmount()) < 0)
            throw new InsufficientFundsException(messageConfig.getMessage(ERROR_INSUFFICIENT_FUNDS));
        validateAmount(to.getBalance().add(request.getAmount()), true);
        from.setBalance(from.getBalance().subtract(request.getAmount()));
        to.setBalance(to.getBalance().add(request.getAmount()));
        walletRepository.save(from);
        walletRepository.save(to);
        return transactionService.create(request);
    }

    @Transactional
    public CommandResponse addFunds(TransactionRequest request) {
        validateAmount(request.getAmount(), false);
        request.setFromWalletIban(request.getToWalletIban());
        request.setTypeId(2L);
        var wallet = lockedWallet(request.getToWalletIban(), walletRepository.lockByIbans(List.of(request.getToWalletIban())));
        var replay = transactionService.replay(request);
        if (replay != null) return replay;
        validateAmount(wallet.getBalance().add(request.getAmount()), true);
        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        walletRepository.save(wallet);
        return transactionService.create(request);
    }

    @Transactional
    public CommandResponse withdrawFunds(TransactionRequest request) {
        validateAmount(request.getAmount(), false);
        request.setToWalletIban(request.getFromWalletIban());
        request.setTypeId(3L);
        var wallet = lockedWallet(request.getFromWalletIban(), walletRepository.lockByIbans(List.of(request.getFromWalletIban())));
        var replay = transactionService.replay(request);
        if (replay != null) return replay;
        if (wallet.getBalance().compareTo(request.getAmount()) < 0)
            throw new InsufficientFundsException(messageConfig.getMessage(ERROR_INSUFFICIENT_FUNDS));
        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        walletRepository.save(wallet);
        return transactionService.create(request);
    }

'''+s[b:]
s=s.replace('    public CommandResponse update(long id, WalletRequest request) {','    @Transactional\n    public CommandResponse update(long id, WalletRequest request) {')
a=s.index('        // check if the iban is changed');b=s.index('        log.info(messageConfig.getMessage(INFO_WALLET_UPDATED',a)
s=s[:a]+'''        if (!java.util.Objects.equals(request.getIban(), foundWallet.getIban()) ||
            !java.util.Objects.equals(request.getUserId(), foundWallet.getUser().getId()) ||
            request.getBalance().compareTo(foundWallet.getBalance()) != 0) throw new WalletRuleException("IMMUTABLE_WALLET");
        String name = request.getName().trim();
        if (!name.equalsIgnoreCase(foundWallet.getName()) && walletRepository.existsByUserIdAndNameIgnoreCase(request.getUserId(), name))
            throw new ElementAlreadyExistsException(messageConfig.getMessage(ERROR_WALLET_NAME_EXISTS));
        final Wallet wallet = foundWallet;
        wallet.setName(name);
        walletRepository.save(wallet);
'''+s[b:]
s=s.replace('    public void deleteById(long id) {','    @Transactional\n    public void deleteById(long id) {').replace('        walletRepository.delete(wallet);','''        if (wallet.getBalance().signum() != 0 || transactionService.hasHistory(id)) throw new WalletRuleException("IMMUTABLE_WALLET");
        walletRepository.delete(wallet);''')
p.write_text(s,encoding='utf-8')
Path('backend/src/main/resources/db/migration/V6__wallet_operation_types.sql').write_text('''INSERT INTO public.type (id, name, description) VALUES
    (2, 'Deposit', 'Simulated deposit'),
    (3, 'Withdrawal', 'Simulated withdrawal'),
    (4, 'Initial balance', 'Initial simulated balance');
SELECT setval('type_seq', (SELECT MAX(id) FROM public.type));
ALTER TABLE transaction ADD COLUMN request_id UUID;
ALTER TABLE transaction ADD CONSTRAINT uc_transaction_request_id UNIQUE (request_id);
UPDATE transaction SET type_id = 4 WHERE from_wallet_id = to_wallet_id AND description = 'Initial balance';
-- Old self-transfers cannot safely be classified as deposits or withdrawals.
ALTER TABLE wallet ADD CONSTRAINT wallet_nonnegative_balance CHECK (balance >= 0) NOT VALID;
ALTER TABLE transaction ADD CONSTRAINT transaction_nonnegative_amount CHECK (amount >= 0) NOT VALID;
''',encoding='utf-8')
print('Wallet validation, ownership, locking, operation types and retry protection updated')

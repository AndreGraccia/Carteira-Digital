from pathlib import Path
p=Path('backend/src/test/java/com/github/yildizmy/service/WalletServiceTest.java');s=p.read_text()
s=s.replace('        when(walletRepository.existsByIbanIgnoreCase(anyString())).thenReturn(false);','        when(ibanValidator.isValid(anyString(), isNull())).thenReturn(true);\n        when(walletRepository.existsByIbanIgnoreCase(anyString())).thenReturn(false);')
s=s.replace('        when(walletRepository.existsByIbanIgnoreCase(anyString())).thenReturn(true);','        when(ibanValidator.isValid(anyString(), isNull())).thenReturn(true);\n        when(walletRepository.existsByIbanIgnoreCase(anyString())).thenReturn(true);')
s=s.replace('        when(walletRepository.findByIban("FROM123")).thenReturn(Optional.of(fromWallet));\n        when(walletRepository.findByIban("TO123")).thenReturn(Optional.of(toWallet));','        when(walletRepository.lockByIbans(anyList())).thenReturn(java.util.List.of(fromWallet, toWallet));')
s=s.replace('        when(walletRepository.findByIban("TO123")).thenReturn(Optional.of(toWallet));','        when(walletRepository.lockByIbans(anyList())).thenReturn(java.util.List.of(toWallet));')
s=s.replace('        when(walletRepository.findByIban("FROM123")).thenReturn(Optional.of(fromWallet));','        when(walletRepository.lockByIbans(anyList())).thenReturn(java.util.List.of(fromWallet));')
extra='''    @Test
    void operations_rejectInvalidAmountsBeforeTouchingBalances() {
        for (var amount : java.util.List.of(new BigDecimal("-1"), BigDecimal.ZERO, new BigDecimal("0.001"), new BigDecimal("10000000000"))) {
            var request = createTestTransactionRequest("FROM", "TO", amount);
            assertThrows(com.github.yildizmy.exception.WalletRuleException.class, () -> walletService.transferFunds(request));
            assertThrows(com.github.yildizmy.exception.WalletRuleException.class, () -> walletService.addFunds(request));
            assertThrows(com.github.yildizmy.exception.WalletRuleException.class, () -> walletService.withdrawFunds(request));
        }
        verifyNoInteractions(walletRepository, transactionService);
    }

    @Test
    void transfer_rejectsSameWallet() {
        var request = createTestTransactionRequest("SAME", "SAME", BigDecimal.ONE);
        assertThrows(com.github.yildizmy.exception.WalletRuleException.class, () -> walletService.transferFunds(request));
        verifyNoInteractions(walletRepository, transactionService);
    }

    @Test
    void replay_doesNotMoveMoneyAgain() {
        var wallet = createTestWallet(1L, "TO", "Wallet", new BigDecimal("100.00"));
        var request = createTestTransactionRequest("TO", "TO", new BigDecimal("25.00"));
        when(walletRepository.lockByIbans(anyList())).thenReturn(java.util.List.of(wallet));
        when(transactionService.replay(request)).thenReturn(new CommandResponse(77L));
        assertEquals(77L, walletService.addFunds(request).id());
        assertEquals(new BigDecimal("100.00"), wallet.getBalance());
        verify(walletRepository, never()).save(any());
        verify(transactionService, never()).create(any());
    }

    @Test
    void withdrawal_cannotOverdraw() {
        var wallet = createTestWallet(1L, "FROM", "Wallet", BigDecimal.TEN);
        var request = createTestTransactionRequest("FROM", "FROM", new BigDecimal("10.01"));
        when(walletRepository.lockByIbans(anyList())).thenReturn(java.util.List.of(wallet));
        assertThrows(InsufficientFundsException.class, () -> walletService.withdrawFunds(request));
        assertEquals(BigDecimal.TEN, wallet.getBalance());
        verify(transactionService, never()).create(any());
    }

    @Test
    void deposit_setsAuthoritativeTypeAndSameWallet() {
        var wallet = createTestWallet(1L, "TO", "Wallet", BigDecimal.ZERO);
        var request = createTestTransactionRequest("OTHER", "TO", new BigDecimal("0.01"));
        request.setTypeId(999L);
        when(walletRepository.lockByIbans(anyList())).thenReturn(java.util.List.of(wallet));
        when(transactionService.create(request)).thenReturn(new CommandResponse(1L));
        walletService.addFunds(request);
        assertEquals(2L, request.getTypeId());
        assertEquals("TO", request.getFromWalletIban());
        assertEquals(new BigDecimal("0.01"), wallet.getBalance());
    }

'''
s=s.replace('    // Helper Methods',extra+'    // Helper Methods');p.write_text(s,encoding='utf-8')
p=Path('backend/src/test/java/com/github/yildizmy/service/TransactionServiceTest.java');s=p.read_text().replace('findAllByUserId_shouldThrowExceptionWhenNoTransactionsFound','findAllByUserId_shouldReturnEmptyHistory').replace('assertThrows(NoSuchElementFoundException.class, () -> transactionService.findAllByUserId(userId));','assertTrue(transactionService.findAllByUserId(userId).isEmpty());');p.write_text(s,encoding='utf-8')

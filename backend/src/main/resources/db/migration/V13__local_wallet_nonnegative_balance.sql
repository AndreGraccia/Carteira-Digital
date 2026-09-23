-- V8 dropped wallet_nonnegative_balance so Open Finance accounts can mirror an overdraft.
-- That also removed the database guard for wallets the app itself moves money in: since then,
-- only WalletService kept LOCAL balances from going negative. Restore the guard for LOCAL only.
-- NOT VALID, like V6 and V7: new writes are checked, rows already stored are left as they are.
ALTER TABLE wallet
    ADD CONSTRAINT wallet_local_nonnegative_balance
    CHECK (source <> 'LOCAL' OR balance >= 0) NOT VALID;

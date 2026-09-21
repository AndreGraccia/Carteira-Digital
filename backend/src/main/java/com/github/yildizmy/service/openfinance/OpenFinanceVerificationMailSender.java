package com.github.yildizmy.service.openfinance;

public interface OpenFinanceVerificationMailSender {
    void sendCode(String recipient, String firstName, String code, long validityMinutes);
}

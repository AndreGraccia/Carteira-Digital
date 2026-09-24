package com.github.yildizmy.service.mail;

public interface VerificationMailSender {

    /** The sign-up code. */
    void sendCode(String recipient, String firstName, String code, long validityMinutes);

    /**
     * Someone tried to sign up with an address that already has an account. The owner is told by e-mail; the
     * person signing up sees the same answer as a new sign-up, so the form never reveals which addresses exist.
     */
    void sendAccountExists(String recipient, String firstName, String username);
}

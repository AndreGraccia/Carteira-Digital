package com.github.yildizmy.service.openfinance;

import com.github.yildizmy.exception.OpenFinanceVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpOpenFinanceVerificationMailSender implements OpenFinanceVerificationMailSender {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpOpenFinanceVerificationMailSender(
            JavaMailSender mailSender,
            @Value("${app.open-finance.verification.mail-from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendCode(String recipient, String firstName, String code, long validityMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Código para conectar seus dados financeiros");
        message.setText("""
                Olá, %s.

                Seu código de verificação da e-wallet é:

                %s

                Ele é válido por %d minutos e pode ser usado uma única vez.
                Se você não solicitou esta conexão, ignore esta mensagem.

                A e-wallet nunca pedirá este código por telefone, mensagem ou redes sociais.
                """.formatted(firstName, code, validityMinutes));
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new OpenFinanceVerificationException(
                    "OPEN_FINANCE_EMAIL_UNAVAILABLE",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }
}

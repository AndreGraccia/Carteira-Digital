package com.github.yildizmy.service.mail;

import com.github.yildizmy.exception.CodedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpVerificationMailSender implements VerificationMailSender {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpVerificationMailSender(
            JavaMailSender mailSender,
            @Value("${app.email-verification.mail-from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendCode(String recipient, String firstName, String code, long validityMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Wall.ia · código para confirmar seu e-mail");
        message.setText("""
                Olá, %s.

                Seu código de verificação da Wall.ia é:

                %s

                Ele é válido por %d minutos e pode ser usado uma única vez.
                Se você não criou uma conta na Wall.ia, ignore esta mensagem.

                A Wall.ia nunca pedirá este código por telefone, mensagem ou redes sociais.
                """.formatted(firstName, code, validityMinutes));
        send(message);
    }

    @Override
    public void sendAccountExists(String recipient, String firstName, String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Wall.ia · tentativa de cadastro com seu e-mail");
        message.setText("""
                Olá, %s.

                Alguém tentou criar uma conta na Wall.ia com este e-mail, mas ele já é seu.
                Seu usuário é: %s

                Se foi você, é só entrar com esse usuário. Se não foi, nada muda na sua conta.
                """.formatted(firstName, username));
        send(message);
    }

    private void send(SimpleMailMessage message) {
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new CodedException(
                    "EMAIL_UNAVAILABLE",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }
}

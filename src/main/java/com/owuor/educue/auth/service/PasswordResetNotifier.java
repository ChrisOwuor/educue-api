package com.owuor.educue.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetNotifier {

    private final ObjectProvider<JavaMailSender> mailSenders;

    @Value("${app.auth.password-reset.mail-enabled:false}")
    private boolean enabled;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${spring.mail.from:}")
    private String from;

    public void send(String email, String token) {
        if (!enabled) {
            log.info("Password-reset email sending is disabled");
            return;
        }

        JavaMailSender sender = mailSenders.getIfAvailable();

        if (sender == null) {
            throw new IllegalStateException(
                    "Password-reset email is enabled, but JavaMailSender is unavailable"
            );
        }

        SimpleMailMessage message = getSimpleMailMessage(email, token);

        sender.send(message);

        log.info("Password-reset email sent to configured recipient");
    }

    private SimpleMailMessage getSimpleMailMessage(String email, String token) {
        String resetUrl =
                frontendBaseUrl + "/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setReplyTo(from);
        message.setSubject("Reset your EduCue password");
        message.setText("""
                A password reset was requested for your EduCue account.

                Use the link below within 30 minutes:

                %s

                If you did not request this password reset, you can safely ignore this email.
                """.formatted(resetUrl));
        return message;
    }
}

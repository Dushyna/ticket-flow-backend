package io.github.dushyna.ticketflow.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for email sending
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final EmailSender emailSender;
    private final TemplateEngine templateEngine;

    @Async
    public void sendConfirmationEmail(String sentTo, String confirmationCode) {
        String confirmationLink = "%s/api/v1/auth/confirm-redirect/%s".formatted(baseUrl, confirmationCode);

        Map<String, Object> model = Map.of(
                "link", confirmationLink
        );

        String htmlContent = templateEngine.generateHtml("confirm_registration_mail.ftlh", model);
        emailSender.sendEmail(sentTo, "Confirm your registration", htmlContent);
    }

    @Async
    public void sendResetPasswordEmail(String sentTo, String token) {
        String resetLink = "%s/reset-password?token=%s".formatted(frontendUrl, token);

        String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Password Reset</title>
                </head>
                <body>
                    <h1>Password Reset</h1>
                    <p>You requested a password reset.</p>
                    <p>Click the link below to set a new password:</p>
                    <p>
                        <a href="%s">Reset Password</a>
                    </p>
                    <p>If you did not request this, please ignore this email.</p>
                </body>
                </html>
                """.formatted(resetLink);

        emailSender.sendEmail(sentTo, "Reset your password", htmlContent);
    }
}

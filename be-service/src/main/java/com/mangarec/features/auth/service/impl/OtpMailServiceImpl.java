package com.mangarec.features.auth.service.impl;

import com.mangarec.features.auth.service.OtpMailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OtpMailServiceImpl implements OtpMailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Override
    public void sendPasswordResetOtp(String email, String name, String otp) {
        sendOtp(email, name, otp, "MangaRec password reset OTP",
                "Your MangaRec password reset OTP is:",
                "If you did not request a password reset, ignore this email.");
    }

    @Override
    public void sendEmailVerificationOtp(String email, String name, String otp) {
        sendOtp(email, name, otp, "Verify your MangaRec email",
                "Your MangaRec email verification OTP is:",
                "If you did not create a MangaRec account, ignore this email.");
    }

    private void sendOtp(String email, String name, String otp, String subject, String intro, String footer) {
        if (!StringUtils.hasText(fromEmail)) {
            throw new IllegalStateException("Mail sender username is not configured");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(buildHtml(name, otp, intro, footer), true);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw new IllegalStateException("Cannot send OTP email", e);
        }
    }

    private String buildHtml(String name, String otp, String intro, String footer) {
        String displayName = StringUtils.hasText(name) ? name : "MangaRec user";
        return """
                <!DOCTYPE html>
                <html lang="en">
                <body style="font-family: Arial, sans-serif; color: #1f2937;">
                    <p>Hello <strong>%s</strong>,</p>
                    <p>%s</p>
                    <p style="font-size: 28px; font-weight: 700; letter-spacing: 6px;">%s</p>
                    <p>This code expires soon. %s</p>
                </body>
                </html>
                """.formatted(displayName, intro, otp, footer);
    }
}

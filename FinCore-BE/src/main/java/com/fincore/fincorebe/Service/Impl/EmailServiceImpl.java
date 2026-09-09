package com.fincore.fincorebe.Service.Impl;

import com.fincore.fincorebe.Service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${spring.mail.username:noreply.fincore@gmail.com}")
    private String fromEmail;

    @Override
    public String generate6DigitOtp() {
        int code = secureRandom.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    @Override
    public void sendVerificationOtpEmail(String toEmail, String fullName, String otpCode, int expirationMinutes) {
        String subject = "FinCore - Mã Xác Thực Kích Hoạt Tài Khoản Của Bạn";
        String htmlContent = buildOtpEmailHtml(fullName, otpCode, expirationMinutes);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "FinCore Security Team");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Sent verification email to {} successfully", toEmail);
        } catch (Exception ex) {
            log.warn("Could not send real email via SMTP (using fallback logging): {}", ex.getMessage());
            log.info("========================================================================");
            log.info("[FINCORE EMAIL SIMULATION] To: {} | FullName: {}", toEmail, fullName);
            log.info("[FINCORE EMAIL SIMULATION] >>> OTP CODE: {} (Valid for {} mins) <<<", otpCode, expirationMinutes);
            log.info("========================================================================");
        }
    }

    private String buildOtpEmailHtml(String fullName, String otpCode, int expirationMinutes) {
        String template = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>FinCore Verification</title>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f9; margin: 0; padding: 0; }
                        .container { max-width: 540px; margin: 30px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.08); }
                        .header { background: linear-gradient(135deg, #1e3c72 0%%, #2a5298 100%%); color: #ffffff; padding: 28px; text-align: center; }
                        .content { padding: 32px 28px; color: #333333; line-height: 1.6; }
                        .otp-box { background: #f0f4ff; border: 2px dashed #2a5298; border-radius: 8px; font-size: 32px; font-weight: 700; letter-spacing: 8px; color: #1e3c72; text-align: center; padding: 18px; margin: 24px 0; }
                        .footer { background: #f9fafb; padding: 18px; text-align: center; font-size: 12px; color: #888888; border-top: 1px solid #eeeeee; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1 style="margin: 0; font-size: 24px; font-weight: 700;">FinCore Transaction Engine</h1>
                            <p style="margin: 6px 0 0 0; opacity: 0.9; font-size: 14px;">Xác thực tài khoản của bạn</p>
                        </div>
                        <div class="content">
                            <p>Xin chào <strong>%s</strong>,</p>
                            <p>Cảm ơn bạn đã đăng ký tài khoản tại <strong>FinCore</strong>. Để kích hoạt tài khoản và mở ví tài chính mặc định, vui lòng nhập mã OTP sau:</p>
                            <div class="otp-box">%s</div>
                            <p style="color: #666666; font-size: 13px;">Mã OTP có hiệu lực trong vòng <strong>%d phút</strong>. Vì lý do an toàn, tuyệt đối không chia sẻ mã này cho bất kỳ ai.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 FinCore Transaction Engine. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """;
        return String.format(template, fullName, otpCode, expirationMinutes);
    }
}

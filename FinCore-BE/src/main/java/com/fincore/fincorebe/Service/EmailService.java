package com.fincore.fincorebe.Service;

public interface EmailService {

    String generate6DigitOtp();

    void sendVerificationOtpEmail(String toEmail, String fullName, String otpCode, int expirationMinutes);
}

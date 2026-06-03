package com.emmanuelandsamuel.savings_project.utilities;

import com.emmanuelandsamuel.savings_project.enumerations.GroupSavingsType;
import com.emmanuelandsamuel.savings_project.exceptions.ApplicationException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Random;

public class AppExtensions {

    private AppExtensions() {
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final String EMAIL_VERIFICATION_EVENT = "EMAIL_VERIFICATION";

    public static final String USER_REGISTERED_EVENT = "USER_REGISTERED";

    public static final String EMAIL_VERIFICATION_KAFKA_TOPIC = "EMAIL_VERIFICATION_TOPIC";

    public static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    public static final String USER_LOGIN_EVENT = "USER_LOGIN";

    public static final int MAX_LOGIN_ATTEMPTS = 3;

    public static <T> String serialize(T object) {

        try {

            if (object == null)
                return null;

            return objectMapper.writeValueAsString(object);

        } catch (Exception e) {

            throw new RuntimeException("Serialization failed", e);

        }
    }

    public static String generateVerificationCode() {

        Random random = new Random();

        int randomVerificationCodeNumber = random.nextInt(999999);

        String verificationCode = Integer.toString(randomVerificationCodeNumber);

        while (verificationCode.length() < 6) {

            verificationCode = "0".concat(verificationCode);
        }

        return verificationCode;

    }

    public static <T> T deserialize(String json, Class<T> type) {

        try {

            if (json == null || json.isEmpty())
                return null;

            return objectMapper.readValue(json, type);

        } catch (Exception e) {

            throw new RuntimeException("Deserialization failed", e);

        }
    }

    public static String generateHash(String input) {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();

            for (byte b : hash)
                hex.append(String.format("%02x", b));

            return hex.toString();

        } catch (NoSuchAlgorithmException e) {

            throw new ApplicationException("SHA-256 not available");

        }
    }

    public static String getVerificationMailBody(String verificationCode) {

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Verify Your Email</title>
                </head>
                <body style="font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 5px;">
                        <h2 style="color: #2196F3;">Verify Your Email Address</h2>

                        <p>Thank you for registering with us! To complete your registration, please verify your email address.</p>

                        <p>Use the verification code below to activate your account:</p>

                        <div style="background-color: #f0f8ff; padding: 20px; border-radius: 4px; margin: 20px 0; text-align: center;">
                            <h1 style="color: #2196F3; margin: 0; font-size: 32px; letter-spacing: 5px;">
                """
                + verificationCode
                + """
                                    </h1>
                                </div>

                                <p>Enter this code on the verification page to activate your account.</p>

                                <p style="background-color: #fff3cd; padding: 15px; border-left: 4px solid #ff9800; border-radius: 4px; margin: 20px 0;">
                                    <strong>Important:</strong> This verification code will expire in <strong>15 minutes</strong>.
                                </p>

                                <p>If you did not request this verification code, please ignore this email or contact our support team.</p>

                                <p style="margin-top: 30px;">Best regards,<br>The Team</p>

                                <hr style="margin-top: 40px; border: none; border-top: 1px solid #ddd;">
                            </div>
                        </body>
                        </html>
                        """;
    }

    public static BigDecimal convertKoboToNaira(Long amountInKobo) {
        if (amountInKobo == null)
            return null;
        return BigDecimal.valueOf(amountInKobo).divide(BigDecimal.valueOf(100));
    }

    public static Long convertNairaToKobo(BigDecimal amountInNaira) {
        if (amountInNaira == null)
            return null;
        return amountInNaira.multiply(BigDecimal.valueOf(100)).longValue();
    }

    public static LocalDate calculateNextDate(LocalDate currentDate, GroupSavingsType cycle) {

        return switch (cycle) {

            case DAILY ->
                currentDate.plusDays(1);

            case WEEKLY ->
                currentDate.plusWeeks(1);

            case BI_WEEKLY ->
                currentDate.plusWeeks(2);

            case MONTHLY ->
                currentDate.plusMonths(1);
        };
    }

    
}

package fu.edu.mss301.digilib.fine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "sepay")
public record SepayProperties(
        String bank,
        String accountNumber,
        String accountName,
        String webhookSecret,
        String qrBaseUrl,
        String paymentCodePrefix,
        Duration paymentTtl,
        Duration webhookReplayTolerance
) {

    public SepayProperties {
        qrBaseUrl = defaultIfBlank(qrBaseUrl, "https://qr.sepay.vn/img");
        paymentCodePrefix = defaultIfBlank(paymentCodePrefix, "FINE");
        paymentTtl = paymentTtl == null ? Duration.ofMinutes(15) : paymentTtl;
        webhookReplayTolerance = webhookReplayTolerance == null
                ? Duration.ofMinutes(5)
                : webhookReplayTolerance;
    }

    public void validateQrConfiguration() {
        requireText(bank, "sepay.bank");
        requireText(accountNumber, "sepay.account-number");
        requireText(qrBaseUrl, "sepay.qr-base-url");
        requireText(paymentCodePrefix, "sepay.payment-code-prefix");
        requirePositive(paymentTtl, "sepay.payment-ttl");

        if (!paymentCodePrefix.matches("[A-Za-z0-9]+")) {
            throw new IllegalStateException(
                    "sepay.payment-code-prefix must contain only letters and digits"
            );
        }
    }

    public void validateWebhookConfiguration() {
        requireText(webhookSecret, "sepay.webhook-secret");
        requireText(accountNumber, "sepay.account-number");
        requirePositive(webhookReplayTolerance, "sepay.webhook-replay-tolerance");
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static void requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must be configured");
        }
    }

    private static void requirePositive(Duration value, String propertyName) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException(propertyName + " must be greater than zero");
        }
    }
}

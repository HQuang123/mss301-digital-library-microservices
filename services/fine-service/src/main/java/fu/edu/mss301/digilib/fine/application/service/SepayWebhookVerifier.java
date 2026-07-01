package fu.edu.mss301.digilib.fine.application.service;

import fu.edu.mss301.digilib.fine.application.exception.InvalidWebhookSignatureException;
import fu.edu.mss301.digilib.fine.config.SepayProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;

@Component
public class SepayWebhookVerifier {

    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final SepayProperties properties;
    private final Clock clock;

    public SepayWebhookVerifier(SepayProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void verify(String rawBody, String signature, String timestampHeader) {
        properties.validateWebhookConfiguration();

        if (rawBody == null || rawBody.isBlank()) {
            throw new InvalidWebhookSignatureException("Webhook body must not be empty");
        }

        long timestamp = parseTimestamp(timestampHeader);
        long now = Instant.now(clock).getEpochSecond();
        long tolerance = properties.webhookReplayTolerance().toSeconds();

        if (Math.abs(now - timestamp) > tolerance) {
            throw new InvalidWebhookSignatureException("Webhook timestamp is outside the allowed window");
        }

        String expected = SIGNATURE_PREFIX + hmacHex(
                timestamp + "." + rawBody,
                properties.webhookSecret()
        );

        byte[] expectedBytes = expected.getBytes(StandardCharsets.US_ASCII);
        byte[] actualBytes = valueOrEmpty(signature).getBytes(StandardCharsets.US_ASCII);

        if (!MessageDigest.isEqual(expectedBytes, actualBytes)) {
            throw new InvalidWebhookSignatureException("Invalid SePay webhook signature");
        }
    }

    private long parseTimestamp(String timestampHeader) {
        try {
            return Long.parseLong(timestampHeader);
        } catch (NumberFormatException | NullPointerException exception) {
            throw new InvalidWebhookSignatureException("Invalid SePay webhook timestamp");
        }
    }

    private String hmacHex(String value, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to verify SePay webhook signature", exception);
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}

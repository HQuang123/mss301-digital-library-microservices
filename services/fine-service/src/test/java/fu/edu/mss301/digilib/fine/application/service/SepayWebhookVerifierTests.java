package fu.edu.mss301.digilib.fine.application.service;

import fu.edu.mss301.digilib.fine.application.exception.InvalidWebhookSignatureException;
import fu.edu.mss301.digilib.fine.config.SepayProperties;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SepayWebhookVerifierTests {

    private static final String SECRET = "webhook-secret";
    private static final long TIMESTAMP = 1782010800L;
    private static final String BODY = "{\"id\":92704}";

    private final SepayWebhookVerifier verifier = new SepayWebhookVerifier(
            new SepayProperties(
                    "Vietcombank",
                    "0010000000355",
                    "DIGITAL LIBRARY",
                    SECRET,
                    null,
                    null,
                    null,
                    Duration.ofMinutes(5)
            ),
            Clock.fixed(Instant.ofEpochSecond(TIMESTAMP), ZoneOffset.UTC)
    );

    @Test
    void acceptsValidHmacSignature() throws Exception {
        String signature = "sha256=" + hmac(TIMESTAMP + "." + BODY);

        assertDoesNotThrow(() -> verifier.verify(BODY, signature, Long.toString(TIMESTAMP)));
    }

    @Test
    void rejectsInvalidSignature() {
        assertThrows(
                InvalidWebhookSignatureException.class,
                () -> verifier.verify(BODY, "sha256=wrong", Long.toString(TIMESTAMP))
        );
    }

    @Test
    void rejectsTimestampOutsideReplayWindow() throws Exception {
        long oldTimestamp = TIMESTAMP - 301;
        String signature = "sha256=" + hmac(oldTimestamp + "." + BODY);

        assertThrows(
                InvalidWebhookSignatureException.class,
                () -> verifier.verify(BODY, signature, Long.toString(oldTimestamp))
        );
    }

    private String hmac(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}

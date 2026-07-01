package fu.edu.mss301.digilib.fine.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.edu.mss301.digilib.fine.api.dto.PaymentStatusResponse;
import fu.edu.mss301.digilib.fine.api.dto.SepayQrResponse;
import fu.edu.mss301.digilib.fine.api.dto.SepayWebhookRequest;
import fu.edu.mss301.digilib.fine.api.dto.SepayWebhookResponse;
import fu.edu.mss301.digilib.fine.application.exception.InvalidWebhookException;
import fu.edu.mss301.digilib.fine.application.service.SepayPaymentService;
import fu.edu.mss301.digilib.fine.application.service.SepayWebhookVerifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fines")
public class FineController {

    private final SepayPaymentService paymentService;
    private final SepayWebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper;

    public FineController(
            SepayPaymentService paymentService,
            SepayWebhookVerifier webhookVerifier,
            ObjectMapper objectMapper
    ) {
        this.paymentService = paymentService;
        this.webhookVerifier = webhookVerifier;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/{fineId}/payments/sepay/qr")
    ResponseEntity<SepayQrResponse> createSepayQr(@PathVariable("fineId") Integer fineId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createQr(fineId));
    }

    @GetMapping("/{fineId}/payments/latest")
    PaymentStatusResponse getLatestPaymentStatus(@PathVariable("fineId") Integer fineId) {
        return paymentService.getLatestPaymentStatus(fineId);
    }

    @PostMapping(
            value = "/payments/sepay/webhook",
            consumes = "application/json",
            produces = "application/json"
    )
    SepayWebhookResponse processSepayWebhook(
            @RequestHeader(value = "X-SePay-Signature", required = false) String signature,
            @RequestHeader(value = "X-SePay-Timestamp", required = false) String timestamp,
            @RequestBody String rawBody
    ) {
        webhookVerifier.verify(rawBody, signature, timestamp);
        paymentService.processWebhook(parseWebhook(rawBody));
        return SepayWebhookResponse.acknowledged();
    }

    private SepayWebhookRequest parseWebhook(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, SepayWebhookRequest.class);
        } catch (JsonProcessingException exception) {
            throw new InvalidWebhookException("Invalid SePay webhook JSON", exception);
        }
    }
}

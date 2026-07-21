package fu.edu.mss301.digilib.fine.application.service;

import fu.edu.mss301.digilib.fine.api.dto.SepayQrResponse;
import fu.edu.mss301.digilib.fine.api.dto.SepayWebhookRequest;
import fu.edu.mss301.digilib.fine.application.exception.InvalidWebhookException;
import fu.edu.mss301.digilib.fine.config.SepayProperties;
import fu.edu.mss301.digilib.fine.domain.entity.Fine;
import fu.edu.mss301.digilib.fine.domain.entity.PaymentAttempt;
import fu.edu.mss301.digilib.fine.domain.vo.FineStatus;
import fu.edu.mss301.digilib.fine.domain.vo.PaymentStatus;
import fu.edu.mss301.digilib.fine.infrastructure.adapter.NotificationClientAdapter;
import fu.edu.mss301.digilib.fine.infrastructure.persistence.FineJpaRepository;
import fu.edu.mss301.digilib.fine.infrastructure.persistence.PaymentAttemptJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SepayPaymentServiceTests {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-21T03:00:00Z"),
            ZoneId.of("Asia/Ho_Chi_Minh")
    );

    @Mock
    private FineJpaRepository fineRepository;

    @Mock
    private PaymentAttemptJpaRepository paymentRepository;

    @Mock
    private SecureRandom secureRandom;

    @Mock
    private NotificationClientAdapter notificationClient;

    private SepayPaymentService service;

    @BeforeEach
    void setUp() {
        SepayProperties properties = new SepayProperties(
                "Vietcombank",
                "0010000000355",
                "DIGITAL LIBRARY",
                "webhook-secret",
                "https://qr.sepay.vn/img",
                "FINE",
                Duration.ofMinutes(15),
                Duration.ofMinutes(5)
        );
        service = new SepayPaymentService(
                fineRepository,
                paymentRepository,
                properties,
                CLOCK,
                secureRandom,
                notificationClient
        );
    }

    @Test
    void createsPendingQrPaymentForPendingFine() {
        Fine fine = pendingFine();
        when(fineRepository.findByIdForUpdate(10)).thenReturn(Optional.of(fine));
        when(paymentRepository.save(any(PaymentAttempt.class))).thenAnswer(invocation -> {
            PaymentAttempt payment = invocation.getArgument(0);
            payment.setId(99L);
            return payment;
        });

        SepayQrResponse response = service.createQr(10);

        assertEquals(99L, response.paymentId());
        assertEquals(25000L, response.amount());
        assertEquals(PaymentStatus.PENDING, response.status());
        assertEquals("FINE000000000000", response.paymentCode());
        assertTrue(response.qrUrl().contains("amount=25000"));
        assertTrue(response.qrUrl().contains("des=FINE000000000000"));
        verify(paymentRepository).expireActiveAttempts(
                eq(10),
                anyCollection(),
                eq(PaymentStatus.EXPIRED),
                any()
        );
    }

    @Test
    void successfulWebhookMarksPaymentAndFinePaid() {
        Fine fine = pendingFine();
        PaymentAttempt payment = pendingPayment(fine);
        SepayWebhookRequest webhook = webhook(25000L);

        when(paymentRepository.findBySepayTransactionId("92704")).thenReturn(Optional.empty());
        when(paymentRepository.findByPaymentCodeForUpdate("FINEABC123"))
                .thenReturn(Optional.of(payment));

        service.processWebhook(webhook);

        assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());
        assertEquals("92704", payment.getSepayTransactionId());
        assertEquals(FineStatus.PAID, fine.getStatus());
        assertNotNull(fine.getPaidAt());
        assertEquals(payment.getPaidAt(), fine.getPaidAt());
        verify(notificationClient).sendFinePaidConfirmation(
                fine.getId(), fine.getStudentId(), fine.getStudentEmail(), payment.getAmount(),
                fine.getPaidAt().toString());
    }

    @Test
    void webhookMatchesPaymentCodeFromContentWhenSepayTruncatesTheCodeField() {
        // Real SePay behavior: the "code" field it auto-extracts truncates at
        // the first non-digit, but the raw "content" still carries the full
        // generated payment code (prefix + 12 hex chars).
        Fine fine = pendingFine();
        PaymentAttempt payment = pendingPayment(fine);
        payment.setPaymentCode("FINE020CBBC6A2EA");

        SepayWebhookRequest webhook = new SepayWebhookRequest(
                69277746L,
                "TPBank",
                "2026-07-21 15:31:05",
                "0010000000355",
                null,
                "FINE020",
                "FINE020CBBC6A2EA chuyen tien",
                "in",
                "BankAPINotify FINE020CBBC6A2EA",
                25000L,
                71018L,
                "669ITC1262028825"
        );

        when(paymentRepository.findBySepayTransactionId("69277746")).thenReturn(Optional.empty());
        when(paymentRepository.findByPaymentCodeForUpdate("FINE020CBBC6A2EA"))
                .thenReturn(Optional.of(payment));

        service.processWebhook(webhook);

        assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());
        verify(paymentRepository, never()).findByPaymentCodeForUpdate("FINE020");
    }

    @Test
    void duplicateWebhookIsAcknowledgedWithoutUpdatingAgain() {
        PaymentAttempt completed = PaymentAttempt.builder()
                .sepayTransactionId("92704")
                .status(PaymentStatus.SUCCEEDED)
                .build();
        when(paymentRepository.findBySepayTransactionId("92704"))
                .thenReturn(Optional.of(completed));

        assertDoesNotThrow(() -> service.processWebhook(webhook(25000L)));

        verify(paymentRepository, never()).findByPaymentCodeForUpdate(anyString());
    }

    @Test
    void rejectsWebhookWhenAmountDoesNotExactlyMatch() {
        Fine fine = pendingFine();
        PaymentAttempt payment = pendingPayment(fine);

        when(paymentRepository.findBySepayTransactionId("92704")).thenReturn(Optional.empty());
        when(paymentRepository.findByPaymentCodeForUpdate("FINEABC123"))
                .thenReturn(Optional.of(payment));

        InvalidWebhookException exception = assertThrows(
                InvalidWebhookException.class,
                () -> service.processWebhook(webhook(24000L))
        );

        assertEquals("Transfer amount does not match the fine amount", exception.getMessage());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals(FineStatus.PENDING, fine.getStatus());
    }

    private Fine pendingFine() {
        return Fine.builder()
                .id(10)
                .amountDue(25000L)
                .status(FineStatus.PENDING)
                .studentId("a91940da-c7e0-477a-ba59-ca34756ced99")
                .studentEmail("phukak12345@gmail.com")
                .build();
    }

    private PaymentAttempt pendingPayment(Fine fine) {
        return PaymentAttempt.builder()
                .id(99L)
                .fine(fine)
                .paymentCode("FINEABC123")
                .amount(25000L)
                .currency("VND")
                .status(PaymentStatus.PENDING)
                .build();
    }

    private SepayWebhookRequest webhook(Long amount) {
        return new SepayWebhookRequest(
                92704L,
                "Vietcombank",
                "2026-06-21 10:00:00",
                "0010000000355",
                "",
                "FINEABC123",
                "FINEABC123 chuyen tien",
                "in",
                "NGUYEN VAN A chuyen tien",
                amount,
                100000L,
                "FT24012345678"
        );
    }
}

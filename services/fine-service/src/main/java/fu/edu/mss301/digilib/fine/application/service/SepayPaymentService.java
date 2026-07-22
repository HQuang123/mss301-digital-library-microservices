package fu.edu.mss301.digilib.fine.application.service;

import fu.edu.mss301.digilib.fine.api.dto.PaymentStatusResponse;
import fu.edu.mss301.digilib.fine.api.dto.SepayQrResponse;
import fu.edu.mss301.digilib.fine.api.dto.SepayWebhookRequest;
import fu.edu.mss301.digilib.fine.application.exception.BusinessConflictException;
import fu.edu.mss301.digilib.fine.application.exception.InvalidWebhookException;
import fu.edu.mss301.digilib.fine.application.exception.ResourceNotFoundException;
import fu.edu.mss301.digilib.fine.config.SepayProperties;
import fu.edu.mss301.digilib.fine.domain.entity.Fine;
import fu.edu.mss301.digilib.fine.domain.entity.PaymentAttempt;
import fu.edu.mss301.digilib.fine.domain.vo.FineStatus;
import fu.edu.mss301.digilib.fine.domain.vo.PaymentProvider;
import fu.edu.mss301.digilib.fine.domain.vo.PaymentStatus;
import fu.edu.mss301.digilib.fine.infrastructure.adapter.NotificationClientAdapter;
import fu.edu.mss301.digilib.fine.infrastructure.persistence.FineJpaRepository;
import fu.edu.mss301.digilib.fine.infrastructure.persistence.PaymentAttemptJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SepayPaymentService {

    private static final Set<PaymentStatus> ACTIVE_PAYMENT_STATUSES = Set.of(PaymentStatus.CREATED,
            PaymentStatus.PENDING);

    private final FineJpaRepository fineRepository;
    private final PaymentAttemptJpaRepository paymentRepository;
    private final SepayProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom;
    private final NotificationClientAdapter notificationClient;

    @Autowired
    public SepayPaymentService(
            FineJpaRepository fineRepository,
            PaymentAttemptJpaRepository paymentRepository,
            SepayProperties properties,
            Clock clock,
            NotificationClientAdapter notificationClient) {
        this(fineRepository, paymentRepository, properties, clock, new SecureRandom(), notificationClient);
    }

    SepayPaymentService(
            FineJpaRepository fineRepository,
            PaymentAttemptJpaRepository paymentRepository,
            SepayProperties properties,
            Clock clock,
            SecureRandom secureRandom,
            NotificationClientAdapter notificationClient) {
        this.fineRepository = fineRepository;
        this.paymentRepository = paymentRepository;
        this.properties = properties;
        this.clock = clock;
        this.secureRandom = secureRandom;
        this.notificationClient = notificationClient;
    }

    @Transactional
    public SepayQrResponse createQr(Integer fineId) {
        properties.validateQrConfiguration();

        Fine fine = fineRepository.findByIdForUpdate(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine " + fineId + " was not found"));

        validatePayableFine(fine);

        LocalDateTime now = LocalDateTime.now(clock);
        paymentRepository.expireActiveAttempts(
                fineId,
                ACTIVE_PAYMENT_STATUSES,
                PaymentStatus.EXPIRED,
                now);

        String paymentCode = generatePaymentCode();
        LocalDateTime expiresAt = now.plus(properties.paymentTtl());

        PaymentAttempt payment = PaymentAttempt.builder()
                .fine(fine)
                .paymentCode(paymentCode)
                .amount(fine.getAmountDue())
                .currency("VND")
                .provider(PaymentProvider.SEPAY)
                .status(PaymentStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        payment = paymentRepository.save(payment);

        return toQrResponse(payment);
    }

    @Transactional
    public PaymentStatusResponse getLatestPaymentStatus(Integer fineId) {
        PaymentAttempt payment = paymentRepository.findFirstByFine_IdOrderByCreatedAtDesc(fineId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No payment attempt exists for fine " + fineId));

        LocalDateTime now = LocalDateTime.now(clock);
        if (payment.getStatus() == PaymentStatus.PENDING
                && payment.getExpiresAt() != null
                && now.isAfter(payment.getExpiresAt())) {
            payment.markExpired();
        }

        return toStatusResponse(payment);
    }

    @Transactional
    public void processWebhook(SepayWebhookRequest webhook) {
        validateWebhookPayload(webhook);

        String transactionId = webhook.id().toString();
        if (paymentRepository.findBySepayTransactionId(transactionId).isPresent()) {
            return;
        }

        String paymentCode = resolvePaymentCode(webhook);
        PaymentAttempt payment = paymentRepository.findByPaymentCodeForUpdate(paymentCode)
                .orElseThrow(() -> new InvalidWebhookException(
                        "Unknown payment code: " + paymentCode));

        if (payment.getSepayTransactionId() != null) {
            if (payment.getSepayTransactionId().equals(transactionId)) {
                return;
            }
            throw new InvalidWebhookException("Payment attempt was already completed");
        }

        Fine fine = payment.getFine();
        validatePaymentMatch(payment, fine, webhook);

        LocalDateTime paidAt = LocalDateTime.now(clock);
        payment.markSucceeded(transactionId, webhook.referenceCode(), paidAt);
        fine.markPaid(paidAt);

        notificationClient.sendFinePaidConfirmation(
                fine.getId(), fine.getStudentId(), fine.getStudentEmail(), payment.getAmount(), paidAt.toString());
    }

    private String resolvePaymentCode(SepayWebhookRequest webhook) {
        // SePay's own "code" field is auto-extracted from the bank content and
        // truncates at the first non-digit, cutting our hex codes short (e.g.
        // "DH020CBBC6A2EA" -> "DH020"). The raw content still carries the full
        // code, so prefer extracting it from there and fall back to SePay's
        // "code" field only if the expected pattern isn't found.
        String fromContent = extractPaymentCodeFromContent(webhook.content());
        if (fromContent != null) {
            return fromContent;
        }
        return webhook.code() == null ? "" : webhook.code().trim();
    }

    private String extractPaymentCodeFromContent(String content) {
        if (content == null) {
            return null;
        }
        String prefix = properties.paymentCodePrefix().toUpperCase(Locale.ROOT);
        Pattern pattern = Pattern.compile(Pattern.quote(prefix) + "[0-9A-F]{12}");
        Matcher matcher = pattern.matcher(content.toUpperCase(Locale.ROOT));
        return matcher.find() ? matcher.group() : null;
    }

    private void validatePayableFine(Fine fine) {
        if (fine.getStatus() != FineStatus.PENDING) {
            throw new BusinessConflictException("Only a pending fine can be paid");
        }
        if (fine.getAmountDue() == null || fine.getAmountDue() <= 0) {
            throw new BusinessConflictException("Fine amount must be greater than zero");
        }
    }

    private void validateWebhookPayload(SepayWebhookRequest webhook) {
        properties.validateWebhookConfiguration();

        if (webhook == null || webhook.id() == null || webhook.id() <= 0) {
            throw new InvalidWebhookException("SePay transaction id is required");
        }
        if (resolvePaymentCode(webhook).isBlank()) {
            throw new InvalidWebhookException("Payment code is required");
        }
        if (!"in".equalsIgnoreCase(webhook.transferType())) {
            throw new InvalidWebhookException("Only incoming transfers can complete a payment");
        }
        if (webhook.transferAmount() == null || webhook.transferAmount() <= 0) {
            throw new InvalidWebhookException("Transfer amount must be greater than zero");
        }
        if (!properties.accountNumber().equals(webhook.accountNumber())) {
            throw new InvalidWebhookException("Webhook account number does not match");
        }
    }

    private void validatePaymentMatch(
            PaymentAttempt payment,
            Fine fine,
            SepayWebhookRequest webhook) {
        if (payment.getStatus() != PaymentStatus.PENDING
                && payment.getStatus() != PaymentStatus.EXPIRED) {
            throw new InvalidWebhookException("Payment attempt is not awaiting payment");
        }
        if (fine.getStatus() != FineStatus.PENDING) {
            throw new InvalidWebhookException("Fine is no longer pending");
        }
        if (!payment.getAmount().equals(webhook.transferAmount())) {
            throw new InvalidWebhookException("Transfer amount does not match the fine amount");
        }
        if (!payment.getAmount().equals(fine.getAmountDue())) {
            throw new InvalidWebhookException(
                    "Fine amount changed after this QR code was generated");
        }
    }

    private SepayQrResponse toQrResponse(PaymentAttempt payment) {
        String qrUrl = UriComponentsBuilder.fromUriString(properties.qrBaseUrl())
                .queryParam("acc", properties.accountNumber())
                .queryParam("bank", properties.bank())
                .queryParam("amount", payment.getAmount())
                .queryParam("des", payment.getPaymentCode())
                .build()
                .encode()
                .toUriString();

        return new SepayQrResponse(
                payment.getId(),
                payment.getFine().getId(),
                payment.getPaymentCode(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                properties.bank(),
                properties.accountNumber(),
                properties.accountName(),
                payment.getPaymentCode(),
                qrUrl,
                payment.getExpiresAt());
    }

    private PaymentStatusResponse toStatusResponse(PaymentAttempt payment) {
        return new PaymentStatusResponse(
                payment.getId(),
                payment.getFine().getId(),
                payment.getPaymentCode(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getFine().getStatus(),
                payment.getPaidAt(),
                payment.getExpiresAt());
    }

    private String generatePaymentCode() {
        byte[] randomBytes = new byte[6];
        secureRandom.nextBytes(randomBytes);
        return properties.paymentCodePrefix().toUpperCase(Locale.ROOT)
                + HexFormat.of().withUpperCase().formatHex(randomBytes);
    }
}

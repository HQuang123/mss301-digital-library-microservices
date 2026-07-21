package fu.edu.mss301.digilib.fine.domain.aggregate;

import fu.edu.mss301.digilib.fine.domain.entity.Fine;
import fu.edu.mss301.digilib.fine.domain.entity.FinePolicy;
import fu.edu.mss301.digilib.fine.domain.vo.FineRange;
import fu.edu.mss301.digilib.fine.domain.vo.FineReason;
import fu.edu.mss301.digilib.fine.domain.vo.FineStatus;

import java.time.LocalDateTime;
import java.util.Objects;

public final class FineAggregate {

    private final FinePolicy policy;

    private FineAggregate(FinePolicy policy) {
        this.policy = policy;
    }

    public static FineAggregate create(FinePolicy policy) {
        validatePolicy(policy);

        return new FineAggregate(policy);
    }

    public Fine createFineFor(
            Long loanId,
            String studentId,
            String studentEmail,
            LocalDateTime dueDate,
            LocalDateTime returnDate,
            FineReason reason,
            Long compensationAmount
    ) {
        validatePolicy(policy);
        Long validLoanId = requirePositive(loanId, "loanId");
        String validStudentId = requireText(studentId, "studentId");
        FineReason validReason = requireReason(reason);
        long validCompensationAmount = requireNonNegative(
                compensationAmount != null ? compensationAmount : 0L, "compensationAmount");
        FineRange range = FineRange.create(dueDate, returnDate);

        return Fine.builder()
                .finePolicy(policy)
                .loanId(validLoanId)
                .studentId(validStudentId)
                .studentEmail(studentEmail == null || studentEmail.isBlank() ? null : studentEmail.trim())
                .reason(validReason)
                .dueDate(range.getDueDate())
                .returnDate(range.getReturnDate())
                .compensationAmount(validCompensationAmount)
                .amountDue(calculateAmount(range, validCompensationAmount))
                .status(FineStatus.PENDING)
                .build();
    }

    public void recalculate(Fine fine, LocalDateTime returnDate, Long compensationAmount) {
        Fine validFine = requirePolicyFine(fine);
        long validCompensationAmount = requireNonNegative(
                compensationAmount != null ? compensationAmount : 0L, "compensationAmount");
        FineRange range = FineRange.create(validFine.getDueDate(), returnDate);

        validFine.setReturnDate(range.getReturnDate());
        validFine.setCompensationAmount(validCompensationAmount);
        validFine.setAmountDue(calculateAmount(range, validCompensationAmount));
    }

    public void markPaid(Fine fine) {
        Fine validFine = requirePolicyFine(fine);

        validFine.markPaid(LocalDateTime.now());
    }

    public void waive(Fine fine, String waiverReason) {
        Fine validFine = requirePolicyFine(fine);
        String validWaiverReason = requireText(waiverReason, "waiverReason");

        validFine.setStatus(FineStatus.WAIVED);
        validFine.setPaidAt(null);
        validFine.setWaiverReason(validWaiverReason);
    }

    public Integer getId() {
        return policy.getId();
    }

    public Long getDailyRate() {
        return policy.getDailyRate();
    }

    public Integer getLostThresholdDays() {
        return policy.getLostThresholdDays();
    }

    public Long getLostPenalty() {
        return policy.getLostPenalty();
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(policy.getIsActive());
    }

    private Long calculateAmount(FineRange range, long compensationAmount) {
        long overdueDays = range.overdueDays();
        long amount;

        try {
            amount = Math.multiplyExact(overdueDays, policy.getDailyRate());
        } catch (ArithmeticException exception) {
            throw new IllegalStateException("calculated fine amount exceeds supported range", exception);
        }

        if (range.isLost(policy.getLostThresholdDays())) {
            try {
                amount = Math.addExact(amount, policy.getLostPenalty());
            } catch (ArithmeticException exception) {
                throw new IllegalStateException("calculated fine amount exceeds supported range", exception);
            }
        }

        try {
            amount = Math.addExact(amount, compensationAmount);
        } catch (ArithmeticException exception) {
            throw new IllegalStateException("calculated fine amount exceeds supported range", exception);
        }

        return amount;
    }

    private Fine requirePolicyFine(Fine fine) {
        if (fine == null) {
            throw new IllegalArgumentException("fine must not be null");
        }

        FinePolicy finePolicy = fine.getFinePolicy();
        if (finePolicy == null) {
            throw new IllegalArgumentException("fine policy must not be null");
        }

        if (!isSamePolicy(finePolicy)) {
            throw new IllegalArgumentException("fine does not belong to this policy");
        }

        return fine;
    }

    private boolean isSamePolicy(FinePolicy otherPolicy) {
        Integer policyId = policy.getId();
        Integer otherPolicyId = otherPolicy.getId();

        if (policyId != null && otherPolicyId != null) {
            return Objects.equals(policyId, otherPolicyId);
        }

        return policy == otherPolicy;
    }

    private static void validatePolicy(FinePolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("finePolicy must not be null");
        }

        requireNonNegative(policy.getDailyRate(), "dailyRate");
        requirePositive(policy.getLostThresholdDays(), "lostThresholdDays");
        requireNonNegative(policy.getLostPenalty(), "lostPenalty");

        if (!Boolean.TRUE.equals(policy.getIsActive())) {
            throw new IllegalStateException("finePolicy must be active");
        }
    }

    private static Integer requirePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }

        return value;
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }

        return value;
    }

    private static FineReason requireReason(FineReason reason) {
        if (reason == null) {
            throw new IllegalArgumentException("reason must not be null");
        }

        return reason;
    }

    private static Long requireNonNegative(Long value, String fieldName) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }

        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return value.trim();
    }
}

package fu.edu.mss301.digilib.loan.api.dto;

public record BorrowEligibilityResponse(
        boolean eligible,
        long activeLoans,
        int borrowingLimit,
        long remainingSlots,
        int loanPeriodDays,
        String message
) {
}

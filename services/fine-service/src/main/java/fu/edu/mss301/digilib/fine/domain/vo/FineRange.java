package fu.edu.mss301.digilib.fine.domain.vo;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public final class FineRange {

    private final LocalDateTime dueDate;
    private final LocalDateTime returnDate;

    private FineRange(LocalDateTime dueDate, LocalDateTime returnDate) {
        this.dueDate = dueDate;
        this.returnDate = returnDate;
    }

    public static FineRange create(LocalDateTime dueDate, LocalDateTime returnDate) {
        if (dueDate == null) {
            throw new IllegalArgumentException("dueDate must not be null");
        }

        if (returnDate != null && returnDate.isBefore(dueDate)) {
            throw new IllegalArgumentException("returnDate must not be before dueDate");
        }

        return new FineRange(dueDate, returnDate);
    }

    public long overdueDays() {
        LocalDateTime endDate = returnDate != null ? returnDate : LocalDateTime.now();
        long days = ChronoUnit.DAYS.between(dueDate.toLocalDate(), endDate.toLocalDate());

        return Math.max(days, 0);
    }

    public boolean isOverdue() {
        return overdueDays() > 0;
    }

    public boolean isLost(Integer lostThresholdDays) {
        if (lostThresholdDays == null || lostThresholdDays <= 0) {
            return false;
        }

        return overdueDays() >= lostThresholdDays;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public LocalDateTime getReturnDate() {
        return returnDate;
    }
}

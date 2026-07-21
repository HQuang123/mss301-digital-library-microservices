package fu.edu.mss301.digilib.fine.api.dto;

public record BorrowEligibilityResponse(boolean canBorrow, String reason) {
}

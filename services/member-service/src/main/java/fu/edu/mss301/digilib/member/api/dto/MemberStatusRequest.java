package fu.edu.mss301.digilib.member.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MemberStatusRequest(
        @NotBlank(message = "Status cannot be blank")
        @Pattern(regexp = "^(?i)(UNLOCKED|SOFT_LOCKED|LOCKED)$", message = "Status must be UNLOCKED, SOFT_LOCKED, or LOCKED")
        String status
) {
}

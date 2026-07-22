package fu.edu.mss301.digilib.member.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Self-service onboarding payload. Lets a newly registered user select whether
 * they are a {@code student} or {@code lecturer}; the choice is mirrored into
 * both the Keycloak realm role and the profile's {@code memberType}.
 */
public record RoleSelectionRequest(
        @NotBlank(message = "Role cannot be blank")
        @Pattern(regexp = "^(?i)(student|lecturer)$", message = "Role must be 'student' or 'lecturer'")
        String role
) {
}
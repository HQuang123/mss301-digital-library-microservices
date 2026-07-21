package fu.edu.mss301.digilib.fine.api.dto;

import jakarta.validation.constraints.NotBlank;

public record WaiveFineRequest(@NotBlank String waiverReason) {
}

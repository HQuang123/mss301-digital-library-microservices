package fu.edu.mss301.digilib.member.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record OAuth2ExchangeRequest(

        @NotBlank(message = "Authorization code is required")
        String code,

        @NotBlank(message = "Code verifier is required")
        String codeVerifier,

        @NotBlank(message = "Redirect URI is required")
        String redirectUri
) {}

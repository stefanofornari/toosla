package ste.toosla.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Represents a login request.
 *
 * @param credentials The user's credentials, typically a base64-encoded string.
 */
public record LoginRequest(
    @NotBlank(message = "Credentials cannot be blank")
    String credentials
) {
}
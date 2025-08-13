package ste.toosla.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Credentials cannot be blank")
    String credentials
) {
}
package ste.toosla.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ReadRequest(@NotBlank String path) {
}

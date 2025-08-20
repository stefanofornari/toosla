package ste.toosla.api.dto;

import jakarta.validation.constraints.NotBlank;

public record WriteRequest(
    @NotBlank String path,
    @NotBlank String content
) {}

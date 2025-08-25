package ste.toosla.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Represents a request to write a file to storage.
 *
 * @param path The path to the file to write.
 * @param content The content of the file to write.
 */
public record WriteRequest(
    @NotBlank String path,
    @NotBlank String content
) {}

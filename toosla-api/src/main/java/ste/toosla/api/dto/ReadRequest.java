package ste.toosla.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Represents a request to read a file from storage.
 *
 * @param path The path to the file to read.
 */
public record ReadRequest(@NotBlank String path) {
}

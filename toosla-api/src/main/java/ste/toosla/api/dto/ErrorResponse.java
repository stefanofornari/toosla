package ste.toosla.api.dto;

/**
 * Represents an error response from the API.
 */
public class ErrorResponse {

    private boolean success;
    private String message;
    private String details;

    public ErrorResponse(String message, String details) {
        this.success = false;
        this.message = message;
        this.details = details;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }
}

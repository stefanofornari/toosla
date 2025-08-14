package ste.toosla.api;

import java.util.logging.Logger;
import java.util.logging.Level;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ste.toosla.api.dto.ErrorResponse;

import java.security.GeneralSecurityException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class.getName());

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return buildErrorResponse(ex, "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR, Level.SEVERE);
    }

    @ExceptionHandler(GeneralSecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(GeneralSecurityException ex) {
        return buildErrorResponse(ex, "Authentication failed", HttpStatus.UNAUTHORIZED, Level.SEVERE);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return buildErrorResponse(ex, "Malformed JSON input", HttpStatus.BAD_REQUEST, Level.WARNING);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        LOG.warning(() -> "Validation error");
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage()) // Get only the default message
                .collect(Collectors.joining(", "));
        ErrorResponse error = new ErrorResponse(
            errors, // Use the specific error messages from the annotations as the main message
            "Validation failed for input fields" // Provide a more general detail
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(Exception ex, String message, HttpStatus status, Level level) {
        LOG.log(level, ex, () -> message);
        ErrorResponse error = new ErrorResponse(
            message,
            ex.getMessage()
        );
        return new ResponseEntity<>(error, status);
    }
}

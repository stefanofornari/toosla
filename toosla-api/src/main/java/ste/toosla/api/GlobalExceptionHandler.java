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
        LOG.log(Level.SEVERE, ex, () -> "Unexpected exception occurred");
        ErrorResponse error = new ErrorResponse(
            "An unexpected error occurred",
            ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(GeneralSecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(GeneralSecurityException ex) {
        LOG.log(Level.SEVERE, ex, () -> "Security Exception occurred");
        ErrorResponse error = new ErrorResponse(
            "Authentication failed",
            ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        LOG.warning(() -> "Malformed JSON input");
        ErrorResponse error = new ErrorResponse(
            "Malformed JSON input",
            ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
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
}

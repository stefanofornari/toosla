package ste.toosla.api;

import com.fasterxml.jackson.core.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unexpected exception occurred", ex);
        ErrorResponse error = new ErrorResponse(
            "An unexpected error occurred",
            ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(GeneralSecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(GeneralSecurityException ex) {
        logger.error("Security Exception occurred", ex);
        ErrorResponse error = new ErrorResponse(
            "Authentication failed",
            ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        logger.error("Malformed JSON input", ex);
        ErrorResponse error = new ErrorResponse(
            "Malformed JSON input",
            ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        logger.error("Validation error", ex);
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

package ste.toosla.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import ste.toosla.api.dto.ErrorResponse;

import java.security.GeneralSecurityException;

import static org.assertj.core.api.BDDAssertions.then;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    public void handleGenericException() {
        Exception ex = new Exception("Generic error");
        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleGenericException(ex);

        then(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        then(responseEntity.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        then(responseEntity.getBody().getDetails()).isEqualTo("Generic error");
    }

    @Test
    public void handleSecurityException() {
        GeneralSecurityException ex = new GeneralSecurityException("Security error");
        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleSecurityException(ex);

        then(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        then(responseEntity.getBody().getMessage()).isEqualTo("Authentication failed");
        then(responseEntity.getBody().getDetails()).isEqualTo("Security error");
    }

    @Test
    public void handleHttpMessageNotReadableException() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Malformed JSON");
        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleHttpMessageNotReadableException(ex);

        then(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        then(responseEntity.getBody().getMessage()).isEqualTo("Malformed JSON input");
        then(responseEntity.getBody().getDetails()).isEqualTo("Malformed JSON");
    }

    @Test
    public void handleMethodArgumentNotValidException() {
        BindingResult bindingResult = new BeanPropertyBindingResult(null, "objectName");
        bindingResult.addError(new FieldError("objectName", "fieldName", "Name cannot be empty"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleMethodArgumentNotValidException(ex);

        then(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        then(responseEntity.getBody().getMessage()).isEqualTo("Name cannot be empty");
        then(responseEntity.getBody().getDetails()).isEqualTo("Validation failed for input fields");
    }
}

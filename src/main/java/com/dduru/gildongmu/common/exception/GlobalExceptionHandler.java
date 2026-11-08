package com.dduru.gildongmu.common.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("Business Exception: code={}, message={}", errorCode.name(), e.getMessage());
        ErrorResponse response = ErrorResponse.of(errorCode, e.getMessage());
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(errorCode.getStatus());
        if (e instanceof RetryableBusinessException retryable && retryable.getRetryAfterSeconds() != null) {
            builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(retryable.getRetryAfterSeconds()));
        }
        return builder.body(response);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(BindException e) {
        log.warn("Validation Exception: {}", e.getMessage());
        return ResponseEntity.badRequest().body(buildValidationErrorResponse(e.getBindingResult()));
    }

    private ErrorResponse buildValidationErrorResponse(BindingResult bindingResult) {
        FieldError fieldError = bindingResult.getFieldError();
        if (fieldError != null) {
            return buildFieldErrorResponse(fieldError);
        }

        ObjectError globalError = bindingResult.getGlobalError();
        if (globalError != null) {
            return ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, globalError.getDefaultMessage());
        }

        return ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE);
    }

    private ErrorResponse buildFieldErrorResponse(FieldError fieldError) {
        return ErrorResponse.ofField(
                ErrorCode.INVALID_INPUT_VALUE,
                fieldError.getField(),
                fieldError.getDefaultMessage()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("Constraint Violation Exception: {}", e.getMessage());

        ConstraintViolation<?> violation = e.getConstraintViolations().stream()
                .findFirst()
                .orElse(null);

        String message = violation != null ? violation.getMessage() : "잘못된 요청입니다.";

        String field = null;
        if (violation != null) {
            String path = violation.getPropertyPath().toString();
            field = path.contains(".") ? path.substring(path.lastIndexOf(".") + 1) : path;
        }

        ErrorResponse response = ErrorResponse.ofField(ErrorCode.INVALID_INPUT_VALUE, field, message);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal Argument Exception: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 요청 본문(JSON) 역직렬화 실패 시 처리.
     * Spring이 Jackson의 InvalidFormatException을 HttpMessageNotReadableException으로 감싸서 전달함.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        Throwable cause = e.getCause();
        if (cause instanceof InvalidFormatException ife) {
            return handleInvalidFormatException(ife);
        }
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, "요청 본문 형식이 올바르지 않습니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    private ResponseEntity<ErrorResponse> handleInvalidFormatException(InvalidFormatException e) {
        String fieldName = e.getPath().isEmpty()
                ? "request"
                : e.getPath().get(e.getPath().size() - 1).getFieldName();

        log.warn("Invalid Format Exception: field={}, value={}, targetType={}",
                fieldName, e.getValue(), e.getTargetType().getSimpleName());

        String message;
        if (e.getTargetType().isEnum()) {
            Object[] enumConstants = e.getTargetType().getEnumConstants();
            String allowed = java.util.stream.Stream.of(enumConstants)
                    .map(Object::toString)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            message = String.format("%s은(는) [%s] 중 하나여야 합니다. 받은 값: %s", fieldName, allowed, e.getValue());
        } else {
            message = String.format("필드 '%s'의 값 '%s' 형식이 올바르지 않습니다.", fieldName, e.getValue());
        }

        ErrorResponse response = ErrorResponse.ofField(ErrorCode.INVALID_INPUT_VALUE, fieldName, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFoundException(Exception e) {
        log.warn("Endpoint Not Found: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(ErrorCode.NOT_FOUND, "요청한 API를 찾을 수 없습니다.");
        return ResponseEntity.status(ErrorCode.NOT_FOUND.getStatus()).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("Method Not Supported: {}", e.getMessage());
        String message = "지원하지 않는 HTTP 메서드입니다: " + e.getMethod();
        ErrorResponse response = ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED, message);
        return ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOWED.getStatus()).body(response);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("Media Type Not Supported: {}", e.getMessage());

        String contentType = e.getContentType() == null ? "unknown" : e.getContentType().toString();
        String supportedTypes = e.getSupportedMediaTypes().stream()
                .map(MediaType::toString)
                .reduce((a, b) -> a + ", " + b)
                .orElse(MediaType.APPLICATION_JSON_VALUE);
        String message = "지원하지 않는 Content-Type입니다: %s. 지원 형식: %s".formatted(contentType, supportedTypes);

        ErrorResponse response = ErrorResponse.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE, message);
        return ResponseEntity.status(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected Exception: ", e);
        ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.internalServerError().body(response);
    }
}

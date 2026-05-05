package com.ccp.skAI_castle_server.exception;

import com.ccp.skAI_castle_server.dto.response.ApiResponse;
import com.ccp.skAI_castle_server.dto.response.ApiResultCode;
import com.ccp.skAI_castle_server.dto.response.ValidationErrorDetail;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Centralized exception handling hub for the application.
 * Intercepts various exceptions thrown by Controllers and converts them
 * into a standardized ApiResponse format for the client.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ================================================================
    // === Business Logic & Custom Exceptions
    // ================================================================

    /**
     * Handles custom business exceptions (ApiException).
     * These are intentional errors thrown during business logic.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException e) {
        log.warn("Business Exception: code={}, message={}", e.getResultCode(), e.getMessage());
        return ApiResponse.Rest.fail(e.getResultCode(), e.getMessage());
    }


    // ================================================================
    // === Validation & Input Errors
    // ================================================================

    /**
     * Handles validation failures (@Valid, @Validated).
     * Extracts field-specific or global error details from the binding result.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ValidationErrorDetail>>> handleValidationException(MethodArgumentNotValidException e) {
        List<ValidationErrorDetail> details = extractErrors(e);
        log.warn("Validation Failed: {}", details);
        return ApiResponse.Rest.failWithDetails(ApiResultCode.INVALID_INPUT, details);
    }

    /**
     * Handles illegal arguments, often thrown by Record's Compact Constructors
     * or manual validation checks in business logic.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal Argument: {}", e.getMessage());
        return ApiResponse.Rest.fail(ApiResultCode.INVALID_INPUT, e.getMessage());
    }

    /**
     * Handles constraint violations (e.g., @Min, @Max on @RequestParam or @PathVariable).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<ValidationErrorDetail>>> handleConstraintViolationException(ConstraintViolationException e) {
        List<ValidationErrorDetail> details = e.getConstraintViolations().stream()
                .map(violation -> ValidationErrorDetail.of(
                        violation.getPropertyPath().toString(),
                        Objects.requireNonNullElse(violation.getMessage(), "Invalid value")
                ))
                .collect(Collectors.toList());
        log.warn("Constraint Violation: {}", details);
        return ApiResponse.Rest.failWithDetails(ApiResultCode.INVALID_INPUT, details);
    }

    /**
     * Handles validation failures for method parameters annotated with constraints
     * This exception is thrown by Spring Boot 3.2+ (Spring Framework 6.1+) for parameter-level validation,
     * replacing the older ConstraintViolationException in many cases.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<List<ValidationErrorDetail>>>
    handleHandlerMethodValidationException(HandlerMethodValidationException e) {

        List<ValidationErrorDetail> details = e.getAllErrors().stream()
                .map(error -> {
                    String field = (error instanceof FieldError fe)
                            ? fe.getField()
                            : ValidationErrorDetail.GLOBAL_FIELD;

                    String msg = Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value");

                    return ValidationErrorDetail.of(field, msg);
                })
                .collect(Collectors.toList());

        log.warn("HandlerMethodValidation Failed: {}", details);
        return ApiResponse.Rest.failWithDetails(ApiResultCode.INVALID_INPUT, details);
    }

    /**
     * Handles parameter binding failures, similar to MethodArgumentNotValidException.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<List<ValidationErrorDetail>>> handleBindException(BindException e) {
        List<ValidationErrorDetail> details = extractErrors(e);
        log.warn("Binding Failed: {}", details);
        return ApiResponse.Rest.failWithDetails(ApiResultCode.INVALID_INPUT, details);
    }

    /**
     * Handles cases where the requested HTTP method is not supported (e.g., POST instead of GET).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("Method Not Supported: {}", e.getMessage());
        return ApiResponse.Rest.fail(ApiResultCode.METHOD_NOT_ALLOWED);
    }

    /**
     * Handles type mismatches for method arguments (e.g., passing a string to a numeric PathVariable).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("Type Mismatch: {}", e.getMessage());
        return ApiResponse.Rest.fail(ApiResultCode.INVALID_INPUT);
    }

    /**
     * Handles JSON deserialization errors or missing request bodies.
     * Specific cause extraction is added to capture validation failures inside Record constructors.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("JSON Parsing Error: {}", e.getMessage());
        Throwable cause = e.getMostSpecificCause();
        if (cause instanceof IllegalArgumentException iae) {
            return ApiResponse.Rest.fail(ApiResultCode.INVALID_INPUT, iae.getMessage());
        }
        return ApiResponse.Rest.fail(ApiResultCode.INVALID_INPUT);
    }

    /**
     * Handles cases where a required @RequestParam(required = true) or @RequestPart is missing.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("Missing Parameter: {}", e.getMessage());
        return ApiResponse.Rest.fail(ApiResultCode.INVALID_INPUT);
    }

    /**
     * Handles cases where the resource does not exist.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("Resource Not Found: {}", e.getMessage());
        return ApiResponse.Rest.fail(ApiResultCode.DATA_NOT_FOUND);
    }

    /**
     * Handles requests with unsupported Content-Type header.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("Media Type Not Supported: {}", e.getMessage());
        return ApiResponse.Rest.fail(ApiResultCode.UNSUPPORTED_MEDIA_TYPE);
    }

    /**
     * Handles file upload failures when the request body exceeds the configured maximum size.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("File Size Exceeded: {}", e.getMessage());
        return ApiResponse.Rest.fail(ApiResultCode.PAYLOAD_TOO_LARGE);
    }

    /**
     * Handles authentication failures due to incorrect credentials.
     * Uses enum message only for security reasons (no detailed error exposure).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("Authentication Failed: {}", e.getMessage());
        return ApiResponse.Rest.fail(ApiResultCode.INVALID_ID_OR_PASSWORD);
    }

    /**
     * Handles authorization failures when the authenticated user lacks required roles.
     * Uses enum message only for security reasons (no detailed error exposure).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access Denied: {}", e.getMessage());
        return ApiResponse.Rest.fail(ApiResultCode.ACCESS_DENIED);
    }

    /**
     * Handles response serialization failures (e.g., JSON writing error).
     * Uses enum message only for security reasons (no detailed error exposure).
     */
    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotWritableException(HttpMessageNotWritableException e) {
        log.error("Response Serialization Error: ", e);
        return ApiResponse.Rest.fail(ApiResultCode.INTERNAL_ERROR);
    }


    // ================================================================
    // === Global Fallback
    // ================================================================

    /**
     * Ultimate fallback for any unhandled or unexpected system exceptions.
     * Uses enum message only to avoid leaking internal stack traces.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled System Exception: ", e);
        return ApiResponse.Rest.fail(ApiResultCode.INTERNAL_ERROR);
    }


    // ================================================================
    // === Helper Methods
    // ================================================================

    /**
     * Extracts field errors and global errors from BindException into a list of ValidationErrorDetail.
     */
    private List<ValidationErrorDetail> extractErrors(BindException e) {
        return e.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fe) {
                        return ValidationErrorDetail.from(fe);
                    }
                    return ValidationErrorDetail.of(
                            ValidationErrorDetail.GLOBAL_FIELD,
                            Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value")
                    );
                })
                .collect(Collectors.toList());
    }
}
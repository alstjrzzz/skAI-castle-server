package com.ccp.skAI_castle_server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.validation.FieldError;

/**
 * Data Transfer Object (DTO) that holds detailed information about validation errors.
 * Used to provide specific field-level error messages to the client.
 */
@Getter
@Schema(description = "Detailed information about a specific validation failure")
public class ValidationErrorDetail {

    /**
     * Field name used when validation error is at class level
     * (e.g. @AssertTrue method validation, @Valid on nested object, etc.)
     */
    public static final String GLOBAL_FIELD = "global";

    @Schema(description = "The name of the field that failed validation. Returns 'global' for class-level errors.",
            example = "title")
    private final String field;

    @Schema(description = "The error message describing why the validation failed.",
            example = "must not be blank")
    private final String message;

    private ValidationErrorDetail(String field, String message) {
        this.field = field;
        this.message = message;
    }

    /**
     * Creates a ValidationErrorDetail from Spring's FieldError object.
     * Use this when handling MethodArgumentNotValidException or BindException.
     */
    public static ValidationErrorDetail from(FieldError fieldError) {
        return new ValidationErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
    }

    /**
     * Creates a ValidationErrorDetail with a specific field name and message.
     * Use GLOBAL_FIELD for class-level validation errors
     * (e.g. @Valid on DTO with @AssertTrue method).
     */
    public static ValidationErrorDetail of(String field, String message) {
        return new ValidationErrorDetail(field, message);
    }
}

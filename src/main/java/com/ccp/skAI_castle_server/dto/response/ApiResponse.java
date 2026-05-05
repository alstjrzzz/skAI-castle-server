package com.ccp.skAI_castle_server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

/**
 * Standardized API response wrapper used for all service responses.
 * Ensures a consistent JSON structure containing business codes, messages, and payloads.
 */
@Getter
@Schema(description = "Standardized API response wrapper used for all service responses")
public class ApiResponse<T> {

    @Schema(description = "Internal business logic code", example = "1000")
    private final int code;

    @Schema(description = "Response message for users or developers", example = "Request processed successfully.")
    private final String message;

    @Schema(description = "True if the request was successful, false otherwise", example = "true")
    private final boolean success;

    @Schema(description = "Response data payload.")
    private final T data;

    /**
     * Private constructor to enforce the use of static factory methods.
     */
    private ApiResponse(ApiResultCode resultCode, String customMessage, T data) {
        this.code = resultCode.getCode();
        this.message = (customMessage != null) ? customMessage : resultCode.getMessage();
        this.success = resultCode.isSuccess();
        this.data = data;
    }

    // ====================== REST ONLY ======================
    /**
     * Used in REST Controllers.
     * Returns a complete HTTP response wrapped in ResponseEntity.
     */
    public static class Rest {

        /**
         * Returns a success response with a specific result code and data payload.
         */
        public static <T> ResponseEntity<ApiResponse<T>> success(ApiResultCode resultCode, T data) {
            validateSuccessState(resultCode);
            return ResponseEntity.status(resultCode.getHttpStatus())
                    .body(new ApiResponse<>(resultCode, null, data));
        }

        /**
         * Returns a success response with a specific result code but no data payload.
         */
        public static ResponseEntity<ApiResponse<Void>> success(ApiResultCode resultCode) {
            return success(resultCode, null);
        }

        /**
         * Returns a failure response with a specific result code.
         * The default message from ApiResultCode will be used.
         */
        public static ResponseEntity<ApiResponse<Void>> fail(ApiResultCode resultCode) {
            return fail(resultCode, null);
        }

        /**
         * Returns a failure response with a specific result code and a custom error message.
         */
        public static ResponseEntity<ApiResponse<Void>> fail(ApiResultCode resultCode, String customMessage) {
            validateFailureState(resultCode);
            return ResponseEntity.status(resultCode.getHttpStatus())
                    .body(new ApiResponse<>(resultCode, customMessage, null));
        }

        /**
         * Returns a failure response with additional error details (e.g., validation errors).
         * The errorDetails will be placed in the 'data' field.
         */
        public static <T> ResponseEntity<ApiResponse<T>> failWithDetails(ApiResultCode resultCode, T errorDetails) {
            validateFailureState(resultCode);
            return ResponseEntity.status(resultCode.getHttpStatus())
                    .body(new ApiResponse<>(resultCode, null, errorDetails));
        }
    }

    // ====================== Security ONLY ======================
    /**
     * Used in Spring Security filter chain, AuthenticationEntryPoint, and AccessDeniedHandler.
     * Returns only the pure ApiResponse object (no ResponseEntity).
     * Necessary because filters operate outside Spring MVC's DispatcherServlet,
     * making ResponseEntity unusable in this context.
     */
    public static class Security {

        /**
         * Returns a failure response with a specific result code.
         * The default message from ApiResultCode will be used.
         */
        public static <T> ApiResponse<T> fail(ApiResultCode resultCode) {
            validateFailureState(resultCode);
            return new ApiResponse<>(resultCode, null, null);
        }
    }

    // --- Helper Methods ---

    private static void validateSuccessState(ApiResultCode resultCode) {
        if (!resultCode.isSuccess()) {
            throw new IllegalArgumentException("Success factory used with error code: " + resultCode.name());
        }
    }

    private static void validateFailureState(ApiResultCode resultCode) {
        if (resultCode.isSuccess()) {
            throw new IllegalArgumentException("Failure factory used with success code: " + resultCode.name());
        }
    }
}

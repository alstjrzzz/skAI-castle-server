package com.ccp.skAI_castle_server.exception;

import lombok.Getter;
import com.ccp.skAI_castle_server.dto.response.ApiResultCode;

/**
 * Custom RuntimeException to handle business logic errors.
 * It wraps an ApiResultCode to provide structured error responses,
 * including business-specific codes and HTTP statuses.
 */
@Getter
public class ApiException extends RuntimeException {

    private final ApiResultCode resultCode;

    /**
     * Constructs a new ApiException with the default message from the result code.
     */
    public ApiException(ApiResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    /**
     * Constructs a new ApiException with a custom error message.
     */
    public ApiException(ApiResultCode resultCode, String customMessage) {
        super(customMessage);
        this.resultCode = resultCode;
    }

    /**
     * Constructs a new ApiException with the default message and the underlying cause.
     */
    public ApiException(ApiResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.resultCode = resultCode;
    }

    /**
     * Constructs a new ApiException with a custom message and the underlying cause.
     */
    public ApiException(ApiResultCode resultCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.resultCode = resultCode;
    }
}

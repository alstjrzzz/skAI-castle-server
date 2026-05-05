package com.ccp.skAI_castle_server.dto.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Defines business status and matching HTTP status.
 */
@Getter
public enum ApiResultCode {

    /**
     * 1000 ~ 1999: Success Responses
     */
    SUCCESS(1000, "Request processed successfully.", HttpStatus.OK),
    CREATED(1001, "Resource created successfully.", HttpStatus.CREATED),

    /**
     * 2000 ~ 2999: Client Input Errors
     */
    INVALID_INPUT(2001, "Invalid input value.", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS(2002, "Email already exists.", HttpStatus.CONFLICT),
    INVALID_ID_OR_PASSWORD(2003, "Invalid ID or password.", HttpStatus.UNAUTHORIZED),
    INVALID_CURRENT_PASSWORD(2004, "Current password does not match.", HttpStatus.UNAUTHORIZED),
    INVALID_VERIFICATION_CODE(2005, "Invalid verification code.", HttpStatus.BAD_REQUEST),
    METHOD_NOT_ALLOWED(2006, "Unsupported HTTP method.", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE(2007, "Unsupported media type.", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    PAYLOAD_TOO_LARGE(2008, "Payload too large. Maximum upload size exceeded.", HttpStatus.PAYLOAD_TOO_LARGE),
    SESSION_NOT_ACTIVE(2009, "Chat session is not active.", HttpStatus.CONFLICT),
    OUTLINE_ALREADY_EXISTS(2010, "Outline already exists for this topic.", HttpStatus.CONFLICT),
    EVALUATION_ALREADY_SUBMITTED(2011, "Evaluation has already been submitted.", HttpStatus.CONFLICT),

    /**
     * 3000 ~ 3999: Authentication and Authorization Errors
     */
    UNAUTHORIZED(3001, "Authentication is required.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(3002, "Token is invalid.", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(3003, "User authentication information is missing or invalid.", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN(3004, "Token has expired.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(3005, "Access denied.", HttpStatus.FORBIDDEN),

    /**
     * 4000 ~ 4999: Resource Status Errors
     */
    DATA_NOT_FOUND(4001, "Data not found.", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(4002, "User not found.", HttpStatus.NOT_FOUND),
    TOPIC_NOT_FOUND(4003, "Study topic not found.", HttpStatus.NOT_FOUND),
    SESSION_NOT_FOUND(4004, "Chat session not found.", HttpStatus.NOT_FOUND),
    EVALUATION_NOT_FOUND(4005, "Evaluation not found.", HttpStatus.NOT_FOUND),
    QUESTION_NOT_FOUND(4006, "Evaluation question not found.", HttpStatus.NOT_FOUND),
    NOTIFICATION_NOT_FOUND(4007, "Notification not found.", HttpStatus.NOT_FOUND),

    /**
     * 5000 ~ 5999: Internal Server Errors
     */
    INTERNAL_ERROR(5001, "Internal server error occurred.", HttpStatus.INTERNAL_SERVER_ERROR),
    EXTERNAL_API_ERROR(5002, "External API server error.", HttpStatus.BAD_GATEWAY);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ApiResultCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public boolean isSuccess() {
        return 1000 <= code && code < 2000;
    }
}
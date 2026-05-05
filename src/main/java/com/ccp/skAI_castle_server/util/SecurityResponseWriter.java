package com.ccp.skAI_castle_server.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ccp.skAI_castle_server.dto.response.ApiResponse;
import com.ccp.skAI_castle_server.dto.response.ApiResultCode;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Utility class for writing error responses directly to HttpServletResponse.
 *
 * Spring MVC exception handlers (e.g., @RestControllerAdvice) are not invoked
 * for errors that occur in the security filter chain, since the request never
 * reaches the DispatcherServlet. This class provides a way to write a structured
 * JSON error response directly to the response output stream from within filters
 * or security entry points.
 */

public class SecurityResponseWriter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void writeError(HttpServletResponse response, ApiResultCode resultCode) throws IOException {
        response.setStatus(resultCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.Security.fail(resultCode))
        );
    }
}
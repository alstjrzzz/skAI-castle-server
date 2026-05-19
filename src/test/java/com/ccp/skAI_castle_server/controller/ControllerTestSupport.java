package com.ccp.skAI_castle_server.controller;

import com.ccp.skAI_castle_server.security.PrincipalDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Shared utilities for @WebMvcTest controller tests.
 */
public abstract class ControllerTestSupport {

    protected static final String TEST_UUID = "550e8400-e29b-41d4-a716-446655440000";

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Returns a RequestPostProcessor that sets a PrincipalDetails authentication
     * directly in the SecurityContext — bypasses the JWT filter entirely.
     */
    protected static RequestPostProcessor mockAuth() {
        PrincipalDetails principal = PrincipalDetails.of(TEST_UUID, "USER");
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}

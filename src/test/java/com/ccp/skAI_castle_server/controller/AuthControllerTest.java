package com.ccp.skAI_castle_server.controller;

import com.ccp.skAI_castle_server.config.SecurityConfig;
import com.ccp.skAI_castle_server.dto.response.ApiResponse;
import com.ccp.skAI_castle_server.dto.response.TokenResponse;
import com.ccp.skAI_castle_server.security.JwtProvider;
import com.ccp.skAI_castle_server.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.ccp.skAI_castle_server.dto.response.ApiResultCode.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest extends ControllerTestSupport {

    @Autowired MockMvc mockMvc;
    @MockitoBean AuthService authService;
    @MockitoBean JwtProvider jwtProvider;

    @Test
    void register_validInput_returns201() throws Exception {
        given(authService.register("user@example.com", "password123", "TestUser"))
                .willReturn(ApiResponse.Rest.success(CREATED));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"password123","name":"TestUser"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void register_blankEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","password":"password123","name":"TestUser"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        given(authService.login(eq("user@example.com"), eq("password123"), any(HttpServletResponse.class)))
                .willReturn(ApiResponse.Rest.success(SUCCESS, new TokenResponse("jwt-access-token")));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("jwt-access-token"));
    }

    @Test
    void login_missingPassword_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_authenticated_returns200() throws Exception {
        given(authService.logout(any(HttpServletResponse.class)))
                .willReturn(ApiResponse.Rest.success(SUCCESS));

        mockMvc.perform(post("/auth/logout")
                        .with(mockAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void logout_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized());
    }
}


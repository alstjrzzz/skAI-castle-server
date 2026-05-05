package com.ccp.skAI_castle_server.controller;

import com.ccp.skAI_castle_server.api.AuthApi;
import com.ccp.skAI_castle_server.dto.request.LoginRequest;
import com.ccp.skAI_castle_server.dto.request.RegisterRequest;
import com.ccp.skAI_castle_server.dto.response.ApiResponse;
import com.ccp.skAI_castle_server.dto.response.TokenResponse;
import com.ccp.skAI_castle_server.security.PrincipalDetails;
import com.ccp.skAI_castle_server.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request.email(), request.password(), request.name());
    }

    @Override
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request,
                                                            HttpServletResponse response) {
        return authService.login(request.email(), request.password(), response);
    }

    @Override
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(HttpServletRequest request,
                                                              HttpServletResponse response) {
        return authService.reissue(request, response);
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal PrincipalDetails principal,
                                                    HttpServletResponse response) {
        return authService.logout(response);
    }
}

package com.ccp.skAI_castle_server.service;

import com.ccp.skAI_castle_server.domain.UserRole;
import com.ccp.skAI_castle_server.domain.entity.User;
import com.ccp.skAI_castle_server.dto.response.ApiResponse;
import com.ccp.skAI_castle_server.dto.response.TokenResponse;
import com.ccp.skAI_castle_server.exception.ApiException;
import com.ccp.skAI_castle_server.repository.UserRepository;
import com.ccp.skAI_castle_server.security.JwtProvider;
import com.ccp.skAI_castle_server.security.PrincipalDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

import static com.ccp.skAI_castle_server.dto.response.ApiResultCode.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Transactional
    public ResponseEntity<ApiResponse<Void>> register(String email, String password, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .nickname(name)
                .role(UserRole.USER)
                .build();
        userRepository.save(user);

        return ApiResponse.Rest.success(CREATED);
    }

    public ResponseEntity<ApiResponse<TokenResponse>> login(String email, String password,
                                                            HttpServletResponse response) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(INVALID_ID_OR_PASSWORD));

        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new ApiException(INVALID_ID_OR_PASSWORD);
        }

        PrincipalDetails principal = PrincipalDetails.of(
                user.getUuid().toString(), user.getRole().name());

        String jwtAccess = jwtProvider.generateAccessToken(principal);
        String jwtRefresh = jwtProvider.generateRefreshToken(user.getUuid().toString());

        setRefreshTokenCookie(response, jwtRefresh);

        return ApiResponse.Rest.success(SUCCESS, new TokenResponse(jwtAccess));
    }

    public ResponseEntity<ApiResponse<TokenResponse>> reissue(HttpServletRequest request,
                                                              HttpServletResponse response) {
        String refreshToken = extractRefreshTokenCookie(request);
        if (!StringUtils.hasText(refreshToken)) {
            throw new ApiException(INVALID_REFRESH_TOKEN);
        }

        Claims claims;
        try {
            jwtProvider.validateToken(refreshToken);
            claims = jwtProvider.parseClaims(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new ApiException(EXPIRED_TOKEN);
        } catch (JwtException e) {
            throw new ApiException(INVALID_TOKEN);
        }

        String uuid = claims.getSubject();

        User user = userRepository.findByUuid(UUID.fromString(uuid))
                .orElseThrow(() -> new ApiException(USER_NOT_FOUND));

        PrincipalDetails principal = PrincipalDetails.of(uuid, user.getRole().name());
        String newAccessToken = jwtProvider.generateAccessToken(principal);
        String newRefreshToken = jwtProvider.generateRefreshToken(uuid);

        setRefreshTokenCookie(response, newRefreshToken);

        return ApiResponse.Rest.success(SUCCESS, new TokenResponse(newAccessToken));
    }

    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        clearRefreshTokenCookie(response);
        return ApiResponse.Rest.success(SUCCESS);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .sameSite("Lax")
                .path(contextPath + "/auth")
                .maxAge(refreshExpiration / 1000)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .sameSite("Lax")
                .path(contextPath + "/auth")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extractRefreshTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}

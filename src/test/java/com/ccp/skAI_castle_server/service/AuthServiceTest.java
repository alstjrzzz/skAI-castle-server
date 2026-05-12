package com.ccp.skAI_castle_server.service;

import com.ccp.skAI_castle_server.domain.UserRole;
import com.ccp.skAI_castle_server.domain.entity.User;
import com.ccp.skAI_castle_server.dto.response.ApiResponse;
import com.ccp.skAI_castle_server.dto.response.TokenResponse;
import com.ccp.skAI_castle_server.exception.ApiException;
import com.ccp.skAI_castle_server.repository.UserRepository;
import com.ccp.skAI_castle_server.security.JwtProvider;
import com.ccp.skAI_castle_server.security.PrincipalDetails;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static com.ccp.skAI_castle_server.dto.response.ApiResultCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock JwtProvider jwtProvider;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks AuthService authService;

    @Test
    void register_success() {
        given(userRepository.existsByEmail("test@example.com")).willReturn(false);
        given(passwordEncoder.encode("password")).willReturn("encoded");

        ResponseEntity<ApiResponse<Void>> response =
                authService.register("test@example.com", "password", "TestUser");

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(CREATED.getCode());
        then(userRepository).should().save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsApiException() {
        given(userRepository.existsByEmail("dup@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register("dup@example.com", "password", "User"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getResultCode()).isEqualTo(EMAIL_ALREADY_EXISTS));
    }

    @Test
    void login_success() {
        User user = User.builder()
                .email("test@example.com")
                .password("encoded")
                .nickname("TestUser")
                .role(UserRole.USER)
                .build();

        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password", "encoded")).willReturn(true);
        given(jwtProvider.generateAccessToken(any(PrincipalDetails.class))).willReturn("access-token");
        given(jwtProvider.generateRefreshToken(anyString())).willReturn("refresh-token");

        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        ResponseEntity<ApiResponse<TokenResponse>> response =
                authService.login("test@example.com", "password", httpResponse);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().accessToken()).isEqualTo("access-token");
    }

    @Test
    void login_userNotFound_throwsApiException() {
        given(userRepository.findByEmail("none@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("none@example.com", "pw", mock(HttpServletResponse.class)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getResultCode()).isEqualTo(INVALID_ID_OR_PASSWORD));
    }

    @Test
    void login_wrongPassword_throwsApiException() {
        User user = User.builder()
                .email("test@example.com")
                .password("encoded")
                .nickname("TestUser")
                .role(UserRole.USER)
                .build();

        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongpw", "encoded")).willReturn(false);

        assertThatThrownBy(() -> authService.login("test@example.com", "wrongpw", mock(HttpServletResponse.class)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getResultCode()).isEqualTo(INVALID_ID_OR_PASSWORD));
    }

    @Test
    void logout_success() {
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);

        ResponseEntity<ApiResponse<Void>> response = authService.logout(httpResponse);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
    }
}

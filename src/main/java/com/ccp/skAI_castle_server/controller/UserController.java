package com.ccp.skAI_castle_server.controller;

import com.ccp.skAI_castle_server.domain.entity.User;
import com.ccp.skAI_castle_server.dto.response.ApiResponse;
import com.ccp.skAI_castle_server.dto.response.UserProfileResponse;
import com.ccp.skAI_castle_server.exception.ApiException;
import com.ccp.skAI_castle_server.repository.UserRepository;
import com.ccp.skAI_castle_server.security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.ccp.skAI_castle_server.dto.response.ApiResultCode.SUCCESS;
import static com.ccp.skAI_castle_server.dto.response.ApiResultCode.USER_NOT_FOUND;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMe(
            @AuthenticationPrincipal PrincipalDetails principal) {
        User user = userRepository.findByUuid(UUID.fromString(principal.getUuid()))
                .orElseThrow(() -> new ApiException(USER_NOT_FOUND));
        return ApiResponse.Rest.success(SUCCESS,
                new UserProfileResponse(user.getId(), user.getEmail(), user.getNickname()));
    }
}

package com.ccp.skAI_castle_server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "JWT access token issued upon successful authentication")
public record TokenResponse(String accessToken) {
}


package com.ccp.skAI_castle_server.api;

import com.ccp.skAI_castle_server.dto.request.LoginRequest;
import com.ccp.skAI_castle_server.dto.request.RegisterRequest;
import com.ccp.skAI_castle_server.dto.response.ApiResponse;
import com.ccp.skAI_castle_server.dto.response.TokenResponse;
import com.ccp.skAI_castle_server.security.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Auth", description = "Authentication operations")
@RequestMapping("/auth")
public interface AuthApi {

    @Operation(
            summary = "Register",
            description = "Creates a new user account with email and password."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Registration successful.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "code": 1001,
                                              "message": "Resource created successfully.",
                                              "success": true,
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Conflict: Email already exists.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "code": 2002,
                                              "message": "Email already exists.",
                                              "success": false,
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/register")
    ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request);

    @Operation(
            summary = "Login",
            description = "Authenticates with email and password. " +
                    "Issues a JWT access token (body) and refresh token (HttpOnly Set-Cookie)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful. Returns JWT access token in body; refresh token is set as HttpOnly cookie.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TokenResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "code": 1000,
                                              "message": "Request processed successfully.",
                                              "success": true,
                                              "data": {
                                                "accessToken": "eyJhbGciOiJIUzI1NiIs..."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized: Invalid email or password.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "code": 2003,
                                              "message": "Invalid ID or password.",
                                              "success": false,
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/login")
    ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request,
                                                     HttpServletResponse response);

    @Operation(
            summary = "Reissue tokens",
            description = "Validates the refresh token from the HttpOnly cookie and issues a new access token (body) " +
                    "and a new refresh token (HttpOnly Set-Cookie). No Authorization header required."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Token reissue successful.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TokenResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "code": 1000,
                                              "message": "Request processed successfully.",
                                              "success": true,
                                              "data": {
                                                "accessToken": "eyJhbGciOiJIUzI1NiIs..."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized: Refresh token cookie is missing, invalid, or expired.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "code": 3003,
                                              "message": "User authentication information is missing or invalid.",
                                              "success": false,
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/reissue")
    ResponseEntity<ApiResponse<TokenResponse>> reissue(HttpServletRequest request, HttpServletResponse response);

    @Operation(
            summary = "Logout",
            description = "Clears the refresh token cookie. The access token expires naturally.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Logout successful.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "code": 1000,
                                              "message": "Request processed successfully.",
                                              "success": true,
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized: Missing or invalid access token.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "code": 3001,
                                              "message": "Authentication is required.",
                                              "success": false,
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/logout")
    ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal PrincipalDetails principal,
                                             HttpServletResponse response);
}

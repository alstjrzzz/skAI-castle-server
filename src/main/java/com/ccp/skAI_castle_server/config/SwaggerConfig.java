package com.ccp.skAI_castle_server.config;

import com.ccp.skAI_castle_server.dto.response.ApiResultCode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String securitySchemeName = "BearerAuth";

        return new OpenAPI()

                // JWT Security Configuration
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your actual JWT token to test protected APIs.")))

                // Info & Business Codes Table
                .info(new Info()
                        .title("skAI castle API Specification")
                        .version("1.0.0")
                        .description(generateDescription()));
    }

    /**
     * Generates a Markdown table for business status codes from ApiResultCode enum.
     */
    private String generateDescription() {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("### Unified API Response Wrapper\n");
        sb.append("All responses are wrapped in the `ApiResponse` object, containing `code`, `message`, `success`, and `data`.\n\n");

        // Manual Group Descriptions
        sb.append("### Code Classification\n");
        sb.append("- **1000 ~ 1999**: Success responses.\n");
        sb.append("- **2000 ~ 2999**: Client-side input or logic errors.\n");
        sb.append("- **3000 ~ 3999**: Authentication and authorization failures.\n");
        sb.append("- **4000 ~ 4999**: Resource-related errors (e.g., Not Found).\n");
        sb.append("- **5000 ~ 5999**: Internal server-side issues.\n\n");

        // Automated Business Codes Table
        sb.append("### Business Status Codes\n");
        sb.append("| Code | Message | Success | HTTP Status |\n");
        sb.append("| :--- | :--- | :--- | :--- |\n");

        String tableRows = Arrays.stream(ApiResultCode.values())
                .map(rc -> String.format("| %d | %s | %b | %s |",
                        rc.getCode(),
                        rc.getMessage(),
                        rc.isSuccess(),
                        rc.getHttpStatus().name()))
                .collect(Collectors.joining("\n"));

        sb.append(tableRows);

        return sb.toString();
    }
}


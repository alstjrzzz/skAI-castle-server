package com.ccp.skAI_castle_server.api;

import com.ccp.skAI_castle_server.dto.response.ApiResponse;
import com.ccp.skAI_castle_server.dto.response.NotificationResponse;
import com.ccp.skAI_castle_server.security.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Notification", description = "User notifications for review reminders")
@RequestMapping("/api/notifications")
@SecurityRequirement(name = "BearerAuth")
public interface NotificationApi {

    @Operation(
            summary = "List my notifications",
            description = "Returns all notifications for the authenticated user, newest first. " +
                    "Notifications are generated daily at 09:00 by the review scheduler."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Notifications retrieved successfully.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 1000,
                                      "message": "Request processed successfully.",
                                      "success": true,
                                      "data": [
                                        {
                                          "id": 2,
                                          "message": "You have 3 review cards due today.",
                                          "link": "/reviews/today",
                                          "isRead": false,
                                          "createdAt": "2024-01-05T09:00:00"
                                        },
                                        {
                                          "id": 1,
                                          "message": "You have 1 review card due today.",
                                          "link": "/reviews/today",
                                          "isRead": true,
                                          "createdAt": "2024-01-04T09:00:00"
                                        }
                                      ]
                                    }
                                    """)
                    )
            )
    })
    @GetMapping
    ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal PrincipalDetails principal
    );

    @Operation(
            summary = "Mark notification as read",
            description = "Sets the is_read flag to true for the specified notification."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Notification marked as read.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 1000,
                                      "message": "Request processed successfully.",
                                      "success": true,
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Notification not found or does not belong to the authenticated user.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 4007,
                                      "message": "Notification not found.",
                                      "success": false,
                                      "data": null
                                    }
                                    """))
            )
    })
    @PatchMapping("/{notificationId}/read")
    ResponseEntity<ApiResponse<Void>> markAsRead(
            @Parameter(description = "Notification ID", example = "1") @PathVariable Long notificationId,
            @AuthenticationPrincipal PrincipalDetails principal
    );

    @Operation(
            summary = "Mark all notifications as read",
            description = "Sets is_read to true for all unread notifications belonging to the authenticated user."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "All notifications marked as read.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 1000,
                                      "message": "Request processed successfully.",
                                      "success": true,
                                      "data": null
                                    }
                                    """)
                    )
            )
    })
    @PatchMapping("/read-all")
    ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal PrincipalDetails principal
    );
}

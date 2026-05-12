package com.ccp.skAI_castle_server.service;

import com.ccp.skAI_castle_server.domain.UserRole;
import com.ccp.skAI_castle_server.domain.entity.Notification;
import com.ccp.skAI_castle_server.domain.entity.User;
import com.ccp.skAI_castle_server.dto.response.NotificationResponse;
import com.ccp.skAI_castle_server.exception.ApiException;
import com.ccp.skAI_castle_server.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static com.ccp.skAI_castle_server.dto.response.ApiResultCode.NOTIFICATION_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @InjectMocks NotificationService notificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com").password("encoded")
                .nickname("Test").role(UserRole.USER).build();
    }

    @Test
    void getNotifications_returnsMappedList() {
        Notification n = Notification.builder()
                .user(testUser)
                .message("오늘 복습할 카드가 3개 있습니다.")
                .link("/reviews/today")
                .build();
        ReflectionTestUtils.setField(n, "id", 1L);

        given(notificationRepository.findByUserOrderByCreatedAtDesc(testUser)).willReturn(List.of(n));

        List<NotificationResponse> result = notificationService.getNotifications(testUser);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessage()).isEqualTo("오늘 복습할 카드가 3개 있습니다.");
        assertThat(result.get(0).isRead()).isFalse();
    }

    @Test
    void getNotifications_emptyList_returnsEmpty() {
        given(notificationRepository.findByUserOrderByCreatedAtDesc(testUser)).willReturn(List.of());

        List<NotificationResponse> result = notificationService.getNotifications(testUser);

        assertThat(result).isEmpty();
    }

    @Test
    void markAsRead_success_marksNotification() {
        Notification n = Notification.builder()
                .user(testUser).message("test").link("/").build();
        ReflectionTestUtils.setField(n, "id", 1L);

        given(notificationRepository.findByIdAndUser(1L, testUser)).willReturn(Optional.of(n));

        notificationService.markAsRead(1L, testUser);

        assertThat(n.getIsRead()).isTrue();
    }

    @Test
    void markAsRead_notFound_throwsApiException() {
        given(notificationRepository.findByIdAndUser(99L, testUser)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(99L, testUser))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getResultCode()).isEqualTo(NOTIFICATION_NOT_FOUND));
    }

    @Test
    void markAllAsRead_callsBulkUpdate() {
        notificationService.markAllAsRead(testUser);

        then(notificationRepository).should().markAllAsReadByUser(testUser);
    }
}

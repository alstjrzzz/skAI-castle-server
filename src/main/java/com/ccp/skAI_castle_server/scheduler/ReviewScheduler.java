package com.ccp.skAI_castle_server.scheduler;

import com.ccp.skAI_castle_server.domain.entity.EvaluationQuestion;
import com.ccp.skAI_castle_server.domain.entity.Notification;
import com.ccp.skAI_castle_server.domain.entity.User;
import com.ccp.skAI_castle_server.repository.EvaluationQuestionRepository;
import com.ccp.skAI_castle_server.repository.NotificationRepository;
import com.ccp.skAI_castle_server.repository.UserRepository;
import com.ccp.skAI_castle_server.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewScheduler {

    private final EvaluationQuestionRepository evaluationQuestionRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendDailyReviewNotifications() {
        LocalDate today = LocalDate.now();
        log.info("Running daily review notification job for {}", today);

        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            List<EvaluationQuestion> dueCards = evaluationQuestionRepository.findDueForReview(user, today);
            if (dueCards.isEmpty()) continue;

            int count = dueCards.size();
            String message = "오늘 복습할 카드가 " + count + "개 있습니다.";

            Notification notification = Notification.builder()
                    .user(user)
                    .message(message)
                    .link("/reviews/today")
                    .build();
            notificationRepository.save(notification);

            mailService.sendReviewReminder(user.getEmail(), count);
            log.info("Sent review reminder to user {} ({} cards)", user.getEmail(), count);
        }
    }
}

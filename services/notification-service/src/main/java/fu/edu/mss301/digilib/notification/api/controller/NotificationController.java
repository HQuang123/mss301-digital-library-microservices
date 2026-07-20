package fu.edu.mss301.digilib.notification.api.controller;

import fu.edu.mss301.digilib.notification.api.dto.NotificationCreateRequest;
import fu.edu.mss301.digilib.notification.api.dto.NotificationResponse;
import fu.edu.mss301.digilib.notification.api.dto.ReturnConfirmationRequest;
import fu.edu.mss301.digilib.notification.application.command.NotificationCommand;
import fu.edu.mss301.digilib.notification.application.scheduler.DueSoonReminderJob;
import fu.edu.mss301.digilib.notification.application.scheduler.OverdueReminderJob;
import fu.edu.mss301.digilib.notification.application.usecase.CreateNewNotificationUseCase;
import fu.edu.mss301.digilib.notification.application.usecase.MarkNotificationReadUseCase;
import fu.edu.mss301.digilib.notification.domain.entity.NotificationLog;
import fu.edu.mss301.digilib.notification.domain.vo.NotificationChannel;
import fu.edu.mss301.digilib.notification.domain.vo.NotificationEventType;
import fu.edu.mss301.digilib.notification.domain.vo.NotificationStatus;
import fu.edu.mss301.digilib.notification.domain.repository.NotificationRepository;
import fu.edu.mss301.digilib.notification.infrastructure.specification.NotificationSearchCriteria;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final CreateNewNotificationUseCase createNewNotificationUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;
    private final NotificationRepository notificationRepository;
    private final DueSoonReminderJob dueSoonReminderJob;
    private final OverdueReminderJob overdueReminderJob;

    @PostMapping
    public ResponseEntity<List<NotificationResponse>> create(@Valid @RequestBody NotificationCreateRequest request) {
        List<NotificationLog> logs = createNewNotificationUseCase.execute(NotificationCommand.builder()
                .eventType(request.getEventType())
                .studentId(request.getStudentId())
                .studentEmail(request.getStudentEmail())
                .templateVariables(request.getTemplateVariables())
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(logs.stream().map(NotificationResponse::from).toList());
    }

    @PostMapping("/return-confirmation")
    public ResponseEntity<List<NotificationResponse>> returnConfirmation(
            @Valid @RequestBody ReturnConfirmationRequest request) {
        LocalDateTime returnedAt = request.getReturnedAt() != null ? request.getReturnedAt() : LocalDateTime.now();

        List<NotificationLog> logs = createNewNotificationUseCase.execute(NotificationCommand.builder()
                .eventType(NotificationEventType.RETURN_CONFIRMATION.name())
                .studentId(request.getStudentId())
                .studentEmail(request.getStudentEmail())
                .templateVariables(Map.of(
                        "bookTitle", request.getBookTitle(),
                        "returnedAt", returnedAt.toString()))
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(logs.stream().map(NotificationResponse::from).toList());
    }

    @PostMapping("/jobs/due-soon/run")
    public ResponseEntity<Void> runDueSoonJob() {
        dueSoonReminderJob.run();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/jobs/overdue/run")
    public ResponseEntity<Void> runOverdueJob() {
        overdueReminderJob.run();
        return ResponseEntity.accepted().build();
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Integer id,
            @AuthenticationPrincipal Jwt jwt) {
        NotificationLog log = markNotificationReadUseCase.execute(id, jwt.getSubject());
        return ResponseEntity.ok(NotificationResponse.from(log));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) NotificationStatus status,
            Pageable pageable) {
        NotificationSearchCriteria criteria = NotificationSearchCriteria.builder()
                .studentId(jwt.getSubject())
                .channel(NotificationChannel.WEBSITE)
                .status(status)
                .build();

        Page<NotificationResponse> page = notificationRepository.search(criteria, pageable)
                .map(NotificationResponse::from);

        return ResponseEntity.ok(page);
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> search(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) NotificationChannel channel,
            @RequestParam(required = false) NotificationStatus status,
            Pageable pageable) {
        NotificationSearchCriteria criteria = NotificationSearchCriteria.builder()
                .eventType(eventType)
                .studentId(studentId)
                .channel(channel)
                .status(status)
                .build();

        Page<NotificationResponse> page = notificationRepository.search(criteria, pageable)
                .map(NotificationResponse::from);

        return ResponseEntity.ok(page);
    }
}

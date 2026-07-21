package fu.edu.mss301.digilib.fine.api.controller;

import fu.edu.mss301.digilib.fine.api.dto.FineResponse;
import fu.edu.mss301.digilib.fine.api.dto.PaymentAttemptResponse;
import fu.edu.mss301.digilib.fine.application.exception.ForbiddenException;
import fu.edu.mss301.digilib.fine.application.service.FineHistoryService;
import fu.edu.mss301.digilib.fine.domain.vo.FineStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Student "My Fines" screen (FINE_HISTORY_FLOW.md Screen 1). Ownership is
 * enforced by comparing the path studentId against X-User-Id, a header set
 * by API Gateway from the caller's validated JWT (see
 * UserContextHeadersFilter in api-gateway) — fine-service trusts this header
 * because it is only reachable through the gateway.
 */
@RestController
@RequestMapping("/api/fines")
public class FineHistoryController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final FineHistoryService fineHistoryService;

    public FineHistoryController(FineHistoryService fineHistoryService) {
        this.fineHistoryService = fineHistoryService;
    }

    @GetMapping("/students/{studentId}")
    Page<FineResponse> getMyFines(
            @PathVariable("studentId") String studentId,
            @RequestHeader(USER_ID_HEADER) String callerId,
            @RequestParam(name = "status", required = false) List<FineStatus> statuses,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        requireOwner(callerId, studentId);
        return fineHistoryService.getStudentFines(studentId, statuses, pageable);
    }

    @GetMapping("/{fineId}/payments")
    List<PaymentAttemptResponse> getMyFinePayments(
            @PathVariable("fineId") Integer fineId,
            @RequestHeader(USER_ID_HEADER) String callerId
    ) {
        String ownerId = fineHistoryService.getFineStudentId(fineId);
        requireOwner(callerId, ownerId);
        return fineHistoryService.getPayments(fineId);
    }

    private void requireOwner(String callerId, String resourceOwnerId) {
        if (callerId == null || callerId.isBlank() || !callerId.equals(resourceOwnerId)) {
            throw new ForbiddenException("You may only view your own fines");
        }
    }
}

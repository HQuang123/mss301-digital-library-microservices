package fu.edu.mss301.digilib.fine.api.controller;

import fu.edu.mss301.digilib.fine.api.dto.FineResponse;
import fu.edu.mss301.digilib.fine.api.dto.PaymentAttemptResponse;
import fu.edu.mss301.digilib.fine.api.dto.WaiveFineRequest;
import fu.edu.mss301.digilib.fine.application.service.FineHistoryService;
import fu.edu.mss301.digilib.fine.domain.vo.FineStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Librarian "Member Fine Lookup" screen (FINE_HISTORY_FLOW.md Screen 2).
 * No role check here: the entire /api/fines/librarian/** prefix is restricted
 * to LIBRARIAN/ADMIN at API Gateway (GatewaySecurityConfig), which is the
 * single enforcement point for role-based access in this system.
 */
@RestController
@RequestMapping("/api/fines/librarian")
public class FineLibrarianController {

    private final FineHistoryService fineHistoryService;

    public FineLibrarianController(FineHistoryService fineHistoryService) {
        this.fineHistoryService = fineHistoryService;
    }

    @GetMapping("/students/{studentId}")
    Page<FineResponse> getStudentFines(
            @PathVariable("studentId") String studentId,
            @RequestParam(name = "status", required = false) List<FineStatus> statuses,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return fineHistoryService.getStudentFines(studentId, statuses, pageable);
    }

    @GetMapping
    Page<FineResponse> getAllFines(
            @RequestParam(name = "status", required = false) List<FineStatus> statuses,
            @PageableDefault(size = 20, sort = "dueDate") Pageable pageable
    ) {
        return fineHistoryService.getAllFines(statuses, pageable);
    }

    @GetMapping("/{fineId}/payments")
    List<PaymentAttemptResponse> getFinePayments(@PathVariable("fineId") Integer fineId) {
        return fineHistoryService.getPayments(fineId);
    }

    @PostMapping("/{fineId}/waive")
    FineResponse waiveFine(
            @PathVariable("fineId") Integer fineId,
            @Valid @RequestBody WaiveFineRequest request
    ) {
        return fineHistoryService.waiveFine(fineId, request.waiverReason());
    }

    @PostMapping("/{fineId}/mark-paid")
    FineResponse markFinePaid(@PathVariable("fineId") Integer fineId) {
        return fineHistoryService.markFinePaid(fineId);
    }
}

package fu.edu.mss301.digilib.fine.api.controller;

import fu.edu.mss301.digilib.fine.api.dto.BorrowEligibilityResponse;
import fu.edu.mss301.digilib.fine.api.dto.FineResponse;
import fu.edu.mss301.digilib.fine.api.dto.LostBookFineRequest;
import fu.edu.mss301.digilib.fine.api.dto.OverdueReturnFineRequest;
import fu.edu.mss301.digilib.fine.api.dto.OverdueThresholdFineRequest;
import fu.edu.mss301.digilib.fine.application.service.InternalFineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service endpoints called only by Loan Service. Every path here
 * is guarded by InternalApiKeyFilter (X-Internal-Api-Key), not by end-user
 * auth, and is called directly (services.fine.base-url), bypassing API Gateway.
 */
@RestController
@RequestMapping("/internal/fines")
public class InternalFineController {

    private final InternalFineService internalFineService;

    public InternalFineController(InternalFineService internalFineService) {
        this.internalFineService = internalFineService;
    }

    @GetMapping("/students/{studentId}/borrow-eligibility")
    BorrowEligibilityResponse getBorrowEligibility(@PathVariable("studentId") String studentId) {
        return internalFineService.checkBorrowEligibility(studentId);
    }

    @PostMapping("/from-overdue-return")
    ResponseEntity<FineResponse> createOverdueReturnFine(@Valid @RequestBody OverdueReturnFineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(internalFineService.createOverdueReturnFine(request));
    }

    @PostMapping("/from-overdue-threshold")
    ResponseEntity<FineResponse> createOrUpdateThresholdFine(
            @Valid @RequestBody OverdueThresholdFineRequest request) {
        return ResponseEntity.ok(internalFineService.createOrUpdateThresholdFine(request));
    }

    @PostMapping("/from-lost-book")
    ResponseEntity<FineResponse> createOrUpdateLostBookFine(@Valid @RequestBody LostBookFineRequest request) {
        return ResponseEntity.ok(internalFineService.createOrUpdateLostBookFine(request));
    }
}

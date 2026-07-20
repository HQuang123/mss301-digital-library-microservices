package fu.edu.mss301.digilib.loan.api.controller;

import fu.edu.mss301.digilib.loan.api.dto.BorrowRequestResponse;
import fu.edu.mss301.digilib.loan.api.dto.CreateBorrowRequest;
import fu.edu.mss301.digilib.loan.api.dto.LoanResponse;
import fu.edu.mss301.digilib.loan.api.dto.RejectBorrowRequest;
import fu.edu.mss301.digilib.loan.application.usecase.BorrowRequestUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/borrow-requests")
@RequiredArgsConstructor
public class BorrowRequestController {

    static final String AUTHENTICATED_USER_HEADER = "X-Authenticated-User-Id";

    private final BorrowRequestUseCase useCase;

    @PostMapping
    public ResponseEntity<BorrowRequestResponse> create(
            @RequestHeader(AUTHENTICATED_USER_HEADER) String memberId,
            @Valid @RequestBody CreateBorrowRequest request) {
        BorrowRequestResponse response = useCase.create(memberId, request);
        return ResponseEntity.created(URI.create("/api/v1/borrow-requests/" + response.requestId()))
                .body(response);
    }

    @GetMapping("/me")
    public Page<BorrowRequestResponse> findMine(
            @RequestHeader(AUTHENTICATED_USER_HEADER) String memberId,
            Pageable pageable) {
        return useCase.findMine(memberId, pageable);
    }

    @GetMapping
    public Page<BorrowRequestResponse> findByStatus(
            @RequestParam(defaultValue = "PENDING") String status,
            Pageable pageable) {
        return useCase.findByStatus(status, pageable);
    }

    @PostMapping("/{requestId}/approve")
    public LoanResponse approve(
            @PathVariable Long requestId,
            @RequestHeader(AUTHENTICATED_USER_HEADER) String actorId) {
        return useCase.approve(requestId, actorId);
    }

    @PostMapping("/{requestId}/reject")
    public BorrowRequestResponse reject(
            @PathVariable Long requestId,
            @RequestHeader(AUTHENTICATED_USER_HEADER) String actorId,
            @Valid @RequestBody RejectBorrowRequest request) {
        return useCase.reject(requestId, request.reason(), actorId);
    }

    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> cancel(
            @PathVariable Long requestId,
            @RequestHeader(AUTHENTICATED_USER_HEADER) String memberId) {
        useCase.cancel(requestId, memberId);
        return ResponseEntity.noContent().build();
    }
}

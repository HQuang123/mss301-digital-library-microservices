package fu.edu.mss301.digilib.loan.api.controller;

import fu.edu.mss301.digilib.loan.api.dto.BorrowRequestCreateRequest;
import fu.edu.mss301.digilib.loan.api.dto.BorrowRequestDecisionRequest;
import fu.edu.mss301.digilib.loan.api.dto.BorrowRequestResponse;
import fu.edu.mss301.digilib.loan.application.usecase.ManageBorrowRequestUseCase;
import fu.edu.mss301.digilib.loan.domain.vo.LoanStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/borrow-requests")
@RequiredArgsConstructor
public class BorrowRequestController {

    private final ManageBorrowRequestUseCase useCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BorrowRequestResponse create(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody BorrowRequestCreateRequest request) {
        return BorrowRequestResponse.from(useCase.create(
                jwt.getSubject(), request.bookId(), request.bookType(), request.idempotencyKey()));
    }

    @GetMapping("/me")
    public Page<BorrowRequestResponse> mine(@AuthenticationPrincipal Jwt jwt, Pageable pageable) {
        return useCase.findMine(jwt.getSubject(), pageable).map(BorrowRequestResponse::from);
    }

    @DeleteMapping("/{requestId}")
    public BorrowRequestResponse cancel(@PathVariable Long requestId, @AuthenticationPrincipal Jwt jwt) {
        return BorrowRequestResponse.from(useCase.cancel(requestId, jwt.getSubject()));
    }

    @GetMapping
    public Page<BorrowRequestResponse> reviewQueue(
            @RequestParam(required = false) LoanStatus status, Pageable pageable) {
        return useCase.findForReview(status, pageable).map(BorrowRequestResponse::from);
    }

    @PostMapping("/{requestId}/approve")
    public BorrowRequestResponse approve(@PathVariable Long requestId, @AuthenticationPrincipal Jwt jwt) {
        return BorrowRequestResponse.from(useCase.approve(requestId, jwt.getSubject()));
    }

    @PostMapping("/{requestId}/reject")
    public BorrowRequestResponse reject(@PathVariable Long requestId,
                                        @AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody BorrowRequestDecisionRequest request) {
        return BorrowRequestResponse.from(useCase.reject(requestId, jwt.getSubject(), request.reason()));
    }
}

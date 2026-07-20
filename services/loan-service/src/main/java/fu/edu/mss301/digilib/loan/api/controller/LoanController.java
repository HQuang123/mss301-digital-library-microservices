package fu.edu.mss301.digilib.loan.api.controller;

import fu.edu.mss301.digilib.loan.api.dto.BorrowLoanRequest;
import fu.edu.mss301.digilib.loan.api.dto.LoanResponse;
import fu.edu.mss301.digilib.loan.api.dto.ReturnLoanRequest;
import fu.edu.mss301.digilib.loan.application.command.BorrowBookCommand;
import fu.edu.mss301.digilib.loan.application.usecase.BorrowBookUseCase;
import fu.edu.mss301.digilib.loan.application.usecase.ManageLoanUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LoanController {

    private final BorrowBookUseCase borrowBookUseCase;
    private final ManageLoanUseCase manageLoanUseCase;

    @PostMapping("/rent-books")
    public ResponseEntity<LoanResponse> borrow(@Valid @RequestBody BorrowLoanRequest request) {
        LoanResponse response = LoanResponse.from(borrowBookUseCase.handle(new BorrowBookCommand(
                request.memberId(), request.bookId(), request.bookType(), request.idempotencyKey())));
        return ResponseEntity.created(URI.create("/api/v1/loans/" + response.loanId())).body(response);
    }

    @PostMapping("/loans/return")
    public LoanResponse returnBook(@Valid @RequestBody ReturnLoanRequest request) {
        return LoanResponse.from(manageLoanUseCase.returnBook(request.loanId(), request.idempotencyKey()));
    }

    @PutMapping("/loans/{loanId}/renew")
    public LoanResponse renew(
            @PathVariable Long loanId,
            @AuthenticationPrincipal Jwt jwt) {
        ensureOwnerOrStaff(manageLoanUseCase.findById(loanId).getMemberId(), jwt);
        return LoanResponse.from(manageLoanUseCase.renew(loanId, jwt.getSubject()));
    }

    @PostMapping("/loans/{loanId}/lost")
    public LoanResponse reportLost(
            @PathVariable Long loanId,
            @RequestHeader(name = "X-Actor-Id", defaultValue = "SYSTEM") String actorId) {
        return LoanResponse.from(manageLoanUseCase.reportLost(loanId, actorId));
    }

    @GetMapping("/loans/{loanId}")
    public LoanResponse findById(@PathVariable Long loanId, @AuthenticationPrincipal Jwt jwt) {
        var loan = manageLoanUseCase.findById(loanId);
        ensureOwnerOrStaff(loan.getMemberId(), jwt);
        return LoanResponse.from(loan);
    }

    @GetMapping("/loans")
    public Page<LoanResponse> findAll(Pageable pageable) {
        return manageLoanUseCase.findAll(pageable).map(LoanResponse::from);
    }

    @GetMapping("/loans/my-loans")
    public List<LoanResponse> findByMember(@AuthenticationPrincipal Jwt jwt) {
        return manageLoanUseCase.findByMember(jwt.getSubject()).stream().map(LoanResponse::from).toList();
    }

    private void ensureOwnerOrStaff(String ownerId, Jwt jwt) {
        if (ownerId.equals(jwt.getSubject()) || hasStaffRole(jwt)) return;
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "Loan belongs to another member");
    }

    private boolean hasStaffRole(Jwt jwt) {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return false;
        Object roles = realmAccess.get("roles");
        if (!(roles instanceof java.util.List<?> roleList)) return false;
        return roleList.stream().map(String::valueOf)
                .anyMatch(role -> role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("librarian"));
    }
}

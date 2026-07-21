Flow 7: View fine payment history

This flow covers two screens on top of the same Fine Service read APIs:

1. Student screen — "My Fines" (own history only).
2. Librarian screen — "Member Fine Lookup" (any student's history, plus realm-wide search).

The two screens do NOT share endpoints. All librarian-facing endpoints live under the /api/fines/librarian/** path prefix; student-facing endpoints stay on the plain /api/fines/** path.

Fine Service does not decode JWTs itself — all role/identity checking happens at API Gateway, and only that. GatewaySecurityConfig restricts /api/fines/librarian/** to hasAnyRole('LIBRARIAN','ADMIN'). Separately, UserContextHeadersFilter (a GlobalFilter in api-gateway) reads the caller's already-validated JWT and forwards two trusted headers to every downstream service call:
X-User-Id: the JWT sub claim.
X-User-Roles: comma-joined granted authorities (e.g. ROLE_STUDENT,SCOPE_openid).
Any inbound copies of these headers on the original request are stripped before forwarding, so a caller cannot spoof identity by setting them directly. This is only safe because business services are reachable only through the gateway (docs/infra/security-bootstrap.md, "Direct-access boundary").
Fine Service's plain /api/fines/** endpoints use X-User-Id purely for an ownership check (path studentId, or the fine's stored studentId, must equal X-User-Id) — this is lightweight request-level logic, not a security framework. Fine Service's /api/fines/librarian/** endpoints do no check at all; they trust the gateway's role gate entirely.

---

Screen 1: Student — "My Fines"

Student opens the "My Fines" page.
Frontend reads the student's own id from the JWT (sub claim) — it never lets the student type an arbitrary studentId.
Frontend calls Fine Service:

GET /api/fines/students/{studentId}?status=PENDING,PAID,WAIVED&page=0&size=20

Fine Service (FineHistoryController) compares the studentId path variable against the X-User-Id header API Gateway forwarded; if they don't match (or the header is missing, meaning the request didn't come through the gateway as expected), it returns 403 Forbidden. There is no role check here — any caller whose X-User-Id matches the path studentId is allowed, which in practice is only ever the student themselves (a librarian's X-User-Id would not match another student's id).
Fine Service queries fines by studentId, optionally filtered by status.
Fine Service returns a paginated list of fines. Each fine already has bookId (persisted from the payload Loan Service sent when the fine was created — see Flow 3/4/5), so Fine Service enriches the response with the book title itself:
For each fine, Fine Service calls Catalog Service via CatalogServiceClient (Feign, resolved through Eureka as catalog-service): GET /api/catalog/books/{bookId}.
Fine Service does not persist the title — bookId is the only book reference stored on Fine; the title is resolved live on every read.
If Catalog Service is unavailable or the book was deleted, Fine Service returns the fine with bookTitle = null rather than failing the whole list (best-effort enrichment).
{
"fineId": "FINE001",
"loanId": "LOAN001",
"bookId": 42,
"bookTitle": "Clean Code",
"dueDate": "2026-06-01",
"returnDate": "2026-06-05",
"amountDue": 20000,
"status": "PAID",
"paidAt": "2026-06-06T10:15:00"
}
Frontend groups the list client-side into tabs: "Unpaid" (status = PENDING), "Paid" (status = PAID), "Waived" (status = WAIVED).
Student selects a fine to see full detail, including payment attempt history:

GET /api/fines/{fineId}/payments

Fine Service looks up the fine's stored studentId and compares it against X-User-Id the same way; mismatch or missing header returns 403 Forbidden.
Fine Service returns all PaymentAttempt records for that fine (statuses: CREATED, PENDING, SUCCEEDED, FAILED, EXPIRED, CANCELLED, REFUNDED), ordered by createdAt descending. This is a separate endpoint from FineController's existing GET /{fineId}/payments/latest, which returns only the single most recent PaymentStatusResponse.
If the fine is still PENDING, frontend shows a "Pay now" action that starts Flow 6 (SePay QR payment).
If the fine is PAID, frontend shows the SUCCEEDED PaymentAttempt (sepayTransactionId, paidAt, amount) as a receipt.
Student cannot see any other student's fines from this screen — the studentId is never taken from user input.

---

Screen 2: Librarian — "Member Fine Lookup"

Librarian opens the "Member Fine Lookup" page and searches for a student (by member code, email, or name) via Member Service, or arrives here from a member's profile page with studentId already known.
Frontend calls Fine Service under the librarian-only path prefix:

GET /api/fines/librarian/students/{studentId}?status=PENDING,PAID,WAIVED&page=0&size=20

Fine Service (FineLibrarianController) performs no role or ownership check of its own — access to every /api/fines/librarian/** route is enforced entirely by API Gateway's GatewaySecurityConfig rule (hasAnyRole('LIBRARIAN','ADMIN') on that path prefix). A STUDENT-only token is rejected with 403 at the gateway before the request ever reaches Fine Service.
Any studentId is allowed — librarians are not restricted to their own id.
Fine Service queries fines by studentId, optionally filtered by status, same response shape as Screen 1.
Additionally, librarians can list fines across all students (not scoped to one studentId) for operational triage:

GET /api/fines/librarian?status=PENDING&page=0&size=20&sort=dueDate,asc

Fine Service returns a paginated list of fines across all students, including studentId and studentEmail on each row so the librarian can identify the member. As with Screen 1, bookTitle is enriched server-side via CatalogServiceClient using the fine's stored bookId.
Librarian selects a fine to see full detail, including payment attempt history:

GET /api/fines/librarian/{fineId}/payments

Fine Service returns all PaymentAttempt records for that fine, ordered by createdAt descending (no ownership check needed — the gateway's role gate already restricts every caller of this prefix to LIBRARIAN/ADMIN).
Librarian screen additionally exposes fine-management actions not available to students:
Waive a fine — POST /api/fines/librarian/{fineId}/waive with a waiverReason. Transitions the fine to WAIVED per FineAggregate.waive(...).
Mark a fine paid manually (e.g. cash payment at the desk) — POST /api/fines/librarian/{fineId}/mark-paid. Transitions the fine to PAID per FineAggregate.markPaid(...).
These management actions are out of scope for the student screen, which is read-only plus "Pay now" (Flow 6) and never touches the /api/fines/librarian/** prefix.

---

Authorization summary

| Endpoint | Enforced by | STUDENT | LIBRARIAN / ADMIN |
|---|---|---|---|
| GET /api/fines/students/{studentId} | Fine Service (X-User-Id == studentId) | only own studentId | only if X-User-Id happens to equal that studentId (never true in practice) |
| GET /api/fines/{fineId}/payments | Fine Service (X-User-Id == fine.studentId) | only if fine belongs to them | same rule — not realistically reachable for a librarian |
| GET /api/fines/librarian/students/{studentId} | API Gateway (role gate on path prefix) | 403 Forbidden | any studentId |
| GET /api/fines/librarian (all students) | API Gateway | 403 Forbidden | allowed |
| GET /api/fines/librarian/{fineId}/payments | API Gateway | 403 Forbidden | any fine |
| POST /api/fines/librarian/{fineId}/waive | API Gateway | 403 Forbidden | allowed |
| POST /api/fines/librarian/{fineId}/mark-paid | API Gateway | 403 Forbidden | allowed |

Implementation status: all endpoints above are implemented (InternalFineController for Flow 1/3/4/5, FineController + FineHistoryController + FineLibrarianController for Flow 6/7). FineAggregate.waive(...) and FineAggregate.markPaid(...) — previously unused domain logic — now back the two librarian management actions via FineHistoryService.

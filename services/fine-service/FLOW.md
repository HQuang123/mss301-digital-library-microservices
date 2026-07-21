Flow 1: Borrow book

Student requests to borrow a book.
Frontend calls Loan Service.
Loan Service calls Fine Service:

GET /internal/fines/students/{studentId}/borrow-eligibility

Fine Service checks whether the student has any PENDING fine.
If the student has unpaid fines:
Fine Service returns canBorrow = false.
Loan Service rejects the borrow request.
Suggested HTTP status: 409 Conflict.
If the student has no unpaid fines:
Fine Service returns canBorrow = true.
Loan Service continues checking book availability and creates the loan.

Flow 2: Return book on time

Student returns a book.
Loan Service checks dueDate and actualReturnDate.
If overdueDays <= 0:
Loan Service updates loan status to RETURNED.
Loan Service does not call Fine Service.
Book copy can be marked as AVAILABLE.

Flow 3: Return book late

Student returns a book late.
Loan Service calculates overdueDays.
If overdueDays > 0, Loan Service calls Fine Service:

POST /internal/fines/from-overdue-return

Request body example:
{
"studentId": "STU001",
"loanId": "LOAN001",
"bookId": "BOOK001",
"bookCopyId": "COPY001",
"bookTitle": "Clean Code",
"bookValue": 250000,
"dueDate": "2026-06-01",
"returnDate": "2026-06-05",
"overdueDays": 4
}

Fine Service calculates:
fineAmount = overdueDays * 5000
compensationAmount = 0
totalAmount = fineAmount
Fine Service creates Fine with status = PENDING.
Loan Service updates loan status to RETURNED_WITH_FINE or RETURNED.

Flow 4: Overdue more than 30 days

Loan Service runs a scheduled job to find loans that are overdue for more than 30 days and not returned.
Loan Service gets book value from Book Service if needed.
Loan Service calls Fine Service:

POST /internal/fines/from-overdue-threshold

Request body example:
{
"studentId": "STU001",
"loanId": "LOAN001",
"bookId": "BOOK001",
"bookCopyId": "COPY001",
"bookTitle": "Clean Code",
"bookValue": 250000,
"overdueDays": 31,
"compensationEnabled": true
}

Fine Service checks whether a fine already exists for this loan.
If no fine exists:
Create a new fine.
If a fine already exists:
Update the existing fine.
Fine Service calculates:
fineAmount = overdueDays * 5000
compensationAmount = bookValue if compensationEnabled = true
totalAmount = fineAmount + compensationAmount
Fine status remains PENDING.

Flow 5: Lost book

Student or librarian reports that a borrowed book is lost.
Frontend calls Loan Service.
Loan Service checks the active loan.
Loan Service gets book value from Book Service.
Loan Service calculates overdueDays if the book is already overdue.
Loan Service calls Fine Service:

POST /internal/fines/from-lost-book

Request body example:
{
"studentId": "STU001",
"loanId": "LOAN001",
"bookId": "BOOK001",
"bookCopyId": "COPY001",
"bookTitle": "Clean Code",
"bookValue": 250000,
"overdueDays": 10
}

Fine Service calculates:
fineAmount = overdueDays * 5000 if overdueDays > 0
compensationAmount = bookValue
totalAmount = fineAmount + compensationAmount
Fine Service creates Fine with reason = LOST_BOOK and status = PENDING.
Loan Service updates loan status to LOST.
Book Service updates book copy status to LOST.

Note on the bookId/bookCopyId/bookTitle/bookValue fields sent in Flow 3/4/5 request bodies: Fine Service persists only bookId (as a reference for later enrichment — see FINE_HISTORY_FLOW.md). bookCopyId, bookTitle, and bookValue are not stored; bookValue is used transiently to calculate compensationAmount at creation time, and bookTitle is deliberately not persisted since it can go stale — Fine Service re-resolves it from Catalog Service on every read instead.

Flow 6: Pay fine using SePay QR code

Student opens fine detail page.
Frontend calls Fine Service:

POST /api/fines/{fineId}/payments/sepay/qr

Fine Service checks:
Fine exists
Fine status is PENDING
Fine totalAmount is greater than 0
Fine Service creates a PaymentAttempt with status = CREATED (transitions to PENDING once the QR is generated).
Fine Service generates SePay QR payment information.
Fine Service returns QR information to frontend.
Student scans QR code and pays.
SePay sends webhook to Fine Service:

POST /api/fines/payments/sepay/webhook

Fine Service validates:
transactionCode
amount
payment status
duplicate webhook handling
If valid:
Update PaymentAttempt status to SUCCEEDED (via PaymentAttempt.markSucceeded(...)).
Update fine status to PAID.
Set fine.paidAt.
Student can borrow again if there are no other PENDING fines.

See FINE_HISTORY_FLOW.md for Flow 7: viewing fine payment history (student screen and librarian screen).

Important design rules:

Loan Service does not store fine amount.
Fine Service does not manage loan lifecycle.
Loan Service decides when a fine should be created.
Fine Service decides how much the fine is and whether it is paid.
Loan Service must call Fine Service before creating a new loan.
Fine Service should be idempotent when handling webhook and duplicate fine creation.
Fine Service should not create duplicate fines for the same loan and same reason.
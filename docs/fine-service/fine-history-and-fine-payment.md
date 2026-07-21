# Fine History & Fine Payment — Frontend Implementation Guide

This guide covers implementing **Flow 7** (view fine history) and **Flow 6** (pay a fine via SePay QR) on the `mss301-digilib-fe` frontend. The backend for both flows is fully implemented in `fine-service`; nothing described here requires backend changes. Two scaffold files already exist and are currently empty:

- `src/api/fineApi.js`
- `src/pages/fines/FineManage.jsx`

Reference docs on the backend side: `services/fine-service/FLOW.md` (Flow 6) and `services/fine-service/FINE_HISTORY_FLOW.md` (Flow 7).

---

## 1. Where this screen lives

The fine history + payment UI belongs on its own **Fine History** page, not folded into the Loan screen — loan-service and fine-service are deliberately decoupled (loan-service does not store fine amounts), and the empty `pages/fines/` scaffold already reflects this. Optionally, `LoanHistory.jsx` can show a lightweight "you have an unpaid fine" badge linking to the new page, but the QR payment flow itself should not be duplicated there.

Two screens are needed, matching the two backend controllers:

| Screen | Route (suggested) | Backend controller | Access |
|---|---|---|---|
| Student "My Fines" | `/fines` | `FineHistoryController` (`/api/fines/**`) | any authenticated user |
| Librarian "Member Fine Lookup" | `/librarian/fines` | `FineLibrarianController` (`/api/fines/librarian/**`) | `admin`, `librarian` |

Register both in `src/App.jsx` next to the existing `/loans` and `/librarian/*` routes:

```jsx
import FineHistory from './pages/fines/FineHistory'
import LibrarianFineLookup from './pages/fines/LibrarianFineLookup'

// Authenticated (any role)
<Route path="/fines" element={<PrivateRoute><FineHistory /></PrivateRoute>} />

// Librarian routes
<Route path="/librarian/fines" element={<PrivateRoute requiredRoles={['admin', 'librarian']}><LibrarianFineLookup /></PrivateRoute>} />
```

### 1.1 Entry points (required)

A route with no link pointing at it is unreachable. Two entry points are required, not optional:

**a) Header button "Nộp phạt" → `/fines`** (`src/components/layout/Header.jsx`)

Add it to the authenticated action cluster, next to `NotificationBell`/the profile button (`Header.jsx:146-164`) — not to the public `navItems` array, since it's meaningless for logged-out visitors:

```jsx
import { Wallet } from 'lucide-react' // pick any fitting icon

{isAuthenticated ? (
  <>
    <NotificationBell />
    <NavLink to="/fines">
      <Button variant="ghost" size="sm" className="rounded-full px-3" aria-label="Nộp phạt">
        <Wallet size={16} />
        Nộp phạt
      </Button>
    </NavLink>
    <NavLink to="/profile">
      ...
```

Also add the equivalent link inside the mobile menu (`Header.jsx:166-170` currently only renders a bare hamburger button with no menu implementation — if/when that mobile menu gets built, include "Nộp phạt" there too).

**b) Librarian sidebar link "Phạt" → `/librarian/fines`** (`src/pages/books/librarian/LibrarianLayout.jsx`)

The sidebar (`LibrarianLayout.jsx:136-158`) is built from `MainLink`/`SidebarGroup` entries. Add a top-level `MainLink` (a `SidebarGroup` is unnecessary since there's only one fine-related page, unlike the multi-page "Quản lý sách" group):

```jsx
import { Wallet } from 'lucide-react' // add to the existing lucide-react import

<MainLink icon={UsersRound} label="Người dùng" to="/librarian/members" active={active === 'users'} />
<MainLink icon={Wallet} label="Phạt" to="/librarian/fines" active={active === 'fines'} />
```

Then pass `active="fines"` when rendering `LibrarianLayout` from the new `LibrarianFineLookup.jsx` page (same pattern as every other librarian page — see `LibrarianDashboard.jsx:132`, `active="dashboard"`).

Optionally, also surface it as a `StatCard` on `LibrarianDashboard.jsx` itself (`LibrarianDashboard.jsx:5-10`, the `stats` array) — e.g. "Phạt chưa thu" (unpaid fines), sourced from `getAllFines({ status: 'PENDING' })`'s `totalElements`, linking through to `/librarian/fines`. The sidebar link is the required piece; the dashboard stat card is a nice-to-have on top of it.

(`FineManage.jsx` is empty — either fill it in as one of these two pages, or leave it unused and create fresh files as above; either is fine, just don't leave a dead empty file behind if unused.)

---

## 2. Identity: how the frontend proves "this is my own fine"

Fine Service does not decode JWTs itself (see `FINE_HISTORY_FLOW.md`). API Gateway validates the JWT and forwards two trusted headers downstream (`UserContextHeadersFilter`); fine-service compares the `studentId` path variable against the caller's identity. On the frontend side this means:

- **Never let the student type or edit a studentId.** The student's own id is `useAuthStore((s) => s.user)?.id` — this is the member profile id, which equals the Keycloak `sub` claim (same value the gateway forwards as `X-User-Id`). Always call `GET /api/fines/students/{studentId}` with `user.id`, taken from the store, not from a route param or form field.
- **Role checks reuse the existing pattern.** `useAuthStore` already extracts lowercase role names (`admin`, `librarian`, `student`, ...) from the JWT into `roles`. Gate the librarian page with `<PrivateRoute requiredRoles={['admin', 'librarian']}>`, exactly like every other `/librarian/*` route in `App.jsx`.
- If `user` is `null` (profile not loaded yet), show a loading/empty state — same pattern `LoanHistory.jsx` already uses for `memberId`.

---

## 3. Backend contract

All calls go through the existing `axiosClient` (Bearer token attached automatically, 401 triggers auto-logout).

### 3.1 Student endpoints (`/api/fines`)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/fines/students/{studentId}?status=PENDING,PAID,WAIVED&page=0&size=20` | Paginated, `status` optional (omit for all) |
| GET | `/api/fines/{fineId}/payments` | Full `PaymentAttempt` history for one fine |
| POST | `/api/fines/{fineId}/payments/sepay/qr` | Starts a SePay payment attempt, returns QR info |
| GET | `/api/fines/{fineId}/payments/latest` | Poll this for payment status |

### 3.2 Librarian endpoints (`/api/fines/librarian`)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/fines/librarian/students/{studentId}?status=...&page=&size=` | Any student, not just self |
| GET | `/api/fines/librarian?status=PENDING&page=0&size=20&sort=dueDate,asc` | All-students triage list |
| GET | `/api/fines/librarian/{fineId}/payments` | Payment history, any fine |
| POST | `/api/fines/librarian/{fineId}/waive` | Body: `{ "waiverReason": "..." }` |
| POST | `/api/fines/librarian/{fineId}/mark-paid` | No body — manual/cash payment |

### 3.3 Response shapes

```ts
// FineResponse
{
  fineId: number,
  loanId: number,
  bookId: number | null,
  bookTitle: string | null,   // enriched server-side from Catalog Service; may be null
  studentId: string,
  studentEmail: string | null,
  reason: "OVERDUE_RETURN" | "OVERDUE_THRESHOLD" | "LOST_BOOK",
  dueDate: string,             // ISO LocalDateTime
  returnDate: string | null,
  amountDue: number,           // total owed (already includes compensationAmount)
  compensationAmount: number,  // breakdown only — do not add to amountDue again
  status: "PENDING" | "PAID" | "WAIVED",
  paidAt: string | null,
}

// PaymentAttemptResponse
{
  id: number,
  fineId: number,
  paymentCode: string,
  amount: number,
  currency: string,             // "VND"
  provider: "SEPAY",
  status: "CREATED" | "PENDING" | "SUCCEEDED" | "FAILED" | "EXPIRED" | "CANCELLED" | "REFUNDED",
  sepayTransactionId: string | null,
  sepayReferenceCode: string | null,
  paidAt: string | null,
  expiresAt: string | null,
  createdAt: string,
}

// SepayQrResponse (from POST .../sepay/qr)
{
  paymentId: number,
  fineId: number,
  paymentCode: string,
  amount: number,
  currency: string,
  status: "PENDING",
  bank: string,
  accountNumber: string,
  accountName: string,
  transferContent: string,      // == paymentCode, put in the bank transfer note
  qrUrl: string,                 // <img src={qrUrl}> — SePay-hosted QR image
  expiresAt: string,
}

// PaymentStatusResponse (from GET .../payments/latest) — poll this
{
  paymentId: number,
  fineId: number,
  paymentCode: string,
  amount: number,
  paymentStatus: "CREATED" | "PENDING" | "SUCCEEDED" | "FAILED" | "EXPIRED" | "CANCELLED" | "REFUNDED",
  fineStatus: "PENDING" | "PAID" | "WAIVED",   // the fine itself, not the attempt
  paidAt: string | null,
  expiresAt: string | null,
}
```

Paginated endpoints return a Spring `Page<T>` envelope: `{ content: [...], totalElements, totalPages, number, size, ... }` — unwrap `response.data.content`, same as any other paginated endpoint in this codebase.

---

## 4. `fineApi.js`

Fill in the empty scaffold, following the exact pattern of `loanApi.js`:

```js
import axiosClient from './axiosClient.js'

// ── Student ──────────────────────────────────────────────────────────────
export const getMyFines = (studentId, { status, page = 0, size = 20 } = {}) =>
  axiosClient.get(`/api/fines/students/${studentId}`, { params: { status, page, size } })

export const getFinePayments = (fineId) =>
  axiosClient.get(`/api/fines/${fineId}/payments`)

export const createSepayQr = (fineId) =>
  axiosClient.post(`/api/fines/${fineId}/payments/sepay/qr`)

export const getLatestPaymentStatus = (fineId) =>
  axiosClient.get(`/api/fines/${fineId}/payments/latest`)

// ── Librarian ────────────────────────────────────────────────────────────
export const getStudentFinesAsLibrarian = (studentId, { status, page = 0, size = 20 } = {}) =>
  axiosClient.get(`/api/fines/librarian/students/${studentId}`, { params: { status, page, size } })

export const getAllFines = ({ status, page = 0, size = 20, sort = 'dueDate,asc' } = {}) =>
  axiosClient.get('/api/fines/librarian', { params: { status, page, size, sort } })

export const getFinePaymentsAsLibrarian = (fineId) =>
  axiosClient.get(`/api/fines/librarian/${fineId}/payments`)

export const waiveFine = (fineId, waiverReason) =>
  axiosClient.post(`/api/fines/librarian/${fineId}/waive`, { waiverReason })

export const markFinePaid = (fineId) =>
  axiosClient.post(`/api/fines/librarian/${fineId}/mark-paid`)
```

---

## 5. Student "My Fines" page (`pages/fines/FineHistory.jsx`)

Mirror `LoanHistory.jsx`'s structure: `Header`/`Footer` shell, `SummaryCard` row, filterable table.

- **Data load**: `getMyFines(user.id)` once `user` is available (same `useCallback` + `useEffect` pattern as `loadLoans`).
- **Tabs/filter**: client-side or server-side status filter — "Tất cả" (all), "Chưa thanh toán" (`PENDING`), "Đã thanh toán" (`PAID`), "Đã miễn" (`WAIVED`). Reuse the `STATUS_META` + `StatusBadge` pattern from `LoanHistory.jsx`, with fine-specific labels/colors:
  ```js
  const FINE_STATUS_META = {
    PENDING: { label: 'Chưa thanh toán', classes: 'bg-red-50 text-red-700 ring-red-600/10' },
    PAID: { label: 'Đã thanh toán', classes: 'bg-emerald-50 text-emerald-700 ring-emerald-600/10' },
    WAIVED: { label: 'Đã miễn', classes: 'bg-slate-100 text-slate-700 ring-slate-500/10' },
  }
  ```
- **Row content**: `bookTitle` (fall back to `Sách #{bookId}` if `bookTitle` is null — Catalog Service enrichment is best-effort), `dueDate`, `amountDue` (format as VND currency), `status` badge.
- **Row action**: for `PENDING` rows, a "Thanh toán ngay" (Pay now) button that opens the QR modal (below). For `PAID` rows, a "Xem biên lai" (view receipt) action that opens the payment-history panel/modal showing the `SUCCEEDED` `PaymentAttemptResponse`.
- **Detail drill-in**: clicking a row (or a dedicated button) calls `getFinePayments(fineId)` and shows the full attempt list — useful for a fine with multiple failed/expired attempts before a successful one.

---

## 6. SePay QR Payment Modal

This is the "click → QR popup → scan → auto-close" flow. Key design point: **the browser has no direct signal that payment succeeded** — SePay's webhook hits fine-service directly, not the browser (see `FLOW.md` Flow 6). The modal must poll.

```jsx
function SepayQrModal({ fineId, onClose, onPaid }) {
  const [qr, setQr] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    createSepayQr(fineId)
      .then((res) => { if (!cancelled) setQr(res.data) })
      .catch((e) => { if (!cancelled) setError(e?.response?.data?.message || 'Không thể tạo mã QR.') })
    return () => { cancelled = true }
  }, [fineId])

  useEffect(() => {
    if (!qr) return undefined
    const interval = setInterval(async () => {
      try {
        const res = await getLatestPaymentStatus(fineId)
        if (res.data.fineStatus === 'PAID') {
          clearInterval(interval)
          onPaid(res.data)
        } else if (res.data.paymentStatus === 'EXPIRED') {
          clearInterval(interval)
          setError('Mã QR đã hết hạn. Vui lòng thử lại.')
        }
      } catch {
        // transient poll failure — keep trying until expiresAt
      }
    }, 3000) // poll every 3s
    return () => clearInterval(interval)
  }, [qr, fineId, onPaid])

  // ... render qr.qrUrl as <img>, qr.amount, qr.transferContent, qr.accountNumber,
  //     a countdown to qr.expiresAt, and the error state.
}
```

Notes:
- Watch `fineStatus` (the fine as a whole) to detect success, not `paymentStatus` (the individual attempt) — a fine can have several failed/expired attempts before one succeeds.
- Stop polling and show an expired/retry state once `expiresAt` passes, even if the interval hasn't caught an `EXPIRED` status yet (clock skew / poll latency).
- On success (`onPaid`), close the modal and refresh the fines list (re-run `getMyFines`) so the row flips to `PAID` immediately rather than waiting for the next manual refresh.
- 3-second polling is a reasonable default; there is no push/webhook-to-browser channel in this codebase to do better without adding one (e.g. SSE/WebSocket), which is out of scope here.

---

## 7. Librarian "Member Fine Lookup" page (`pages/fines/LibrarianFineLookup.jsx`)

Two modes on one page (or two tabs), matching `FINE_HISTORY_FLOW.md` Screen 2:

1. **Lookup by student** — a search box (member code / email / name via `memberApi`, or a `studentId` passed in via route state from a member's profile page) → `getStudentFinesAsLibrarian(studentId)`.
2. **All-fines triage** — default view, `getAllFines({ status: 'PENDING' })`, sorted by `dueDate` ascending (oldest overdue first) so librarians can act on the most urgent unpaid fines. Each row shows `studentId`/`studentEmail` (present on this response, unlike... actually present on both, but essential here since rows span multiple students).

Row actions (not present on the student screen):
- **Waive** — opens a small form/modal for `waiverReason` (required, non-blank per `WaiveFineRequest` validation) → `waiveFine(fineId, waiverReason)`.
- **Mark paid** — confirmation prompt (e.g. `window.confirm`, matching `handleRenew`'s pattern in `LoanHistory.jsx`) → `markFinePaid(fineId)`. Use this for cash/in-person payment, not SePay.

Both actions only apply to `PENDING` fines — disable/hide them for `PAID`/`WAIVED` rows (the backend also rejects with `409 Conflict` via `BusinessConflictException` if attempted on a non-pending fine, so treat that as a normal error-toast case, not a crash).

---

## 8. Implementation checklist

- [ ] `fineApi.js` — 9 functions (§4)
- [ ] `pages/fines/FineHistory.jsx` — student screen, list + tabs (§5)
- [ ] `SepayQrModal` component — QR display + polling (§6)
- [ ] `pages/fines/LibrarianFineLookup.jsx` — librarian screen, lookup + triage + waive/mark-paid (§7)
- [ ] Routes in `App.jsx`: `/fines` (any authenticated user), `/librarian/fines` (`admin`, `librarian`)
- [ ] Header "Nộp phạt" button → `/fines` (§1.1a) — **required**, otherwise the student screen is unreachable
- [ ] Librarian sidebar link "Phạt" → `/librarian/fines` in `LibrarianLayout.jsx` (§1.1b) — **required**, otherwise the librarian screen is unreachable
- [ ] Optional: "Phạt chưa thu" stat card on `LibrarianDashboard.jsx` (§1.1b)
- [ ] Optional: unpaid-fine badge/link on `LoanHistory.jsx` pointing to `/fines` (§1)

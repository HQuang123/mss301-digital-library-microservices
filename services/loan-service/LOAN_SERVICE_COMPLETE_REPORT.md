# Báo cáo hoàn thiện Loan Service

## 1. Phạm vi công việc

Loan Service đã được hoàn thiện ở mức MVP để quản lý vòng đời mượn sách trong hệ thống Digital Library. Phần đã làm bao gồm:

- Mượn sách vật lý hoặc sách số.
- Trả sách.
- Gia hạn khoản mượn.
- Tra cứu một khoản mượn, toàn bộ khoản mượn và lịch sử theo thành viên.
- Kiểm tra điều kiện mượn với Member Service.
- Tìm và cập nhật trạng thái bản sao sách với Catalog Service.
- Lưu lịch sử trạng thái và sự kiện nghiệp vụ theo Outbox Pattern.
- Chuẩn hóa cấu hình PostgreSQL, Config Server và Eureka.
- Chuẩn bị dữ liệu SQL để kiểm thử bằng Postman.

## 2. Kiến trúc và các thành phần đã triển khai

Loan Service được tổ chức theo các lớp:

- `api`: controller, request/response DTO và exception handler.
- `application`: use case và command cho các thao tác mượn, trả, gia hạn.
- `domain`: aggregate `Loan`, entity lịch sử, saga, outbox và các enum nghiệp vụ.
- `infrastructure`: JPA repository, specification và HTTP adapter gọi các service khác.

Các model nghiệp vụ chính:

- `Loan`: lưu thành viên, sách, bản sao, thời gian mượn/trả, hạn trả và trạng thái.
- `LoanStatusHistory`: lưu lịch sử thay đổi trạng thái.
- `SagaOutbox`: lưu các sự kiện cần phát ra message broker.
- `SagaLog`: nền tảng theo dõi các bước xử lý saga.
- `Reservation`: nền tảng cho chức năng đặt trước trong giai đoạn tiếp theo.

## 3. Business rule đã áp dụng

### 3.1. Khi mượn sách

1. `memberId`, `bookId`, `bookType` và `idempotencyKey` là bắt buộc.
2. Nếu `idempotencyKey` đã tồn tại, hệ thống trả lại kết quả cũ để tránh tạo loan trùng.
3. Loan Service lấy chính sách của thành viên từ Member Service.
4. Thành viên có `outstandingBalance > 0` không được mượn.
5. Số loan ở trạng thái `BORROWED` hoặc `OVERDUE` không được vượt `borrowingLimit`.
6. Hạn trả được tính theo `loanPeriodDays` của thành viên.
7. Sách vật lý phải có một bản sao ở trạng thái `AVAILABLE`.
8. Bản sao được chọn sẽ chuyển sang `BORROWED`.
9. Sách số không cần giữ một bản sao vật lý.
10. Loan mới có trạng thái `BORROWED`, `renewalCount = 0` và tối đa 3 lần gia hạn.
11. Nếu lưu loan thất bại sau khi giữ bản sao, hệ thống cố gắng trả bản sao về `AVAILABLE`.

### 3.2. Khi trả sách

1. Loan phải tồn tại và chưa được trả.
2. Hệ thống ghi `returnedAt`, chuyển trạng thái sang `RETURNED` và lưu lịch sử.
3. Bản sao vật lý được chuyển lại thành `AVAILABLE`.
4. Trả đúng hạn tạo `BookReturnedEvent`.
5. Trả quá hạn tạo `BookReturnedLateEvent`.
6. Không cho phép trả cùng một loan lần thứ hai.

### 3.3. Khi gia hạn

1. Chỉ loan đang ở trạng thái `BORROWED` mới được gia hạn.
2. Không vượt quá số lần gia hạn tối đa.
3. Mỗi lần gia hạn cộng thêm 14 ngày vào hạn trả.
4. Hệ thống tăng `renewalCount`, lưu lịch sử và tạo `LoanRenewedEvent`.

## 4. API đã hoàn thiện

Base URL khi gọi trực tiếp Loan Service:

```text
http://localhost:8084/api/loan
```

Nếu chạy qua API Gateway và Eureka:

```text
http://localhost:8080/api/loan
```

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `POST` | `/api/loan` | Mượn sách |
| `POST` | `/api/loan/{loanId}/return` | Trả sách |
| `POST` | `/api/loan/{loanId}/renew` | Gia hạn |
| `GET` | `/api/loan/{loanId}` | Xem chi tiết loan |
| `GET` | `/api/loan?page=0&size=20` | Xem danh sách có phân trang |
| `GET` | `/api/loan/member/{memberId}` | Xem lịch sử theo thành viên |

### Request mượn sách vật lý

```json
{
  "memberId": "postman-student-001",
  "bookId": 9001,
  "bookType": "PHYSICAL",
  "idempotencyKey": "postman-borrow-001"
}
```

Mỗi lần muốn tạo một loan mới phải đổi `idempotencyKey`.

### Request trả sách

```json
{
  "idempotencyKey": "postman-return-001"
}
```

### Request gia hạn

Request không cần body. Có thể thêm header để lưu người thực hiện:

```text
X-Actor-Id: postman-user
```

## 5. Tích hợp với các service khác

### Member Service

Loan Service gọi endpoint nội bộ:

```text
GET http://localhost:8083/api/v1/members/internal/{memberId}
X-Internal-Api-Key: local-dev-internal-key
```

Dữ liệu được sử dụng gồm `borrowingLimit`, `loanPeriodDays` và `outstandingBalance`.

Để xử lý lỗi `401 Unauthorized`, đã thực hiện:

- Tạo endpoint `/api/v1/members/internal/{memberId}` dành cho giao tiếp service-to-service.
- Cho phép endpoint này đi qua Spring Security mà không cần JWT người dùng.
- Bắt buộc kiểm tra header `X-Internal-Api-Key` trong controller.
- Loan Service tự gửi API key khi gọi Member Service.
- Cả hai service dùng cùng biến môi trường `INTERNAL_API_KEY`.

### Catalog Service

Loan Service gọi:

```text
GET   http://localhost:8082/api/catalog/books/{bookId}/copies?size=100
PATCH http://localhost:8082/api/catalog/book-copies/{copyId}/status
```

Khi mượn, bản sao chuyển từ `AVAILABLE` sang `BORROWED`. Khi trả hoặc rollback, bản sao chuyển về `AVAILABLE`.

## 6. Cấu hình đã sửa

- Loan Service chạy mặc định ở cổng `8084`.
- Catalog Service chạy ở cổng `8082`.
- Member Service chạy ở cổng `8083`.
- API Gateway chạy ở cổng `8080`.
- Eureka Server chạy ở cổng `8761`.
- Config Server chạy ở cổng `8888`.
- Chuyển Loan Service sang PostgreSQL driver.
- Thêm H2 dành riêng cho test.
- Cấu hình `springdoc-openapi` phiên bản `2.8.5` tương thích Spring Boot.
- Cho phép bật/tắt Eureka bằng `EUREKA_ENABLED`.
- Cho phép cấu hình URL của Eureka bằng `EUREKA_URL`.
- Bật `maven.compiler.parameters=true` trong parent POM.
- Ghi rõ tên `@PathVariable("bookId")` và `@PathVariable("memberId")` tại các endpoint liên service.

Các biến môi trường quan trọng:

| Biến | Giá trị mặc định |
| --- | --- |
| `LOAN_SERVICE_PORT` | `8084` |
| `LOAN_DB_URL` | `jdbc:postgresql://localhost:5432/loan_service_dbs` |
| `LOAN_DB_USERNAME` | `postgres` |
| `LOAN_DB_PASSWORD` | `123` |
| `CATALOG_SERVICE_URL` | `http://localhost:8082` |
| `MEMBER_SERVICE_URL` | `http://localhost:8083` |
| `INTERNAL_API_KEY` | `local-dev-internal-key` |
| `EUREKA_ENABLED` | `true` |
| `EUREKA_URL` | `http://localhost:8761/eureka/` |

## 7. Database phục vụ kiểm thử

Tạo ba database PostgreSQL cần thiết:

```sql
CREATE DATABASE loan_service_dbs;
CREATE DATABASE catalog_service_dbs;
CREATE DATABASE digilib_member;
```

Sau khi các service khởi động và tạo schema, chạy file:

```text
services/loan-service/POSTMAN_TEST_DATA.sql
```

File này tạo:

- Thành viên hợp lệ `postman-student-001`, không có dư nợ.
- Thành viên `postman-debt-member-001`, có dư nợ để kiểm tra rule từ chối mượn.
- Sách `9001`.
- Hai bản sao `9001` và `9002` ở trạng thái `AVAILABLE`.
- Dọn dữ liệu Loan Service từ lần test trước.

Ví dụ chạy bằng `psql` tại thư mục gốc project:

```powershell
psql -U postgres -f services/loan-service/POSTMAN_TEST_DATA.sql
```

## 8. Thứ tự chạy để test bằng Postman

### Cách 1: Chạy trực tiếp, không dùng Eureka và Config Server

Đặt biến môi trường `EUREKA_ENABLED=false`, sau đó chạy theo thứ tự:

1. PostgreSQL.
2. Catalog Service tại `8082`.
3. Member Service tại `8083`.
4. Loan Service tại `8084`.
5. Gửi request tới `http://localhost:8084/api/loan`.

`INTERNAL_API_KEY` của Loan Service và Member Service phải giống nhau.

### Cách 2: Chạy đầy đủ microservices

1. PostgreSQL.
2. Config Server tại `8888`.
3. Eureka Server tại `8761`.
4. Catalog Service, Member Service và Loan Service.
5. API Gateway tại `8080`.
6. Gửi request tới `http://localhost:8080/api/loan`.

## 9. Các lỗi đã gặp và cách xử lý

### `ECONNREFUSED 127.0.0.1:8082`

Nguyên nhân: Loan Service cần gọi Catalog Service nhưng không có ứng dụng lắng nghe ở cổng `8082`.

Cách xử lý:

- Khởi động Catalog Service trước khi gửi request mượn sách vật lý.
- Kiểm tra Catalog Service thực sự chạy ở `8082`.
- Nếu chạy cổng khác, đặt lại `CATALOG_SERVICE_URL`.

### `401 Unauthorized` khi gọi Member Service

Nguyên nhân: endpoint Member Service ban đầu yêu cầu JWT, trong khi request nội bộ từ Loan Service không có access token.

Cách xử lý đã triển khai: endpoint nội bộ và header `X-Internal-Api-Key` như mô tả ở mục 5.

### `IllegalArgumentException: Name for argument of type [java.lang.Long] not specified`

Nguyên nhân: Java không giữ tên tham số khi compile hoặc `@PathVariable` không khai báo tên rõ ràng.

Cách xử lý:

- Bật `maven.compiler.parameters=true`.
- Dùng `@PathVariable("bookId")` và `@PathVariable("memberId")` rõ ràng.
- Build lại các service sau khi sửa.

### Gọi `GET http://localhost:8084/api/loan` nhưng không thấy kết quả mong muốn

Endpoint `GET` chỉ lấy danh sách, không tạo loan. Muốn mượn sách phải dùng `POST`, chọn Body → raw → JSON và đặt `Content-Type: application/json`.

## 10. Kiểm thử tự động

Đã bổ sung cấu hình test bằng H2 và tắt Config Server/Eureka trong môi trường test. Các trường hợp đã có test gồm:

- Spring context khởi động thành công.
- Tạo loan với giá trị mặc định hợp lệ.
- Trả sách thành công và chặn trả lần hai.
- Chặn gia hạn loan đã trả.

Chạy test bằng Maven Wrapper của Loan Service:

```powershell
Set-Location services\loan-service
.\mvnw.cmd test
```

Hoặc nếu máy đã cài Maven:

```powershell
mvn -pl services/loan-service -am test
```

## 11. Các phần chưa hoàn thiện

- Hàng đợi reservation và quy tắc ưu tiên Lecturer.
- Hủy, hoàn tất và tự động hết hạn reservation.
- Scheduled job tự động chuyển loan quá hạn.
- Gửi nhắc nhở trước hạn trả.
- Publisher đọc outbox và gửi sự kiện sang Kafka/RabbitMQ.
- Retry, dead-letter queue và consumer sự kiện.
- Tích hợp đầy đủ Fine Service và Notification Service.
- Phân quyền Student, Lecturer và Librarian tại Loan API.
- Kiểm tra reservation trước khi gia hạn.
- Chuyển schema Loan Service sang Flyway migration.
- Integration test với PostgreSQL và các service thật.
- Xử lý nguyên tử việc chọn bản sao để tránh hai request đồng thời cùng giữ một bản sao.
- Báo cáo thống kê loan và overdue.
- Các màn hình Loan Service ở frontend chưa nằm trong repository backend này và cần thực hiện trong workspace frontend.

## 12. Commit message đề xuất

Nếu commit toàn bộ phần Loan Service và các sửa đổi liên service:

```text
feat(loan-service): complete loan workflows and service integrations
```

Commit body đề xuất:

```text
- implement borrow, return, renew, and query APIs
- enforce member eligibility and borrowing limits
- synchronize physical copy status with catalog service
- secure internal member lookup with a shared API key
- add loan history, outbox events, tests, and Postman data
- fix path variable metadata and local service configuration
```

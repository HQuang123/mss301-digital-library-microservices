# 📚 Digital Library Microservices Backend (`mss301-digital-library-microservices`)

## 📖 Giới thiệu
Đây là kho lưu trữ trung tâm (Monorepo) chứa toàn bộ mã nguồn Backend của hệ thống **Digital Library Management System**. 

Dự án được phát triển theo kiến trúc **Microservices** kết hợp cấu trúc **Maven Multi-module**, giúp tối ưu hóa việc quản lý code tập trung, chia sẻ thư viện chung nhưng vẫn đảm bảo tính độc lập khi triển khai (Selective Build & Deploy) cho từng service.

## 🏢 Kiến trúc thư mục
Dự án áp dụng cấu trúc phân tách rõ ràng giữa hạ tầng kỹ thuật và logic nghiệp vụ:

```text
digilib/
├── .github/                 # Chứa các luồng CI/CD tự động (GitHub Actions)
│
├── services/                    # 📦 CORE BUSINESS SERVICES (Nghiệp vụ thư viện)
│   ├── book-service/     # Quản lý danh mục sách
│   ├── member-service/      # Quản lý hồ sơ độc giả
│   ├── loan-service/   # Quản lý mượn/trả sách
│   ├── notification-service/# Gửi email/thông báo nhắc nhở
│   └── fine-service/        # Quản lý các khoản phạt trễ hạn
│
├── infra/                # ⚙️ INFRASTRUCTURE SERVICES (Hạ tầng hệ thống)
│   ├── eureka-server/       # Service Registry & Discovery (Trái tim hệ thống)
│   └── api-gateway/         # Cổng giao tiếp duy nhất cho Frontend
│
├── .gitattributes           # Chuẩn hóa ký tự xuống dòng (LF/CRLF) cho toàn nhóm
├── .gitignore               # Lọc file rác, file build, cấu hình IDE
└── pom.xml                  # Root Parent POM (Quản lý các module)
```

## 🛠 Tech Stack
* **Ngôn ngữ:** Java 21
* **Framework chính:** Spring Boot 3.x
* **Cloud & Microservices:** Spring Cloud Netflix (Eureka), Spring Cloud Gateway
* **Quản lý dự án:** Maven
* **Database:** MySQL / PostgreSQL (Database-per-service)

## ⚙️ Hướng dẫn cài đặt & Khởi chạy (Local)

### 1. Yêu cầu hệ thống
* JDK 21
* Maven 3.8+
* IDE: IntelliJ IDEA (Khuyến nghị) hoặc VS Code
* Git

### 2. Clone và Load dự án
Mở terminal và chạy lệnh:
```bash
git clone https://github.com/PhuNguyen12345/mss301-digital-library-microservices.git
cd mss301-digital-library-microservices
```
*Mở thư mục này bằng IntelliJ IDEA. IDE sẽ tự động đọc file `pom.xml` gốc và tải các module con về.*

### 3. Thứ tự khởi động BẮT BUỘC
Vì đây là hệ thống Microservices, các service phụ thuộc lẫn nhau để đăng ký mạng. Bạn phải chạy theo đúng thứ tự sau:
1. **Eureka Server** (`platform/eureka-server`): Khởi động đầu tiên. Đợi đến khi truy cập được `http://localhost:8761`.
2. **API Gateway** (`infra/api-gateway`): Khởi động thứ hai.
3. **Các Business Services** (`services/...`): Khởi động các service bạn đang phụ trách phát triển. Chúng sẽ tự động tìm thấy Eureka và đăng ký mình lên hệ thống.

## ⚠️ Quy định làm việc nhóm (Convention)
* **Ký tự xuống dòng:** Hệ thống đã được cấu hình `.gitattributes` để tự động ép kiểu LF. Vui lòng không tự ý sửa đổi file này để tránh conflict toàn dự án.
* **Biến môi trường:** Mỗi service sẽ có file `application.properties` riêng biệt nằm trong `src/main/resources`. Chú ý cấu hình Port và Database tương ứng với service của mình, không dùng chung Database.
* **Giao tiếp liên dịch vụ (Inter-service):** Các service giao tiếp với nhau thông qua tên miền đăng ký trên Eureka (không hardcode `localhost:port`).

## 👥 Đội ngũ phát triển
Dự án được phát triển bởi nhóm sinh viên chuyên ngành Kỹ thuật Phần mềm (Software Engineering), Đại học FPT:
* **Hạ tầng & Tích hợp (Tech Lead):** Nguyễn An Phú
* **Phát triển Core Services:** Nguyễn Quang Huy, Nguyễn Công Hoàng, Nguyễn Bảo Nam, Hà Thị Duyên
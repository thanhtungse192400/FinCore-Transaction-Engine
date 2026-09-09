# 💳 FinCore Transaction Engine - Backend Core Service

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg?style=for-the-badge&logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.4.3-blue.svg?style=for-the-badge&logo=springsecurity)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16+-blue.svg?style=for-the-badge&logo=postgresql)](https://www.postgresql.org/)
[![JWT](https://img.shields.io/badge/JWT-HS256-black.svg?style=for-the-badge&logo=jsonwebtokens)](https://jwt.io/)
[![Build Status](https://img.shields.io/badge/Build-Passing-success.svg?style=for-the-badge&logo=githubactions)](https://github.com/)

**FinCore Transaction Engine** là hệ thống Backend lõi mô phỏng kiến trúc Ngân hàng số & Ví điện tử (Digital Banking & Financial Ledger Core). Hệ thống được thiết kế theo chuẩn **Layered Architecture (Phân tầng)** và các nguyên lý **SOLID**, tập trung vào tính toàn vẹn dữ liệu tài chính, hiệu năng cao, và bảo mật đa tầng.

---

## 🚀 Key Highlights & Architectural Strengths (Điểm Nổi Bật Kỹ Thuật)

- 🔒 **Stateless Authentication & Authorization**: Triển khai cơ chế xác thực kép **Access Token (JWT HS256, 15m)** và **Refresh Token Rotation - RTR (7d)** lưu trữ cơ sở dữ liệu.
- 🛡️ **Token Blacklist System**: Vô hiệu hóa Access Token ngay lập tức khi đăng xuất bằng cơ chế Blacklist + Interceptor Filter, giải quyết triệt để nhược điểm của Stateless JWT.
- ⚡ **Financial Concurrency Control**: Tích hợp **Optimistic Locking (`@Version`)** trên thực thể Ví tiền (`Wallet`) kết hợp kiểu dữ liệu `BigDecimal`, ngăn chặn tuyệt đối lỗi *Double Spending* và *Race Conditions* khi giao dịch tài chính đồng thời.
- 📬 **Two-Factor Verification (2FA / OTP)**: Sinh mã OTP 6 chữ số bằng bộ sinh số ngẫu nhiên mật mã học (`SecureRandom`), tích hợp gửi qua Gmail SMTP với HTML template chuyên nghiệp và cơ chế Fallback Console an toàn.
- 🔐 **Transaction PIN Protection**: Bảo vệ thao tác giao dịch tài chính nhạy cảm bằng mã PIN 6 số riêng biệt, mã hóa an toàn bằng thuật toán **BCrypt** (Salted Hashing).
- 🧹 **Automated Database Maintenance**: Thiết lập tác vụ ngầm chạy định kỳ (`@Scheduled` cron job lúc 02:00 AM) dọn sạch OTP hết hạn, token bị thu hồi, duy trì hiệu năng truy vấn tối đa.
- 🌐 **12-Factor App Standard**: Quản trị cấu hình và biến môi trường bảo mật hoàn toàn qua file `.env`, không lộ lọt credentials trên Git.
- 📋 **Centralized Exception Handling**: Bộ bắt lỗi tập trung (`@RestControllerAdvice`) chuẩn hóa toàn bộ phản hồi lỗi theo format RFC với mã trạng thái HTTP chuẩn mực.

---

## 🛠️ Technology Stack (Ngăn Xếp Công Nghệ)

| Phân tầng / Thành phần | Công nghệ / Thư viện | Vai trò & Mục đích |
| :--- | :--- | :--- |
| **Language** | Java 21 (LTS) | Virtual Threads, Pattern Matching, Records, hiệu năng tối ưu. |
| **Framework** | Spring Boot 3.4.x | Nền tảng cốt lõi, Dependency Injection (IoC), Auto-configuration. |
| **Security** | Spring Security 6.4.x | Cấu hình Security Filter Chain, Session Stateless, CORS & CSRF. |
| **Database & ORM** | PostgreSQL & Spring Data JPA (Hibernate 6) | Quản lý quan hệ dữ liệu ACID, Indexing, Dirty Checking, Schema auto-ddl. |
| **Token Authentication** | JJWT (`io.jsonwebtoken` 0.12.6) | Ký & verify chữ ký số cho JSON Web Token. |
| **Password Hashing** | Spring Security Crypto (`BCryptPasswordEncoder`) | Băm mật khẩu & mã PIN với Salt ngẫu nhiên. |
| **Mail Service** | Spring Boot Starter Mail (JavaMailSender) | Gửi email xác thực tài khoản qua giao thức SMTP. |
| **Validation** | Hibernate Validator (`jakarta.validation`) | Kiểm tra tính hợp lệ của DTO đầu vào (Regex Phone, Email, Size, NotBlank). |
| **Task Scheduling** | Spring Scheduling (`@EnableScheduling`) | Quản lý các tác vụ cron job chạy định kỳ. |
| **API Documentation** | SpringDoc OpenAPI 3 / Swagger UI | Tự động sinh tài liệu tương tác API trực quan. |
| **Utilities & Testing** | Lombok, JUnit 5, Mockito | Giảm thiểu mã lặp (Boilerplate) và Unit Test 100% Mock dependencies. |

---

## 🏛️ System Architecture (Kiến Trúc Hệ Thống)

```
                  ┌───────────────────────────────────────────────┐
                  │          Client (Web / Mobile / Postman)      │
                  └───────────────────────┬───────────────────────┘
                                          │ HTTP / REST JSON
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            Security & Filter Layer                              │
│  - JwtAuthenticationFilter (Kiểm tra Access Token & đối chiếu Token Blacklist)  │
│  - SecurityFilterChain (Phân tách Public Endpoints & Protected Endpoints)       │
└─────────────────────────────────────────┬───────────────────────────────────────┘
                                          │
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                               Controller Layer                                  │
│  - AuthController: Tiếp nhận Request DTO, Kích hoạt @Valid, Trả về ApiResponse  │
└─────────────────────────────────────────┬───────────────────────────────────────┘
                                          │
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           Service Interface Layer                               │
│  - AuthService, UserService, WalletService, EmailService, TokenBlacklistService │
├─────────────────────────────────────────────────────────────────────────────────┤
│                        Service Implementation Layer                             │
│  - AuthServiceImpl, UserServiceImpl, WalletServiceImpl, EmailServiceImpl...     │
│  - Xử lý Business Logic, Hashing mật khẩu/PIN, Sinh mã OTP, Quản lý Transaction │
└─────────────────────────────────────────┬───────────────────────────────────────┘
                                          │
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                               Repository Layer                                  │
│  - UserRepository, WalletRepository, RefreshTokenRepository, TokenBlacklistRepo │
│  - Giao tiếp Database thông qua Spring Data JPA / Hibernate                     │
└─────────────────────────────────────────┬───────────────────────────────────────┘
                                          │
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              PostgreSQL Database                                │
│  - Tables: users, wallets, refresh_tokens, email_verification_otps, blacklist   │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 📡 API Reference & Endpoints

Tất cả các API trả về cấu trúc phản hồi chuẩn mực:
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": "2026-09-09T10:00:00Z"
}
```

### 🔐 Authentication & Account Management (`/api/v1/auth`)

| Method | Endpoint | Auth Required | Request Body / Params | Mô tả |
| :---: | :--- | :---: | :--- | :--- |
| `POST` | `/api/v1/auth/register` | ❌ Public | `RegisterRequest` | Đăng ký tài khoản (họ tên, sđt, email, mật khẩu) & gửi mã OTP 6 số qua email |
| `POST` | `/api/v1/auth/verify-otp` | ❌ Public | `VerifyOtpRequest` | Xác thực mã OTP, kích hoạt tài khoản `ACTIVE`, tự động mở Ví VND & cấp Tokens |
| `POST` | `/api/v1/auth/resend-otp` | ❌ Public | `ResendOtpRequest` | Gửi lại mã OTP 6 số mới vào email |
| `POST` | `/api/v1/auth/login` | ❌ Public | `LoginRequest` | Đăng nhập bằng Email & Password -> Nhận Access Token & Refresh Token |
| `POST` | `/api/v1/auth/refresh-token` | ❌ Public | `RefreshTokenRequest` | Đổi Access Token mới thông qua Refresh Token hợp lệ (RTR Mechanism) |
| `POST` | `/api/v1/auth/logout` | 🔒 Bearer | Header `Authorization` | Đăng xuất, hủy toàn bộ Refresh Token và đưa Access Token vào Blacklist |
| `GET` | `/api/v1/auth/me` | 🔒 Bearer | Header `Authorization` | Lấy thông tin hồ sơ cá nhân và số ví, số dư tài chính của User hiện tại |
| `POST` | `/api/v1/auth/set-pin` | 🔒 Bearer | `SetPinRequest` | Cài đặt / Thay đổi mã PIN giao dịch 6 số (mã hóa BCrypt) |

---

## ⚙️ Local Development & Setup Guide

### 1. Yêu Cầu Môi Trường
- **JDK 21** trở lên.
- **PostgreSQL 15+** (hoặc chạy qua Docker).
- **Maven 3.9+** (hoặc dùng `mvnw` đính kèm dự án).

### 2. Cấu Hình Biến Môi Trường (`.env`)
Tạo file `.env` tại thư mục gốc của project (sử dụng mẫu `.env.example`):

```env
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=fincore_db
DB_USERNAME=postgres
DB_PASSWORD=your_postgres_password

# JWT Security
JWT_SECRET_KEY=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
JWT_ACCESS_TOKEN_EXPIRATION=900000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# OTP & Mail (Gmail SMTP)
OTP_EXPIRATION_MINUTES=10
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_gmail@gmail.com
MAIL_PASSWORD=your_gmail_app_password
```

### 3. Build & Chạy Ứng Dụng
```bash
# Clone repository
git clone https://github.com/thanhtungse192400/FinCore-Transaction-Engine.git
cd FinCore-Transaction-Engine/FinCore-BE

# Chạy Unit Tests
./mvnw clean test

# Khởi chạy ứng dụng
./mvnw spring-boot:run
```

- Server mặc định chạy tại: `http://localhost:8080`
- Swagger UI Interactive API Docs: `http://localhost:8080/swagger-ui/index.html`

---

## 🧪 Unit & Integration Testing

Toàn bộ các luồng nghiệp vụ cốt lõi đều được kiểm thử tự động với Mockito & JUnit 5:
- **`AuthServiceTest`**: Kiểm thử luồng Đăng ký, Xác thực OTP, Kích hoạt ví, Đăng nhập, Đăng xuất, Thu hồi token.
- **`JwtTokenProviderTest`**: Kiểm thử tính hợp lệ của chữ ký JWT, trích xuất Claims, sinh Refresh Token ngẫu nhiên an toàn.
- **`ScheduledCleanupServiceTest`**: Kiểm thử tác vụ dọn dẹp cơ sở dữ liệu ngầm.

```bash
[INFO] Results:
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 👤 Author & Contact

- **Author**: Nguyen Thanh Tung
- **Project**: FinCore Transaction Engine
- **Role**: Backend Developer / Software Engineer

# BÁO CÁO THIẾT KẾ HẠ TẦNG CONFIG SERVER VÀ EUREKA SERVER
## HỆ THỐNG ĐẶT VÉ XEM PHIM TRỰC TUYẾN (CINEMAX)

---

### 1. TỔNG QUAN TÌNH HUỐNG NGHIỆP VỤ

**CinemaX** là một hệ thống đặt vé xem phim trực tuyến phục vụ hàng triệu người dùng. Để đáp ứng tính sẵn sàng cao, khả năng mở rộng linh hoạt và quản lý cấu hình tập trung, hệ thống chuyển sang kiến trúc Microservices với 4 service nghiệp vụ chính và lớp hạ tầng bao gồm Spring Cloud Config Server và Eureka Service Discovery.

#### 4 Service nghiệp vụ chính:
1. **`movie-service`**: Quản lý danh mục phim, lịch chiếu, danh sách phòng chiếu và sơ đồ ghế.
2. **`booking-service`**: Xử lý logic đặt vé, giữ ghế tạm thời, tính giá tiền và tạo đơn đặt vé.
3. **`payment-service`**: Tích hợp các cổng thanh toán (VNPay, Momo, ZaloPay, Visa), xử lý giao dịch và hoàn tiền.
4. **`notification-service`**: Gửi email vé điện tử (kèm mã QR), gửi tin nhắn SMS thông báo giờ chiếu và thông báo khuyến mãi.

---

### 2. DỰ TRÙ CẤU HÌNH TẬP TRUNG CHO CÁC SERVICE

Mỗi service quản lý ít nhất 3 tham số cấu hình tĩnh & động được lưu trữ tại `config-repo`:

| Service Name | Tên thuộc tính (Key) | Giá trị mẫu | Ý nghĩa nghiệp vụ |
|---|---|---|---|
| **movie-service** | `spring.datasource.url` | `jdbc:postgresql://db:5432/movie_db` | Kết nối CSDL danh mục phim |
| | `movie.catalog.cache-ttl-minutes` | `60` | Thời gian sống (TTL) bộ nhớ đệm danh sách phim |
| | `feature.recommendation-engine.enabled` | `true` | Feature flag bật/tắt gợi ý phim thông minh |
| **booking-service** | `spring.datasource.url` | `jdbc:postgresql://db:5432/booking_db` | Kết nối CSDL đặt vé |
| | `booking.seat-hold.timeout-seconds` | `300` | Thời gian giữ ghế tạm thời (5 phút) |
| | `feature.instant-discount.enabled` | `true` | Feature flag áp dụng mã giảm giá tức thì |
| **payment-service** | `payment.gateway.vnpay.timeout-ms` | `5000` | Timeout kết nối cổng thanh toán VNPay |
| | `payment.gateway.max-retry` | `3` | Số lần thử lại tối đa khi giao dịch lỗi |
| | `feature.crypto-payment.enabled` | `false` | Feature flag thanh toán bằng tiền điện tử |
| **notification-service** | `spring.mail.host` | `smtp.cinemax.com` | Cấu hình máy chủ Mail SMTP |
| | `notification.sms.provider-url` | `https://api.sms-brandname.vn` | Địa chỉ API gửi SMS |
| | `feature.async-email-queue.enabled` | `true` | Feature flag xử lý email bất đồng bộ qua Queue |

---

### 3. CẤU TRÚC GIT REPOSITORY CẤU HÌNH (`config-repo`)

Cấu trúc thư mục repository chứa tập tin cấu hình tuân thủ quy ước: `{application-name}-{profile}.yml` hoặc `{application-name}.yml`

```text
config-repo/
├── application.yml                    # Cấu hình chung cho toàn bộ hệ thống
├── movie-service.yml                  # Cấu hình chung của movie-service
├── movie-service-dev.yml              # Cấu hình môi trường dev
├── movie-service-prod.yml             # Cấu hình môi trường production
├── booking-service.yml                # Cấu hình chung của booking-service
├── booking-service-dev.yml            # Cấu hình môi trường dev
├── booking-service-prod.yml           # Cấu hình môi trường production
├── payment-service.yml                # Cấu hình chung của payment-service
├── payment-service-prod.yml           # Cấu hình môi trường production
├── notification-service.yml           # Cấu hình chung của notification-service
└── notification-service-prod.yml      # Cấu hình môi trường production
```

---

### 4. SƠ ĐỒ TỔNG THỂ HẠ TẦNG (CONFIG SERVER & EUREKA SERVICE DISCOVERY)

```text
+---------------------------------------------------------------------------------------------------------+
|                                             GIT REPOSITORY                                              |
|                                    (https://github.com/cinemax/config-repo)                             |
+---------------------------------------------------------------------------------------------------------+
                                                     | (1) Fetch & Load Configs
                                                     v
+---------------------------------------------------------------------------------------------------------+
|                                           CONFIG SERVER                                                 |
|                                          (Port: 8888)                                                   |
+---------------------------------------------------------------------------------------------------------+
       ^                                  ^                                  ^                                  ^
       | (2) Get Config                   | (2) Get Config                   | (2) Get Config                   | (2) Get Config
+--------------+                   +--------------+                   +--------------+                   +--------------+
| movie-service|                   |booking-service|                   |payment-service|                   |notification-s|
+--------------+                   +--------------+                   +--------------+                   +--------------+
       |                                  |                                  |                                  |
       | (3) Register                     | (3) Register                     | (3) Register                     | (3) Register
       +----------------------------------+----------------------------------+----------------------------------+
                                                     |
                                                     v
+---------------------------------------------------------------------------------------------------------+
|                                           EUREKA SERVER                                                 |
|                                          (Port: 8761)                                                   |
+---------------------------------------------------------------------------------------------------------+
                                                     |
              +--------------------------------------+--------------------------------------+
              | (4) Discovery & Inter-service Communication                                |
              v                                                                             v
   [booking-service] -------- (Lookup payment-service via Eureka) -------> [payment-service]
```

---

### 5. CHI TIẾT LUỒNG HOẠT ĐỘNG KHỞI ĐỘNG VÀ TRA CỨU

#### Luồng 1: Load Cấu hình từ Config Server
1. Khi `booking-service` khởi động, nó đọc tập tin `application.yml` nội bộ chứa `spring.config.import=optional:configserver:http://localhost:8888`.
2. Service gửi request tới Config Server đòi hỏi file cấu hình tương ứng với tên ứng dụng và profile (`booking-service-dev.yml`).
3. Config Server đọc Git Repo, clone/pull file cấu hình mới nhất và trả lại cho `booking-service` nạp vào Environment.

#### Luồng 2: Đăng ký và Tra cứu trên Eureka Server
1. `booking-service` đăng ký thông tin instance (AppName: `BOOKING-SERVICE`, IP/Host, Port) với Eureka Server tại `http://localhost:8761/eureka/`.
2. Định kỳ 30 giây, service gửi Heartbeat để thông báo trạng thái UP.
3. Khi `booking-service` cần gọi `payment-service`, nó không hardcode URL mà tra cứu trên Eureka Server tên service `PAYMENT-SERVICE` để lấy danh sách IP/Port khả dụng và thực hiện Client-side Load Balancing (Spring Cloud LoadBalancer).

#### Mở rộng Scale nhiều Instance:
Khi hệ thống chịu tải cao, scale `booking-service` thành 3 instance (Port 8081, 8082, 8083):
- Cả 3 instance đều đăng ký dưới tên `BOOKING-SERVICE` trên Eureka.
- Client/Gateway sử dụng LoadBalancer tự động điều phối request theo giải thuật Round-Robin.

---

### 6. HƯỚNG DẪN CHẠY DỰ ÁN

1. Khởi động Config Server:
   `cd config-server && ./mvnw spring-boot:run` (Lắng nghe tại port 8888)
2. Khởi động Eureka Server:
   `cd eureka-server && ./mvnw spring-boot:run` (Lắng nghe tại port 8761, UI tại http://localhost:8761)
3. Khởi động Booking Service:
   `cd booking-service && ./mvnw spring-boot:run` (Lắng nghe tại port 8081)
4. Truy cập API kiểm tra lấy cấu hình đã load từ Config Server:
   `GET http://localhost:8081/api/v1/bookings/config`
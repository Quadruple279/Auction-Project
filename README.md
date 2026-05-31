# 🏆 Hệ thống Đấu giá Trực tuyến
### Bài tập lớn – Lập trình Nâng cao | UET – VNU

---

## 📖 Giới thiệu

**Auction System** là một nền tảng đấu giá trực tuyến được xây dựng theo mô hình **Client–Server**, cho phép nhiều người dùng cùng tham gia cạnh tranh giá để mua sản phẩm hoặc dịch vụ trong một khoảng thời gian xác định. Hệ thống được phát triển bằng **Java**, sử dụng **JavaFX** cho giao diện phía Client và giao tiếp qua **Socket** với định dạng dữ liệu **JSON**.

Dự án áp dụng các nguyên lý lập trình hướng đối tượng (OOP), các Design Pattern phổ biến và kỹ thuật xử lý đồng thời (Concurrency) nhằm đảm bảo hệ thống hoạt động ổn định, an toàn và có khả năng mở rộng.

---

## Báo cáo PDF - Video Demo

 - **Báo cáo PDF:** [PDF](https://drive.google.com/file/d/1oQxCvGkq9cOyMwrsstBLRNooGxpAjl11/view?usp=sharing)
 - **Video demo:** [VIDEO](https://drive.google.com/file/d/1y-JK6F-vw8JvBRipwVZQ9HPekk2z7vPO/view?usp=sharing)

---

## 👥 Thành viên nhóm

| Họ và tên | Vai trò | Tài khoản |
|---|---|---|
| **Trần Quang Lâm** | Trưởng nhóm | Quadruple279 |
| **Nguyễn Quang Minh** | Backend / Logic | minhquangng091107 |
| **Nguyễn Xuân Sáng** | Frontend / Client | 25020344 |
| **Vũ Hải Nam** | Fullstack / Logic | vuhainampkjaz123 |

---

## ✨ Tính năng

### Chức năng bắt buộc

- **Quản lý người dùng** – Đăng ký, đăng nhập với phân quyền theo vai trò (Bidder, Seller, Admin)
- **Quản lý sản phẩm đấu giá** – Thêm, sửa, xóa sản phẩm; hiển thị đầy đủ thông tin (tên, mô tả, giá khởi điểm, giá hiện tại, thời gian)
- **Tham gia đấu giá** – Đặt giá theo thời gian thực, kiểm tra tính hợp lệ, cập nhật người dẫn đầu
- **Kết thúc phiên đấu giá** – Tự động đóng phiên khi hết giờ, xác định người thắng; quản lý trạng thái `OPEN → RUNNING → FINISHED → PAID / CANCELED`
- **Xử lý lỗi & ngoại lệ** – Xử lý đặt giá thấp hơn giá hiện tại, đấu giá khi phiên đóng, lỗi kết nối
- **Giao diện đồ họa (JavaFX)** – Danh sách phiên, chi tiết sản phẩm, màn hình đấu giá realtime, trang quản lý dành cho Seller

### Chức năng nâng cao

- **Auto-Bidding** – Đặt giá tự động với `maxBid` và `increment`; hệ thống tự trả giá thay người dùng, xử lý xung đột bằng PriorityQueue
- **Gia hạn phiên (Anti-sniping)** – Tự động gia hạn thêm Y giây nếu có bid mới trong X giây cuối trước khi kết thúc
- **Bid History Visualization** – Biểu đồ đường (line chart) hiển thị lịch sử giá theo thời gian thực

---

## 🗂️ Cấu trúc dự án

```
auction-big-project/
├── src/
│   └── main/
│       └── java/
│           ├── client/                  # Phía Client (JavaFX)
│           │   ├── controller/          # Xử lý sự kiện giao diện (MVC)
│           │   ├── AppLauncher.java     # Điểm khởi chạy ứng dụng
│           │   ├── AuctionDataTest.java # Dữ liệu kiểm thử
│           │   ├── ClientApp.java       # Ứng dụng Client chính
│           │   └── ClientSocket.java    # Quản lý kết nối Socket tới Server
│           │
│           ├── server/                  # Phía Server
│           │   ├── controller/          # Xử lý logic nghiệp vụ
│           │   ├── dao/                 # Truy cập dữ liệu (Data Access Object)
│           │   ├── exception/           # Các lớp ngoại lệ tùy chỉnh
│           │   ├── model/               # Các lớp thực thể (Entity/Model)
│           │   ├── network/             # Xử lý kết nối mạng & Socket
│           │   └── ServerApp.java       # Điểm khởi chạy Server
│           │
│           └── shared/                  # Dùng chung Client & Server
│               └── protocol/
│                   ├── AuctionEvent.java   # Sự kiện đấu giá
│                   ├── AuctionObserver.java # Interface Observer
│                   ├── Message.java         # Đối tượng tin nhắn trao đổi
│                   └── MessageType.java     # Enum loại tin nhắn
│
├── resources/                           # Tài nguyên (FXML, CSS, hình ảnh)
├── test/                                # Unit Test (JUnit)
├── auctions.json                        # Dữ liệu đấu giá (JSON)
├── pom.xml                              # Cấu hình Maven
└── README.md
```

---

## 🔐 Các loại tài khoản

| Vai trò | Quyền hạn |
|---|---|
| **Bidder** | Xem danh sách phiên đấu giá · Đặt giá · Sử dụng Auto-Bidding · Theo dõi lịch sử giá |
| **Seller** | Tất cả quyền của Bidder · Thêm / sửa / xóa sản phẩm đấu giá · Xem kết quả phiên |
| **Admin** | Toàn quyền quản trị hệ thống · Quản lý tài khoản người dùng · Giám sát và can thiệp các phiên đấu giá |

---

## 🛠️ Công nghệ sử dụng

- **Ngôn ngữ:** Java 17+
- **GUI:** JavaFX + FXML
- **Giao tiếp mạng:** Java Socket (TCP) · JSON
- **Build tool:** Maven
- **Kiểm thử:** JUnit
- **CI/CD:** GitHub Actions
- **Coding convention:** Google Java Style Guide

## 🎨 Design Pattern

| Pattern | Ứng dụng |
|---|---|
| **Singleton** | Quản lý kết nối, Auction Manager |
| **Factory Method** | Tạo các loại Item (Electronics, Art, Vehicle, ...) |
| **Observer** | Realtime update giá và bid tới toàn bộ Client |
| **Strategy / Command** | Xử lý các loại bid khác nhau |

---

## 🚀 Hướng dẫn chạy

### Yêu cầu

- **Java 21+**
- **Maven 3.8+**
- ****
- Kết nối Internet (Server dùng MySQL trên Aiven Cloud – đã cấu hình sẵn)

---

**Bước 1 – Clone repository**
```bash
git clone https://github.com/Quadruple279/Auction-Project.git
cd Auction-Project
```

**Bước 2 – Cấu hình database**

```bash
File cấu hình kết nối DB nằm tại `src/main/resources/db.properties`.  
Database đã được host sẵn trên cloud
```

**Tải file db.properties ở đây:** [db.properties](https://drive.google.com/file/d/1H2eJBcBMle1fMWkZ7jzk55aaElfBkTWf/view?usp=sharing)

**Bước 3 – Build dự án**

```bash
mvn clean install -DskipTests
```

> Dùng `-DskipTests` để bỏ qua bước test (các unit test yêu cầu môi trường CI riêng).

**Bước 4 – Khởi động Server**

Mở **terminal thứ nhất**, chạy:

```bash
mvn exec:java -Dexec.mainClass="server.ServerApp"
```

Server sẽ lắng nghe kết nối tại cổng **8080**. Khi thấy dòng sau là thành công:
```
[O] Server đã sẵn sàng tại cổng 8080!
```

**Bước 5 – Khởi động Client**

Mở **terminal thứ hai** (giữ nguyên terminal Server), chạy:

```bash
mvn javafx:run -Djavafx.mainClass="client.AppLauncher"
```

Giao diện đăng nhập sẽ xuất hiện. Có thể mở nhiều cửa sổ Client cùng lúc để mô phỏng nhiều người dùng.

---

### Tài khoản có sẵn để test

| Vai trò | Username | Password |
|---|----------|---|
| Admin | `Admin`  | `admin123` |
| Seller | `Seller` | `123456` |
| Bidder | `Bidder` | `123456` |

> Các tài khoản trên đã có sẵn trong database. Nếu muốn tạo tài khoản mới, sử dụng chức năng **Đăng ký** trên giao diện Client.

---

> *Bài tập lớn – Lập trình Nâng cao · Trường Đại học Công nghệ – ĐHQGHN*

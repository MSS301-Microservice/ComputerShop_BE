# Hướng dẫn cài đặt & chạy dự án ComputerShop (MSS301 — Microservices)

Dự án gồm 2 repo tách biệt:
- **Backend**: https://github.com/MSS301-Microservice/ComputerShop_BE — monorepo chứa API Gateway + 6 service
- **Frontend**: https://github.com/MSS301-Microservice/ComputerShop_FE — React 19 + Vite SPA

> Lưu ý: đây là 2 repo hoàn toàn mới, tách riêng khỏi dự án SBA gốc. Đừng nhầm với `ComputerShop_BE`/`ComputerShop_FE` của dự án SBA (khác remote git, khác mục đích).

---

## 1. Kiến trúc & danh sách service

| Service | Thư mục trong repo BE | Port | Database (SQL Server) |
|---|---|---|---|
| API Gateway | `gateway/` | 8888 | — |
| Monolith (remnant: Payment/Installment, Warranty, Promotion, Blog, Chat, Reporting) | `monolith/` | 8080 | `ComputerShopDB` |
| User/Identity | `user-service/` | 8081 | `ComputerShopUserDB` |
| Catalog (Product/Brand/Category/Attribute) | `catalog-service/` | 8082 | `ComputerShopCatalogDB` |
| Cart | `cart-service/` | 8083 | `ComputerShopCartDB` |
| PC Builder | `pcbuilder-service/` | 8084 | `ComputerShopPCBuildDB` |
| Order | `order-service/` | 8085 | `ComputerShopOrderDB` |
| Frontend | (repo FE riêng) | 3000 | — |

Frontend **chỉ gọi qua Gateway** (`http://localhost:8888`), không gọi thẳng vào service nào khác. Kiến trúc chi tiết xem [`docs/microservices-analysis.md`](microservices-analysis.md) trong repo BE.

---

## 2. Yêu cầu môi trường

- **Java 21** (khớp với Lombok 1.18.30 — dùng bản JDK 21, không dùng JDK 24 trở lên vì sẽ lỗi biên dịch Lombok)
- **IntelliJ IDEA** (khuyến nghị — hướng dẫn dưới đây dùng IntelliJ để chạy 7 service cùng lúc bằng 1 nút bấm). Không bắt buộc cài Maven riêng, mỗi service đã có sẵn Maven Wrapper (`mvnw` / `mvnw.cmd`) dùng được cả khi chạy tay qua terminal.
- **SQL Server** cài native trên máy (không dùng Docker), lắng nghe cổng mặc định `1433`, đăng nhập bằng user `sa`
- **Node.js** ≥ 18 (khuyến nghị bản đang test: v22) + npm, cho Frontend

---

## 3. Clone dự án

```bash
mkdir MSS_PJ && cd MSS_PJ
git clone https://github.com/MSS301-Microservice/ComputerShop_BE.git
git clone https://github.com/MSS301-Microservice/ComputerShop_FE.git
```

Sau bước này sẽ có 2 thư mục độc lập: `ComputerShop_BE/` (chứa `gateway/`, `monolith/`, `user-service/`, `catalog-service/`, `cart-service/`, `pcbuilder-service/`, `order-service/`) và `ComputerShop_FE/`.

---

## 4. Tạo database

Kết nối SQL Server (SSMS / `sqlcmd` / Azure Data Studio) bằng tài khoản `sa`, chạy:

```sql
CREATE DATABASE ComputerShopDB;
CREATE DATABASE ComputerShopUserDB;
CREATE DATABASE ComputerShopCatalogDB;
CREATE DATABASE ComputerShopCartDB;
CREATE DATABASE ComputerShopPCBuildDB;
CREATE DATABASE ComputerShopOrderDB;
```

Chỉ cần tạo **database rỗng** — bảng bên trong sẽ tự sinh khi từng service khởi động lần đầu (`ddl-auto: update`), không cần chạy script SQL nào khác.

---

## 5. Lấy & cài đặt file `.env`

Mỗi service Backend đọc secret từ file `.env` đặt **ngay trong thư mục gốc của service đó** (cùng cấp với `pom.xml`) — file này bị `.gitignore`, **không nằm trong repo git**, phải copy tay.

### 5.1. Nhận folder `env` tổng

Người quản lý repo sẽ gửi 1 folder tên `env/` (qua Zalo/Drive/USB/...), cấu trúc như sau — **tên từng thư mục con khớp đúng tên thư mục service**, để bạn dễ đối chiếu khi copy:

```
env/
├── gateway/            (rỗng — Gateway không cần .env)
├── monolith/
│   └── .env
├── user-service/
│   └── .env
├── catalog-service/
│   └── .env
├── cart-service/
│   └── .env
├── pcbuilder-service/
│   └── .env
├── order-service/
│   └── .env
└── ComputerShop_FE/
    └── .env
```

Tải folder `env/` này về, đặt tạm ở đâu cũng được (khuyến nghị đặt ngay cạnh `ComputerShop_BE/` và `ComputerShop_FE/` cho dễ nhớ, ví dụ `MSS_PJ/env/`).

### 5.2. Copy đúng vị trí

Copy nội dung **từng thư mục con** trong `env/` sang đúng thư mục service tương ứng đã clone ở bước 3:

| Copy từ | Copy vào |
|---|---|
| `env/monolith/.env` | `ComputerShop_BE/monolith/.env` |
| `env/user-service/.env` | `ComputerShop_BE/user-service/.env` |
| `env/catalog-service/.env` | `ComputerShop_BE/catalog-service/.env` |
| `env/cart-service/.env` | `ComputerShop_BE/cart-service/.env` |
| `env/pcbuilder-service/.env` | `ComputerShop_BE/pcbuilder-service/.env` |
| `env/order-service/.env` | `ComputerShop_BE/order-service/.env` |
| `env/ComputerShop_FE/.env` | `ComputerShop_FE/.env` |
| *(gateway — bỏ qua, không cần)* | — |

Trên Windows có thể copy tay bằng Explorer (kéo-thả từng file), hoặc PowerShell cho nhanh (chỉnh lại 2 đường dẫn gốc cho đúng máy bạn):

```powershell
$env_src = "D:\MSS_PJ\env"
$be = "D:\MSS_PJ\ComputerShop_BE"
$fe = "D:\MSS_PJ\ComputerShop_FE"

Copy-Item "$env_src\monolith\.env"          "$be\monolith\.env"
Copy-Item "$env_src\user-service\.env"      "$be\user-service\.env"
Copy-Item "$env_src\catalog-service\.env"   "$be\catalog-service\.env"
Copy-Item "$env_src\cart-service\.env"      "$be\cart-service\.env"
Copy-Item "$env_src\pcbuilder-service\.env" "$be\pcbuilder-service\.env"
Copy-Item "$env_src\order-service\.env"     "$be\order-service\.env"
Copy-Item "$env_src\ComputerShop_FE\.env"   "$fe\.env"
```

### 5.3. Danh sách biến trong từng `.env` (để đối chiếu, không cần tự điền — file đã có sẵn)

| Service | Biến cần có |
|---|---|
| Gateway | *(không cần)* |
| Monolith | `DB_PASSWORD`, `SA_PASSWORD`, `JWT_SIGNER_KEY`, `JWT_VALID_DURATION`, `JWT_REFRESHABLE_DURATION`, `INTERNAL_API_KEY`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`, `VNP_TMN_CODE`, `VNP_HASH_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `EMAIL_ADDRESS`, `PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `NGROK_AUTHTOKEN` |
| User-service | `DB_PASSWORD`, `JWT_SIGNER_KEY`, `JWT_VALID_DURATION`, `JWT_REFRESHABLE_DURATION`, `INTERNAL_API_KEY`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `MAIL_USERNAME`, `MAIL_PASSWORD` |
| Catalog-service | `DB_PASSWORD`, `JWT_SIGNER_KEY`, `JWT_VALID_DURATION`, `JWT_REFRESHABLE_DURATION`, `INTERNAL_API_KEY`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` |
| Cart-service | `DB_PASSWORD`, `JWT_SIGNER_KEY`, `JWT_VALID_DURATION`, `JWT_REFRESHABLE_DURATION`, `INTERNAL_API_KEY` |
| PCBuilder-service | `DB_PASSWORD`, `JWT_SIGNER_KEY`, `JWT_VALID_DURATION`, `JWT_REFRESHABLE_DURATION`, `INTERNAL_API_KEY` |
| Order-service | `DB_PASSWORD`, `JWT_SIGNER_KEY`, `JWT_VALID_DURATION`, `JWT_REFRESHABLE_DURATION`, `INTERNAL_API_KEY` |
| Frontend | `VITE_API_BASE_URL` (trỏ về Gateway, ví dụ `http://localhost:8888/api/v1`), `VITE_GOOGLE_CLIENT_ID` |

**Quan trọng**: `JWT_SIGNER_KEY` và `INTERNAL_API_KEY` phải **giống hệt nhau** ở tất cả 6 service Backend — đây là secret dùng chung để xác thực JWT khách hàng và xác thực gọi nội bộ giữa các service. Không tự đổi giá trị khác nhau giữa các service.

---

## 6. Setup chạy 7 service cùng lúc bằng IntelliJ (khuyến nghị)

### 6.1. Mở project & load đủ 7 Maven module

1. Mở IntelliJ → **Open** → chọn thư mục `ComputerShop_BE` (thư mục cha, chứa cả 7 folder con).
2. IntelliJ thường tự phát hiện nhiều `pom.xml` lồng bên trong và hiện thông báo **"Maven projects need to be imported"** ở góc phải màn hình → bấm **Import/Load All**.
   - Nếu không thấy thông báo: chuột phải vào từng `pom.xml` (trong `gateway/`, `monolith/`, `user-service/`, `catalog-service/`, `cart-service/`, `pcbuilder-service/`, `order-service/`) → **Add as Maven Project**.
3. Đợi IntelliJ index xong (thanh loading dưới cùng chạy hết).

### 6.2. Tạo Run Configuration cho từng service

Mở lần lượt 7 file entrypoint sau, mỗi file bấm nút ▶️ xanh cạnh `public static void main` **một lần** (chỉ cần chạy 1 lần để IntelliJ tự tạo Run Configuration, sau đó có thể dừng ngay):

| Service | File chứa `main()` |
|---|---|
| Gateway | `gateway/.../GatewayApplication.java` |
| Monolith | `monolith/.../Sba301ComputerShopApplication.java` |
| User-service | `user-service/.../UserServiceApplication.java` |
| Catalog-service | `catalog-service/.../CatalogServiceApplication.java` |
| Cart-service | `cart-service/.../CartServiceApplication.java` |
| PCBuilder-service | `pcbuilder-service/.../PCBuildServiceApplication.java` |
| Order-service | `order-service/.../OrderServiceApplication.java` |

Sau bước này, panel **Services** (mở qua `View → Tool Windows → Services`, hoặc icon ở thanh công cụ trái) sẽ hiện nhóm **Spring Boot** chứa đủ 7 entry.

### 6.3. ⚠️ Bước quan trọng nhất: chỉnh lại Working Directory cho từng config

Mặc định IntelliJ có thể set sai Working Directory (trỏ về thư mục cha `ComputerShop_BE` thay vì đúng thư mục service) — khi đó app sẽ **không đọc được file `.env`** dù bạn đã copy đúng ở bước 5, dẫn đến lỗi kiểu `Login failed for user 'sa'` (SQL Server từ chối vì mật khẩu rỗng) dù `.env` hoàn toàn đúng.

Cách sửa:
1. **Run → Edit Configurations...**
2. Chọn từng config trong danh sách bên trái (VD: `OrderServiceApplication`)
3. Tìm mục **Working directory** (nếu không thấy, bấm **Modify options** → tick **Working directory**)
4. Sửa thành đúng đường dẫn thư mục service đó — ví dụ:

| Run Configuration | Working directory |
|---|---|
| `GatewayApplication` | `<đường-dẫn>\ComputerShop_BE\gateway` |
| `Sba301ComputerShopApplication` | `<đường-dẫn>\ComputerShop_BE\monolith` |
| `UserServiceApplication` | `<đường-dẫn>\ComputerShop_BE\user-service` |
| `CatalogServiceApplication` | `<đường-dẫn>\ComputerShop_BE\catalog-service` |
| `CartServiceApplication` | `<đường-dẫn>\ComputerShop_BE\cart-service` |
| `PCBuildServiceApplication` | `<đường-dẫn>\ComputerShop_BE\pcbuilder-service` |
| `OrderServiceApplication` | `<đường-dẫn>\ComputerShop_BE\order-service` |

5. Bấm **Apply → OK**.

### 6.4. Chạy tất cả 7 service bằng 1 nút

Trong panel **Services**, mục **Spring Boot** liệt kê sẵn cả 7 config (tick chọn hết nếu chưa tick) — bấm nút ▶️ **màu xanh ở góc trên-trái của chính panel Services** (không phải nút Run to ở toolbar trên cùng của IntelliJ, nút đó chỉ chạy 1 config đang chọn).

→ Cả 7 service khởi động song song, mỗi cái có tab log riêng ngay trong panel Services để theo dõi/dừng độc lập từng cái.

**Thứ tự khởi động không quan trọng** — các service chỉ gọi nhau qua REST lúc xử lý request, không gọi nhau lúc khởi động, nên chạy song song hoàn toàn bình thường.

### 6.5. (Thay thế) Chạy tay bằng terminal, không dùng IntelliJ

```bash
cd <thư-mục-service>
./mvnw spring-boot:run
```
(Windows PowerShell dùng `.\mvnw.cmd spring-boot:run`) — mở 7 cửa sổ terminal, mỗi cửa sổ `cd` vào 1 service rồi chạy lệnh trên.

---

## 7. Chạy Frontend

```bash
cd ComputerShop_FE
npm install
npm run dev
```

Mặc định chạy ở `http://localhost:3000` (nếu cổng bận sẽ tự nhảy sang 3001).

---

## 8. Kiểm tra nhanh sau khi chạy

**Kiểm tra cổng đã lên chưa** (PowerShell):
```powershell
Get-NetTCPConnection -State Listen | Where-Object {$_.LocalPort -in 8080,8081,8082,8083,8084,8085,8888} | Select-Object LocalPort,OwningProcess | Sort-Object LocalPort
```
Đủ 7 dòng (8080–8085, 8888) là cả 7 service đã lên.

**Gọi thử qua Gateway** (không gọi thẳng service lẻ):
```bash
curl http://localhost:8888/api/v1/products
```
Nếu trả về JSON danh sách sản phẩm (hoặc mảng rỗng) là hệ thống đã thông suốt Gateway → catalog-service.

---

## 9. Sự cố thường gặp

- **`Login failed for user 'sa'` dù `.env` đã có đúng `DB_PASSWORD`**: chạy qua IntelliJ mà quên set Working Directory (xem lại mục 6.3) — thử chạy `./mvnw spring-boot:run` bằng terminal trong đúng thư mục service để xác nhận `.env`/mật khẩu SQL Server không phải nguyên nhân.
- **Lỗi biên dịch Lombok kiểu `TypeTag :: UNKNOWN`**: đang dùng JDK 24 trở lên thay vì JDK 21 — chuyển `JAVA_HOME`/Project SDK về JDK 21.
- **Service báo lỗi thiếu placeholder `${...}` khi khởi động**: thiếu biến trong file `.env` của đúng service đó — đối chiếu lại bảng ở mục 5.3.
- **Gọi qua Gateway bị 404 dù service đích đang chạy**: kiểm tra đúng thứ tự route trong `gateway/src/main/resources/application.yml` (route cụ thể phải có `order` nhỏ hơn route catch-all).
- **`401/403 Unauthorized` khi gọi API nội bộ (`/internal/**`)**: `INTERNAL_API_KEY` không khớp giữa 2 service đang gọi nhau.
- **Console 1 service liên tục có log dù không thao tác gì**: bình thường nếu là `monolith` — FE có ChatWidget tự động poll tin nhắn mới mỗi vài giây chạy nền trên mọi trang, không liên quan hành động đang test.

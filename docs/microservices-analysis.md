# ComputerShop — Monolith → Microservices: Architecture Analysis

> Phase: **Phân tích & Thiết kế**. Không có dòng code triển khai microservice nào trong tài liệu này (ngoại trừ phần dọn secrets đã thực hiện theo yêu cầu — xem mục "Secrets Cleanup Log"). Chờ phê duyệt trước khi bắt đầu Phase triển khai.
>
> **Đã chốt theo phản hồi của chủ dự án (2026-07-13):**
> - Domain `Review` — bỏ, không đưa vào ranh giới service nào (chưa triển khai, dở dang).
> - Database — chỉ dùng **SQL Server**, profile MySQL (`application-server.yml`) là cấu hình quên xóa, không dùng tới.
> - Phạm vi tách — **5 service** (không phải 10), đúng 5 domain lõi đầu tiên, đổi tên `identity-service` → `user-service`.
> - Secrets hardcode — đã dọn trực tiếp trong codebase, chi tiết ở cuối tài liệu.

---

## Current Architecture Analysis

### Tổng quan
- **Backend**: Java 21, Spring Boot 3.5.6, Maven, layered monolith duy nhất (`controller → service → repository → entity`), context path `/api/v1`.
- **Database**: 1 database `ComputerShopDB` (SQL Server). Có file `application-server.yml` (MySQL) còn sót lại trong repo nhưng **không được dùng** — khuyến nghị xóa file này ở Giai đoạn 0 để tránh nhầm lẫn về sau. Không dùng Flyway/Liquibase — schema tự sinh bằng `hibernate.ddl-auto=update`.
- **Frontend**: React 19 + TypeScript + Vite, `react-router-dom` v7, không dùng Redux — Context API (`AuthContext`, `CartContext`, `CompareContext`). Lớp `api/` mirror gần như 1:1 các controller backend — đây là điểm thuận lợi lớn cho việc thiết kế API Gateway (FE đã quen giao tiếp qua một base URL + endpoint map tập trung).
- **Realtime**: WebSocket/STOMP (`/ws`) cho Chat — module duy nhất hiện có tính chất bất đồng bộ.
- **Không có** message broker (Kafka/RabbitMQ) — mọi giao tiếp giữa các "domain" hiện là in-process method call hoặc JPA relationship (FK), không phải API call.

### Package/Module structure (packages theo layer, không theo domain)
```
controller/   21 REST controllers
service/ +impl/  24 service interfaces + impl
repository/ + projection/  26 Spring Data JPA repositories
entity/       28 @Entity (27 sau khi loại Review)
dto/request, dto/response(+report)
mapper/       8 MapStruct mappers
enums/        11 enum
config/       Security, JWT, Swagger, Cloudinary, VNPay, WebSocket
exception/    Global exception handling
util/         VNPayUtil
```

### Authentication/Authorization hiện tại
- JWT tự ký HS512 bằng Nimbus (`CustomJwtDecoder`), không dùng issuer chuẩn OAuth2 dù có khai báo resource-server.
- Google OAuth2 login song song (kiểm tra token với Google, tự tạo `User` role mặc định).
- **Stateful blacklist**: bảng `InvalidatedToken` (jti) dùng để logout/revoke — đây là trạng thái tập trung, không phải stateless JWT thuần túy.
- Role model thô: `Role` (ADMIN/STAFF/MEMBER/USER) gắn `@ManyToOne` vào `User`, không có entity `Permission` riêng — authorization dùng `@PreAuthorize(hasRole/hasAnyRole)` cấp method.
- OTP (email) cho verify/forgot-password — entity `OtpVerification` + `EmailService` (SMTP Gmail).
- FE: không có route guard (`ProtectedRoute`) — kiểm soát truy cập admin/staff gần như chỉ dựa vào backend `@PreAuthorize`.

### Điểm mạnh của monolith hiện tại
- Domain boundary ở tầng entity/FK khá rõ ràng (Catalog, Cart, Order, Payment/Installment, Warranty, Promotion, Chat, Blog, Reporting, PC-Builder, User/Identity) — thuận lợi cho strangler-fig decomposition.
- FE đã tách lớp `api/services/*` theo domain — giảm thay đổi cần thiết khi chuyển sang Gateway.
- Transaction toàn vẹn dễ dàng (1 DB, 1 JVM) — đặc biệt hữu ích cho luồng Order → Payment → Warranty hiện đang dùng transaction cục bộ.

### Điểm yếu của monolith hiện tại
- Không có schema migration tool → khó version hóa schema khi tách DB.
- `Order` ↔ `Warranty` ↔ `Payment/Installment` liên kết trực tiếp qua JPA `@ManyToOne/@OneToOne` — sẽ phải chuyển thành API call hoặc event nếu các domain này được tách trong tương lai.
- Reporting join trực tiếp Order/Payment/Product — cần read-model riêng nếu tách.
- ~~Secrets hygiene: hardcoded SMTP password, `NGROK_AUTHTOKEN`, `SA_PASSWORD`~~ — **đã dọn, xem "Secrets Cleanup Log" cuối tài liệu.**
- File `application-server.yml` (MySQL) không dùng, nên xóa để tránh cấu hình chết gây nhầm lẫn.

---

## Business Domain Analysis

Phân tích theo nghiệp vụ thực tế (không theo tên package). 12 domain nghiệp vụ (đã loại Review — không triển khai):

### 1. User & Identity
- **Trách nhiệm**: đăng ký/đăng nhập (local + Google), phát hành/thu hồi JWT, quản lý Role, OTP, quên mật khẩu.
- **Phạm vi dữ liệu**: `User`, `Role`, `InvalidatedToken`, `OtpVerification`.
- **Quan hệ**: mọi domain khác đều tham chiếu `User` (owner/actor) — domain nền tảng, gần như không phụ thuộc ngược lại domain nào.
- **Mức độ phụ thuộc**: rất cao (là nền tảng), nhưng logic nghiệp vụ tự chứa nên tách độc lập được.

### 2. Catalog (Product/Brand/Category/Attribute)
- **Trách nhiệm**: quản lý sản phẩm, biến thể (variant), đơn vị serial hóa (item), hình ảnh, thuộc tính kỹ thuật (EAV), thương hiệu, danh mục cây (self-referential).
- **Phạm vi dữ liệu**: `Product`, `ProductVariant`, `ProductItem`, `ProductImage`, `Attribute`, `ProductVariantAttribute`, `Brand`, `Category`.
- **Quan hệ**: là domain "nguồn sự thật" được Cart, Order, PCBuild, Promotion tham chiếu (chỉ đọc `variantId`/`productId`).
- **Mức độ phụ thuộc**: domain lõi, độc lập cao, thay đổi chậm → ứng viên tốt để tách sớm.

### 3. PC Builder / Compatibility
- **Trách nhiệm**: cho phép người dùng build PC từ các variant, kiểm tra tương thích linh kiện theo `CompatibilityRule`.
- **Phạm vi dữ liệu**: `PCBuild`, `PCBuildItem`, `CompatibilityRule`.
- **Quan hệ**: đọc Catalog (variant + attribute), ghi vào Order khi build được đặt hàng (`/pc-builds/draft/order`).
- **Mức độ phụ thuộc**: phụ thuộc Catalog (đọc), được Order phụ thuộc (khi convert build → order).

### 4. Cart
- **Trách nhiệm**: giỏ hàng theo user, từng dòng tham chiếu variant.
- **Phạm vi dữ liệu**: `Cart`, `CartItem`.
- **Quan hệ**: phụ thuộc User (owner) và Catalog (variant, giá hiển thị).
- **Mức độ phụ thuộc**: nhẹ, vòng đời ngắn, dữ liệu có thể tái tạo.

### 5. Order Management
- **Trách nhiệm**: tạo đơn hàng, quản lý trạng thái, dòng đơn hàng gắn với `ProductItem` cụ thể (serial).
- **Phạm vi dữ liệu**: `Order`, `OrderItem`.
- **Quan hệ**: domain trung tâm nghiệp vụ — phụ thuộc User, Catalog (ProductItem), kích hoạt Payment/Installment, sinh ra Warranty.
- **Mức độ phụ thuộc**: cao nhất về mặt orchestration.

### 6. Payment & Installment/Financing *(không tách trong đợt này — xem "Phạm vi tách đợt này")*
- **Trách nhiệm**: xử lý thanh toán VNPay, trả góp (installment package), lịch thanh toán, tính phạt trễ hạn.
- **Phạm vi dữ liệu**: `InstallmentPackage`, `OrderPaymentSchedule`.
- **Quan hệ**: phụ thuộc chặt vào Order; gọi external gateway VNPay; có job nền (`PaymentScheduleJobService`) xử lý quá hạn.

### 7. Promotion/Voucher *(không tách trong đợt này)*
- **Trách nhiệm**: mã khuyến mãi, áp dụng theo sản phẩm/category/brand.
- **Phạm vi dữ liệu**: `Promotion`, `PromotionProduct` (liên kết category/brand hiện xử lý ở service-layer, không có entity join riêng).
- **Quan hệ**: phụ thuộc Catalog (đọc), được Order/Cart đọc để tính giá.

### 8. Warranty & Claims (hậu mãi) *(không tách trong đợt này)*
- **Trách nhiệm**: bảo hành theo serial (`OrderItem`), xử lý yêu cầu bảo hành (claim).
- **Phạm vi dữ liệu**: `Warranty`, `WarrantyClaim`.
- **Quan hệ**: phụ thuộc Order (1-1 với OrderItem) — phát sinh *sau* khi đơn hàng hoàn tất.

### 9. Blog/Content *(không tách trong đợt này)*
- **Trách nhiệm**: nội dung blog do user (thường admin/staff) đăng.
- **Phạm vi dữ liệu**: `Blog`.
- **Quan hệ**: phụ thuộc User (author). Không giao tiếp với domain thương mại nào khác.

### 10. Chat/Support (real-time) *(không tách trong đợt này)*
- **Trách nhiệm**: nhắn tin thời gian thực giữa khách và nhân viên qua STOMP/WebSocket.
- **Phạm vi dữ liệu**: `ChatConversation`, `ChatMessage`.
- **Quan hệ**: phụ thuộc User (2 user tham gia). Không phụ thuộc domain thương mại.

### 11. Reporting/Analytics *(không tách trong đợt này)*
- **Trách nhiệm**: báo cáo doanh thu theo thời gian/sản phẩm/trả góp, export Excel.
- **Phạm vi dữ liệu**: không sở hữu bảng riêng — đọc tổng hợp Order, Payment, Product.
- **Quan hệ**: phụ thuộc (đọc) Order + Payment + Catalog.

> Domain `Review` đã loại khỏi phân tích theo xác nhận của chủ dự án — entity/repository có thể xóa hoặc giữ lại làm chỗ trống cho tính năng tương lai, không ảnh hưởng kiến trúc hiện tại.

---

## Proposed Microservices

### Phạm vi tách đợt này: 5 service

Theo quyết định của chủ dự án, đợt này chỉ tách **5 domain lõi** (đúng luồng mua hàng chính: xem sản phẩm → build PC → thêm giỏ → đặt hàng, cộng với domain nền tảng User). **6 domain còn lại (Payment/Installment, Promotion, Warranty, Blog, Chat, Reporting) tạm thời ở lại trong monolith** — vẫn chạy như hiện tại, không đổi gì, có thể tách tiếp ở đợt sau nếu cần (xem "Mở rộng sau này" cuối mục này).

### 1. `user-service` *(đổi tên từ identity-service)*
- **Chức năng**: đăng ký/đăng nhập, Google OAuth2, JWT issue/introspect/refresh/revoke, OTP, forgot-password, quản lý Role.
- **API sở hữu**: `/auth/*`, `/users/*`, `/roles/*`, `/otp/*`, `/forgot-password/*`.
- **DB sở hữu**: `users`, `roles`, `invalidated_token`, `otp_verifications`.
- **Truy cập bởi**: API Gateway (xác thực mọi request), tất cả service khác (qua JWT, không qua DB) và phần monolith còn lại (Payment/Promotion/Warranty/Blog/Chat/Reporting vẫn cần xác thực user).
- **Phụ thuộc**: Google OAuth2 (external), SMTP (external).
- Tách **đầu tiên** vì mọi domain khác (kể cả phần chưa tách) phụ thuộc vào nó để xác thực.

### 2. `catalog-service`
- **Chức năng**: CRUD Product/Variant/Item/Image/Attribute/Brand/Category, upload ảnh (Cloudinary).
- **API sở hữu**: `/products/*`, `/brands/*`, `/categories/*`, `/attributes/*`.
- **DB sở hữu**: `products`, `product_variants`, `product_items`, `product_images`, `attributes`, `product_variant_attributes`, `brands`, `categories`.
- **Truy cập bởi**: `cart-service`, `pcbuilder-service`, `order-service`, và phần monolith còn lại (Promotion đọc Catalog để validate; Reporting đọc để lấy tên/giá sản phẩm).
- **Phụ thuộc**: Cloudinary (external).

### 3. `pcbuilder-service`
- **Chức năng**: build PC, kiểm tra tương thích linh kiện.
- **API sở hữu**: `/pc-builds/*`.
- **DB sở hữu**: `pc_builds`, `pc_build_items`, `compatibility_rules`.
- **Phụ thuộc**: gọi `catalog-service` (đọc variant/attribute) đồng bộ (REST) để validate compatibility real-time.
- **Truy cập bởi**: `order-service` (khi convert build → order).

### 4. `cart-service`
- **Chức năng**: giỏ hàng.
- **API sở hữu**: `/cart/*`.
- **DB sở hữu**: `carts`, `cart_items`.
- **Phụ thuộc**: gọi `catalog-service` đồng bộ để lấy giá/tồn kho hiển thị.

### 5. `order-service`
- **Chức năng**: tạo/quản lý đơn hàng.
- **API sở hữu**: `/orders/*`.
- **DB sở hữu**: `orders`, `order_items`.
- **Phụ thuộc**: gọi `user-service` (JWT), `catalog-service` (đọc ProductItem/giá).
- **Truy cập bởi**: Gateway, và phần monolith còn lại — Payment/Warranty/Reporting (trong monolith) vẫn cần đọc dữ liệu Order.

> **Lưu ý quan trọng về ranh giới Order ↔ Payment/Warranty**: vì Payment và Warranty **chưa tách** ở đợt này, chúng vẫn nằm chung 1 DB/1 JVM với nhau như hiện tại, nhưng giờ phải gọi **qua API** sang `order-service` thay vì JOIN trực tiếp bảng `orders`/`order_items` (vì bảng này đã chuyển sang DB riêng của `order-service`). Đây là điểm cần chú ý kỹ nhất khi triển khai Giai đoạn tách Order — chi tiết ở mục "Migration Roadmap".

### Mở rộng sau này (không triển khai trong đợt này, chỉ để tham khảo hướng đi tiếp theo)
Nếu sau này muốn tách tiếp, thứ tự hợp lý theo mức độ phụ thuộc: `payment-service` (gộp Payment+Installment) → `warranty-service` → `content-service` (gộp Blog+Chat) → `promotion-service` → `reporting-service` (read-model, tách sau cùng vì cần event bus ổn định từ các service trước).

---

## Database Strategy

### Đánh giá hiện tại
1 DB duy nhất (SQL Server, `ComputerShopDB`), không migration tool. File cấu hình MySQL không dùng, nên xóa.

### Chiến lược đề xuất: **Database per Service** cho 5 service tách ra, phần còn lại giữ Shared DB tạm thời
- 5 service mới (`user`, `catalog`, `pcbuilder`, `cart`, `order`) — mỗi service **1 DB SQL Server riêng**.
- 6 domain chưa tách (Payment/Installment, Promotion, Warranty, Blog, Chat, Reporting) — **tiếp tục dùng chung 1 DB** (phần "monolith còn lại"), giữ nguyên transaction cục bộ như hiện tại. Đây là kiến trúc **hybrid** phù hợp cho quy mô đợt tách này.

### Mapping bảng → service (đợt này)
| Service | Bảng sở hữu |
|---|---|
| user-service | users, roles, invalidated_token, otp_verifications |
| catalog-service | products, product_variants, product_items, product_images, attributes, product_variant_attributes, brands, categories |
| pcbuilder-service | pc_builds, pc_build_items, compatibility_rules |
| cart-service | carts, cart_items |
| order-service | orders, order_items |
| **monolith còn lại** (1 DB dùng chung) | installment_package, order_payment_schedule, promotions, promotion_product, warranties, warranty_claim, blogs, chat_conversations, chat_messages |

### Quan hệ giữa các service (thay cho FK trực tiếp)
- `OrderItem.productItem` (FK → catalog) → thay bằng **snapshot dữ liệu** tại thời điểm đặt hàng (lưu `productItemId`, `sku`, `unitPrice`, `productName` dạng denormalized trong `order_items`) để `order-service` không phải gọi `catalog-service` mỗi lần hiển thị lịch sử đơn hàng cũ.
- `CartItem.variant` (FK → catalog) → giữ `variantId` reference, gọi `catalog-service` on-demand khi hiển thị giỏ hàng (dữ liệu tạm thời, không cần snapshot).
- Phần monolith còn lại (Payment/Warranty đang có FK trực tiếp tới `Order`/`OrderItem`) → **bắt buộc đổi thành gọi REST API sang `order-service`** vì bảng `orders`/`order_items` không còn trong cùng DB nữa. Đây là thay đổi code bắt buộc, không thể tránh, kể cả khi Payment/Warranty chưa trở thành service riêng.

### Chiến lược đồng bộ dữ liệu
- **Reference id + on-demand lookup (REST đồng bộ)**: đủ dùng cho quy mô 5 service này — chưa cần message broker/event bus ở đợt tách này vì tất cả giao tiếp đều là request-response ngắn (Cart/PCBuilder/Order gọi Catalog để lấy giá/tồn kho, monolith gọi Order để lấy trạng thái đơn).
- **Snapshot tại thời điểm giao dịch**: cho `order_items` như trên — tránh lịch sử đơn hàng bị "đổi" khi sản phẩm bị sửa/xóa sau này.
- **Migration tool**: giới thiệu Flyway cho 5 service mới ngay từ khi tách (thay `ddl-auto: update`); phần monolith còn lại có thể giữ nguyên `ddl-auto: update` cho tới khi tách tiếp.

---

## Service Communication Strategy

Với 5 service + phần monolith còn lại, **toàn bộ giao tiếp đợt này là REST đồng bộ** — chưa cần message broker (Kafka/RabbitMQ). Lý do: không có luồng nào trong 5 domain này có đặc tính "kết quả có thể trễ, không chặn UX" giống Payment/Warranty (vốn chưa tách). Khi tách tiếp Payment/Warranty ở đợt sau, lúc đó mới cần cân nhắc event-driven.

| Cặp giao tiếp | Kiểu | Lý do |
|---|---|---|
| Gateway → mọi service/monolith | REST (sync) | Client-facing, cần phản hồi ngay |
| order-service → catalog-service | REST (sync) | Cần giá/tồn kho real-time để tạo đơn hợp lệ |
| cart-service → catalog-service | REST (sync) | Hiển thị giá/tồn kho tức thời |
| pcbuilder-service → catalog-service | REST (sync) | Validate compatibility cần dữ liệu attribute mới nhất |
| order-service → user-service | REST (sync, qua JWT introspect nếu cần) | Xác thực; phần lớn xử lý ở Gateway nên ít khi cần gọi lại |
| monolith (Payment/Warranty) → order-service | REST (sync) | Cần đọc/cập nhật trạng thái đơn hàng theo thời gian thực |
| monolith (Promotion) → catalog-service | REST (sync) | Validate product/category/brand khi tạo khuyến mãi |
| monolith (Reporting) → catalog-service, order-service | REST (sync, định kỳ/on-demand) | Đủ dùng ở quy mô hiện tại; nếu về sau chậm do gọi nhiều lần, cân nhắc event-driven khi tách reporting-service riêng |
| Chat (trong monolith) ↔ client | WebSocket/STOMP (giữ nguyên) | Không đổi |

**Không giới thiệu message broker ở đợt này** — giữ đơn giản, đúng quy mô 5 service. Ghi nhận trong "Mở rộng sau này": nếu tách `payment-service`/`warranty-service` ở đợt sau, lúc đó mới cần RabbitMQ cho các luồng `order.created` → payment, `order.completed` → warranty.

---

## API Gateway Design

**Vai trò**: là điểm vào duy nhất cho Frontend — FE **không** gọi trực tiếp bất kỳ microservice nào, kể cả không gọi thẳng vào monolith còn lại.

### Chức năng
- **Routing**: map `/api/v1/auth/**`, `/api/v1/users/**`, `/api/v1/roles/**`, `/api/v1/otp/**`, `/api/v1/forgot-password/**` → `user-service`; `/api/v1/products/**`, `/api/v1/brands/**`, `/api/v1/categories/**`, `/api/v1/attributes/**` → `catalog-service`; `/api/v1/pc-builds/**` → `pcbuilder-service`; `/api/v1/cart/**` → `cart-service`; `/api/v1/orders/**` → `order-service`; **mọi path còn lại** (`/api/v1/payment/**`, `/api/v1/installment-packages/**`, `/api/v1/promotions/**`, `/api/v1/warranties/**`, `/api/v1/blogs/**`, `/api/v1/chat/**`, `/api/v1/reports/**`, `/ws`) → route về **monolith** (vẫn chạy song song, chưa tách). FE giữ nguyên `API_ENDPOINTS`, không cần sửa gì.
- **Authentication**: Gateway xác thực JWT tập trung (cùng secret với `user-service`) — vẫn giữ resource-server filter ở từng service/monolith như lớp phòng thủ thứ hai.
- **Authorization**: forward role/claims qua header (`X-User-Id`, `X-User-Role`) sau khi xác thực.
- **Rate limiting**: theo IP/user, đặc biệt `/auth/login`, `/otp/*`.
- **Logging & Monitoring**: correlation id xuyên suốt Gateway → service/monolith.
- **Load balancing**: giữa các instance cùng 1 service (nếu scale nhiều instance).

### Công nghệ đề xuất
Spring Cloud Gateway (đồng bộ hệ Spring Boot hiện có, tái dùng `SecurityConfig` đã quen thuộc) + Spring Cloud LoadBalancer.

---

## Authentication Strategy

Giữ nguyên **kiến trúc trung tâm hóa qua `user-service`**, không cần Keycloak/Authorization Server riêng ở quy mô này:

- `user-service` tiếp tục phát hành JWT HS512 như hiện tại (giữ nguyên claim `sub`, `userId`, `scope`, `jti`).
- **Token revocation**: bảng `invalidated_token` có thể giữ nguyên dạng SQL trong `user-service` ở đợt tách này (đơn giản hơn, đủ dùng cho 5 service + monolith cùng gọi qua API để introspect) — chuyển sang Redis là cải tiến tùy chọn, không bắt buộc cho quy mô đợt này.
- **Refresh token**: giữ cơ chế hiện tại, chỉ tồn tại ở `user-service`.
- **Google OAuth2**: giữ nguyên luồng hiện tại, chuyển code sang `user-service`.
- **SSO**: 1 Frontend duy nhất, JWT phát từ `user-service` + Gateway xác thực tập trung là đủ, không cần Keycloak trừ khi có thêm client thứ 2.
- **Role/Permission**: giữ role thô như hiện tại (ADMIN/STAFF/MEMBER/USER); nâng cấp `Permission` chi tiết là cải tiến tùy chọn cho tương lai.

---

## Frontend Integration Strategy

### Hiện trạng thuận lợi
`src/api/config.ts` đã có `API_BASE_URL` tập trung + `API_ENDPOINTS` map toàn bộ route — gần như đã là "client-side gateway abstraction" sẵn có.

### Thay đổi cần thiết
1. **Không đổi base URL semantics**: FE tiếp tục gọi 1 base URL duy nhất (trỏ tới Gateway thay vì monolith trực tiếp) — Gateway giữ nguyên `context-path /api/v1` và toàn bộ path hiện có, **không cần sửa `API_ENDPOINTS`**.
2. **`client.ts` (ApiClient) không cần đổi logic** — vẫn fetch + Bearer token, miễn 5 service mới trả đúng envelope `ApiResponse<T>` (`code/message/result`) như monolith hiện tại.
3. **Route guard**: nên bổ sung `ProtectedRoute` ở FE cùng đợt (gap có sẵn, không bắt buộc do migration nhưng tiện làm cùng lúc).
4. **Zero-downtime trong quá trình chuyển đổi**: chuyển từng path nhỏ sang service mới mà FE không hay biết, đúng tinh thần strangler-fig.

### Đảm bảo ổn định trong transition
Gateway route theo path prefix: domain nào đã tách (5 domain trên) → route tới service mới; domain nào chưa tách → route tiếp tục về monolith. Cả hai chạy song song.

---

## Deployment Architecture

- **Docker**: mỗi trong 5 service + monolith 1 Dockerfile riêng (tái dùng `docker-compose.yml`/`.dockerignore` hiện có làm base).
- **Docker Compose**: dùng cho dev/staging — 5 service + DB SQL Server riêng mỗi service + 1 DB SQL Server cho monolith còn lại + Gateway, trong 1 file compose.
- **Kubernetes**: có thể cân nhắc sau nếu lên production thật, nhưng với 5-6 thành phần (5 service + monolith), Docker Compose vẫn đủ dùng cho môi trường học tập/demo — không bắt buộc K8s ngay.
- **Configuration Management**: mỗi service có `.env` riêng (không commit — xem "Secrets Cleanup Log"); khi lên production thật, cân nhắc Vault/K8s Secrets.
- **Centralized Logging / Monitoring**: có thể để giai đoạn sau; ở đợt 5-service này, log riêng từng service + Gateway log là đủ dùng.
- **Health Check**: Spring Boot Actuator `/actuator/health` cho mỗi service.

---

## Migration Roadmap

Nguyên tắc: **strangler-fig** — không big-bang rewrite, mỗi giai đoạn có thể rollback bằng cách route Gateway trở lại monolith.

### Giai đoạn 0 — Chuẩn bị nền tảng
- Xóa file `application-server.yml` (MySQL, không dùng).
- ~~Xử lý secrets hygiene~~ **— đã hoàn thành, xem log cuối tài liệu.**
- Chuẩn hóa response envelope `ApiResponse<T>` nếu chưa nhất quán 100%.
- Thêm Actuator health vào monolith.

### Giai đoạn 1 — Đưa API Gateway vào trước, chưa tách service nào
- Triển khai Gateway đứng trước monolith, route 100% traffic về monolith như cũ.
- FE đổi `VITE_API_BASE_URL` trỏ sang Gateway — xác minh không đổi hành vi (bước rollback-an-toàn-nhất, gần như zero-risk).

### Giai đoạn 2 — Tách `user-service`
- Bắt buộc tách đầu tiên vì mọi domain khác cần nó để xác thực.
- Route `/auth/**`, `/users/**`, `/roles/**`, `/otp/**`, `/forgot-password/**` sang `user-service` mới; monolith còn lại gọi API sang `user-service` thay vì dùng chung bảng `users`/`roles`.

### Giai đoạn 3 — Tách `catalog-service`
- Domain lõi, ít thay đổi transaction, chuẩn bị nền cho Cart/Order/PCBuilder gọi tới sau này.
- Monolith còn lại (Promotion, Reporting) chuyển từ JOIN trực tiếp sang gọi API `catalog-service`.

### Giai đoạn 4 — Tách `cart-service` và `pcbuilder-service`
- Cả hai phụ thuộc Catalog (đã sẵn sàng ở Giai đoạn 3), rủi ro thấp, có thể làm song song.

### Giai đoạn 5 — Tách `order-service`
- Tách sau cùng vì phức tạp nhất: Payment và Warranty (còn ở lại monolith) phải đổi từ JOIN trực tiếp bảng `orders`/`order_items` sang gọi REST API `order-service` — đây là điểm rủi ro kỹ thuật cao nhất trong đợt tách 5 service này, cần test kỹ luồng đặt hàng → thanh toán → bảo hành trước khi go-live.

### Giai đoạn 6 — Ổn định & đánh giá
- Theo dõi 5 service mới hoạt động ổn định (health check, logs) ít nhất 1-2 tuần trước khi quyết định có tách tiếp Payment/Warranty/Promotion/Blog/Chat/Reporting hay không.

**Rollback tại mọi giai đoạn**: Gateway route theo path prefix, monolith vẫn "sống" song song cho tới khi từng giai đoạn được xác nhận ổn định — rollback chỉ cần đổi route Gateway về monolith.

---

## Risk Analysis

| Khía cạnh | Đánh giá |
|---|---|
| **Ưu điểm** | Scale độc lập cho Catalog/Order (hot path) tách khỏi phần còn lại; triển khai độc lập giảm rủi ro deploy; ranh giới domain rõ ràng, dễ maintain hơn dần theo thời gian. |
| **Nhược điểm** | Payment/Warranty (chưa tách) phải đổi từ JOIN sang gọi API sang `order-service` — tăng độ trễ nhẹ và cần xử lý lỗi mạng (timeout, retry) ở nơi trước đây chỉ là 1 transaction cục bộ. |
| **Độ phức tạp** | Vừa phải — 5 service + 1 monolith là quy mô hợp lý để học/áp dụng kiến trúc microservices mà không quá tải, đúng với gợi ý "4-5 service" ban đầu của chủ dự án. |
| **Chi phí** | Tăng nhẹ (5 DB SQL Server thay vì 1, Gateway) nhưng chưa cần message broker/K8s nên chi phí vận hành thấp hơn thiết kế 10-service ban đầu. |
| **Khả năng mở rộng** | Đủ tốt cho 5 domain lõi; phần còn lại (monolith) vẫn scale như cũ, có thể tách tiếp khi cần. |
| **Khả năng bảo trì** | Cải thiện rõ với 5 domain lõi (code nhỏ hơn, dễ đọc); phần monolith còn lại giữ nguyên độ phức tạp hiện tại. |
| **Rủi ro kỹ thuật cụ thể** | (1) Giai đoạn 5 (tách Order) là điểm rủi ro cao nhất — Payment/Warranty cần đổi code gọi API; nên test kỹ trước go-live. (2) `user-service` tách sớm ảnh hưởng toàn hệ thống — cần đảm bảo mọi service/monolith gọi introspect JWT đúng cách trước khi tắt code xác thực cũ trong monolith. (3) `.env` đã từng bị commit lên GitHub (xem log cleanup) — các secret cũ (Cloudinary, VNPay, Google OAuth) nên được xoay vòng (rotate) sớm, độc lập với việc tách service. |

---

## Final Recommendation

1. **Tách đúng 5 service đã chốt**: `user-service`, `catalog-service`, `pcbuilder-service`, `cart-service`, `order-service`. Phần còn lại (Payment/Installment, Promotion, Warranty, Blog, Chat, Reporting) giữ nguyên trong monolith, chỉ sửa để gọi API sang service mới thay vì JOIN trực tiếp.
2. **Chưa cần message broker/Kubernetes** ở quy mô 5 service này — REST đồng bộ + Docker Compose là đủ, giảm gánh nặng học tập/vận hành.
3. **Gateway triển khai trước tiên (Giai đoạn 1)** — gần như zero-risk, tạo đòn bẩy rollback an toàn cho các giai đoạn sau.
4. **Frontend gần như không cần viết lại** nhờ lớp `api/` đã tập trung sẵn.
5. **Điểm cần cẩn trọng nhất**: Giai đoạn 5 (tách `order-service`) vì Payment/Warranty trong monolith phải đổi cách truy cập dữ liệu Order — nên dành thời gian test kỹ luồng đặt hàng/thanh toán/bảo hành trước khi go-live giai đoạn này.
6. Chờ phê duyệt tài liệu này trước khi bắt đầu code từng service.

---

## Secrets Cleanup Log (đã thực hiện theo yêu cầu)

Trong quá trình khảo sát phát hiện file `.env` ở `ComputerShop_BE` **đã bị commit và push lên GitHub** (`ComputerShop-vn/ComputerShop_BE`, 5 commit trong lịch sử) — nghĩa là các secret thật đã từng bị lộ trong lịch sử git, không chỉ nằm cục bộ trên máy. Đã thực hiện các thay đổi sau (chỉ đổi **cách cấu hình**, không đổi giá trị secret hiện tại, để không làm hỏng môi trường đang chạy):

1. **`src/main/resources/application-local.yml`**
   - `datasource.password: 123456` → `${DB_PASSWORD}` (đọc từ `.env`).
   - `mail.username`/`mail.password` (hardcode `vinhhien8882004@gmail.com` + app password) → `${MAIL_USERNAME}` / `${MAIL_PASSWORD}`.

2. **`docker-compose.yml`**
   - `SA_PASSWORD=Sa@12345` (hardcode) → `SA_PASSWORD=${SA_PASSWORD}`.
   - Healthcheck `sqlcmd ... -P Sa@12345` → `-P ${SA_PASSWORD}` (Docker Compose tự thay thế biến khi đọc file, không cần sửa gì thêm).
   - `NGROK_AUTHTOKEN=<token thật>` (hardcode) → `NGROK_AUTHTOKEN=${NGROK_AUTHTOKEN}`.

3. **`.env`** — thêm các key mới tương ứng, giữ nguyên giá trị đang dùng để không phá vỡ môi trường hiện tại:
   ```
   DB_PASSWORD=123456
   SA_PASSWORD=Sa@12345
   MAIL_USERNAME=vinhhien8882004@gmail.com
   MAIL_PASSWORD=vvps ftmw mimc aohp
   NGROK_AUTHTOKEN=3AFMOtslcgnRSFHP1IAGg0zaWeQ_5UytTyNXzDxbZjm9qbzDU
   ```

4. **`.gitignore`** — thêm dòng `.env` để từ commit tiếp theo trở đi, file này không bị track nữa.

### Việc CHƯA làm — cần bạn quyết định
- **Chưa xóa `.env` khỏi lịch sử git** (dùng `git filter-repo`/BFG + force-push) — đây là hành động phá hủy lịch sử commit và ảnh hưởng mọi người đang clone repo, nên cần bạn xác nhận trước khi làm. Có thể để nguyên nếu đây là repo học tập/không production thật.
- **Chưa xoay vòng (rotate) các secret thật** — vì `.env` đã từng lên GitHub, các giá trị hiện tại (Cloudinary API secret, VNPay hash secret, Google Client secret, JWT signer key, Gmail app password) về lý thuyết đã bị lộ. Nếu đây là dự án chỉ chạy local/demo thì rủi ro thấp, có thể bỏ qua; nếu có ý định deploy thật, nên tạo secret mới cho từng dịch vụ (Cloudinary, VNPay, Google Console, Gmail App Password) và cập nhật lại `.env`.
- **Chưa git commit các thay đổi trên** — các file đã sửa trên đĩa nhưng chưa `git add`/`git commit`, để bạn tự review diff trước khi commit.

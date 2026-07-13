# ComputerShop_Gateway

API Gateway cho ComputerShop — **Giai đoạn 1** của roadmap chuyển đổi Microservices
(xem `../docs/microservices-analysis.md`).

Ở giai đoạn này, Gateway chỉ đứng trước monolith và route 100% traffic về monolith
như cũ, chưa tách service nào. Mục tiêu là xác nhận Frontend hoạt động ổn định khi
đi qua Gateway trước khi bắt đầu tách các service ở giai đoạn tiếp theo.

## Chạy local (dev)

Yêu cầu: monolith `ComputerShop_BE` đang chạy ở `localhost:8080` (như bình thường).

```bash
./mvnw spring-boot:run
```

Gateway sẽ lắng nghe ở `http://localhost:8888`, và forward:
- `/api/v1/**` → `http://localhost:8080`
- `/ws/**` (chat WebSocket) → `ws://localhost:8080`

## Cấu hình Frontend

Đổi `VITE_API_BASE_URL` trong `ComputerShop_FE/.env` thành `http://localhost:8888`
để FE gọi qua Gateway thay vì gọi thẳng monolith.

## Thêm route khi tách service mới

Khi một domain được tách ra service riêng (vd. `catalog-service` chạy ở port 8081),
thêm 1 route MỚI có path cụ thể hơn, đặt **trước** (`order` nhỏ hơn) route
`monolith-catch-all` trong `src/main/resources/application.yml`:

```yaml
- id: catalog-service
  uri: http://localhost:8081
  order: 10
  predicates:
    - Path=/api/v1/products/**,/api/v1/brands/**,/api/v1/categories/**,/api/v1/attributes/**
```

Route cụ thể hơn có `order` nhỏ hơn sẽ được match trước, phần traffic còn lại vẫn
rơi vào `monolith-catch-all` — không cần đổi gì ở Frontend.

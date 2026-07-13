# ComputerShop — Backend (microservices)

Kiến trúc chi tiết: [docs/microservices-analysis.md](docs/microservices-analysis.md)

| Service | Thư mục | Port | DB |
|---|---|---|---|
| API Gateway | `gateway/` | 8888 | — |
| Monolith (remnant: Payment, Warranty, Promotion, Blog, Chat, Reporting) | `monolith/` | 8080 | `ComputerShopDB` |
| User/Identity | `user-service/` | 8081 | `ComputerShopUserDB` |
| Catalog | `catalog-service/` | 8082 | `ComputerShopCatalogDB` |
| Cart | `cart-service/` | 8083 | `ComputerShopCartDB` |
| PC Builder | `pcbuilder-service/` | 8084 | `ComputerShopPCBuildDB` |
| Order | `order-service/` | 8085 | `ComputerShopOrderDB` |

Mỗi service là 1 project Maven độc lập (`mvnw`), có `.env` riêng (không commit — xem `.gitignore`) chứa `DB_PASSWORD`, `JWT_SIGNER_KEY`, `INTERNAL_API_KEY`, và các secret khác tùy service.

Frontend nằm ở repo riêng: `ComputerShop_FE`.

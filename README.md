# ShopSphere Backend

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-database-336791)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue)
![CI](https://github.com/ajayeadara2001/shopsphere-backend/actions/workflows/ci.yml/badge.svg)

Production-style e-commerce REST API built with Spring Boot. It provides JWT authentication, role-based access control, product/catalog management, carts, orders, payments, refunds, coupons, reviews, Flyway migrations, Swagger documentation, automated tests, CI, and Docker support.

## Table of Contents

- [Tech Stack](#tech-stack)
- [Features](#features)
- [Business Rules](#business-rules)
- [Getting Started](#getting-started)
- [Run with Docker](#run-with-docker)
- [API Overview](#api-overview)
- [API Documentation](#api-documentation)
- [Error Format](#error-format)
- [Testing](#testing)
- [Continuous Integration](#continuous-integration)
- [Project Structure](#project-structure)
- [Roadmap](#roadmap)
- [License](#license)
- [Author](#author)

## Tech Stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Web | Spring Web MVC |
| Database | PostgreSQL |
| Persistence | Spring Data JPA, Hibernate |
| Database migration | Flyway |
| Authentication | JWT with JJWT 0.12.6 |
| Authorization | Spring Security, RBAC, `@PreAuthorize` |
| Password hashing | BCrypt |
| API documentation | springdoc-openapi 3.2.0, Swagger UI |
| Monitoring | Spring Boot Actuator |
| Build tool | Maven Wrapper |
| Testing | JUnit 5, Mockito, MockMvc, Spring Boot Test |
| CI | GitHub Actions with PostgreSQL |
| Containerization | Docker and Docker Compose |

## Features

- User registration and JWT login
- BCrypt-hashed passwords
- Stateless JWT authentication
- Role-based authorization for `CUSTOMER` and `ADMIN`
- Centralized JSON exception handling
- Consistent 401 and 403 security responses
- Public listing of active products and categories
- Admin-only product and category management
- Soft delete through deactivate/reactivate operations
- Cart management with stock validation and price snapshots
- Transactional order placement from the cart
- Coupon validation, discount calculation, usage limits, and expiry validation
- Order status workflow with validated transitions
- Customer cancellation while an order is `PLACED`
- Stock deduction, restoration, pessimistic locks, and rollback protection
- Simulated payment creation and payment retry after failure
- Admin payment success/failed updates
- Automatic pending refund when an admin cancels a paid confirmed order
- Partial and full refund workflow
- Address book with automatic default-address handling
- Wishlist duplicate protection
- Product reviews available only after delivery
- Swagger UI with JWT bearer authentication
- Flyway migration-based schema management
- GitHub Actions CI
- Dockerized backend and PostgreSQL local environment

## Business Rules

### Order lifecycle

| Current status | Allowed next status |
|---|---|
| `PLACED` | `CONFIRMED`, `CANCELLED` |
| `CONFIRMED` | `SHIPPED`, `CANCELLED` |
| `SHIPPED` | `OUT_FOR_DELIVERY` |
| `OUT_FOR_DELIVERY` | `DELIVERED` |
| `DELIVERED`, `CANCELLED` | Final state |

- Customers can cancel only their own order and only while it is `PLACED`.
- Order placement validates stock and coupon rules, snapshots prices, deducts stock, and clears the cart in one transaction.
- If any product has insufficient stock, the complete order transaction rolls back.
- Cancelling an order restores stock.

### Payments

- A customer can create a payment only for their own `PLACED` order.
- Only one `PENDING` payment is allowed per order.
- A customer can retry after a `FAILED` payment.
- Admin payment success changes the order to `CONFIRMED`.
- Admin payment failure keeps the order at `PLACED`.

### Refunds

- Refunds require a successful payment.
- The total of `PENDING` and `COMPLETED` refunds cannot exceed the payment amount.
- Admin can mark a pending refund as `COMPLETED` or `FAILED`.
- When completed refunds cover the complete payment amount, payment status becomes `REFUNDED`.
- Admin cancellation of a confirmed paid order automatically creates a pending refund for the remaining refundable amount.

### Coupons

- Coupons must be active, unexpired, within usage limits, and meet the minimum cart amount.
- Codes are normalized to uppercase.
- Discount calculation uses two-decimal `HALF_UP` rounding.
- Deleting a coupon deactivates it instead of deleting the row.

### Concurrency

- `PESSIMISTIC_WRITE` locks protect critical order, payment, and product operations.
- Products are locked in sorted ID order to reduce deadlock risk.
- `@Version` optimistic locking is used for `Product` and `Coupon`.

## Getting Started

### Prerequisites

- JDK 21
- PostgreSQL
- Docker Desktop (recommended for one-command local setup)
- Maven is optional because the Maven Wrapper is included

### Manual setup

1. Clone the repository:

   ```bash
   git clone https://github.com/ajayeadara2001/shopsphere-backend.git
   cd shopsphere-backend
   ```

2. Create the PostgreSQL database:

   ```sql
   CREATE DATABASE shopsphere_db;
   ```

3. Set environment variables.

| Variable | Required | Description |
|---|---|---|
| `DB_URL` | Yes | JDBC URL, for example `jdbc:postgresql://localhost:5432/shopsphere_db` |
| `DB_USERNAME` | Yes | PostgreSQL username |
| `DB_PASSWORD` | Yes | PostgreSQL password |
| `JWT_SECRET` | Yes | Random secret with at least 32 characters |
| `JWT_EXPIRATION` | No | Token lifetime in milliseconds; default `86400000` |
| `SERVER_PORT` | No | HTTP port; default `8080` |

Windows PowerShell example:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/shopsphere_db"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password"
$env:JWT_SECRET="your-long-random-secret-at-least-32-characters"
```

4. Run the application.

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw spring-boot:run
```

Flyway creates the schema from `src/main/resources/db/migration/V1__initial_schema.sql` on first startup. Hibernate validates the schema using `ddl-auto=validate`.

### Creating an admin

Registration always creates a `CUSTOMER`. Create the first local admin with a BCrypt-hashed password:

```sql
INSERT INTO users (user_name, email, password, phone_no, role, created_at)
VALUES ('Admin', 'admin@shopsphere.com', '<bcrypt-hash>', '9876543210', 'ADMIN', now());
```

Use a strong password outside local development.

## Run with Docker

Docker starts ShopSphere and PostgreSQL in isolated containers. It does not use or modify an existing PostgreSQL database installed on your computer.

1. Install and start Docker Desktop.

2. Create the local Docker environment file:

   Windows PowerShell:

   ```powershell
   Copy-Item .env.example .env
   ```

3. Update `.env` with local values:

   ```properties
   DB_USERNAME=postgres
   DB_PASSWORD=replace-with-a-local-password
   JWT_SECRET=replace-with-a-random-secret-at-least-32-characters
   ```

   Do not commit `.env`.

4. Build and start all containers:

   ```bash
   docker compose up --build
   ```

5. Open these resources:

| Resource | URL |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Docker PostgreSQL from host | `localhost:5433` |

On first Docker startup, Flyway automatically applies `V1__initial_schema.sql` to the Docker PostgreSQL database.

Useful Docker commands:

```bash
# Show running containers
docker compose ps

# Follow backend logs
docker compose logs -f app

# Stop containers but preserve Docker PostgreSQL data
docker compose down

# Stop containers and permanently delete Docker PostgreSQL data
docker compose down -v
```

> `docker compose down -v` deletes only the ShopSphere Docker database volume. Use it only when you need a fresh Docker database.

## API Overview

Public endpoints:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login/me`
- `GET /api/v1/products/active/all`
- `GET /api/v1/categories/active/all`
- Swagger UI and OpenAPI endpoints

All other endpoints require:

```text
Authorization: Bearer <token>
```

Base path:

```text
/api/v1
```

Admin endpoints are under:

```text
/api/v1/admin/**
```

### Authentication

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | Register a customer |
| POST | `/api/v1/auth/login/me` | Public | Log in and receive a JWT |

### Users

| Method | Endpoint | Access | Description |
|---|---|---|---|
| PUT | `/api/v1/users/{userId}` | Customer, own account | Update user details |
| GET | `/api/v1/admin/users/all` | Admin | List users |
| GET | `/api/v1/admin/users/{userId}` | Admin | Get user |
| DELETE | `/api/v1/admin/users/{userId}` | Admin | Delete user |

### Categories

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/v1/categories/active/all` | Public | List active categories |
| GET | `/api/v1/categories/{categoryId}` | Authenticated | Get category |
| GET | `/api/v1/admin/categories/all` | Admin | List all categories |
| POST | `/api/v1/admin/categories` | Admin | Create category |
| PUT | `/api/v1/admin/categories/{categoryId}` | Admin | Update category |
| DELETE | `/api/v1/admin/categories/{categoryId}` | Admin | Deactivate category |
| PATCH | `/api/v1/admin/categories/{categoryId}/active` | Admin | Reactivate category |

### Products

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/v1/products/active/all` | Public | List active products |
| GET | `/api/v1/products/{productId}` | Authenticated | Get active product |
| GET | `/api/v1/admin/products/all` | Admin | List all products, including inactive |
| POST | `/api/v1/admin/products` | Admin | Create product |
| PUT | `/api/v1/admin/products/{productId}` | Admin | Update product |
| DELETE | `/api/v1/admin/products/{productId}` | Admin | Deactivate product |
| PATCH | `/api/v1/admin/products/{productId}/active` | Admin | Reactivate product |

### Cart

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/cart/items` | Customer | Add item |
| GET | `/api/v1/cart` | Customer | View cart |
| PUT | `/api/v1/cart/items/{productId}?quantity=` | Customer | Update item quantity |
| DELETE | `/api/v1/cart/items/{productId}` | Customer | Remove item |
| DELETE | `/api/v1/cart` | Customer | Clear cart |

### Addresses

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/address` | Customer | Add address |
| GET | `/api/v1/address/{addressId}` | Customer | Get address |
| GET | `/api/v1/address/all` | Customer | List addresses |
| PUT | `/api/v1/address/{addressId}` | Customer | Update address |
| DELETE | `/api/v1/address/{addressId}` | Customer | Delete address |

### Wishlist

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/wishlist` | Customer | Add product |
| GET | `/api/v1/wishlist/{wishlistId}` | Customer | Get wishlist item |
| GET | `/api/v1/wishlist/all` | Customer | List wishlist |
| DELETE | `/api/v1/wishlist/{wishlistId}` | Customer | Remove wishlist item |

### Reviews

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/reviews/{productId}` | Customer | Add review |
| GET | `/api/v1/reviews/{reviewId}` | Customer | Get review |
| GET | `/api/v1/reviews/all/{productId}` | Customer | List product reviews |
| PUT | `/api/v1/reviews/{reviewId}` | Customer | Update own review |
| DELETE | `/api/v1/reviews/{reviewId}` | Customer | Delete own review |

### Coupons

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/coupons/apply-coupon` | Customer | Preview discount |
| POST | `/api/v1/admin/coupons` | Admin | Create coupon |
| GET | `/api/v1/admin/coupons/{couponId}` | Admin | Get coupon |
| GET | `/api/v1/admin/coupons/all` | Admin | List all coupons |
| GET | `/api/v1/admin/coupons/all/active` | Admin | List active coupons |
| PUT | `/api/v1/admin/coupons/{couponId}` | Admin | Update coupon |
| DELETE | `/api/v1/admin/coupons/{couponId}` | Admin | Deactivate coupon |

### Orders

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/orders?couponCode=` | Customer | Place order from cart |
| GET | `/api/v1/orders/me` | Customer | List own orders |
| GET | `/api/v1/orders/{orderId}` | Customer | Get own order |
| POST | `/api/v1/orders/{orderId}/cancel` | Customer | Cancel placed order |
| GET | `/api/v1/admin/orders/all` | Admin | List all orders |
| PUT | `/api/v1/admin/orders/{orderId}/status` | Admin | Update status |

### Payments

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/payments/{orderId}` | Customer | Create payment |
| GET | `/api/v1/payments/me` | Customer | List own payments |
| PUT | `/api/v1/admin/payments/{paymentId}/success` | Admin | Mark payment successful |
| PUT | `/api/v1/admin/payments/{paymentId}/failed` | Admin | Mark payment failed |
| GET | `/api/v1/admin/payments/payment-history` | Admin | List payment history |

Payments are simulated. No real payment gateway is integrated.

### Refunds

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/refund` | Customer | Request refund |
| GET | `/api/v1/refund/{refundId}` | Customer | Get refund |
| GET | `/api/v1/refund/payment/{paymentId}` | Customer | List payment refunds |
| GET | `/api/v1/admin/refund/all` | Admin | List all refunds |
| PUT | `/api/v1/admin/refund/{refundId}/status` | Admin | Complete or fail refund |

## API Documentation

Interactive API documentation is provided through springdoc-openapi.

| Resource | URL |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| OpenAPI YAML | `http://localhost:8080/v3/api-docs.yaml` |

To call protected endpoints:

1. Log in using `POST /api/v1/auth/login/me`.
2. Copy the returned token.
3. Click **Authorize** in Swagger UI.
4. Paste the token without the `Bearer ` prefix.

## Error Format

Application exceptions return a consistent JSON response:

```json
{
  "timeStamp": "2026-10-31 15:55:06",
  "status": 400,
  "message": "Validation Failed",
  "errors": {
    "email": "Please provide a valid email address"
  }
}
```

| Status | Meaning |
|---|---|
| 400 | Validation or business-rule failure |
| 401 | Missing, expired, invalid JWT, or wrong credentials |
| 403 | Authenticated user lacks permission |
| 404 | Requested resource not found |
| 409 | Duplicate data or invalid state transition |
| 500 | Unexpected server error |

## Testing

ShopSphere has **173 automated tests** covering services, controllers, security, and the complete order-payment-refund lifecycle.

| Level | Test class | Tests | Coverage |
|---|---|---:|---|
| Service | `OrderServiceImplTest` | 56 | Transitions, cancellations, stock restoration, automatic refunds |
| Service | `PaymentServiceImplTest` | 17 | Payment creation, retry, success/failed status handling |
| Service | `RefundServiceImplTest` | 18 | Refund limits, ownership, completion/failure, payment reconciliation |
| Service | `JWTServiceImplTest` | 7 | Token generation and validation |
| Controller | `CartControllerApiTest` | 8 | Cart security, validation, responses |
| Controller | `OrderControllerApiTest` | 6 | Order endpoints and roles |
| Controller | `PaymentControllerApiTest` | 5 | Customer payment endpoints |
| Controller | `RefundControllerApiTest` | 6 | Customer refund endpoints |
| Controller | `UserControllerApiTest` | 5 | Registration and profile update |
| Controller | `CatalogControllerApiTest` | 2 | Public catalog endpoints |
| Controller | Admin controller tests | 20 | Admin product, category, payment, and refund endpoints |
| Security | JWT and security tests | 14 | Filter, entry point, authorization rules |
| Integration | `OrderPaymentRefundIntegrationTest` | 8 | PostgreSQL lifecycle, coupon, rollback, payment retry, refunds |
| Integration | `ShopsphereBackendApplicationTests` | 1 | Application context startup |

### Test database setup

Tests use the `test` profile with a separate PostgreSQL database.

Create it once:

```sql
CREATE DATABASE shopsphere_test_db;
```

Set these variables before running tests:

| Variable | Description |
|---|---|
| `TEST_DB_URL` | Example: `jdbc:postgresql://localhost:5432/shopsphere_test_db` |
| `TEST_DB_USERNAME` | Test database username |
| `TEST_DB_PASSWORD` | Test database password |
| `JWT_SECRET` | JWT secret for Spring context startup |

Windows PowerShell:

```powershell
$env:TEST_DB_URL="jdbc:postgresql://localhost:5432/shopsphere_test_db"
$env:TEST_DB_USERNAME="postgres"
$env:TEST_DB_PASSWORD="your_password"
$env:JWT_SECRET="your-long-random-secret-at-least-32-characters"

.\mvnw.cmd clean verify
```

macOS/Linux:

```bash
./mvnw clean verify
```

## Continuous Integration

GitHub Actions workflow: `.github/workflows/ci.yml`

The **Backend CI** workflow runs on every push and pull request to `main`.

1. Starts a PostgreSQL 18 service container.
2. Creates `shopsphere_test_db`.
3. Sets up Java 21 with Maven caching.
4. Runs:

   ```bash
   ./mvnw -B clean verify
   ```

The workflow uses its own `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD`, and JWT values. It never accesses local, QA, or production databases.

## Project Structure

```text
.
├── .github/workflows/ci.yml      # GitHub Actions CI
├── .mvn/                         # Maven Wrapper files
├── src/
│   ├── main/
│   │   ├── java/com/shopsphere/
│   │   │   ├── config/            # Security and OpenAPI configuration
│   │   │   ├── constant/          # Roles, statuses, enums
│   │   │   ├── controller/        # REST controllers
│   │   │   ├── dto/               # Request and response DTOs
│   │   │   ├── entity/            # JPA entities
│   │   │   ├── exception/         # Custom exceptions and global handler
│   │   │   ├── mapper/            # DTO/entity mapping helpers
│   │   │   ├── repository/        # Spring Data JPA repositories
│   │   │   ├── security/          # JWT authentication components
│   │   │   └── service/           # Business services
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/      # Flyway SQL migrations
│   └── test/
│       ├── java/com/shopsphere/
│       │   ├── controllerTest/
│       │   ├── integratedTest/
│       │   ├── securityTest/
│       │   └── service/serviceImplTest/
│       └── resources/application-test.properties
├── .dockerignore                  # Docker build exclusions
├── .env.example                   # Safe Docker environment template
├── docker-compose.yml             # Backend + PostgreSQL containers
├── Dockerfile                     # Multi-stage Java image build
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

## Roadmap

Completed:

- [x] JWT authentication and role-based authorization
- [x] Order, payment, coupon, and refund workflows
- [x] Transactional stock management and locking
- [x] Unit, controller, security, and integration tests
- [x] Flyway database migrations
- [x] Swagger UI and OpenAPI documentation
- [x] GitHub Actions CI with PostgreSQL
- [x] Configuration through environment variables
- [x] Dockerfile and Docker Compose one-command local setup

Planned improvements:

- [ ] Richer endpoint documentation with `@Operation`, `@Tag`, examples, and response descriptions
- [ ] Unit tests for cart, coupon, product, category, address, wishlist, review, and user services
- [ ] Controller tests for coupons, addresses, wishlist, reviews, and login
- [ ] Testcontainers PostgreSQL integration tests
- [ ] Admin account seeding for local development
- [ ] Pagination and filtering for list endpoints
- [ ] Payment idempotency keys
- [ ] Production deployment with protected secrets and a managed PostgreSQL database
- [ ] Frontend integration

## License

This project is licensed under the MIT License. See [LICENSE.md](LICENSE.md).

## Author

**Ajay Shankar Ganesh Eadara**

- [LinkedIn](https://linkedin.com/in/ajay-shankar-ganesh-eadara-69821241a/)
- [GitHub](https://github.com/ajayeadara2001)
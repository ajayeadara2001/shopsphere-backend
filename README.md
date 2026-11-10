# ShopSphere Backend

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-database-336791)
![Docker](https://img.shields.io/badge/Docker-containerized-2496ED)
![License](https://img.shields.io/badge/License-MIT-blue)
![CI](https://github.com/ajayeadara2001/shopsphere-backend/actions/workflows/ci.yml/badge.svg)

Production-style e-commerce REST API built with Spring Boot, Spring Security, JWT, PostgreSQL, Flyway, Docker, GitHub Actions, Render, and Neon.

ShopSphere supports customers and administrators managing users, categories, products, carts, addresses, wishlists, orders, payments, coupons, reviews, and refunds. Order, payment, and refund flows use transactions and database locking to keep stock and payment data consistent.

## Live Demo

ShopSphere is deployed as a Dockerized Spring Boot service on Render with Neon PostgreSQL.

| Resource | URL |
|---|---|
| API base URL | https://shopsphere-backend-tqav.onrender.com |
| Swagger UI | https://shopsphere-backend-tqav.onrender.com/swagger-ui/index.html |
| OpenAPI JSON | https://shopsphere-backend-tqav.onrender.com/v3/api-docs |

> The free Render service spins down after inactivity. The first request after it sleeps can take around one minute.

## Table of Contents

- [Tech Stack](#tech-stack)
- [Features](#features)
- [Business Rules](#business-rules)
- [Getting Started](#getting-started)
- [Docker Setup](#docker-setup)
- [API Overview](#api-overview)
- [Swagger UI](#swagger-ui)
- [Testing](#testing)
- [Continuous Integration](#continuous-integration)
- [Deployment](#deployment)
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
| Security | Spring Security, JWT, BCrypt |
| Database | PostgreSQL |
| Persistence | Spring Data JPA, Hibernate |
| Database migrations | Flyway |
| Validation | Jakarta Bean Validation |
| API documentation | springdoc-openapi / Swagger UI |
| Monitoring | Spring Boot Actuator |
| Build tool | Maven Wrapper |
| Testing | JUnit 5, Mockito, MockMvc, Spring Security Test |
| Containerization | Docker and Docker Compose |
| CI | GitHub Actions with PostgreSQL |
| Hosting | Render |
| Hosted database | Neon PostgreSQL |

## Features

- Customer registration and JWT login
- BCrypt password hashing
- Stateless JWT authentication
- Role-based access control for `CUSTOMER` and `ADMIN`
- Method-level authorization using `@PreAuthorize`
- Centralized JSON error handling
- Public active category and product catalog
- Admin category and product management
- Soft delete through deactivate/reactivate actions
- Shopping cart with stock validation and price snapshots
- Address book with automatic default-address handling
- Wishlist with duplicate protection
- Product reviews for delivered orders only
- Coupon validation, expiry, minimum amount, usage limits, and discount calculation
- Order placement from the cart
- Validated order status transitions
- Pessimistic locking for stock, orders, and payments
- Payment creation, failure, retry, and success workflow
- Partial and full refund workflows
- Automatic pending refund when an admin cancels a paid confirmed order
- Flyway-managed database schema
- Swagger UI with JWT bearer authentication
- Dockerized local setup
- Automated tests and GitHub Actions CI
- Public cloud deployment on Render with Neon PostgreSQL

## Business Rules

### Order Lifecycle

| Current status | Allowed next status |
|---|---|
| `PLACED` | `CONFIRMED`, `CANCELLED` |
| `CONFIRMED` | `SHIPPED`, `CANCELLED` |
| `SHIPPED` | `OUT_FOR_DELIVERY` |
| `OUT_FOR_DELIVERY` | `DELIVERED` |
| `DELIVERED` | Final status |
| `CANCELLED` | Final status |

- Customers can cancel only their own orders.
- Customers can cancel only while an order is `PLACED`.
- Placing an order validates stock, applies an optional coupon, snapshots prices, deducts stock, and clears the cart in one transaction.
- If one item has insufficient stock, the complete order operation rolls back.
- Cancelling an order restores stock.

### Payments

- A customer can create a payment only for their own `PLACED` order.
- Only one pending payment can exist for an order.
- A customer can retry payment after a previous payment fails.
- Admin can mark payments as `SUCCESS` or `FAILED`.
- A successful payment confirms the order.

### Refunds

- Refunds can be requested only for successful payments.
- Total pending and completed refunds cannot exceed the original payment amount.
- Admin can complete or fail a pending refund.
- When completed refunds equal the payment amount, payment status becomes `REFUNDED`.
- Admin cancellation of a confirmed paid order creates a pending refund for the remaining refundable amount.

### Coupons

- Coupons must be active and unexpired.
- The cart total must satisfy the coupon minimum amount.
- Coupon usage cannot exceed its usage limit.
- Coupon codes are normalized to uppercase.
- Discounts are rounded to two decimal places using `HALF_UP`.
- Deleting a coupon deactivates it instead of deleting its database row.

### Concurrency

- Products, orders, and payments use pessimistic write locks where required.
- Products are locked in consistent ID order during order placement to reduce deadlock risk.
- `Product` and `Coupon` use optimistic locking with `@Version`.

## Getting Started

### Prerequisites

- JDK 21
- PostgreSQL
- Docker Desktop, optional but recommended
- Maven 3.9+, optional because Maven Wrapper is included

### Clone the Repository

```bash
git clone https://github.com/ajayeadara2001/shopsphere-backend.git
cd shopsphere-backend
```

### Create a Local Database

```sql
CREATE DATABASE shopsphere_db;
CREATE DATABASE shopsphere_test_db;
```

### Configure Environment Variables

The application reads runtime configuration from environment variables.

| Variable | Required | Description |
|---|---|---|
| `DB_URL` | Yes | JDBC URL for the application database |
| `DB_USERNAME` | Yes | PostgreSQL username |
| `DB_PASSWORD` | Yes | PostgreSQL password |
| `JWT_SECRET` | Yes | Strong private JWT signing secret |
| `JWT_EXPIRATION` | No | Token lifetime in milliseconds; default is `86400000` |
| `SERVER_PORT` | No | HTTP server port; default is `8080` |
| `TEST_DB_URL` | For tests | JDBC URL for the isolated test database |
| `TEST_DB_USERNAME` | For tests | Test database username |
| `TEST_DB_PASSWORD` | For tests | Test database password |

Example PowerShell configuration:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/shopsphere_db"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_local_password"
$env:JWT_SECRET="your_private_jwt_secret"
$env:SERVER_PORT="8080"
```

Run the application:

```powershell
.\mvnw.cmd spring-boot:run
```

The API starts at:

```text
http://localhost:8080
```

Flyway creates the schema from:

```text
src/main/resources/db/migration/V1__initial_schema.sql
```

Hibernate validates the schema instead of creating it automatically.

## Docker Setup

Docker Compose starts ShopSphere and PostgreSQL together.

### 1. Create Local Environment File

Copy the template:

```powershell
Copy-Item .env.example .env
```

Edit `.env` with private local values:

```properties
DB_USERNAME=postgres
DB_PASSWORD=your_local_database_password
JWT_SECRET=your_private_jwt_secret
```

`.env` is ignored by Git and must never be committed.

### 2. Start the Application

```powershell
docker compose up --build
```

Run in the background:

```powershell
docker compose up -d
```

Check container status:

```powershell
docker compose ps
```

View logs:

```powershell
docker compose logs -f app
```

Open Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

### Stop Containers

```powershell
docker compose down
```

This stops containers while preserving the Docker database volume.

To remove the Docker database completely:

```powershell
docker compose down -v
```

> `down -v` deletes Docker-managed PostgreSQL data. Use it only when a fresh database is intended.

## API Overview

Public endpoints do not require authentication:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login/me`
- `GET /api/v1/products/active/all`
- `GET /api/v1/categories/active/all`
- Swagger UI and OpenAPI endpoints

All other protected endpoints require:

```http
Authorization: Bearer <token>
```

Base API path:

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
| POST | `/api/v1/auth/login/me` | Public | Log in and receive JWT |

### Users

| Method | Endpoint | Access | Description |
|---|---|---|---|
| PUT | `/api/v1/users/{userId}` | Customer | Update own profile |
| GET | `/api/v1/admin/users/all` | Admin | List users |
| GET | `/api/v1/admin/users/{userId}` | Admin | Get user |
| DELETE | `/api/v1/admin/users/{userId}` | Admin | Delete user |

### Categories

| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/v1/categories/active/all` | Public |
| GET | `/api/v1/categories/{categoryId}` | Authenticated |
| GET | `/api/v1/admin/categories/all` | Admin |
| POST | `/api/v1/admin/categories` | Admin |
| PUT | `/api/v1/admin/categories/{categoryId}` | Admin |
| DELETE | `/api/v1/admin/categories/{categoryId}` | Admin |
| PATCH | `/api/v1/admin/categories/{categoryId}/active` | Admin |

### Products

| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/v1/products/active/all` | Public |
| GET | `/api/v1/products/{productId}` | Authenticated |
| GET | `/api/v1/admin/products/all` | Admin |
| POST | `/api/v1/admin/products` | Admin |
| PUT | `/api/v1/admin/products/{productId}` | Admin |
| DELETE | `/api/v1/admin/products/{productId}` | Admin |
| PATCH | `/api/v1/admin/products/{productId}/active` | Admin |

### Cart

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/v1/cart/items` | Customer |
| GET | `/api/v1/cart` | Customer |
| PUT | `/api/v1/cart/items/{productId}?quantity=` | Customer |
| DELETE | `/api/v1/cart/items/{productId}` | Customer |
| DELETE | `/api/v1/cart` | Customer |

### Addresses

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/v1/address` | Customer |
| GET | `/api/v1/address/{addressId}` | Customer |
| GET | `/api/v1/address/all` | Customer |
| PUT | `/api/v1/address/{addressId}` | Customer |
| DELETE | `/api/v1/address/{addressId}` | Customer |

### Wishlist

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/v1/wishlist` | Customer |
| GET | `/api/v1/wishlist/{wishlistId}` | Customer |
| GET | `/api/v1/wishlist/all` | Customer |
| DELETE | `/api/v1/wishlist/{wishlistId}` | Customer |

### Reviews

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/v1/reviews/{productId}` | Customer |
| GET | `/api/v1/reviews/{reviewId}` | Customer |
| GET | `/api/v1/reviews/all/{productId}` | Customer |
| PUT | `/api/v1/reviews/{reviewId}` | Customer |
| DELETE | `/api/v1/reviews/{reviewId}` | Customer |

### Coupons

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/v1/coupons/apply-coupon` | Customer |
| POST | `/api/v1/admin/coupons` | Admin |
| GET | `/api/v1/admin/coupons/{couponId}` | Admin |
| GET | `/api/v1/admin/coupons/all` | Admin |
| GET | `/api/v1/admin/coupons/all/active` | Admin |
| PUT | `/api/v1/admin/coupons/{couponId}` | Admin |
| DELETE | `/api/v1/admin/coupons/{couponId}` | Admin |

### Orders

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/v1/orders?couponCode=` | Customer |
| GET | `/api/v1/orders/me` | Customer |
| GET | `/api/v1/orders/{orderId}` | Customer |
| POST | `/api/v1/orders/{orderId}/cancel` | Customer |
| GET | `/api/v1/admin/orders/all` | Admin |
| PUT | `/api/v1/admin/orders/{orderId}/status` | Admin |

### Payments

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/v1/payments/{orderId}` | Customer |
| GET | `/api/v1/payments/me` | Customer |
| PUT | `/api/v1/admin/payments/{paymentId}/success` | Admin |
| PUT | `/api/v1/admin/payments/{paymentId}/failed` | Admin |
| GET | `/api/v1/admin/payments/payment-history` | Admin |

Payments are simulated. No real payment gateway is connected.

### Refunds

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/v1/refund` | Customer |
| GET | `/api/v1/refund/{refundId}` | Customer |
| GET | `/api/v1/refund/payment/{paymentId}` | Customer |
| GET | `/api/v1/admin/refund/all` | Admin |
| PUT | `/api/v1/admin/refund/{refundId}/status` | Admin |

## Swagger UI

Swagger UI provides interactive API documentation and JWT authorization support.

| Resource | Local URL |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

To use protected endpoints:

1. Register or log in.
2. Copy the JWT token.
3. Click **Authorize** in Swagger UI.
4. Paste the token.
5. Execute protected endpoints.

## Error Format

Application errors are returned in JSON.

```json
{
  "timeStamp": "2026-11-10 12:00:00",
  "status": 400,
  "message": "Validation Failed",
  "errors": {
    "email": "Please provide a valid email address"
  }
}
```

| Status | Meaning |
|---|---|
| 400 | Validation failure or invalid business rule |
| 401 | Missing, invalid, or expired JWT |
| 403 | Authenticated user lacks permission |
| 404 | Requested resource was not found |
| 409 | Duplicate resource or invalid status transition |
| 500 | Unexpected server error |

## Testing

ShopSphere has **173 automated tests** covering service, security, controller, and PostgreSQL integration behavior.

| Level | Coverage |
|---|---|
| Service tests | Order, payment, refund, JWT service behavior |
| Controller tests | Cart, order, payment, refund, user, catalog, and admin endpoint responses |
| Security tests | JWT filter, authentication entry point, access control |
| Integration tests | Order → payment → refund lifecycle with real PostgreSQL |
| Application test | Spring context startup |

Run the complete test suite:

```powershell
.\mvnw.cmd clean verify
```

Before running tests, configure the isolated test database:

```powershell
$env:TEST_DB_URL="jdbc:postgresql://localhost:5432/shopsphere_test_db"
$env:TEST_DB_USERNAME="postgres"
$env:TEST_DB_PASSWORD="your_test_database_password"
```

> Tests use a separate database. Never point test configuration to the production or main development database.

## Continuous Integration

GitHub Actions runs automatically on every push and pull request to `main`.

The pipeline:

1. Starts a PostgreSQL service container.
2. Sets up Java 21.
3. Runs the Maven test suite.
4. Packages the Spring Boot application.
5. Validates the Docker image build.

Workflow file:

```text
.github/workflows/ci.yml
```

## Deployment

The production deployment uses:

| Component | Provider |
|---|---|
| Spring Boot Docker service | Render |
| PostgreSQL database | Neon |
| Source code and CI | GitHub |

Render environment variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION
SERVER_PORT
```

The production database schema is applied automatically by Flyway at startup.

No real credentials are stored in GitHub. Local secrets remain in `.env`, which is ignored by Git.

## Project Structure

```text
com.shopsphere
├── config          # Security, OpenAPI, application configuration
├── constant        # Roles, order status, payment status, refund status
├── controller      # REST controllers and admin controllers
├── dto             # Request and response DTOs
├── entity          # JPA entities
├── exception       # Custom exceptions and global exception handler
├── mapper          # DTO and entity mapping helpers
├── repository      # Spring Data JPA repositories and locking queries
├── security        # JWT filter, authentication, authorization handlers
└── service         # Service interfaces and implementations

src/main/resources
├── application.properties
└── db/migration
    └── V1__initial_schema.sql

src/test/java/com/shopsphere
├── controllerTest
├── integratedTest
├── securityTest
└── service/serviceImplTest

.github/workflows
└── ci.yml

Dockerfile
docker-compose.yml
.env.example
```

## Roadmap

Completed:

- [x] JWT authentication and role-based authorization
- [x] Order, payment, refund, cart, coupon, address, wishlist, and review workflows
- [x] Service-level Mockito tests
- [x] MockMvc controller and security tests
- [x] PostgreSQL integration tests
- [x] Flyway schema migration
- [x] Swagger UI and OpenAPI documentation
- [x] Docker and Docker Compose setup
- [x] GitHub Actions CI pipeline
- [x] Public Render deployment with Neon PostgreSQL

Planned improvements:

- [ ] Richer OpenAPI annotations and endpoint examples
- [ ] Pagination and filtering for list endpoints
- [ ] Unit tests for remaining services
- [ ] Controller tests for coupons, addresses, wishlist, reviews, and login
- [ ] Testcontainers integration tests
- [ ] Admin account seeding for local development
- [ ] Payment gateway integration
- [ ] Email notifications
- [ ] Rate limiting and production observability
- [ ] Custom domain and paid always-on hosting when required

## License

This project is licensed under the MIT License. See [LICENSE.md](LICENSE.md).

## Author

**Ajay Shankar Ganesh Eadara**

- [GitHub](https://github.com/ajayeadara2001)
- [LinkedIn](https://linkedin.com/in/ajay-shankar-ganesh-eadara-69821241a/)
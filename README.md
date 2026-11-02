# ShopSphere Backend

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-database-336791)
![License](https://img.shields.io/badge/License-MIT-blue)

REST API for an e-commerce platform built with Spring Boot. It provides JWT-based authentication, role-based access control (Customer / Admin), and management of users, categories, products, carts, orders, payments, addresses, wishlists, reviews, coupons and refunds.

## Table of Contents

- [Tech Stack](#tech-stack)
- [Features](#features)
- [Getting Started](#getting-started)
- [API Overview](#api-overview)
- [Error Format](#error-format)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Roadmap](#roadmap)
- [License](#license)
- [Author](#author)

## Tech Stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation) |
| Database | PostgreSQL |
| Authentication | JWT (JJWT 0.12.6), BCrypt password hashing |
| Build tool | Maven (wrapper included) |
| Utilities | Lombok |
| Testing | JUnit, Mockito (via Spring Boot Test) |

## Features

- User registration and login with BCrypt-hashed passwords
- Stateless JWT authentication
- Role-based authorization (`CUSTOMER`, `ADMIN`) using `@PreAuthorize`
- Partial updates with Bean Validation groups
- Centralized exception handling with a consistent JSON error format
- Admin-only category and product management with soft delete (deactivate / reactivate)
- Shopping cart with stock validation
- Order placement from the cart, order history and cancellation
- Simulated payment flow with admin payment status updates
- Admin order management and status updates
- Address book with automatic default address handling
- Wishlist with duplicate protection
- Product reviews and ratings
- Discount coupons with admin management
- Refund requests with an admin approval workflow
- Default admin account seeded on first startup

## Getting Started

### Prerequisites

- JDK 21
- PostgreSQL
- Maven 3.9+ (optional, the included Maven wrapper works without it)

### Setup

1. **Clone the repository**

   ```bash
   git clone https://github.com/ajayeadara2001/shopsphere-backend.git
   cd shopsphere-backend
   ```

2. **Create the database**

   ```sql
   CREATE DATABASE shopsphere_db;
   ```

3. **Set the environment variables**

   | Variable | Description |
   |---|---|
   | `DB_PASSWORD` | PostgreSQL password for user `postgres` |
   | `JWT_SECRET` | Base64-encoded secret of at least 32 bytes |

   Generate a JWT secret:

   **macOS / Linux**

   ```bash
   openssl rand -base64 32
   ```

   **Windows PowerShell**

   ```powershell
   $b = New-Object byte[] 32
   [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
   [Convert]::ToBase64String($b)
   ```

   Set the variables in your terminal before running the app:

   **macOS / Linux**

   ```bash
   export DB_PASSWORD=your_password
   export JWT_SECRET=your_base64_secret
   ```

   **Windows PowerShell**

   ```powershell
   $env:DB_PASSWORD="your_password"
   $env:JWT_SECRET="your_base64_secret"
   ```

   Replace the placeholder values with your own. Never commit real credentials.

4. **Run the application**

   **macOS / Linux**

   ```bash
   ./mvnw spring-boot:run
   ```

   **Windows PowerShell**

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   The API starts on `http://localhost:8080`.

### Default admin

On first startup an admin account is created. Override the defaults with the `admin.email` and `admin.password` properties. Change the password immediately outside local development.

## API Overview

All endpoints except registration and login require a token:

```
Authorization: Bearer <token>
```

Base path: `/api/v1`

### Quick start

1. Register a customer: `POST /api/v1/auth/register`
2. Log in and copy the token: `POST /api/v1/auth/login`
3. Send the token in the `Authorization` header on every other request.

### Authentication

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | Register a customer |
| POST | `/api/v1/auth/login` | Public | Log in and receive a JWT |

### Users

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/v1/users` | Admin | List users (paginated) |
| GET | `/api/v1/users/{id}` | Self / Admin | Get a user |
| PATCH | `/api/v1/users/{id}` | Self / Admin | Update a user (partial) |
| DELETE | `/api/v1/users/{id}` | Self / Admin | Delete a user |

### Categories

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/v1/categories/active/all` | Authenticated | List active categories |
| GET | `/api/v1/categories/{categoryId}` | Authenticated | Get a category |
| GET | `/api/v1/categories/all` | Admin | List all categories |
| POST | `/api/v1/categories` | Admin | Create a category |
| PUT | `/api/v1/categories/{categoryId}` | Admin | Update a category (partial) |
| DELETE | `/api/v1/categories/{categoryId}` | Admin | Delete (deactivate) a category |
| PATCH | `/api/v1/categories/{categoryId}/active` | Admin | Reactivate a category |

### Products

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/v1/products/active/all` | Authenticated | List active products |
| GET | `/api/v1/products/{productId}` | Authenticated | Get a product |
| GET | `/api/v1/products/all` | Admin | List all products, including inactive |
| POST | `/api/v1/products` | Admin | Create a product |
| PUT | `/api/v1/products/{productId}` | Admin | Update a product (partial) |
| DELETE | `/api/v1/products/{productId}` | Admin | Delete (deactivate) a product |
| PATCH | `/api/v1/products/{productId}/active` | Admin | Reactivate a product |

### Cart

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/cart/items` | Customer | Add an item to the cart |
| GET | `/api/v1/cart` | Customer | View my cart |
| PUT | `/api/v1/cart/items/{productId}?quantity=` | Customer | Set the quantity of an item |
| DELETE | `/api/v1/cart/items/{productId}` | Customer | Remove an item |
| DELETE | `/api/v1/cart` | Customer | Clear the cart |

### Addresses

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/address` | Customer | Add an address |
| GET | `/api/v1/address/{addressId}` | Customer | Get one of my addresses |
| GET | `/api/v1/address/all` | Customer | List my addresses |
| PUT | `/api/v1/address/{addressId}` | Customer | Update an address (partial) |
| DELETE | `/api/v1/address/{addressId}` | Customer | Delete an address |

The first address becomes the default automatically. Marking another address as default clears the previous one.

### Wishlist

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/wishlist` | Customer | Add a product to the wishlist |
| GET | `/api/v1/wishlist/{wishlistId}` | Customer | Get a wishlist item |
| GET | `/api/v1/wishlist/all` | Customer | List my wishlist |
| DELETE | `/api/v1/wishlist/{wishlistId}` | Customer | Remove a wishlist item |

### Reviews

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/reviews/{productId}` | Customer | Review a product |
| GET | `/api/v1/reviews/{reviewId}` | Customer | Get a review |
| GET | `/api/v1/reviews/all/{productId}` | Customer | List reviews of a product |
| PUT | `/api/v1/reviews/{reviewId}` | Customer | Update my review |
| DELETE | `/api/v1/reviews/{reviewId}` | Customer | Delete my review |

### Coupons

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/coupons/apply-coupon` | Customer | Check and apply a coupon code |
| POST | `/api/v1/admin/coupons` | Admin | Create a coupon |
| GET | `/api/v1/admin/coupons/{couponId}` | Admin | Get a coupon |
| GET | `/api/v1/admin/coupons/all` | Admin | List all coupons |
| GET | `/api/v1/admin/coupons/all/active` | Admin | List active coupons |
| PUT | `/api/v1/admin/coupons/{couponId}` | Admin | Update a coupon (partial) |
| DELETE | `/api/v1/admin/coupons/{couponId}` | Admin | Delete a coupon |

### Orders

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/orders?couponCode=` | Customer | Place an order from the cart (coupon optional) |
| GET | `/api/v1/orders/me` | Customer | List my orders |
| GET | `/api/v1/orders/{orderId}` | Customer | Get one of my orders |
| POST | `/api/v1/orders/{orderId}/cancel` | Customer | Cancel an order |
| GET | `/api/v1/admin/orders/all` | Admin | List all orders |
| PUT | `/api/v1/admin/orders/{orderId}/status` | Admin | Update order status |

### Payments

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/payments/{orderId}` | Customer | Create a payment for an order |
| GET | `/api/v1/payments/me` | Customer | List my payments |
| PUT | `/api/v1/admin/payments/{paymentId}/success` | Admin | Mark a payment successful |
| PUT | `/api/v1/admin/payments/{paymentId}/failed` | Admin | Mark a payment failed |
| GET | `/api/v1/admin/payments/payment-history` | Admin | List all payments |

Payments are simulated. No real payment gateway is integrated.

### Refunds

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/refund` | Customer | Request a refund for a payment |
| GET | `/api/v1/refund/{refundId}` | Customer | Get a refund request |
| GET | `/api/v1/refund/payment/{paymentId}` | Customer | List refunds of a payment |
| GET | `/api/v1/admin/refund/all` | Admin | List all refund requests |
| PUT | `/api/v1/admin/refund/{refundId}/status` | Admin | Approve or reject a refund |

## Error Format

All errors return a consistent JSON body:

```json
{
  "timeStamp": "2026-10-31 15:55:06",
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "email": "Please provide a valid email address"
  }
}
```

## Testing

ShopSphere includes automated tests for the JWT authentication components and application context startup.

### Test coverage

- **`JwtServiceImplTest`**
    - JWT generation and email extraction
    - Token validation for the correct user
    - Rejection of a token belonging to a different user
    - Expired, malformed and tampered token handling
    - Null user-details handling

- **`JWTAuthenticationFilterTest`**
    - Missing and non-Bearer Authorization headers
    - Authentication using a valid JWT
    - Expired and malformed token handling
    - Rejection when the token does not match the user
    - Existing authentication handling
    - Null extracted email and user lookup failure

- **`JWTAuthenticationEntryPointTest`**
    - JSON response for HTTP 401 Unauthorized
    - Exception message and request path in the response

- **`ShopsphereBackendApplicationTests`**
    - Spring application context startup

### Run tests

**Windows PowerShell**

```powershell
.\mvnw.cmd clean verify
```

**macOS / Linux**

```bash
./mvnw clean verify
```

### Latest test result

| Metric | Result |
|---|---:|
| Tests run | 19 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Build | SUCCESS |

## Project Structure

```text
com.shopsphere
├── config        # Security config, data seeding
├── constant      # Enums (roles, statuses, address types)
├── controller    # REST controllers (adminController for admin endpoints)
├── dto           # Request/response objects and validation groups
├── entity        # JPA entities
├── exception     # Custom exceptions and global handler
├── mapper        # Entity <-> DTO mapping
├── repository    # Spring Data repositories
├── security      # JWT filter, entry point, access denied handler
└── service       # Business logic (interfaces and serviceImpl)
```

## Roadmap

Planned improvements:

- [ ] Unit tests for the service layer (cart, orders, coupons, refunds)
- [ ] Integration tests with Testcontainers (PostgreSQL)
- [ ] OpenAPI / Swagger documentation
- [ ] Dockerfile and `docker-compose.yml` for one-command setup
- [ ] CI with GitHub Actions (`./mvnw clean verify`)
- [ ] Concurrency-safe stock deduction and idempotent payment handling

## License

This project is licensed under the MIT License. See the [LICENSE.md](LICENSE.md) file for details.

## Author

**Ajay Shankar Ganesh Eadara**

- [LinkedIn](https://linkedin.com/in/ajay-shankar-ganesh-eadara-69821241a/)
- [GitHub](https://github.com/ajayeadara2001)
# ShopSphere Backend

REST API for an e-commerce platform built with Spring Boot. It provides JWT-based
authentication, role-based access control (Customer / Admin), and management of
users, categories, products, carts, orders and payments.

## Tech Stack

- Java 21
- Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation)
- PostgreSQL
- JWT (JJWT 0.12.6)git rebase --continue
- Maven
- Lombok

## Features

- User registration and login with BCrypt-hashed passwords
- Stateless JWT authentication
- Role-based authorization (`CUSTOMER`, `ADMIN`) using `@PreAuthorize`
- Partial updates with Bean Validation groups
- Centralized exception handling with a consistent JSON error format
- Admin-only category and product management
- Shopping cart with stock validation
- Order placement from the cart, order history and cancellation
- Payment flow (simulated) with admin payment status updates
- Admin order management and status updates
- Default admin seeded on first startup

## Getting Started

### Prerequisites

- JDK 21
- PostgreSQL
- Maven 3.9+ (optional, the included Maven wrapper works without it)

### Setup

1. Clone the repository

```bash
   git clone https://github.com/ajayeadara2001/shopsphere-backend.git
   cd shopsphere-backend
```

2. Create the database

```sql
   CREATE DATABASE shopsphere_db;
```

3. Set the environment variables

   | Variable | Description |
      |---|---|
   | `DB_PASSWORD` | PostgreSQL password for user `postgres` |
   | `JWT_SECRET` | Base64-encoded secret of at least 32 bytes |

   Generate a JWT secret with:

```bash
   openssl rand -base64 32
```

Set them in your terminal before running the app:

```bash
   # macOS / Linux
   export DB_PASSWORD=your_password
   export JWT_SECRET=your_base64_secret
```

```powershell
   # Windows PowerShell
   $env:DB_PASSWORD="your_password"
   $env:JWT_SECRET="your_base64_secret"
```

Replace the placeholder values with your own. Never commit real credentials.

4. Run the application

```bash
   ./mvnw spring-boot:run
```

On Windows use `mvnw.cmd spring-boot:run`. The API starts on `http://localhost:8080`.

### Default admin

On first startup an admin account is created. Override the defaults with the
`admin.email` and `admin.password` properties. Change the password immediately
outside local development.

## API Overview

Send the token as `Authorization: Bearer <token>`.

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

### Orders

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/orders` | Customer | Place an order from the cart |
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

### Error format

```json
{
   "timeStamp": "2026-10-19 15:55:06",
   "status": 400,
   "message": "Validation failed",
   "errors": { "email": "Please provide a valid email address" }
}
```

## Project Structure

```
com.shopsphere
├── config        # Security config, data seeding
├── constant      # Enums (roles, statuses)
├── controller    # REST controllers (adminController for admin endpoints)
├── dto           # Request/response objects and validation groups
├── entity        # JPA entities
├── exception     # Custom exceptions and global handler
├── mapper        # Entity <-> DTO mapping
├── repository    # Spring Data repositories
├── security      # JWT filter, entry point, access denied handler
└── service       # Business logic (interfaces and serviceImpl)
```

## License

This project is licensed under the MIT License. See the [LICENSE.md](LICENSE.md) file for details.

## Author

**Ajay Shankar Ganesh Eadara**: [LinkedIn](https://linkedin.com/in/ajay-shankar-ganesh-eadara-69821241a/) · [GitHub](https://github.com/ajayeadara2001)
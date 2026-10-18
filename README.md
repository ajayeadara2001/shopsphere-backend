# ShopSphere Backend

REST API for an e-commerce platform built with Spring Boot. It provides JWT-based
authentication, role-based access control (Customer / Admin), and management of
users, categories and products.

## Tech Stack

- Java 21
- Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation)
- PostgreSQL
- JWT (JJWT 0.12.6)
- Maven
- Lombok

## Features

- User registration and login with BCrypt-hashed passwords
- Stateless JWT authentication
- Role-based authorization (`CUSTOMER`, `ADMIN`) using `@PreAuthorize`
- Partial updates (PATCH) with Bean Validation groups
- Centralized exception handling with a consistent JSON error format
- Admin-only category and product management
- Default admin seeded on first startup

## Getting Started

### Prerequisites

- JDK 21
- Maven 3.9+
- PostgreSQL

### Setup

1. Clone the repository
```bash
   git clone https://github.com/ajayeadara2001/shopsphere-backend.git
   cd <your-repo>
```
2. Create the database
```sql
    CREATE DATABASE shopsphere_db;
```
3. Set the environment variables

   | Variable | Description |
   | `DB_PASSWORD` | PostgreSQL password for user `postgres` |
   | `JWT_SECRET` | Base64-encoded secret of at least 32 bytes |

   Generate a JWT secret with:
```bash
   openssl rand -base64 32
```
4. Run the application
```bash
   mvn spring-boot:run
```
The API starts on `http://localhost:8080`.

### Default admin

On first startup an admin account is created. Override the defaults with the
`admin.email` and `admin.password` properties. Change the password immediately
outside local development.

## API Overview

| Method | Endpoint | Access | Description |
| POST | `/api/v1/auth/register` | Public | Register a customer |
| POST | `/api/v1/auth/login` | Public | Log in and receive a JWT |
| GET | `/api/v1/users` | Admin | List users (paginated) |
| GET | `/api/v1/users/{id}` | Self / Admin | Get a user |
| PATCH | `/api/v1/users/{id}` | Self / Admin | Update a user (partial) |
| DELETE | `/api/v1/users/{id}` | Self / Admin | Delete a user |
| GET | `/api/v1/products/active/all` | Authenticated | List active products |
| GET | `/api/v1/products/{productId}` | Authenticated | Get a product |
| GET | `/api/v1/products/all` | Admin | List all products, including inactive |
| POST | `/api/v1/products` | Admin | Create a product |
| PUT | `/api/v1/products/{productId}` | Admin | Update a product (partial) |
| DELETE | `/api/v1/products/{productId}` | Admin | Delete (deactivate) a product |
| PATCH | `/api/v1/products/{productId}/active` | Admin | Reactivate a product |
| GET | `/api/v1/categories/active/all` | Authenticated | List active categories |
| GET | `/api/v1/categories/{categoryId}` | Authenticated | Get a category |
| GET | `/api/v1/categories/all` | Admin | List all categories |
| POST | `/api/v1/categories` | Admin | Create a category |
| PUT | `/api/v1/categories/{categoryId}` | Admin | Update a category (partial) |
| DELETE | `/api/v1/categories/{categoryId}` | Admin | Delete (deactivate) a category |
| PATCH | `/api/v1/categories/{categoryId}/active` | Admin | Reactivate a category |

Send the token as `Authorization: Bearer <token>`.

### Error format
```json
{
  "timeStamp": "2026-10-18 15:55:06",
  "status": 400,
  "message": "Validation failed",
  "errors": { "email": "Please provide a valid email address" }
}
```

## Project Structure
com.shopsphere
├── config        # Security config, data seeding
├── controller    # REST controllers
├── dto           # Request/response objects
├── entity        # JPA entities
├── exception     # Custom exceptions and global handler
├── mapper        # Entity <-> DTO mapping
├── repository    # Spring Data repositories
├── security      # JWT filter, entry point, access denied handler
├── service       # Business logic
└── validation    # Validation groups
```

## Roadmap
- [ ] Cart and orders
- [ ] Payment integration
- [ ] Unit and integration tests
- [ ] Docker setup
- [ ] Swagger / OpenAPI documentation

## License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Author
**Ajay Shankar Ganesh Eadara**: [LinkedIn](https://linkedin.com/in/ajay-shankar-ganesh-eadara-69821241a/) · [GitHub](https://github.com/ajayeadara2001)
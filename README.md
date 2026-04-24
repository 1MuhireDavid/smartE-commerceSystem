# Smart E-Commerce System

A Spring Boot REST + GraphQL API for an e-commerce platform, backed by PostgreSQL.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Database Setup](#database-setup)
3. [Running the Application](#running-the-application)
4. [Project Structure](#project-structure)
5. [REST API](#rest-api)
6. [GraphQL API](#graphql-api)
7. [AOP — Logging & Monitoring](#aop--logging--monitoring)
8. [Validation](#validation)
9. [REST vs GraphQL — Trade-offs](#rest-vs-graphql--trade-offs)
10. [Environment Profiles](#environment-profiles)
11. [Dependencies](#dependencies)

---

## Prerequisites

| Tool | Version |
|---|---|
| Java JDK | 21+ |
| Apache Maven | 3.9+ |
| PostgreSQL | 14+ |

---

## Database Setup

### 1. Create the database

```sql
CREATE DATABASE smart_ecommerce;
```

### 2. Run the schema

```bash
psql -U postgres -d smart_ecommerce -f src/main/resources/schema.sql
```

This creates all tables, indexes, constraints, and triggers. Re-running it is safe — the script drops and recreates everything.

### 3. Load sample data *(optional)*

```bash
psql -U postgres -d smart_ecommerce -f src/main/resources/sample_data.sql
```

### 4. Configure the Spring Boot connection

Edit the `dev` profile in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/smart_ecommerce
    username: postgres
    password: your_password   # ← change this
```

---

## Running the Application

```bash
# dev profile (default)
mvn spring-boot:run

# explicit profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

The API starts on **port 8080**.

| URL | Purpose |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Swagger / OpenAPI interactive docs |
| `http://localhost:8080/v3/api-docs` | Raw OpenAPI JSON |
| `http://localhost:8080/graphiql` | GraphiQL explorer *(dev only)* |
| `http://localhost:8080/graphql` | GraphQL endpoint (POST) |
| `http://localhost:8080/api/monitoring/metrics` | Live AOP performance stats |

---

## Project Structure

```
src/main/java/org/ecommerce/api/
│
├── SmartEcommerceApplication.java      # Spring Boot entry point
│
├── entity/                             # JPA entities (one per table)
│   ├── UserEntity.java
│   ├── CategoryEntity.java
│   ├── ProductEntity.java
│   ├── InventoryEntity.java
│   ├── OrderEntity.java
│   ├── OrderItemEntity.java
│   ├── PaymentEntity.java
│   ├── CartEntity.java
│   ├── CartItemEntity.java
│   ├── ReviewEntity.java
│   └── ActivityLogEntity.java
│
├── repository/                         # Spring Data JPA repositories
│
├── service/                            # Service interfaces
├── service/impl/                       # Service implementations (@Transactional)
│
├── controller/                         # REST controllers
│   ├── UserController.java
│   ├── CategoryController.java
│   ├── ProductController.java
│   ├── CartController.java
│   ├── OrderController.java
│   ├── PaymentController.java
│   ├── ReviewController.java
│   ├── ActivityLogController.java
│   └── MonitoringController.java
│
├── graphql/                            # GraphQL resolvers (reuse same services)
│   ├── UserGraphQlController.java
│   ├── CategoryGraphQlController.java
│   ├── ProductGraphQlController.java
│   ├── GraphQlExceptionResolver.java
│   └── input/
│
├── aspect/                             # AOP cross-cutting concerns
│   ├── LoggingAspect.java              # @Before + @After entry/exit logging
│   ├── PerformanceMonitoringAspect.java # @Around timing + slow-call detection
│   ├── ExceptionLoggingAspect.java     # @AfterThrowing structured error logging
│   └── MethodMetrics.java              # Thread-safe stats accumulator
│
├── dto/
│   ├── ApiResponse.java                # { status, message, data }
│   ├── PagedResponse.java
│   └── request/                        # Validated request DTOs
│
├── validation/                         # Custom constraint annotations
│   ├── ValidSlug + SlugValidator
│   ├── ValidEnum + EnumValidator
│   └── ValidDiscount + DiscountValidator
│
└── config/
    ├── OpenApiConfig.java
    └── GlobalExceptionHandler.java

src/main/resources/
├── application.yml                     # Multi-profile Spring Boot config
├── schema.sql                          # Full PostgreSQL DDL (run once)
├── sample_data.sql                     # Seed data
└── graphql/schema.graphqls             # GraphQL type schema
```

---

## REST API

### Response envelope

Every response is wrapped in a standard envelope:

```json
{
  "status":  "success | error",
  "message": "Human-readable description",
  "data":    { }
}
```

Paginated list responses wrap a `PagedResponse` inside `data`:

```json
{
  "data": {
    "content":       [ ],
    "page":          0,
    "size":          20,
    "totalElements": 154,
    "totalPages":    8,
    "last":          false
  }
}
```

### Endpoint Reference

#### Users — `/api/users`

| Method | Path | Description |
|---|---|---|
| GET | `/api/users` | List users; filter by `keyword`, `role`, `active`; sort by `sortBy`/`sortDir` |
| GET | `/api/users/{id}` | Get user by ID |
| POST | `/api/users` | Create user |
| PUT | `/api/users/{id}` | Update user |
| DELETE | `/api/users/{id}` | Delete user |

#### Categories — `/api/categories`

| Method | Path | Description |
|---|---|---|
| GET | `/api/categories` | List categories; filter by `keyword`, `active` |
| GET | `/api/categories/{id}` | Get category by ID |
| POST | `/api/categories` | Create category |
| PUT | `/api/categories/{id}` | Update category |
| DELETE | `/api/categories/{id}` | Delete category |

#### Products — `/api/products`

| Method | Path | Description |
|---|---|---|
| GET | `/api/products` | List products; filter by `keyword`, `categoryId`, `status`, `sellerId` |
| GET | `/api/products/{id}` | Get product by ID |
| POST | `/api/products` | Create product (also initialises inventory) |
| PUT | `/api/products/{id}` | Update product |
| DELETE | `/api/products/{id}` | Delete product |

#### Carts — `/api/carts`

| Method | Path | Description |
|---|---|---|
| GET | `/api/carts` | List all carts (paginated) |
| GET | `/api/carts/{id}` | Get cart by ID |
| GET | `/api/carts/user/{userId}` | Get the active cart for a user |
| POST | `/api/carts` | Create a new cart for a user (409 if one already exists) |
| GET | `/api/carts/{id}/items` | List items in a cart |
| POST | `/api/carts/{id}/items` | Add a product to a cart (merges qty if already present) |
| PUT | `/api/carts/{id}/items/{itemId}` | Update item quantity |
| DELETE | `/api/carts/{id}/items/{itemId}` | Remove item from cart |

#### Orders — `/api/orders`

| Method | Path | Description |
|---|---|---|
| GET | `/api/orders` | List orders; filter by `userId`, `status` |
| GET | `/api/orders/{id}` | Get order by ID |
| GET | `/api/orders/{id}/items` | Get line items for an order |
| POST | `/api/orders` | Place a new order (calculates subtotal from current product prices) |
| PATCH | `/api/orders/{id}/status` | Update order status (`pending → processing → completed \| cancelled`) |

#### Payments — `/api/payments`

| Method | Path | Description |
|---|---|---|
| GET | `/api/payments` | List payments; filter by `orderId`, `status` |
| GET | `/api/payments/{id}` | Get payment by ID |
| POST | `/api/payments` | Record a payment against an order |
| PATCH | `/api/payments/{id}/status` | Update payment status; `completed` also records `paidAt` |

Allowed `status` values: `pending` · `completed` · `failed` · `refunded`

#### Reviews — `/api/reviews`

| Method | Path | Description |
|---|---|---|
| GET | `/api/reviews` | List reviews; filter by `productId`, `approved` |
| GET | `/api/reviews/{id}` | Get review by ID |
| POST | `/api/reviews` | Submit a review (one per user per product) |
| PATCH | `/api/reviews/{id}/approve` | Approve a review (makes it publicly visible) |
| DELETE | `/api/reviews/{id}` | Delete a review |

#### Activity Logs — `/api/activity-logs`

Append-only user event log. `eventData` must be a valid JSON string.

| Method | Path | Description |
|---|---|---|
| GET | `/api/activity-logs` | List logs; filter by `userId`, `eventType` |
| POST | `/api/activity-logs` | Record an event (`userId` optional for anonymous events) |

Example `eventData` values: `add_to_cart`, `page_view`, `checkout_started`, `search`.

---

## GraphQL API

**Endpoint:** `POST /graphql`  
**Explorer:** `GET /graphiql` *(dev profile only)*

GraphQL and REST share the same service and repository beans — there is no duplicated business logic.

### Schema overview

```graphql
type Query {
  user(id: ID!): User
  users(filter: UserFilter, page: Int, size: Int,
        sortBy: String, sortDir: String): UserPage!

  product(id: ID!): Product
  products(filter: ProductFilter, page: Int, size: Int,
           sortBy: String, sortDir: String): ProductPage!

  category(id: ID!): Category
  categories(filter: CategoryFilter, page: Int, size: Int): CategoryPage!
}

type Mutation {
  createUser(input: UserInput!): User!
  updateUser(id: ID!, input: UserInput!): User!
  deleteUser(id: ID!): Boolean!

  createProduct(input: ProductInput!): Product!
  updateProduct(id: ID!, input: ProductInput!): Product!
  deleteProduct(id: ID!): Boolean!

  createCategory(input: CategoryInput!): Category!
  updateCategory(id: ID!, input: CategoryInput!): Category!
  deleteCategory(id: ID!): Boolean!
}
```

### Example queries

```graphql
# Filtered product catalog — fetch only the fields the client needs
query {
  products(
    filter: { keyword: "headphones", status: "active" }
    page: 0, size: 5
    sortBy: "basePrice", sortDir: "asc"
  ) {
    totalElements totalPages
    content {
      productId name basePrice effectivePrice
      category { name }
      seller   { fullName }
      inventory { qtyInStock }
    }
  }
}

# Create a product
mutation {
  createProduct(input: {
    name: "USB-C Hub", slug: "usb-c-hub"
    basePrice: 49.99,  discountPrice: 39.99
    sellerId: "1",     status: "active"
    stockQuantity: 100
  }) {
    productId name effectivePrice status
  }
}
```

---

## AOP — Logging & Monitoring

Three aspects intercept the service layer as cross-cutting concerns so no business-logic class contains logging or timing code.

### How the aspects interact on a single service call

```
Request arrives
   │
   ├─ LoggingAspect @Before
   │     → "→ ENTER ProductServiceImpl.create()  [args: 1]"
   │
   ├─ PerformanceMonitoringAspect @Around  ← starts nanoTime()
   │     │
   │     ▼  ProductServiceImpl.create()   ← real method executes
   │     │
   │     └─ records elapsedMs, updates ConcurrentHashMap<method, MethodMetrics>
   │        logs DEBUG (or WARN ⚠ if > threshold)
   │
   ├─ LoggingAspect @After
   │     → "← EXIT  ProductServiceImpl.create()"
   │
   └─ ExceptionLoggingAspect @AfterThrowing  (only on exception)
         → WARN for 4xx client errors
         → ERROR + stack trace for unexpected failures
```

### Aspect reference

| Aspect | Advice | Pointcut | Purpose |
|---|---|---|---|
| `LoggingAspect` | `@Before` + `@After` | `service.impl.*.*(..)` | Entry/exit breadcrumbs |
| `PerformanceMonitoringAspect` | `@Around` | `service.impl.*.*(..)` | Execution time + slow-call detection |
| `ExceptionLoggingAspect` | `@AfterThrowing` | `org.ecommerce.api..*(..)` | Structured exception logging |

### Live metrics endpoint

```bash
GET /api/monitoring/metrics
```

```json
{
  "status": "success",
  "data": {
    "ProductServiceImpl.findAll": {
      "methodKey":       "ProductServiceImpl.findAll",
      "invocations":     42,
      "slowInvocations": 2,
      "avgTimeMs":       38.7,
      "lastTimeMs":      45
    }
  }
}
```

Slow-method threshold is configurable per profile:

```yaml
# application.yml
monitoring:
  slow-method-threshold-ms: 500
```

---

## Validation

### Standard Bean Validation

| Annotation | Used on | Rule |
|---|---|---|
| `@NotBlank` | Required string fields | Must not be null or blank |
| `@Email` | `UserRequest.email` | Valid RFC email format |
| `@Size` | Strings | Max length matching DB column |
| `@DecimalMin` | Prices, amounts | Must be ≥ 0 (or > 0 where required) |
| `@Min` / `@Max` | Quantities, rating | Numeric range |

### Custom validators

| Annotation | Rule enforced |
|---|---|
| `@ValidSlug` | Matches `^[a-z0-9]+(-[a-z0-9]+)*$` — no uppercase, no consecutive hyphens |
| `@ValidEnum` | Allowed-values constraint — `@ValidEnum(allowed = {"active","inactive","draft"})` |
| `@ValidDiscount` | Cross-field: `discountPrice < basePrice` when both are present |

Validation errors return **HTTP 400** with all messages joined in the envelope:

```json
{
  "status":  "error",
  "message": "Slug must contain only lowercase letters, digits, and single hyphens; Discount price (150.00) must be less than base price (99.99)",
  "data":    null
}
```

---

## REST vs GraphQL — Trade-offs

Both APIs share the same service and repository layer. Differences come from **data shape**, **round trips**, and **protocol overhead**.

### Over-fetching and under-fetching

REST always returns all mapped fields. A mobile client showing a card view gets the full product object:

```json
{
  "productId": 1, "name": "...", "slug": "...", "description": "...",
  "basePrice": 129.99, "discountPrice": 99.99, "effectivePrice": 99.99,
  "status": "active", "avgRating": 4.3, "reviewCount": 18, ...
}
```

The same client using GraphQL requests only what it renders:

```graphql
query {
  products(filter: { status: "active" }, page: 0, size: 20) {
    content { productId name effectivePrice category { name } }
    totalElements totalPages
  }
}
```

**Payload difference: ~10 KB (REST) vs ~2 KB (GraphQL) for 20 products.**

### Round trips for related data

REST embeds related resources (seller, category, inventory) in the product response — no extra calls needed for those. For anything not embedded, the client makes additional requests.

GraphQL lets the client declare the full shape of the response — related types across multiple associations — in one POST.

### Writes

REST: one HTTP request per resource (`POST /api/users`, `POST /api/products` …).

GraphQL: multiple mutations in one request using aliases:

```graphql
mutation {
  newCategory: createCategory(input: { name: "Audio", slug: "audio" }) { categoryId }
  newProduct:  createProduct(input:  { name: "Headphones", ... })       { productId  }
}
```

### Summary

| Dimension | REST | GraphQL |
|---|---|---|
| Over-fetching | Always returns all mapped fields | Client requests only what it needs |
| Under-fetching | May need multiple requests | One query spans multiple types |
| HTTP caching | Native (GET cached by URL) | Hard — all queries are POST |
| Error model | HTTP status codes | `errors` array inside a 200 response |
| Versioning | URL versioning (`/v2/products`) | Field deprecation in schema |
| Learning curve | Low | Moderate |
| Best for | Server-to-server, public APIs, simple CRUD | Client-driven UIs, mobile, dashboards |

---

## Environment Profiles

| Profile | Database | `ddl-auto` | SQL logging | GraphiQL |
|---|---|---|---|---|
| `dev` *(default)* | `localhost/smart_ecommerce` | `validate` | on | on |
| `test` | `localhost/smart_ecommerce_test` | `create-drop` | off | on |
| `prod` | `${DATABASE_URL}` env var | `validate` | off | **off** |

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

---

## Dependencies

| Starter / Library | Purpose |
|---|---|
| `spring-boot-starter-web` | REST controllers, Tomcat, Jackson |
| `spring-boot-starter-data-jpa` | Hibernate 6, Spring Data repositories |
| `spring-boot-starter-validation` | Bean Validation (Jakarta) |
| `spring-boot-starter-graphql` | Spring GraphQL + graphql-java |
| `spring-boot-starter-aop` | AspectJ-based AOP |
| `spring-boot-starter-actuator` | Health and metrics endpoints |
| `spring-boot-starter-test` | JUnit 5, Mockito, AssertJ *(test scope)* |
| `org.postgresql:postgresql` | PostgreSQL JDBC driver |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI + OpenAPI 3 docs |

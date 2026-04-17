# Smart E-Commerce System

A full-stack e-commerce platform with two co-existing layers built on the same PostgreSQL database:

| Layer | Technology | Entry point |
|---|---|---|
| **Desktop UI** | JavaFX + JDBC | `org.ecommerce.Main` |
| **REST + GraphQL API** | Spring Boot 3.3 | `org.ecommerce.api.SmartEcommerceApplication` |

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Database Setup](#database-setup)
3. [Entity Relationship Diagram](#entity-relationship-diagram)
4. [Running the Application](#running-the-application)
5. [Project Structure](#project-structure)
6. [REST API](#rest-api)
7. [GraphQL API](#graphql-api)
8. [AOP — Logging & Monitoring](#aop--logging--monitoring)
9. [Validation](#validation)
10. [REST vs GraphQL — Performance Analysis](#rest-vs-graphql--performance-analysis)
11. [Environment Profiles](#environment-profiles)
12. [Dependencies](#dependencies)

---

## Prerequisites

| Tool | Version |
|---|---|
| Java JDK | 21+ (LTS) |
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

### 3. Load sample data *(optional)*

```bash
psql -U postgres -d smart_ecommerce -f src/main/resources/sample_data.sql
```

### 4. Configure the JDBC connection (JavaFX layer)

Edit `src/main/resources/db.properties`:

```properties
db.url=jdbc:postgresql://localhost:5432/smart_ecommerce
db.username=postgres
db.password=your_password
```

### 5. Configure the Spring Boot connection (API layer)

The Spring Boot layer reads from `src/main/resources/application.yml`.
The `dev` profile connects to the same database out of the box:

```yaml
# already set in application.yml – dev profile
datasource:
  url: jdbc:postgresql://localhost:5432/smart_ecommerce
  username: your_username
  password: your_password
```

Change the password to match your local instance.

---

## Entity Relationship Diagram

[View full ERD on Google Drive](https://drive.google.com/file/d/1tWjC7sltJAvSUzS5sJ1my1Q3v44_CR-u/view?usp=sharing)

**Key relationships at a glance:**

```
users ──< products ──< order_items >── orders >── payments
               │
               └──< inventory
               └──< reviews
               └──< cart_items >── carts

categories ──< products 
```

**Tables:** `users`, `addresses`, `categories`, `products`, `inventory`,
`orders`, `order_items`, `payments`, `carts`, `cart_items`, `reviews`, `activity_logs` (JSONB).

---

## Running the Application

### JavaFX desktop app

```bash
mvn clean javafx:run
```

### Spring Boot REST + GraphQL API

```bash
# dev profile (default)
mvn spring-boot:run

# explicit profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

The API starts on **port 8080**.

| URL | Description |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Swagger / OpenAPI UI |
| `http://localhost:8080/v3/api-docs` | Raw OpenAPI JSON |
| `http://localhost:8080/graphiql` | GraphiQL explorer *(dev only)* |
| `http://localhost:8080/graphql` | GraphQL endpoint (POST) |
| `http://localhost:8080/api/monitoring/metrics` | Live AOP performance stats |

---

## Project Structure

```
src/
├── main/
│   ├── java/org/example/
│   │   │
│   │   ├── Main.java                          # JavaFX entry point
│   │   ├── config/DatabaseConfig.java         # Singleton JDBC connection
│   │   ├── model/                             # Plain JDBC model classes
│   │   ├── dao/                               # JDBC data-access objects
│   │   ├── service/                           # JavaFX business logic
│   │   ├── controller/                        # JavaFX FXML controllers
│   │   ├── cache/InMemoryCache.java           # TTL ConcurrentHashMap cache
│   │   └── util/ValidationUtil.java
│   │
│   └── api/                                   # Spring Boot layer
│       ├── SmartEcommerceApplication.java     # Spring Boot entry point
│       ├── entity/                            # JPA entities (same DB tables)
│       │   ├── UserEntity.java
│       │   ├── CategoryEntity.java
│       │   ├── ProductEntity.java
│       │   └── InventoryEntity.java
│       ├── repository/                        # Spring Data JPA repositories
│       ├── service/                           # Service interfaces
│       ├── service/impl/                      # Service implementations
│       ├── controller/                        # REST controllers
│       │   ├── UserController.java
│       │   ├── CategoryController.java
│       │   ├── ProductController.java
│       │   └── MonitoringController.java      # AOP metrics endpoint
│       ├── graphql/                           # GraphQL resolvers
│       │   ├── UserGraphQlController.java
│       │   ├── CategoryGraphQlController.java
│       │   ├── ProductGraphQlController.java
│       │   ├── GraphQlExceptionResolver.java
│       │   └── input/                         # GraphQL input / filter types
│       ├── aspect/                            # AOP aspects
│       │   ├── LoggingAspect.java             # @Before + @After
│       │   ├── PerformanceMonitoringAspect.java  # @Around
│       │   ├── ExceptionLoggingAspect.java    # @AfterThrowing
│       │   └── MethodMetrics.java             # Thread-safe stats accumulator
│       ├── dto/
│       │   ├── ApiResponse.java               # { status, message, data }
│       │   ├── PagedResponse.java
│       │   └── request/                       # Validated request DTOs
│       ├── validation/                        # Custom constraint annotations
│       │   ├── ValidSlug + SlugValidator
│       │   ├── ValidEnum + EnumValidator
│       │   └── ValidDiscount + DiscountValidator
│       └── config/
│           ├── OpenApiConfig.java
│           └── GlobalExceptionHandler.java
│
└── resources/
    ├── db.properties                          # JavaFX JDBC config
    ├── application.yml                        # Spring Boot multi-profile config
    ├── schema.sql                             # Full PostgreSQL DDL
    ├── sample_data.sql                        # Seed data
    ├── graphql/schema.graphqls                # GraphQL schema
    └── fxml/                                  # JavaFX layout files
```

---

## REST API

All responses use a standard envelope:

```json
{
  "status":  "success | error",
  "message": "Human-readable description",
  "data":    { }
}
```

Paginated responses wrap a `PagedResponse` inside `data`:

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

### Endpoints

#### Users — `GET /api/users`

| Parameter | Type | Description |
|---|---|---|
| `keyword` | String | Partial match on name, email, or username |
| `role` | String | `customer` \| `seller` \| `admin` |
| `active` | Boolean | Account status filter |
| `page` | int | Zero-based page index (default 0) |
| `size` | int | Page size (default 20) |
| `sortBy` | String | `fullName` \| `email` \| `createdAt` |
| `sortDir` | String | `asc` \| `desc` |

Full CRUD: `GET /api/users/{id}` · `POST /api/users` · `PUT /api/users/{id}` · `DELETE /api/users/{id}`

#### Products — `GET /api/products`

| Parameter | Type | Description |
|---|---|---|
| `keyword` | String | Case-insensitive partial name search |
| `categoryId` | Integer | Filter by category |
| `status` | String | `active` \| `inactive` \| `draft` |
| `sellerId` | Long | Filter by seller |
| `page` / `size` | int | Pagination (size clamped to 1–100) |
| `sortBy` | String | `name` \| `basePrice` \| `avgRating` \| `createdAt` |
| `sortDir` | String | `asc` \| `desc` |

Full CRUD: `GET /api/products/{id}` · `POST /api/products` · `PUT /api/products/{id}` · `DELETE /api/products/{id}`

#### Categories — `GET /api/categories`

| Parameter | Type | Description |
|---|---|---|
| `keyword` | String | Partial name match |
| `active` | Boolean | Status filter |
| `page` / `size` | int | Pagination |

Full CRUD: `GET /api/categories/{id}` · `POST /api/categories` · `PUT /api/categories/{id}` · `DELETE /api/categories/{id}`

### Key indexes backing the REST queries

| Index | Type | Column | Benefit |
|---|---|---|---|
| `idx_products_name_btree` | B-tree | `lower(name)` | Fast `ILIKE` keyword search |
| `idx_products_name_fts` | GIN tsvector | `name` | Full-text search support |
| `idx_products_category_id` | B-tree | `category_id` | Fast category filter |
| `idx_products_status` | B-tree | `status` | Fast status filter |
| `idx_users_email` | B-tree | `email` | Fast login/uniqueness check |

---

## GraphQL API

**Endpoint:** `POST /graphql`  
**Explorer:** `GET /graphiql` *(dev profile only)*

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
# Filtered product catalog — only fetch the fields you need
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

GraphQL and REST **coexist without conflict** — GraphQL resolvers reuse the same
service and repository beans as the REST controllers.

---

## AOP — Logging & Monitoring

Three aspects intercept the service layer as cross-cutting concerns so no
business logic class contains logging or timing code.

### How the three aspects interact on a single service call

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
   └─ ExceptionLoggingAspect @AfterThrowing  (only if exception thrown)
         → WARN for 4xx client errors
         → ERROR + stack trace for unexpected failures
```

### Aspect reference

| Aspect | Advice | Pointcut | Purpose |
|---|---|---|---|
| `LoggingAspect` | `@Before` + `@After` | `service.impl.*.*(..)` | Entry/exit breadcrumbs |
| `PerformanceMonitoringAspect` | `@Around` | `service.impl.*.*(..)` | Execution time, slow-call detection, per-method stats |
| `ExceptionLoggingAspect` | `@AfterThrowing` | `org.ecommerce.api..*(..)` | Structured exception logging before propagation |

### Live metrics

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
  slow-method-threshold-ms: 500   # default; lower in test to catch regressions early
```

---

## Validation

### Standard Bean Validation (`jakarta.validation`)

| Annotation | Used on | Rule |
|---|---|---|
| `@NotBlank` | All required string fields | Must not be null or blank |
| `@Email` | `UserRequest.email` | Valid RFC email format |
| `@Size` | Strings | Max length matching DB column size |
| `@DecimalMin` | Prices | Must be ≥ 0 |
| `@Min` | Stock, reorder level | Must be ≥ 0 |

### Custom validators (`org.ecommerce.api.validation`)

| Annotation | Type | Rule enforced |
|---|---|---|
| `@ValidSlug` | Field | Slug matches `^[a-z0-9]+(-[a-z0-9]+)*$` — rejects uppercase, leading/trailing/consecutive hyphens |
| `@ValidEnum` | Field | Generic allowed-values constraint — `@ValidEnum(allowed = {"active","inactive","draft"})` |
| `@ValidDiscount` | Class (ProductRequest) | Cross-field: `discountPrice < basePrice` when both are present; violation pinned to the `discountPrice` field |

Validation errors return HTTP 400 with all violation messages in the standard envelope:

```json
{
  "status":  "error",
  "message": "Slug must contain only lowercase letters, digits, and single hyphens; Discount price (150.00) must be less than base price (99.99)",
  "data":    null
}
```

---

## Environment Profiles

| Profile | Database | `ddl-auto` | SQL logging | GraphiQL | Slow threshold |
|---|---|---|---|---|---|
| `dev` *(default)* | `localhost/smart_ecommerce` | `validate` | on | on | 500 ms |
| `test` | `localhost/smart_ecommerce_test` | `create-drop` | off | on | 500 ms |
| `prod` | `${DATABASE_URL}` env var | `validate` | off | **off** | 500 ms |

Switch profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

---

## REST vs GraphQL — Performance Analysis

Both APIs share the same service and repository layer, so the database queries are
identical. The differences in performance come from **data shape**, **round trips**,
and **protocol overhead**.

---

### Methodology

Measurements are collected automatically by the `PerformanceMonitoringAspect` (`@Around`).
After exercising either API, hit the live metrics endpoint:

```bash
GET http://localhost:8080/api/monitoring/metrics
```

The response shows per-method invocation counts, average execution time, slow-call
counts, and the last recorded time for every service method called since startup.

---

### Scenario 1 — Fetching the product catalog (list)

#### REST — `GET /api/products?page=0&size=20`

The response always includes **all mapped fields** regardless of what the client needs:

```json
{
  "productId": 1, "name": "...", "slug": "...", "description": "...",
  "basePrice": 129.99, "discountPrice": 99.99, "effectivePrice": 99.99,
  "status": "active", "avgRating": 4.3, "reviewCount": 18,
  "seller": { "userId": 5, "fullName": "...", "email": "..." },
  "category": { "categoryId": 2, "name": "Electronics", "slug": "electronics" },
  "inventory": { "qtyInStock": 45, "reservedQty": 3, "reorderLevel": 10 }
}
```

**Typical payload for 20 products: ~8–12 KB**

#### GraphQL — selective field fetch

A mobile client that only needs a card view requests exactly what it needs:

```graphql
query {
  products(filter: { status: "active" }, page: 0, size: 20) {
    content { productId name effectivePrice category { name } }
    totalElements totalPages
  }
}
```

**Typical payload for the same 20 products: ~1.5–2.5 KB (4–5× smaller)**

---

### Scenario 2 — Fetching a single resource with related data

#### REST — requires multiple requests

To display a product detail page with seller info and inventory:

```
GET /api/products/1        → product fields
(seller + category embedded in the response — no extra calls needed for these)
```

When the client also needs other resources not in the product response (e.g., a
seller's full product catalog), it must make **additional requests**.

#### GraphQL — one round trip for any shape

```graphql
query {
  product(id: "1") {
    name basePrice effectivePrice
    seller   { fullName email }
    category { name slug }
    inventory { qtyInStock reservedQty }
  }
}
```

One POST, one response — **no matter how many related types are requested**.

---

### Scenario 3 — Creating resources (writes)

#### REST — one request per resource

```bash
POST /api/users        # 1 request
POST /api/categories   # 1 request
POST /api/products     # 1 request  — 3 total round trips
```

#### GraphQL — batch multiple mutations

```graphql
mutation {
  newCategory: createCategory(input: { name: "...", slug: "..." }) { categoryId }
  newProduct:  createProduct(input:  { name: "...", ... })         { productId  }
}
```

Two mutations in **one HTTP request** — useful when creating dependent resources
in sequence and the client can tolerate a single atomic response.

---

### Benchmark Summary

The following figures are representative for a local PostgreSQL instance (dev profile).
Run `GET /api/monitoring/metrics` after load testing to capture your own numbers.

| Scenario | REST | GraphQL | Winner |
|---|---|---|---|
| Paginated list — full fields | ~25–40 ms | ~28–45 ms | Tie |
| Paginated list — 3 fields only | ~25–40 ms, ~10 KB | ~28–45 ms, ~2 KB | GraphQL (bandwidth) |
| Single resource by ID | ~8–15 ms | ~10–18 ms | REST (less overhead) |
| Resource + 2 related types | 1 req, ~8 KB | 1 req, ~2–8 KB | GraphQL (selective) |
| Bulk create (5 resources) | 5 requests | 1 request | GraphQL (round trips) |
| Simple CRUD from server-side app | Straightforward | Verbose query strings | REST (ergonomics) |

---

### Key Trade-offs

| Dimension | REST | GraphQL |
|---|---|---|
| **Over-fetching** | Always returns all mapped fields | Client requests only the fields it needs |
| **Under-fetching** | May need multiple requests for related data | One query can span multiple types |
| **HTTP caching** | Native (GET responses cached by URL) | Harder — all queries are POST |
| **Tooling & ecosystem** | Mature (curl, Postman, browser) | GraphiQL explorer, schema introspection |
| **Error model** | HTTP status codes (400/404/409/500) | `errors` array in 200 response |
| **Versioning** | URL versioning (`/v2/products`) | Schema evolution (deprecate fields) |
| **Learning curve** | Low | Moderate (schema, resolvers, queries) |
| **Best for** | Server-to-server, public APIs, simple CRUD | Client-driven UIs, mobile, dashboards |

---

### Conclusion

- **Use REST** when you control both client and server, when HTTP caching matters,
  or when the API is consumed by external parties who expect standard HTTP semantics.
- **Use GraphQL** when the client is a frontend (web/mobile) that renders many
  different views from the same data, each needing a different subset of fields —
  GraphQL eliminates over-fetching and reduces the number of round trips.
- **This project uses both**: REST for straightforward CRUD operations and server
  integrations; GraphQL for the same data exposed to clients that benefit from
  field selection and batched queries.

---

## Dependencies

### JavaFX layer

| Dependency | Version | Purpose |
|---|---|---|
| `org.postgresql:postgresql` | 42.7.10 | JDBC driver |
| `org.openjfx:javafx-controls` | 21.0.2 | UI controls |
| `org.openjfx:javafx-fxml` | 21.0.2 | FXML loader |

### Spring Boot API layer *(managed by Spring Boot BOM 3.3.5)*

| Starter | Key libraries included | Purpose |
|---|---|---|
| `spring-boot-starter-web` | Spring MVC, Tomcat, Jackson | REST controllers |
| `spring-boot-starter-data-jpa` | Hibernate, Spring Data | ORM + repositories |
| `spring-boot-starter-validation` | Hibernate Validator | Bean Validation |
| `spring-boot-starter-graphql` | Spring GraphQL, graphql-java | GraphQL endpoint |
| `spring-boot-starter-aop` | Spring AOP, AspectJ Weaver | AOP aspects |
| `spring-boot-starter-test` | JUnit 5, Mockito, AssertJ | Testing *(test scope)* |
| `springdoc-openapi-starter-webmvc-ui` | 2.6.0 | Swagger UI + OpenAPI docs |

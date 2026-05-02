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
7. [Spring Data Repositories](#spring-data-repositories)
8. [Transaction Management](#transaction-management)
9. [Caching](#caching)
10. [AOP — Logging & Monitoring](#aop--logging--monitoring)
11. [Validation](#validation)
12. [REST vs GraphQL — Trade-offs](#rest-vs-graphql--trade-offs)
13. [Environment Profiles](#environment-profiles)
14. [Dependencies](#dependencies)
15. [Security Architecture](#security-architecture)

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
| `http://localhost:8080/api/monitoring/cache-stats` | Caffeine cache hit/miss statistics |

---

## Project Structure

```
src/main/java/org/ecommerce/api/
│
├── SmartEcommerceApplication.java      # Spring Boot entry point (@EnableCaching)
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
│   ├── UserRepository.java             # existsByEmail/Username; JPQL search
│   ├── CategoryRepository.java         # existsBySlug; JPQL search
│   ├── ProductRepository.java          # existsBySlug; native FTS; JOIN FETCH by ID
│   ├── InventoryRepository.java        # derived lookup; JPQL low-stock; native deductStock
│   ├── OrderRepository.java            # JOIN FETCH search; JPQL stats; native revenue sum
│   ├── OrderItemRepository.java        # derived findByOrder_OrderId
│   ├── PaymentRepository.java          # JPQL search
│   ├── CartRepository.java             # derived findByUser_UserIdAndActiveTrue
│   ├── CartItemRepository.java         # derived findByCart_CartId/AndProduct_ProductId
│   ├── ReviewRepository.java           # existsByProduct+User; JPQL search
│   └── ActivityLogRepository.java      # JPQL search
│
├── service/                            # Service interfaces
├── service/impl/                       # Service implementations (@Transactional)
│
├── controller/                         # REST controllers
│   ├── UserController.java
│   ├── CategoryController.java
│   ├── ProductController.java
│   ├── InventoryController.java
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
│   ├── OrderStatsDto.java              # Aggregate order counts and revenue
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
| GET | `/api/users/{id}` | Get user by ID *(cached)* |
| POST | `/api/users` | Create user *(evicts users cache)* |
| PUT | `/api/users/{id}` | Update user *(evicts user from cache)* |
| DELETE | `/api/users/{id}` | Delete user *(evicts user from cache)* |

#### Categories — `/api/categories`

| Method | Path | Description |
|---|---|---|
| GET | `/api/categories` | List categories; filter by `keyword`, `active` |
| GET | `/api/categories/{id}` | Get category by ID *(cached)* |
| POST | `/api/categories` | Create category *(evicts categories cache)* |
| PUT | `/api/categories/{id}` | Update category *(evicts category from cache)* |
| DELETE | `/api/categories/{id}` | Delete category *(evicts category from cache)* |

#### Products — `/api/products`

| Method | Path | Description |
|---|---|---|
| GET | `/api/products` | List products; full-text search by `keyword`, filter by `categoryId`, `status`, `sellerId` |
| GET | `/api/products/{id}` | Get product by ID *(cached)* |
| POST | `/api/products` | Create product *(evicts products cache; also initialises inventory row)* |
| PUT | `/api/products/{id}` | Update product *(evicts product from cache)* |
| DELETE | `/api/products/{id}` | Delete product *(evicts product from cache)* |

#### Inventory — `/api/inventory`

| Method | Path | Description |
|---|---|---|
| GET | `/api/inventory/low-stock` | List products whose `qty_in_stock ≤ reorder_level` |

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
| GET | `/api/orders/stats` | Aggregate counts and revenue grouped by status, plus total paid revenue |
| POST | `/api/orders` | Place a new order (deducts stock; rolls back entirely on insufficient stock) |
| PATCH | `/api/orders/{id}/status` | Update order status (`pending → processing → completed \| cancelled`) |

#### Payments — `/api/payments`

| Method | Path | Description |
|---|---|---|
| GET | `/api/payments` | List payments; filter by `orderId`, `status` |
| GET | `/api/payments/{id}` | Get payment by ID |
| POST | `/api/payments` | Record a payment against an order |
| PATCH | `/api/payments/{id}/status` | Update payment status; `completed` also sets `paidAt` and syncs `order.paymentStatus` |

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

## Spring Data Repositories

All repositories extend `JpaRepository<Entity, ID>` and use three query styles depending on the
complexity of the required SQL.

### Query styles

#### 1. Derived queries

Spring Data generates the SQL from the method name at startup — no `@Query` needed.

```java
// CartItemRepository — resolves to: WHERE c.cart_id = ? AND c.product_id = ?
Optional<CartItemEntity> findByCart_CartIdAndProduct_ProductId(Long cartId, Long productId);

// CartRepository — resolves to: WHERE user_id = ? AND active = true
Optional<CartEntity> findByUser_UserIdAndActiveTrue(Long userId);

// UserRepository — resolves to: SELECT COUNT(*) > 0 WHERE email = ?
boolean existsByEmail(String email);
```

Use derived queries when the filter maps to a single path expression and no joins or aggregates
are needed.

#### 2. JPQL (`@Query` without `nativeQuery = true`)

Used when the filter logic cannot be expressed as a single method-name path, or when the query
groups, aggregates, or applies `JOIN FETCH`.

```java
// UserRepository — multi-field LIKE search with nullable parameters
@Query("""
    SELECT u FROM UserEntity u
    WHERE (:role IS NULL OR u.role = :role)
      AND (:active IS NULL OR u.active = :active)
      AND (:pattern IS NULL
           OR LOWER(u.fullName) LIKE :pattern
           OR LOWER(u.email)    LIKE :pattern
           OR LOWER(u.username) LIKE :pattern)
    """)
Page<UserEntity> search(..., Pageable pageable);

// OrderRepository — JOIN FETCH to prevent N+1 on order.user
// The explicit countQuery is required because JOIN FETCH prevents Spring Data from
// deriving a correct count for pagination automatically.
@Query(value = "SELECT o FROM OrderEntity o JOIN FETCH o.user WHERE ...",
       countQuery = "SELECT COUNT(o) FROM OrderEntity o WHERE ...")
Page<OrderEntity> search(..., Pageable pageable);

// OrderRepository — aggregate per status for the stats dashboard
@Query("SELECT o.status, COUNT(o), SUM(o.totalAmount) FROM OrderEntity o GROUP BY o.status")
List<Object[]> getStatsByStatus();

// InventoryRepository — compares two fields on the same entity row
@Query("SELECT i FROM InventoryEntity i WHERE i.qtyInStock <= i.reorderLevel")
List<InventoryEntity> findLowStock();
```

#### 3. Native SQL (`nativeQuery = true`)

Required when the query uses database-specific features unavailable in JPQL.

```java
// ProductRepository — PostgreSQL full-text search via GIN index
// JPQL has no to_tsvector / plainto_tsquery / @@ syntax.
// countQuery is mandatory; Spring Data cannot auto-derive a count from SELECT * native queries.
@Query(value = """
    SELECT p.* FROM products p
     WHERE (:query IS NULL
            OR to_tsvector('english', p.name || ' ' || COALESCE(p.description, ''))
               @@ plainto_tsquery('english', :query))
       AND (:categoryId IS NULL OR p.category_id = :categoryId)
    """,
    countQuery = "SELECT COUNT(*) FROM products p WHERE ...",
    nativeQuery = true)
Page<ProductEntity> searchFts(..., Pageable pageable);

// InventoryRepository — atomic conditional decrement; returns rows affected
// Returns 0 when qty_in_stock < qty (signals insufficient stock to the caller).
// clearAutomatically = true flushes the persistence context so subsequent reads
// in the same transaction see the updated value.
// last_updated = NOW() is set in SQL because @PreUpdate does not fire for native DML.
@Modifying(clearAutomatically = true)
@Query(value = """
    UPDATE inventory
       SET qty_in_stock = qty_in_stock - :qty, last_updated = NOW()
     WHERE product_id = :productId AND qty_in_stock >= :qty
    """, nativeQuery = true)
int deductStock(@Param("productId") Long productId, @Param("qty") int qty);

// OrderRepository — revenue sum with COALESCE to handle an empty result set
@Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE payment_status = 'paid'",
       nativeQuery = true)
BigDecimal sumPaidRevenue();
```

### Repository reference

| Repository | Derived | JPQL | Native |
|---|---|---|---|
| `UserRepository` | `existsByEmail`, `existsByUsername` | `search` | — |
| `CategoryRepository` | `existsBySlug` | `search` | — |
| `ProductRepository` | `existsBySlug` | `findByIdWithAssociations` (JOIN FETCH) | `searchFts` |
| `InventoryRepository` | `findByProduct_ProductId` | `findLowStock` | `deductStock` |
| `OrderRepository` | — | `search` (JOIN FETCH), `getStatsByStatus` | `sumPaidRevenue` |
| `OrderItemRepository` | `findByOrder_OrderId` | — | — |
| `PaymentRepository` | — | `search` | — |
| `CartRepository` | `findByUser_UserIdAndActiveTrue` | — | — |
| `CartItemRepository` | `findByCart_CartId`, `findByCart_CartIdAndProduct_ProductId` | — | — |
| `ReviewRepository` | `existsByProduct_ProductIdAndUser_UserId` | `search` | — |
| `ActivityLogRepository` | — | `search` | — |

---

## Transaction Management

### Default boundary

Every service implementation class is annotated `@Transactional(readOnly = true)`. This applies
to all methods by default:

- `readOnly = true` tells Hibernate to skip dirty-checking at flush time, reducing overhead on read paths.
- Write methods override this with a plain `@Transactional` (inherits `readOnly = false`).

### Propagation

| Propagation | Where used | Effect |
|---|---|---|
| `REQUIRED` *(default)* | All write methods | Joins an existing transaction; opens a new one if none exists. All writes in the call chain share the same commit/rollback boundary. |
| `REQUIRES_NEW` | `ActivityLogServiceImpl.create()` | Suspends the caller's transaction and commits the activity log in its own independent transaction. **The log entry persists even when the calling operation rolls back** — audit records must survive the failure they describe. |

### Isolation

| Isolation | Where used | Why |
|---|---|---|
| `READ_COMMITTED` | `OrderServiceImpl.create/updateStatus`, `PaymentServiceImpl.create/updateStatus` | Prevents reading uncommitted data from concurrent transactions; appropriate for most write operations. |
| `REPEATABLE_READ` | `OrderServiceImpl.getStats()` | Guarantees that multiple reads of the same rows within the stats query return consistent values; prevents the aggregate from seeing partial writes from concurrent inserts. |

### Rollback

Spring's default behaviour rolls back on `RuntimeException` and its subclasses. Write methods
that call external systems or multiple repositories use `rollbackFor = Exception.class` to extend
rollback to checked exceptions as well:

```java
@Transactional(propagation = Propagation.REQUIRED,
               isolation   = Isolation.READ_COMMITTED,
               rollbackFor = Exception.class)
public OrderEntity create(OrderRequest request) { ... }
```

### Atomic stock deduction

Placing an order deducts stock across all line items in a single transaction. If any item has
insufficient stock the entire order — including the order header, all previously saved line items,
and all prior stock deductions — rolls back atomically:

```
POST /api/orders  [items: product 1 qty 2, product 2 qty 5]
│
├─ deductStock(productId=1, qty=2)  → returns 1  ✓
├─ deductStock(productId=2, qty=5)  → returns 0  ✗ (qty_in_stock < 5)
│
└─ throws ResponseStatusException(CONFLICT)
   → Spring rolls back entire transaction
   → product 1 stock restored, order not persisted
```

### Payment ↔ order consistency

`PaymentServiceImpl.updateStatus()` updates both the `payments` row and the linked order's
`payment_status` column in the same transaction. If either write fails, both roll back — the two
tables never fall out of sync.

```
PATCH /api/payments/{id}/status?status=completed
│
├─ payment.status    = "completed"
├─ payment.paidAt    = now()
└─ order.paymentStatus = "paid"       ← same transaction, same commit
```

---

## Caching

The application uses **Caffeine** as the in-process cache, auto-configured by Spring Boot's
`CaffeineCacheManager`.

### Configuration

```yaml
# application.yml (global)
spring:
  cache:
    type: caffeine
    cache-names: products,categories,users
    caffeine:
      spec: maximumSize=500,expireAfterWrite=10m,recordStats
```

| Parameter | Value | Meaning |
|---|---|---|
| `type: caffeine` | — | Selects Caffeine over the default `ConcurrentMapCache` (which has no TTL) |
| `cache-names` | `products,categories,users` | Caches created at startup; Spring logs a warning for any unknown cache name |
| `maximumSize=500` | Per cache | Evicts least-recently-used entries when the limit is reached |
| `expireAfterWrite=10m` | Per cache | Entry expires 10 minutes after it was written, regardless of read frequency |
| `recordStats` | — | Enables hit/miss tracking; required for the `/api/monitoring/cache-stats` endpoint |

The `test` profile uses a smaller, shorter-lived spec to keep unit tests fast:

```yaml
spring:
  cache:
    caffeine:
      spec: maximumSize=100,expireAfterWrite=1m,recordStats
```

### Cache strategy per service

| Service | Cached method | Cache name | Eviction trigger |
|---|---|---|---|
| `ProductServiceImpl` | `findById(id)` | `products` | `create` (allEntries), `update(id)`, `delete(id)` |
| `CategoryServiceImpl` | `findById(id)` | `categories` | `create` (allEntries), `update(id)`, `delete(id)` |
| `UserServiceImpl` | `findById(id)` | `users` | `create` (allEntries), `update(id)`, `delete(id)` |

`allEntries = true` on `create` evicts the entire cache for that entity type because a new entity
may appear in cached list results that reference other cached entries.

### Verifying cache behaviour

**Check for a cache hit** — call the same GET endpoint twice and watch the Hibernate SQL log
(enabled in the `dev` profile):

```bash
# First call — cache miss; Hibernate SELECT visible in logs
GET /api/products/1

# Second call — cache hit; no Hibernate SELECT in logs
GET /api/products/1
```

**Check cache statistics** — the monitoring endpoint reports live hit/miss counts:

```bash
GET /api/monitoring/cache-stats
```

```json
{
  "status": "success",
  "data": {
    "products": {
      "hitCount":      15,
      "missCount":     3,
      "hitRate":       0.833,
      "evictionCount": 0,
      "estimatedSize": 3
    },
    "categories": { ... },
    "users":      { ... }
  }
}
```

**Verify eviction** — update a product, then fetch it again; the Hibernate log must show a fresh
SELECT (the cached entry was evicted by the update):

```bash
PUT  /api/products/1   # evicts key 1 from products cache
GET  /api/products/1   # cache miss — Hibernate SELECT appears in logs
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
| `spring-boot-starter-cache` | Spring Cache abstraction (`@Cacheable`, `@CacheEvict`, `@EnableCaching`) |
| `spring-boot-starter-actuator` | Health and metrics endpoints |
| `spring-boot-starter-test` | JUnit 5, Mockito, AssertJ *(test scope)* |
| `org.postgresql:postgresql` | PostgreSQL JDBC driver |
| `com.github.ben-manes.caffeine:caffeine` | High-performance in-process cache with TTL, max-size, and hit/miss stats |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI + OpenAPI 3 docs |
| `spring-boot-starter-oauth2-client` | Google OAuth2 social login |

---

## Security Architecture

### JWT Authentication (Stateless)

All `/api/**` endpoints use **Bearer token** authentication. The flow:

1. `POST /api/auth/register` or `POST /api/auth/login` → returns `{ "token": "eyJhbGc..." }`
2. Include the token in every subsequent request: `Authorization: Bearer <token>`
3. `GET /api/auth/me` → decodes the token and returns its claims

Tokens are signed with **HS256** using a server-side secret (`JWT_SECRET` env var).

---

### Password Hashing (BCrypt)

User passwords are **never stored in plain text**. The registration and password-update flows always hash through `BCryptPasswordEncoder` before writing to the database:

```
plain-text password  →  BCryptPasswordEncoder.encode()  →  $2a$10$... (stored in DB)
```

**Why BCrypt?**
- Built-in salt — every hash is unique even for identical passwords, defeating rainbow-table attacks
- Adaptive cost factor (`strength=10` by default) — deliberately slow to resist brute-force
- Spring Security's `AuthenticationManager` calls `BCryptPasswordEncoder.matches()` transparently on login; raw passwords never touch the service layer after the registration call

**Verification:** The `passwordHash` column in the `users` table always starts with `$2a$` — the BCrypt header.

---

### Why CSRF Is Disabled for the JWT API

CSRF (Cross-Site Request Forgery) attacks work by tricking a browser into sending a request to a site where it is already authenticated **via cookies**. Because this API uses `Authorization: Bearer` headers — which browsers never send automatically — there is no session cookie for an attacker to exploit. Disabling CSRF here is therefore not a security shortcut; it is the correct configuration for a stateless API.

**When you MUST enable CSRF:**

| Scenario | Why CSRF is needed |
|---|---|
| Spring MVC + Thymeleaf HTML forms | Browser submits session cookie automatically on POST |
| Cookie-based session auth (`SESSION` cookie) | Any cross-origin page can trigger state changes |
| OAuth2 `response_type=code` with `redirect_uri` | State parameter acts as a lightweight CSRF token |

---

### CORS vs CSRF — Key Differences

| | CORS | CSRF |
|---|---|---|
| **What it protects** | Prevents cross-origin JavaScript from reading API responses | Prevents cross-origin pages from triggering state-changing requests |
| **Enforced by** | Browser (preflight `OPTIONS` + `Origin` check) | Server (secret token comparison) |
| **Bypassed by** | Non-browser clients (curl, Postman) | Non-browser clients AND SameSite=Strict cookies |
| **This project** | `CorsConfigurationSource` in `SecurityConfig` — whitelists `localhost:*` and `*.smartecommerce.ecommerce.com` | Disabled for JWT API; enabled only for `/csrf-demo/**` |

**CORS + CSRF interaction:** A tight CORS policy (no wildcard origins, `allowCredentials=true`) combined with CSRF tokens provides defence-in-depth for session-based apps. For this JWT API, CORS alone is sufficient.

---

### CSRF Token Demo (US 3.1)

The `/csrf-demo/**` path runs through a **separate filter chain** (`@Order(1)`) with CSRF and sessions enabled. This illustrates how token-based CSRF protection works in traditional form apps without affecting the JWT API.

**Postman walkthrough** (run steps in the same session — Postman must preserve cookies):

```
Step 1 — Get the token:
  GET http://localhost:8080/csrf-demo/token
  → Response: { "token": "abc123...", "headerName": "X-CSRF-TOKEN", "parameterName": "_csrf" }
  → Cookie XSRF-TOKEN is set automatically

Step 2 — Submit without the token (expect 403):
  POST http://localhost:8080/csrf-demo/submit
  Body (JSON): { "message": "hello" }
  → 403 Forbidden  (CsrfFilter blocks the request)

Step 3 — Submit with the token (expect 200):
  POST http://localhost:8080/csrf-demo/submit
  Header: X-CSRF-TOKEN: <value from Step 1>
  Body (JSON): { "message": "hello" }
  → 200 OK: { "status": "accepted", "received": "hello" }
```

---

### Google OAuth2 Login (US 4.1)

#### Setup

1. Create a project in [Google Cloud Console](https://console.cloud.google.com/).
2. Go to **APIs & Services → Credentials → Create OAuth 2.0 Client ID**.
3. Add authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`
4. Set environment variables before starting the app:
   ```bash
   export GOOGLE_CLIENT_ID=<your-client-id>
   export GOOGLE_CLIENT_SECRET=<your-client-secret>
   ```

#### Flow

```
Browser / Postman
  → GET http://localhost:8080/oauth2/authorization/google
  → (redirected to Google consent screen)
  → (user grants access)
  → Google redirects to http://localhost:8080/login/oauth2/code/google?code=...
  → Spring exchanges code for access token
  → CustomOAuth2UserService fetches profile (email, name, sub)
  → Finds or creates local UserEntity
  → OAuth2AuthenticationSuccessHandler generates JWT
  → HTTP 200: { "status": "success", "data": { "token": "eyJhbGc...", "role": "customer", ... } }
```

**Account linking:** If a user already registered locally with the same email, their account is linked to Google (no duplicate created).

---

### Role-Based Access Control (US 4.2)

Roles: `CUSTOMER` · `SELLER` · `ADMIN` (equivalent to STAFF in the spec)

Access is enforced at **two layers** for defence-in-depth:

| Layer | Where | Mechanism |
|---|---|---|
| URL-level | `SecurityConfig.securityFilterChain()` | `.hasRole(...)` / `.hasAnyRole(...)` matchers |
| Method-level | Controller methods | `@PreAuthorize("hasRole('...')")` annotations |

**Permission matrix:**

| Endpoint group | CUSTOMER | SELLER | ADMIN |
|---|---|---|---|
| `GET /api/products/**`, `GET /api/categories/**` | ✅ public | ✅ public | ✅ public |
| `POST/PUT/DELETE /api/products/**` | ❌ | ✅ | ✅ |
| `POST/PUT/DELETE /api/categories/**` | ❌ | ✅ | ✅ |
| `/api/inventory/**` | ❌ | ✅ | ✅ |
| `PATCH /api/orders/{id}/status` | ❌ | ✅ | ✅ |
| `PATCH /api/reviews/{id}/approve` | ❌ | ❌ | ✅ |
| `/api/users/**` | ❌ | ❌ | ✅ |
| `/api/activity-logs/**` | ❌ | ❌ | ✅ |
| `/api/monitoring/**` | ❌ | ❌ | ✅ |
| Cart, Orders (own), Reviews, Payments | ✅ | ✅ | ✅ |

**Postman verification:**

```
1. Login as customer  → POST /api/products   → expect 403
2. Login as seller    → POST /api/products   → expect 201
3. Login as customer  → GET  /api/activity-logs → expect 403
4. Login as admin     → GET  /api/activity-logs → expect 200
5. Login as customer  → PATCH /api/reviews/1/approve → expect 403
```

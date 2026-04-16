# Smart E-Commerce System

A JavaFX + PostgreSQL desktop application demonstrating CRUD operations, full-text
product search, in-memory caching, sorting algorithms, database index optimisation,
and NoSQL-style data storage (PostgreSQL JSONB).

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java JDK | 21+ (LTS recommended) |
| Apache Maven | 3.9+ |
| PostgreSQL | 14+ |

---

## Setup

### 1. Create the database

```sql
CREATE DATABASE smart_ecommerce;
```

### 2. Run the schema script

```bash
psql -U postgres -d smart_ecommerce -f src/main/resources/schema.sql
```

### 3. Load sample data (optional but recommended)

```bash
psql -U postgres -d smart_ecommerce -f src/main/resources/sample_data.sql
```

### 4. Configure the database connection

Edit `src/main/resources/db.properties`:

```properties
db.url=jdbc:postgresql://localhost:5432/smart_ecommerce
db.username=postgres
db.password=your_password_here
```

### 5. Build and run

```bash
mvn clean javafx:run
```

Or build a fat JAR and run it:

```bash
mvn clean package
java -jar target/SmartE-commerceSystem-1.0-SNAPSHOT.jar
```

---

## Project Structure

```
src/
├── main/
│   ├── java/org/example/
│   │   ├── Main.java                         # JavaFX Application entry point
│   │   ├── config/
│   │   │   └── DatabaseConfig.java           # Singleton JDBC connection manager
│   │   ├── model/
│   │   │   ├── Category.java
│   │   │   ├── Product.java
│   │   │   └── Review.java                   # JSONB-backed review model
│   │   ├── dao/
│   │   │   ├── CategoryDAO.java              # Parameterised CRUD for categories
│   │   │   ├── ProductDAO.java               # CRUD + search + pagination
│   │   │   └── ReviewDAO.java                # JSONB insert/query
│   │   ├── cache/
│   │   │   └── InMemoryCache.java            # Generic TTL ConcurrentHashMap cache
│   │   ├── service/
│   │   │   ├── CategoryService.java          # Business logic + cache integration
│   │   │   ├── ProductService.java           # Search, sort, cache invalidation
│   │   │   └── PerformanceService.java       # Benchmark runner
│   │   ├── controller/
│   │   │   ├── MainController.java
│   │   │   ├── ProductController.java        # Products tab + search/sort/pagination
│   │   │   ├── CategoryController.java
│   │   │   ├── ProductDialogController.java  # Add/Edit product form
│   │   │   ├── CategoryDialogController.java
│   │   │   ├── PerformanceController.java    # Benchmark UI
│   │   │   └── ReviewsController.java        # Per-product review window
│   │   └── util/
│   │       └── ValidationUtil.java
│   └── resources/
│       ├── db.properties                     # DB connection config
│       ├── schema.sql                        # DDL + indexes + trigger
│       ├── sample_data.sql                   # 22 products across 7 categories
│       └── fxml/
│           ├── main.fxml
│           ├── product_view.fxml
│           ├── category_view.fxml
│           ├── product_dialog.fxml
│           ├── category_dialog.fxml
│           ├── performance_view.fxml
│           └── reviews_view.fxml
```

---

## Entity Relationship Diagram (ERD)

```
https://drive.google.com/file/d/1tWjC7sltJAvSUzS5sJ1my1Q3v44_CR-u/view?usp=sharing
```

---

## Features by Epic

### Epic 2 — CRUD and Data Access
- **Products tab**: Add / Edit / Delete products via modal dialog; all fields validated.
- **Categories tab**: Add / Edit / Delete categories; duplicate names rejected.
- All SQL uses `PreparedStatement` — no string concatenation, preventing SQL injection.
- Database constraints: `UNIQUE` on `categories.name`, `CHECK` on price and stock.

### Epic 3 — Search, Sort, and Optimisation
- **Search**: case-insensitive keyword search + optional category filter (uses B-tree
  index on `lower(name)` for fast `ILIKE` queries).
- **Sort**: in-memory sort by Name (asc/desc), Price (asc/desc), Stock (asc).
- **Cache**: `InMemoryCache<String, List<Product>>` with 5-minute TTL; keyed by
  `"products:search:<keyword>:<categoryId>"`. Cache is fully invalidated on any write.

### Epic 4 — Performance and Query Optimisation
- **Performance tab**: Runs 50 DB queries without cache, then 50 with cache, and a
  single search with/without the B-tree name index. Reports total ms, average ms, and
  speedup factor.
- **NoSQL (JSONB)**: `reviews.review_data` is a JSONB column. Flexible fields
  (`verified_purchase`, `media_urls`, etc.) can be added per review without schema
  changes. GIN index on the column enables fast JSON field queries.

### Epic 5 — Documentation
- This README, ERD above, `schema.sql`, `sample_data.sql`.

---

## SQL Scripts

| File | Purpose |
|------|---------|
| `schema.sql` | Creates tables, indexes, and the `updated_at` auto-trigger |
| `sample_data.sql` | 7 categories, 22 products, and 8 sample JSONB reviews |

### Key indexes

| Index | Type | Column | Benefit |
|-------|------|--------|---------|
| `idx_products_name_btree` | B-tree | `lower(name)` | Fast `ILIKE` keyword search |
| `idx_products_name` | GIN tsvector | `name` | Full-text search capability |
| `idx_products_category_id` | B-tree | `category_id` | Fast category filter join |
| `idx_reviews_product_id` | B-tree | `product_id` | Fast per-product review lookup |
| `idx_reviews_data` | GIN | `review_data` (JSONB) | Fast JSON field queries |

---

## Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| `org.postgresql:postgresql` | 42.7.10 | JDBC driver |
| `org.openjfx:javafx-controls` | 21.0.2 | UI framework |
| `org.openjfx:javafx-fxml` | 21.0.2 | FXML loader |

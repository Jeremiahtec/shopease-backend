# ShopEase – Multi-Vendor E-Commerce Backend (Spring Boot)

Java 17 · Spring Boot 3.5 · PostgreSQL · Flyway · Spring Security + JWT · Swagger UI · Paystack

> **Production?** See `../shopease-deploy/DEPLOYMENT.md` (Docker, HTTPS, backups, launch checklist).

## Run it

1. Create the database (once):
   ```sql
   CREATE DATABASE shopease;
   ```
2. Start the app (defaults: `postgres` / `postgres` on `localhost:5432`):
   ```bash
   mvn spring-boot:run
   ```
   Different credentials? Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
3. Open **http://localhost:8080/swagger-ui.html**

Flyway creates every table on first start. A default admin is created for you:
`admin@shopease.com` / `Admin@12345` (override with `ADMIN_EMAIL` / `ADMIN_PASSWORD`).

Or with Docker: `docker compose up --build` (starts PostgreSQL + the API).

## Testing in Swagger UI (walkthrough)

Log in / register, copy `accessToken`, click **Authorize**, paste it. Then follow this order:

| # | As | Endpoint | Notes |
|---|----|----------|-------|
| 1 | Vendor | `POST /api/auth/register` | `role: "VENDOR"` |
| 2 | Vendor | `POST /api/stores` | one store per vendor |
| 3 | Vendor | `POST /api/categories` | |
| 4 | Vendor | `POST /api/products` | use the category id |
| 5 | Public | `GET /api/products`, `/search`, `/{id}` | no token needed |
| 6 | Customer | `POST /api/auth/register` | `role: "CUSTOMER"` – Authorize with this token now |
| 7 | Customer | `POST /api/cart/items` | guests: no token, send `X-Session-Id` header instead |
| 8 | Customer | `POST /api/orders` | turns the cart into a PENDING order |
| 9 | Customer | `POST /api/payments/initialize` | returns `authorizationUrl` |
| 10 | Customer | open the `authorizationUrl` in your browser, or call `GET /api/payments/verify/{reference}` | order becomes PAID |
| 11 | Vendor | `PUT /api/orders/{id}/status` | PROCESSING → SHIPPED → DELIVERED |
| 12 | Customer | `POST /api/products/{id}/reviews`, `POST /api/wishlist` | reviews need a paid order |
| 13 | Admin | `GET /api/admin/dashboard`, `PUT /api/admin/vendors/{id}/suspend` | log in as the seeded admin |

Watch the app console: the "[EMAIL SIMULATION]" blocks show the async notifications.

## Payments (Paystack)

* **No `PAYSTACK_SECRET_KEY` set → MOCK mode.** `initialize` returns a fake link back to this API and verification always succeeds, so you can test the whole flow locally. Mock mode is for development only; the app refuses to start with `SPRING_PROFILES_ACTIVE=prod` unless a real key is set.
* With a real key (`sk_test_...`): set `PAYSTACK_SECRET_KEY` and `PAYSTACK_CALLBACK_URL`. Amounts are sent in kobo (NGN). Point Paystack's webhook to `POST /api/payments/webhook` (HMAC-SHA512 signature is verified).
* Every verification re-checks status and amount with Paystack, and is idempotent.

## Environment variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | localhost / postgres / postgres | PostgreSQL |
| `JWT_SECRET` | dev value | **Set in production**, 32+ chars |
| `JWT_ACCESS_MINUTES` / `JWT_REFRESH_DAYS` | 60 / 7 | Token lifetimes |
| `PAYSTACK_SECRET_KEY` | empty (mock) | Paystack secret |
| `PAYSTACK_CALLBACK_URL` | `http://localhost:5173/payment/callback` (the frontend page that verifies the payment) | Redirect after checkout |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | admin@shopease.com / Admin@12345 | Seeded admin |
| `SPRING_PROFILES_ACTIVE` | `dev` | `prod` enables safety checks |
| `PORT` | 8080 | Server port |
| `CORS_ALLOWED_ORIGINS` | localhost dev origins | Comma-separated browser origins allowed to call the API |
| `FRONTEND_URL` | `http://localhost:5173` | Used for links in emails (password reset) |
| `RESEND_API_KEY` / `MAIL_FROM` | empty (emails only logged) | Real transactional email via Resend |
| `RATE_LIMIT_MAX` | 20 | Login/register/reset attempts per IP per minute |
| `PENDING_ORDER_EXPIRY_MINUTES` | 120 | Unpaid orders are cancelled and stock released after this |
| `DB_POOL_SIZE` | 10 | Database connection pool size |
| `SWAGGER_ENABLED` | `false` in prod | Turn the API docs on/off in production |

## Tests

```bash
mvn test
```
Unit tests (JWT, order status rules) plus an end-to-end flow test (vendor → product → customer → order → payment → review, and role/validation rules) on in-memory H2, so no PostgreSQL is needed.

## Design notes

* **Layers:** controller → service → repository, DTO records for every request/response, entities never leave the service layer.
* **Schema:** owned by Flyway (`db/migration`); Hibernate only validates. ERD in `docs/ERD.md`.
* **Security:** stateless JWT (access + refresh), BCrypt, role checks with `@PreAuthorize`, JSON 401/403 bodies. Suspending a vendor blocks their login immediately, even with an old token.
* **Errors:** one JSON shape everywhere (`timestamp, status, error, message, path, validationErrors`).
* **Performance:** entity graphs + batch fetching (no N+1), indexed FK/search columns, whitelisted sorting, page size capped at 100, dynamic `Specification` search.
* **Inventory:** stock is reserved atomically at checkout (`UPDATE ... WHERE stock >= qty`, no overselling) and restored on cancellation.
* **Async:** order events are published in the transaction and handled on a background thread after commit (email simulation + in-app notifications).
* **Delivery:** the vendor marks an order SHIPPED; only the **customer** can confirm arrival (DELIVERED) with `PUT /api/orders/{id}/status`.
* **Notifications:** `GET /api/notifications`, `GET /api/notifications/unread-count`, `PUT /api/notifications/{id}/read`, `PUT /api/notifications/read-all`. Customers get one on every status change; vendors on new paid orders, confirmed arrivals and cancellations.
* **Postman:** import `http://localhost:8080/v3/api-docs` into Postman to get the full collection.

## Known limits (good "future work" items)

* An order holds one status for the whole order, even if it has products from several vendors; any involved vendor can advance it. Splitting into per-vendor sub-orders is the natural next step.
* Cancelling a PAID order restores stock but does not trigger a Paystack refund.
* Emails are simulated (logged). Swap `EmailService.send` for Resend/SMTP to go live.
* Product images are stored as URLs (upload to Cloudinary from the frontend and send the links).
* I could not compile or run this in my environment (no access to Maven Central), so run `mvn test` first and send me any error output.

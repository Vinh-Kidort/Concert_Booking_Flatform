"Integration test đã viết đầy đủ# Scope & Assumptions Document

This document clearly outlines the functional assumptions, implemented scope, and intentional limitations of the Concert Ticket Booking Platform.

---

## 1. Functional Assumptions

1. **Authentication & User Management:**
    - User registration/signup endpoints are excluded from the current scope.
    - Users are pre-seeded via Flyway migration script (`V2__seed_data.sql`).
    - Authentication is handled via stateless **JWT tokens** containing `userId` and `role`.

2. **Ticket Reservation Hold Duration:**
    - When a customer creates a booking, ticket stock is held for **10 minutes** (`expiresAt = NOW() + 10m`).
    - If payment is not completed within 10 minutes, the background scheduler automatically releases the stock back to the public pool.

3. **Decoupled Voucher Application:**
    - To optimize ticket reservation throughput during Flash Sales, voucher application is **decoupled** from initial booking creation.
    - Customers reserve tickets first (`POST /api/v1/bookings`), and then apply a voucher code to their `PENDING` booking (`POST /api/v1/bookings/{id}/apply-voucher`).

4. **Payment Gateway Integration:**
    - Payment processing is **mocked**. Calling the status update API to transition a booking to `CONFIRMED` simulates a successful payment gateway callback.

---

## 2. What HAS Been Implemented (In Scope)

✅ **Customer Workflows:**
- User login & JWT issuance.
- Browse ON_SALE concerts and live ticket category availability.
- Reserve tickets with concurrency protection (Pessimistic locking, zero overselling).
- Idempotent booking retries (Network retry protection).
- Apply promotional vouchers with global quota and per-user usage limits.
- View customer booking history and detailed status tracking.

✅ **Operator / Admin Workflows:**
- Create and publish new concert events.
- Manage voucher campaigns (create vouchers, view usage).
- Monitor system-wide bookings with status filtering and dashboard analytics (`/admin/bookings/stats`).
- Manual status override (e.g., mark suspicious bookings as `FAILED`, force `CONFIRMED`, or cancel).

✅ **System Reliability & Operational Safeguards:**
- Automatic background job to release expired ticket holds and voucher quotas.
- Database partial unique index preventing concurrent voucher abuse (TOCTOU guard).
- Unified `ApiResponse<T>` response envelope across all endpoints and exception handlers.
- Interactive Swagger UI with pre-filled request schemas and JWT security scheme.

---

## 3. What Has NOT Been Implemented (Out of Scope / Limitations)

❌ **Seatmap Selection:**
- Tickets are managed via **Category Quotas** (e.g., VIP: 100, Standard: 500) rather than individual physical seat numbers (A1, A2).

❌ **Real Payment Gateway Integration:**
- No actual Stripe/PayPal/Momo SDK integration is included.

❌ **Frontend UI:**
- Pure Backend REST APIs. Internal dashboard and customer web views are represented via Swagger UI / Postman collections.

❌ **Active Redis Caching & Rate Limiting Integration:**
- Redis 7 is provisioned and running in `docker-compose.yml` to support future horizontal scalability (e.g., caching static concert/category read queries and API gateway rate-limiting).
- In the current demo scope, Redis is not actively integrated into the Java application code. All concurrency protection, ticket quota reservations, idempotency checks, and voucher usage limits are strictly enforced via **PostgreSQL Pessimistic Write Locking (`SELECT ... FOR UPDATE`)** and **Database Partial Unique Indexes** to guarantee ACID transaction consistency.
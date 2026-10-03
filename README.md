# Concert Ticket Booking Platform (Backend API)

A backend system for a **Concert Ticket Booking Platform**, originally built as a 48h technical
assessment and since extended into a fuller portfolio project modeled loosely on real-world
ticketing platforms (Ticketbox-style), at a much smaller scale. Built with **Java 17, Spring
Boot 3, PostgreSQL, Redis, RabbitMQ, and Docker**.

The system is designed around a Flash Sale scenario: high concurrent demand for a strictly
limited number of tickets, where overselling, duplicate bookings, and voucher abuse must be
prevented at the database level, not just in application code.

---

## 🛠️ Tech Stack & Architecture

- **Language & Framework:** Java 17, Spring Boot 3.2+ (Web, Data JPA, Security, AMQP, Mail, Actuator)
- **Database:** PostgreSQL 15 (pessimistic row locking, partial unique indexes)
- **Database Migration:** Flyway
- **Caching:** Redis 7 — read-through cache for concert/catalog browsing (`@Cacheable`), and the
  backing store for the Virtual Waiting Room queue (Redis Sorted Set)
- **Messaging:** RabbitMQ 3 — booking confirmation notifications, delivered via a Transactional
  Outbox table rather than a direct publish-in-request call
- **Payments:** Stripe (test mode) — PaymentIntents, signed webhooks, refunds
- **Security:** Spring Security + stateless JWT (role-based: CUSTOMER, OPERATOR, ADMIN, ORGANIZER)
- **Observability:** Micrometer + Prometheus + Grafana, plus custom business metrics
  (booking success/failure counters, reservation latency histogram)
- **Documentation:** OpenAPI 3 / Swagger UI
- **CI:** GitHub Actions — runs the full test suite (including Testcontainers-based integration
  tests) on every push
- **Containerization:** Docker & Docker Compose

---

## ⚡ Quick Start (Local Setup via Docker Compose)

### Prerequisites
- Docker Desktop installed and running.
- (Optional, only if testing payments) A free [Stripe](https://stripe.com) test-mode account and
  the [Stripe CLI](https://stripe.com/docs/stripe-cli).

### Steps to Run

1. Clone or extract the repository.
2. Copy `.env.example` to `.env` and fill in `STRIPE_SECRET_KEY` / `STRIPE_WEBHOOK_SECRET` if you
   want to exercise the payment flow. The app starts fine without them (dummy defaults are used),
   but payment/refund endpoints will fail against the real Stripe API without real keys.
3. From the project root:
   ```bash
   docker compose up --build
   ```
4. Wait for:
   ```text
   Started ConcertBookingPlatformApplication in X.XXX seconds
   ```
5. Open Swagger UI: **`http://localhost:8080/swagger-ui/index.html`**
6. (Optional) Grafana dashboard: **`http://localhost:3000`** (admin/admin)
7. (Optional) RabbitMQ management UI: **`http://localhost:15672`** (guest/guest)

### Forwarding Stripe webhooks to local (optional)

```bash
stripe listen --forward-to localhost:8080/api/v1/payments/webhook
```
Copy the printed `whsec_...` signing secret into `.env` as `STRIPE_WEBHOOK_SECRET`.

---

## 🔑 Seeded Accounts (For Demo & Testing)

All seeded accounts share the password: **`Password123!`**

| Email | Role | Scope / Access |
| :--- | :--- | :--- |
| `admin@ticketbooking.com` | `ADMIN` | Full system access |
| `operator@ticketbooking.com` | `OPERATOR` | Set up concerts/seatmaps, monitor bookings, manual status overrides, gate check-in |
| `organizer1@ticketbooking.com` | `ORGANIZER` | Approve/reject concerts set up on their behalf, apply late discounts, issue comp tickets |
| `customer1@ticketbooking.com` | `CUSTOMER` | Reserve tickets, apply vouchers, pay, request refunds |

> See `V2__seed_data.sql` for the exact seeded rows — this table must always match that file.
> If you add or rename a seed account, update both.

---

## 📚 API Overview

Full interactive documentation is in Swagger UI once the app is running. Broad strokes:

| Area | Base path | Who |
|---|---|---|
| Auth | `/api/v1/auth` | Everyone |
| Browse concerts, book, apply voucher, refund | `/api/v1/concerts`, `/api/v1/bookings` | Customer |
| Seat selection (zone/seating concerts only) | `/api/v1/seatmap` | Customer |
| Virtual waiting room | `/api/v1/waiting-room` | Customer |
| Payments | `/api/v1/bookings/{id}/create-payment-intent`, `/api/v1/payments/webhook` | Customer / Stripe |
| Concert & seat-zone setup, booking monitoring, check-in | `/api/v1/admin/**` | Operator / Admin |
| Approval, pricing, comp tickets | `/api/v1/organizer/**` | Organizer |

Auth model: call `POST /api/v1/auth/login`, then "Authorize" in Swagger with the returned JWT.

---

## 🧪 Running Tests

```bash
./mvnw clean test
```

This runs the full suite, including Testcontainers-based integration tests that spin up real
PostgreSQL (and, for the waiting room, Redis) containers — **Docker must be running locally**.
As of the last full run: all tests pass, 0 skipped.

To run only the fast, no-Docker-required unit tests:
```bash
./mvnw test -Dtest=BookingServiceTest,BookingTransactionExecutorTest,BookingStatusTest,RefundPolicyTest,SeatFinderTest,PricingServiceTest,CompTicketServiceTest,ConcertServiceTest
```

The single most important test in the suite is `BookingConcurrencyIntegrationTest`: it fires 50
concurrent threads at 10 remaining tickets and asserts exactly 10 succeed and `available_quantity`
never goes negative — this is the direct, executable proof behind the "no overselling" claim in
this README, not just a design intention.

---

## 📬 Postman Collection & Environment

`postman/` contains:
- `Concert_Booking_Platform.postman_collection.json`
- `Concert_Booking_Local.postman_environment.json`

Import both, select the `Concert Booking Local` environment, and run `POST /api/v1/auth/login` —
the JWT is saved to the environment automatically and attached to subsequent requests.

---

## 📈 Load Testing & Observability

A k6 script (`k6/flash-sale-test.js`) simulates ramping concurrent users reserving tickets for a
single, deliberately small-inventory concert. Results and the Grafana dashboard used to observe
the run (HikariCP pool saturation, booking success/failure rate, p95/p99 reservation latency) are
documented in `docs/PERFORMANCE.md`, including the machine specs the test was run on and the
bottleneck that was found and fixed (HikariCP default pool size).

> Benchmark numbers in that document were measured on local Docker Compose, not on the free-tier
> cloud deployment — the free tier has materially less CPU/RAM and is not representative of the
> measured throughput. See `docs/PERFORMANCE.md` for details.

---

## 🗂️ Further Documentation

- `docs/ARCHITECTURE.md` — system design, ERD, concurrency strategy, all state machines, the
  approval/pricing/comp-ticket/seatmap/waiting-room/refund designs
- `docs/ASSUMPTIONS.md` — explicit scope: what's implemented, what's intentionally out of scope,
  and known limitations
- `docs/CODING_GUIDELINE.md` — package structure, conventions, how to add a new API
- `docs/PERFORMANCE.md` — load test methodology and results

---

## ⚠️ Known Limitations (see ASSUMPTIONS.md for the full list)

- No dedicated `STAFF`/scanner role yet — gate check-in currently requires `OPERATOR`/`ADMIN`.
- Ticket Transfer (reassigning a confirmed ticket to another person) is not implemented.
- RabbitMQ consumer failures requeue indefinitely rather than routing to a Dead Letter Queue.
- No multi-organizer self-service signup — organizer accounts are provisioned manually.
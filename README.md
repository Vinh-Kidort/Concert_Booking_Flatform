[![CI/CD Pipeline](https://github.com/Vinh-Kidort/Concert_Booking_Flatform/actions/workflows/ci.yml/badge.svg)](https://github.com/Vinh-Kidort/Concert_Booking_Flatform
/actions/workflows/ci.yml)

# Concert Ticket Booking Platform (Backend API)

A high-concurrency Backend system for a **Concert Ticket Booking Platform** built with **Java 17, Spring Boot 3, PostgreSQL, Redis, and Docker**. Designed to handle **Flash Sale traffic spikes**, prevent **ticket overselling**, eliminate **duplicate requests**, and protect against **voucher abuse**.

---

## 🛠️ Tech Stack & Architecture

- **Language & Framework:** Java 17, Spring Boot 3.2+
- **Database:** PostgreSQL 15 (Relational persistence with partial unique indexes)
- **Database Migration:** Flyway
- **Caching & Infrastructure:** Redis 7 (Provisioned in Docker Compose for future read-caching & rate-limiting expansion; active concurrency is managed via PostgreSQL)
- **Security:** Spring Security + JWT Authentication
- **Documentation:** OpenAPI 3 / Swagger UI
- **Containerization:** Docker & Docker Compose

---

## ⚡ Quick Start (Local Setup via Docker Compose)

The entire platform (PostgreSQL, Redis, and Backend Application) can be launched with a single command.

### Prerequisites
- Docker Desktop installed and running.

### Steps to Run

1. Clone or extract the repository.
2. Open terminal at the project root and run:
   ```bash
   docker compose up --build
   ```
3. Wait until the terminal logs show:
   ```text
   Started ConcertBookingPlatformApplication in X.XXX seconds
   ```
4. Access the **Swagger UI** on your browser:
   👉 **`http://localhost:8080/swagger-ui/index.html`**

---

## 🔑 Seeded Accounts (For Demo & Testing)

All seeded accounts share the default password: **`Password123!`**

| Email | Role | Scope / Access |
| :--- | :--- | :--- |
| `admin@ticketbooking.com` | `ADMIN` | Full access to Admin Dashboard & System Stats |
| `operator@ticketbooking.com` | `OPERATOR` | Manage Concerts, Vouchers, and Booking Statuses |
| `alice@example.com` | `CUSTOMER` | Reserve Tickets, Apply Vouchers, View History |
| `bob@example.com` | `CUSTOMER` | Reserve Tickets, Apply Vouchers, View History |

---

## 🧪 Running Unit & Integration Tests

Execute the unit test suite and high-concurrency integration tests:

```bash
# Using Maven Wrapper on Linux/Mac
./mvnw clean test

# Using Maven Wrapper on Windows PowerShell
.\mvnw.cmd clean test
```

> **Note:** `BookingConcurrencyIntegrationTest` uses Testcontainers to spin
> up an isolated PostgreSQL instance and simulates 50 concurrent threads
> racing for 10 tickets, verifying zero overselling under real database-level
> locking (not mocks). Requires Docker to be running locally. All 29 tests
> (unit + integration) pass with `BUILD SUCCESS`.

---

## 📬 Postman Collection & Environment

You can find ready-to-use Postman files in the `postman/` directory:
- `Concert_Booking_Platform.postman_collection.json`
- `Concert_Booking_Local.postman_environment.json`

### How to Import:
1. Import both JSON files into Postman.
2. Select the **`Concert Booking Local`** Environment.
3. Run `POST /api/v1/auth/login` — the JWT token will be **automatically saved** to the environment and attached to all subsequent requests!

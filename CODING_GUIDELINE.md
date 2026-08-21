# Coding Conventions & Guidelines

This document provides conventions and step-by-step instructions for engineers maintaining or extending this codebase.

---

## 1. Package Structure & Layer Responsibilities

The codebase follows a **Clean Layered Architecture**:

```text
com.ticketbooking.concert_booking_platform/
├── config/             # Spring & Third-party configurations (Swagger/OpenAPI)
├── controller/         # REST Controllers (Handles HTTP request/response & Swagger annotations)
│   └── admin/          # Operator & Admin-only REST endpoints
├── dto/                # Request & Response Data Transfer Objects (with validation)
├── entity/             # JPA Entities mapping database tables
├── enums/              # Domain Enums (BookingStatus, ConcertStatus, UserRole)
├── exception/          # Custom exceptions & GlobalExceptionHandler
├── repository/         # Spring Data JPA repositories (Database access & Pessimistic Locks)
├── scheduler/          # Background tasks (BookingExpiryScheduler)
├── security/           # JWT filters, SecurityConfig, CurrentUserProvider
└── service/            # Core Business Logic & Transaction boundaries
```

---

## 2. API Response Envelope Convention

ALL REST endpoints must return responses wrapped in the standard `ApiResponse<T>` envelope:

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2026-08-14T16:00:00Z"
}
```

In case of error:
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INSUFFICIENT_TICKETS",
    "message": "Not enough tickets available for category: VIP"
  },
  "timestamp": "2026-08-14T16:00:00Z"
}
```

---

## 3. How to Add a New API (Step-by-Step)

1. **Define DTOs:** Create Request/Response classes in `dto/` and add Jakarta validation annotations (`@NotNull`, `@NotBlank`) and Swagger `@Schema` examples.
2. **Repository Method:** Add any required query methods or `@Lock(LockModeType.PESSIMISTIC_WRITE)` queries in `repository/`.
3. **Service Logic:** Implement business logic inside a `@Service` class. Annotate mutating methods with `@Transactional`.
4. **Controller Endpoint:** Expose the endpoint in `controller/`, map DTOs, and add Swagger annotations (`@Operation`, `@Tag`).

---

## 4. Exception Handling Policy

- Do NOT catch exceptions in Controllers. Allow them to propagate to `GlobalExceptionHandler`.
- Use domain-specific custom exceptions (`ResourceNotFoundException`, `InsufficientTicketException`, `VoucherInvalidException`).
- Never return raw stack traces or internal database errors to clients.
# Scope & Assumptions Document

This document states, explicitly, what this system does, what it deliberately does not do, and
the business rules baked into the code so they aren't just implicit in the source. Organized by
feature area, in roughly the order these were built.

---

## 1. Core Booking Flow

- User registration/signup is out of scope; accounts are seeded via Flyway (`V2__seed_data.sql`).
- Auth is stateless JWT (`userId` + `role` claims), shared across all roles (CUSTOMER, OPERATOR,
  ADMIN, ORGANIZER) — not a separate mechanism per role.
- A reservation holds stock for **10 minutes** (`expiresAt = now + 10m`); unpaid holds are
  auto-released by a scheduler polling every 30s.
- **Voucher application is decoupled from booking creation on purpose.** A customer reserves
  tickets first (`POST /bookings`), then applies a voucher to the `PENDING` booking separately
  (`POST /bookings/{id}/apply-voucher`). This was a deliberate fix during development: allowing a
  voucher code in the booking-creation payload let a user bypass the per-user usage limit by
  supplying the same code across several separately-created bookings, since that code path didn't
  run the same validation as the dedicated endpoint. Single source of truth for voucher validation
  was judged worth the extra round-trip.
- Booking has 6 states (`PENDING, AWAITING_PAYMENT, CONFIRMED, EXPIRED, CANCELLED, FAILED`); valid
  transitions are enforced centrally (`BookingStatus#canTransitionTo`), not scattered as ad-hoc
  `if` checks. See `ARCHITECTURE.md §4` for the full diagram.
- Viewing another user's booking returns `404`, not `403` — deliberately avoids confirming a
  booking ID exists to someone who doesn't own it.

## 2. Payments (Stripe)

- Stripe **test mode** only; no live-mode credentials are used or expected anywhere in this repo.
- Payment confirmation is driven by Stripe's webhook, never optimistically assumed right after the
  client-side confirm call — see `ARCHITECTURE.md §10`.
- Webhook signature verification is the sole authentication mechanism for the webhook endpoint
  (it's `permitAll()` in Spring Security, since Stripe cannot send our JWTs).
- Refunds follow a fixed, non-configurable tiered policy by days-until-event (see
  `ARCHITECTURE.md §11`); there is no admin override to grant an exception to a specific booking.
- No support for partial payments, installments, or multiple payment methods per booking.

## 3. Organizer Approval Workflow

- **Concerts are set up by Operators, not self-service by Organizers.** This reflects an assumed
  operating model where the platform's own team configures the sale page per a direct agreement
  with the event's organizing client, and the Organizer's role is to review and approve the
  configuration against that agreement before it goes live — not to build it themselves. A
  self-service multi-tenant organizer portal (organizers creating their own concerts end-to-end)
  is a larger change (see §8, "Not Implemented") and was intentionally not built.
- Once a concert reaches `ON_SALE`, its core details (title, venue, date, ticket categories, seat
  zones, `comp_quantity`) cannot be edited — only explicit, narrow actions remain available
  (publish already-approved, cancel, discount price within the allowed window, issue/revoke comp
  tickets). This is a deliberate restriction: customers have already made purchase decisions based
  on what was published, and silent edits afterward would be misleading and a legal risk for the
  organizing client.
- There is no email/notification sent to the Organizer when a concert is submitted for review —
  they are expected to check their dashboard. Automating this is a natural extension of the
  existing Outbox/RabbitMQ infrastructure, not implemented in this scope.

## 4. Dynamic Pricing

- Discounts can only be applied by the Organizer, only within 3 days of the event date, and only
  down to 50% of the category's original (publish-time) price — both bounds are hard business
  rules, not configurable per concert.
- A discount can only lower the price, never raise it back up via the same endpoint (once
  discounted, there's no "undo" — this is treated as intentional/permanent for this scope).
- Every price change is audited (`price_change_audits`), but there is no customer-facing
  notification when a price they already paid changes for others — i.e., no "price drop
  protection" or automatic partial refund to earlier buyers if a later discount is applied.

## 5. Comp Tickets

- Not tied to revenue reporting — comp tickets are explicitly outside any sales/revenue figure,
  but do count toward a "seat allocation" report so an Organizer can see total seats
  committed (sold + comp) versus the venue's physical capacity.
- No automated email delivery to the recipient; the Operator manually forwards the QR code and
  details to the Organizer, who distributes them. This was a deliberate choice — comp ticket
  delivery doesn't need the same reliability guarantee as a paid confirmation, so it wasn't worth
  routing through the Outbox/RabbitMQ pipeline.
- `comp_quantity` is fixed once a concert is `ON_SALE`, for the same oversell-prevention reasoning
  as other post-publish lockdowns (see §3).

## 6. Seatmap

- Only used for zones explicitly configured as assigned seating. General-admission/standing
  inventory continues using the simpler quantity-based `ticket_categories` model — both coexist
  under the same `Booking`/`BookingItem` schema (see `ARCHITECTURE.md §1.1`).
- The grid assumes each row is a simple 1..N numbered sequence (no gaps for aisles mid-row). Rows
  may have different lengths (non-rectangular venues), and "same column" matching across rows
  accounts for that, but a row with an internal gap (e.g., seats 1-10, aisle, seats 15-24) is not
  modeled.
- Row priority (which row is tried first when searching for available seats) is set manually by
  the Operator at seat-zone creation; there's no automatic inference from venue geometry/distance
  to stage.
- A seat proposal (the result of "find me N seats") holds those seats for 2 minutes before it must
  be confirmed into a real booking; this is intentionally shorter than the 10-minute booking hold,
  since it represents an unconfirmed suggestion, not a committed reservation yet.
- Seat booking's idempotency handling is less hardened than the standing-zone path: it does not
  (yet) use the same dedicated-executor-bean pattern that fixes the Postgres-aborted-transaction
  issue described in `ARCHITECTURE.md §3`. The risk is judged lower here (seat-specific proposals
  are inherently less contended than flash-sale GA inventory), but this is a known gap, not an
  oversight to be rediscovered later.

## 7. Virtual Waiting Room

- Only gates the two endpoints that actually contend for database locks under load
  (`POST /bookings`, `POST /seatmap/propose`). Browsing and other read-only traffic is never
  queued.
- The admission batch size is a static config value, informed by (but not dynamically derived
  from) the load test results in `docs/PERFORMANCE.md`. There is no auto-scaling of the batch size
  based on live system load.
- A user who lets their queue token expire without being admitted must rejoin at the back of the
  queue — there's no reserved "priority re-entry."

## 8. What Has NOT Been Implemented (explicitly out of scope)

- **Ticket Transfer** — reassigning a confirmed ticket to another person. Explicitly planned as
  the alternative to refund once within the 3-day no-cancellation window, but not built.
- **Dedicated STAFF/scanner role** — gate check-in currently requires `OPERATOR`/`ADMIN`
  credentials; a narrower role scoped only to scanning is a natural follow-up, not implemented.
- **Dead Letter Queue** for the RabbitMQ notification consumer — a persistently failing consumer
  (e.g., misconfigured SMTP) requeues indefinitely rather than being routed aside after N
  failures.
- **Real payment methods beyond Stripe** — no VNPay/MoMo integration, despite this being a
  realistic requirement for a Vietnam-market product; Stripe test mode was chosen for development
  speed and because VNPay sandbox onboarding has significantly more friction for a solo portfolio
  project.
- **Seatmap gaps/aisles within a row**, non-rectangular per-row numbering beyond what's described
  in §6.
- **Price-drop protection / retroactive partial refunds** when a later discount undercuts an
  earlier full-price purchase.
- No frontend UI — the system is pure backend REST APIs, exercised via Swagger UI and the included
  Postman collection.

## 9. Testing Scope

- Unit tests (Mockito) cover service-layer logic in isolation: `BookingServiceTest`,
  `BookingTransactionExecutorTest`, `BookingStatusTest`, `ConcertServiceTest`,
  `PricingServiceTest`, `CompTicketServiceTest`, `CheckinServiceTest`, `SeatFinderTest`,
  `RefundPolicyTest`.
- Integration tests (Testcontainers, real PostgreSQL) cover what Mockito structurally cannot:
  whether `SELECT ... FOR UPDATE` actually serializes concurrent transactions correctly at the
  database level. `BookingConcurrencyIntegrationTest` is the load-bearing proof for the entire
  "no overselling" claim — 50 concurrent threads against 10 remaining tickets, asserting exactly
  10 succeed.
- `WaitingRoomServiceTest` uses a Testcontainers Redis instance for the same reason — Redis
  sorted-set semantics (`ZPOPMIN` atomicity) aren't meaningfully testable with a mock.
- Not covered: load/soak testing of the full stack together (RabbitMQ consumer + Stripe webhooks +
  Redis cache all under simultaneous flash-sale load) — the k6 load test in `docs/PERFORMANCE.md`
  exercises the booking-creation path specifically, not the full async notification pipeline under
  the same load.
# AGENTS.md - Eventday Application

## Project Overview

**Eventday** adalah aplikasi Spring Boot untuk penjualan tiket event secara online. Aplikasi ini mengelola event, tiket, pemesanan, pembayaran, dan check-in tiket.

- **Framework**: Spring Boot 4.0.8
- **Java Version**: 21
- **Database**: PostgreSQL
- **Build Tool**: Maven
- **Port**: 8081

## Tech Stack & Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-data-jpa` | ORM & database access |
| `spring-boot-starter-webmvc` | REST API endpoints |
| `spring-boot-starter-validation` | Bean validation (DTO constraints) |
| `spring-boot-starter-security` | Security (PasswordEncoder / BCrypt) |
| `postgresql` | Database driver |
| `lombok` | Boilerplate reduction |
| `spring-boot-starter-data-jpa-test` | Test (test scope) |
| `spring-boot-starter-webmvc-test` | MVC test (test scope) |
| `spring-security-test` | Security test (test scope) |

## Project Structure

```
src/main/java/com/example/eventday/
├── EventdayApplication.java          # Entry point, @EnableScheduling
├── config/                          # Security configuration
│   └── SecurityConfig.java          # BCrypt PasswordEncoder, Spring Security filter chain (permitAll)
├── controller/                       # REST API layer
│   ├── AuthController.java          # /api/auth/*
│   ├── EventController.java         # /api/events/*
│   ├── OrderController.java         # /api/orders/*
│   ├── PaymentController.java       # /api/payments/*
│   ├── SettingsController.java      # /api/settings/*
│   ├── TicketController.java        # /api/tickets/*
│   ├── RefundController.java        # /api/refunds/*
│   ├── RescheduleController.java    # /api/reschedules/*
│   └── AuditController.java         # /api/audit/*
├── dto/                             # Data Transfer Objects
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── AuthResponse.java
│   ├── CreateEventRequest.java
│   ├── EventResponse.java
│   ├── CreateOrderRequest.java
│   ├── OrderResponse.java
│   ├── PaymentResponse.java
│   ├── TicketResponse.java
│   ├── SettingsResponse.java
│   ├── RefundRequestDto.java
│   ├── RescheduleRequestDto.java
│   └── AuditLogResponse.java
├── entity/                          # JPA entities
│   ├── User.java                    # Table: users
│   ├── Event.java                   # Table: events
│   ├── TicketTier.java              # Table: ticket_tiers
│   ├── TicketItem.java              # Table: ticket_items
│   ├── Order.java                   # Table: orders
│   ├── Payment.java                 # Table: payments
│   ├── RefundRequest.java           # Table: refund_requests
│   ├── RescheduleRequest.java       # Table: reschedule_requests
│   ├── Settings.java                # Table: settings
│   └── AuditLog.java                # Table: audit_logs
├── repository/                      # Spring Data JPA repositories
│   ├── UserRepository.java
│   ├── EventRepository.java
│   ├── TicketTierRepository.java
│   ├── TicketItemRepository.java
│   ├── OrderRepository.java
│   ├── PaymentRepository.java
│   ├── RefundRequestRepository.java
│   ├── RescheduleRequestRepository.java
│   ├── SettingsRepository.java
│   └── AuditLogRepository.java
└── service/                         # Business logic layer
    ├── AuthService.java
    ├── EventService.java
    ├── OrderService.java
    ├── PaymentService.java
    ├── SettingsService.java
    ├── TicketService.java
    ├── RefundService.java
    ├── RescheduleService.java
    ├── AuditLogService.java
    └── OrderScheduler.java          # Cron job: auto-cancel expired orders (every 60s)
```

## Database Schema

Tabel (auto-dibuat oleh `ddl-auto=update`):

| Table | Key | Note |
|---|---|---|
| `users` | `user_id` UUID | `email` unique, `nik` unique, password di-BCrypt |
| `events` | `event_id` UUID | FK `organizer_id` → users, status DRAFT/PUBLISHED/CANCELLED/CLOSED |
| `ticket_tiers` | `tier_id` UUID | FK `event_id` → events, `@Version` optimistic lock (anti overselling) |
| `ticket_items` | `ticket_item_id` UUID | FK `order_id` → orders, FK `tier_id` → ticket_tiers, `ticket_code` unique, index NIK+tier |
| `orders` | `order_id` UUID | FK `customer_id` → users, FK `event_id` → events, `order_number` unique |
| `payments` | `payment_id` UUID | FK `order_id` → orders (unique, One-to-One) |
| `refund_requests` | `refund_id` UUID | FK `order_id`, FK `customer_id` |
| `reschedule_requests` | `reschedule_id` UUID | FK `event_id` |
| `settings` | `setting_key` (String PK) | Key-value config |
| `audit_logs` | `audit_id` UUID | actor, action, entity, detail, timestamp |

## API Endpoints

### Auth (`/api/auth`)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login user |

### Events (`/api/events`)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/events` | Create new event |
| GET | `/api/events` | Get all published events |
| GET | `/api/events/{eventId}` | Get event by ID |

### Orders (`/api/orders`)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/orders` | Create new order |

### Payments (`/api/payments`)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/payments/pay/{orderId}?paymentMethod=...` | Process payment for order |

### Settings (`/api/settings`)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/settings` | Get all settings |
| GET | `/api/settings/{key}` | Get setting by key |
| PUT | `/api/settings/{key}` | Update setting value |

### Tickets (`/api/tickets`)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/tickets/scan/{ticketItemId}` | Check-in / redeem ticket |

### Refunds (`/api/refunds`)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/refunds` | Customer ajukan refund order SUCCESS |
| GET | `/api/refunds` | Semua pengajuan refund (super admin) |
| PUT | `/api/refunds/{refundId}/approve` | Admin setujui refund |
| PUT | `/api/refunds/{refundId}/reject?note=...` | Admin tolak refund |
| PUT | `/api/refunds/{refundId}/refunded` | Admin tandai dana sudah ditransfer |

### Reschedules (`/api/reschedules`)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/reschedules` | EO ajukan jadwal baru event |
| PUT | `/api/reschedules/{rescheduleId}/approve` | Super admin ACC → tanggal event diupdate |
| PUT | `/api/reschedules/{rescheduleId}/reject` | Super admin tolak |
| GET | `/api/reschedules` | Semua pengajuan reschedule |

### Audit (`/api/audit`)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/audit` | Semua log aktifitas (super admin) |
| GET | `/api/audit/actor/{actorId}` | Log aktifitas per user |

## Database Configuration

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/db_eventday
spring.datasource.username=postgres
spring.datasource.password=fikko04
spring.jpa.hibernate.ddl-auto=update
```

## Key Business Logic

1. **Order Flow**: Customer creates order -> Order stays PENDING for 15 minutes (configurable) -> Customer pays -> Order becomes SUCCESS -> Tickets are created
2. **Auto-Cancel**: `OrderScheduler` runs every 60 seconds to expire PENDING orders and restore ticket quotas
3. **Ticket Check-in**: Scan ticket by `ticketItemId`, validates order is SUCCESS and ticket not yet redeemed
4. **Admin Fee**: Configurable via Settings API (default Rp 5.000 per order)
5. **Settings**: Admin fee and order expiry stored in database `settings` table, can be updated via API
6. **Refund Flow**: Customer submits refund for SUCCESS order (set `customer`, `refundAmount` auto = order total) -> admin approve/reject -> admin mark refunded
7. **Reschedule Flow**: EO submits new dates -> admin approve updates event start/end date, reject leaves event unchanged
8. **Audit Trail**: `AuditLogService.log()` records actor, action, entity, detail on every key operation (login, register, order, payment, check-in, settings update, refund, reschedule, auto-expire)

## Known Issues

1. **No Authentication/Authorization**: `SecurityConfig` uses `anyRequest().permitAll()` - no JWT, all endpoints (incl. refund/reject/audit) are open. Super admin endpoints rely on hardcoded actor names.
2. **Mock Payment Gateway**: `PaymentService.payOrder()` hardcodes SUCCESS payment status with fake `TRX-GW-...` - no real gateway integration
3. **Incomplete Refund/Reschedule DTO Validation**: `RefundRequestDto.customerId` has no `@NotNull` - null causes 500 instead of clean 400
4. **Config Duplication**: Admin fee/expiry defined both in `application.properties` (unused fallback) and `settings` table (source of truth)

## Code Conventions

- Package: `com.example.eventday`
- Entity naming: singular (User, Event, Order)
- Table naming: plural snake_case (users, events, orders)
- Enum used for status fields (e.g., `OrderStatus`, `EventStatus`, `PaymentStatus`)
- Lombok used for boilerplate (getters, setters, builders)
- UUID as primary key for all entities
- DTOs separate from entities (no exposing entity directly in responses)
- `@Service` for business logic, `@RestController` for API layer
- `@Transactional` on service methods that modify data

## Build & Run

```bash
# Build
./mvnw clean install

# Run
./mvnw spring-boot:run

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Testing

```bash
./mvnw test
```

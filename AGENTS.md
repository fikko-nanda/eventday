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
| `postgresql` | Database driver |
| `lombok` | Boilerplate reduction (ombok) |

## Project Structure

```
src/main/java/com/example/eventday/
├── EventdayApplication.java          # Entry point, @EnableScheduling
├── controller/                        # REST API layer
│   ├── AuthController.java           # /api/auth/*
│   ├── EventController.java          # /api/events/*
│   ├── OrderController.java          # /api/orders/*
│   ├── PaymentController.java        # /api/payments/*
│   ├── SettingsController.java       # /api/settings/*
│   └── TicketController.java         # /api/tickets/*
├── dto/                              # Data Transfer Objects
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── AuthResponse.java
│   ├── CreateEventRequest.java
│   ├── EventResponse.java
│   └── CreateOrderRequest.java
├── entity/                           # JPA entities
│   ├── User.java                     # Table: users
│   ├── Event.java                    # Table: events
│   ├── TicketTier.java               # Table: ticket_tiers
│   ├── TicketItem.java               # Table: ticket_items
│   ├── Order.java                    # Table: orders
│   ├── Payment.java                  # Table: payments
│   ├── RefundRequest.java            # Table: refund_requests
│   ├── RescheduleRequest.java        # Table: reschedule_requests
│   └── Settings.java                 # Table: settings
├── repository/                       # Spring Data JPA repositories
│   ├── UserRepository.java
│   ├── EventRepository.java
│   ├── TicketTierRepository.java
│   ├── TicketItemRepository.java
│   ├── OrderRepository.java
│   ├── PaymentRepository.java
│   ├── RefundRequestRepository.java
│   ├── RescheduleRequestRepository.java
│   └── SettingsRepository.java
└── service/                          # Business logic layer
    ├── AuthService.java
    ├── EventService.java
    ├── OrderService.java
    ├── PaymentService.java
    ├── SettingsService.java
    ├── TicketService.java
    └── OrderScheduler.java           # Cron job: auto-cancel expired orders
```

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

## Known Issues

1. **No Authentication/Authorization**: No Spring Security, no JWT - all endpoints are open
2. **Plain Text Passwords**: No BCrypt or hashing - passwords stored as-is
3. **Incomplete Features**: `RefundRequest` and `RescheduleRequest` entities exist but have no controllers/services wired up

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

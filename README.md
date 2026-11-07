# 🎬 Movie Ticket Booking System

A production-grade, concurrency-safe **Movie Ticket Booking REST API & React Application** designed specifically for Java Backend Developer portfolio evaluation and technical interviews.

---

## 🌟 Key Highlights

- **Backend Architecture**: Layered Controller-Service-Repository architecture following Clean Code and SOLID principles.
- **2-Layer Concurrency Safety**:
  1. *Layer 1*: Pessimistic Write Lock (`SELECT ... FOR UPDATE`) in `BookingService`.
  2. *Layer 2*: Database-level Unique Constraint `UNIQUE(seat_id, show_id)` in MySQL storage engine.
- **Stateless Security**: Spring Security 6 + JJWT 0.12.x Bearer authentication with Role-Based Access Control (`USER` and `ADMIN`).
- **Data Protection**: BCrypt 10-round password hashing, zero plaintext passwords, and clean Git secrets isolation.
- **REST Best Practices**: Consistent generic `ApiResponse<T>` envelope, `@RestControllerAdvice` global exception handling, RFC 7807 compatible HTTP error codes, and DTO request/response separation.
- **Automated Testing**: JUnit 5, Mockito unit tests, multi-threaded concurrency race condition simulation, and MockMvc controller security tests.
- **API Documentation**: Interactive Swagger OpenAPI 3.0 documentation available directly in the browser.
- **Minimal Functional Frontend**: React + Vite UI demonstrating the real booking workflow without mock or fake data.

---

## 🏗️ System Architecture & Database Schema

```
[Users] (1) ──── (N) [Bookings] (1) ──── (N) [BookingSeats] (N) ──── (1) [Seats] (N) ──── (1) [Screens] (N) ──── (1) [Theatres]
                           |
                           └─────── (N) ──── [Shows] (N) ──── (1) [Movies]
```

### Table Overview

| Table | Purpose | Concurrency / Integrity Role |
|---|---|---|
| `users` | Accounts, BCrypt passwords, Roles | Unique username & email constraints |
| `movies` | Movie metadata, genre, duration | Soft delete (`is_active = false`) ensures past bookings stay valid |
| `theatres` | Physical cinema venue details | Venue location and screen grouping |
| `screens` | Cinema halls within a theatre | Screen capacity and layout parent |
| `seats` | Physical seats (`A1`, `B5`, etc.) | Unique `(screen_id, seat_number)` prevents duplicate seats |
| `shows` | Scheduled movie timings & tiered prices | Links movie, screen, showtime, and prices (`REGULAR`, `PREMIUM`, `VIP`) |
| `bookings` | Customer order records | Confirmed vs Cancelled state tracking |
| `booking_seats` | Seats booked per show | **`UNIQUE(seat_id, show_id)` guarantees no two bookings can ever reserve the same seat for the same show** |

---

## 🔒 Concurrency Strategy: Preventing Double Booking

### The Problem
When two users hit "Book" at the exact same millisecond for Seat `A1` on Show `10`:
Without protection, both requests check availability simultaneously, see that `A1` is free, and both insert a booking record.

### The Solution: Two-Layer Defense
1. **Pessimistic Locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`)**:
   `SeatRepository.findByIdInWithPessimisticLock()` issues a SQL `SELECT ... FOR UPDATE` query. MySQL locks the requested seat rows until the transaction commits. The second transaction must wait until the first transaction finishes. When it unblocks, it sees that the seat is already booked and throws `SeatAlreadyBookedException` (HTTP 409 Conflict).
2. **Database Hard Constraint (`UNIQUE(seat_id, show_id)`)**:
   Even if an unexpected bug bypassed service layer checks, MySQL rejects duplicate insertions at the database engine level. Spring catches `DataIntegrityViolationException` and responds with HTTP 409 Conflict.

---

## 🚀 Getting Started (Windows Step-by-Step)

### 1. Database Setup
Open your MySQL client (Command Prompt or MySQL Workbench) and run:

```sql
CREATE DATABASE movie_booking_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'movieapp'@'localhost' IDENTIFIED BY 'MovieApp@2025';
GRANT ALL PRIVILEGES ON movie_booking_db.* TO 'movieapp'@'localhost';
FLUSH PRIVILEGES;
```

*(Note: Pre-configured credentials match `backend/src/main/resources/application-local.properties`)*

---

### 2. Backend Execution

Open Command Prompt or PowerShell in the `backend/` directory:

```cmd
cd "c:\Users\pratik\Movie ticket booking Project\backend"

# Run all automated tests (JUnit 5 + Mockito + Concurrency test)
mvn clean test

# Run the Spring Boot Application
mvn spring-boot:run
```

Once started, the backend automatically seeds sample data on first run:
- **Admin**: `admin` / `Admin@123`
- **User**: `john_doe` / `User@123`
- **Swagger Documentation**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **API Base URL**: `http://localhost:8080/api`

---

### 3. Frontend Execution

In a separate terminal, run:

```cmd
cd "c:\Users\pratik\Movie ticket booking Project\frontend"

# Install dependencies (First time only)
npm install

# Start local React development server
npm run dev
```

Open your browser at: **[http://localhost:3000](http://localhost:3000)**

---

## 🧪 Automated Testing Details

| Test Class | Focus Area | Technology |
|---|---|---|
| `BookingServiceTest` | Booking success, unavailable seats, cancellation > 1 hour rule, unauthorized access | JUnit 5, Mockito |
| `BookingConcurrencyTest` | Simulates multi-threaded race condition on the same seat with `CountDownLatch` and `ExecutorService` | JUnit 5, Concurrency API |
| `AuthServiceTest` | User registration, password hashing verification, duplicate rejection, JWT issuance | JUnit 5, Mockito |
| `AuthControllerTest` | Jakarta Bean Validation, 201 Created, 400 Bad Request | Spring Boot `@WebMvcTest`, MockMvc |
| `AdminSecurityTest` | Role-Based Access Control (`USER` gets 403 Forbidden, `ADMIN` gets 201 Created) | `@WebMvcTest`, `@WithMockUser` |

To run only the concurrency test:
```cmd
mvn test -Dtest=BookingConcurrencyTest
```

---

## 📮 Postman Manual Testing

1. Open Postman.
2. Click **Import** and select `postman/MovieBooking.postman_collection.json`.
3. The collection includes pre-configured environment variables and test scripts that automatically save your JWT token upon login.
4. Execute requests in order:
   - `1. Authentication` → `Login User` (stores `userToken`)
   - `2. Public Discovery` → `Search Shows by City / Movie`
   - `2. Public Discovery` → `Get Live Seat Availability for Show`
   - `3. Booking Flow (User)` → `Book Seats for Show`
   - `3. Booking Flow (User)` → `Cancel Booking`

---

## 💡 Top Interview Questions & How to Answer Them

### Q1: How do you prevent double-booking in a high-concurrency ticket system?
> **Answer**: We use a two-layer strategy. First, in the service layer, we acquire a pessimistic write lock using JPA's `LockModeType.PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`), which serializes competing transactions on the requested seats. Second, at the database layer, the `booking_seats` table enforces a composite `UNIQUE(seat_id, show_id)` constraint, making it physically impossible for MySQL to record two active bookings for the same seat in the same show.

### Q2: Why did you use DTOs instead of exposing JPA Entities?
> **Answer**: Exposing entities leaks internal database schema, can cause Jackson circular reference serialization errors with bidirectional relationships, exposes sensitive fields (like password hashes), and prevents tailored validation annotations on incoming requests.

### Q3: Why did you use BigDecimal for ticket pricing?
> **Answer**: `float` and `double` use binary floating-point representation (IEEE 754) which causes rounding errors in monetary arithmetic (e.g. `0.1 + 0.2 = 0.30000000000000004`). `BigDecimal` provides exact precision required for currency and financial calculations.

### Q4: How is cancellation handled safely?
> **Answer**: A booking can only be cancelled by its owner (or an admin) and only if the showtime is at least 1 hour in the future. Upon cancellation, the booking status transitions to `CANCELLED` and its records in `booking_seats` are deleted, immediately freeing those seats for other users to book without race conditions.

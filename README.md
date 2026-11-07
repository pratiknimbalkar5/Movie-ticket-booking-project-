# 🎬 CinemaPulse — Full-Stack Movie Ticket Booking System

[![Java](https://img.shields.io/badge/Java-21%20%7C%2025-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-5-646CFF?style=for-the-badge&logo=vite&logoColor=white)](https://vitejs.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)

A production-ready, full-stack **Movie Ticket Booking & Cinema Discovery Platform** built with **Java Spring Boot**, **React**, and **MySQL**. Featuring real-time movie synchronization via **The Movie Database (TMDB)** for Indian theatrical releases, GPS-based nearby cinema discovery, multi-tier interactive seat reservation, robust **two-layer concurrency control** against double-booking, and self-service booking cancellation with instant seat release.

---

## 📑 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [System Architecture](#-system-architecture)
- [Database Schema & ER Design](#-database-schema--er-design)
- [Concurrency Defense Strategy](#-concurrency-defense-strategy)
- [Technology Stack](#-technology-stack)
- [REST API Reference](#-rest-api-reference)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Environment Variables](#environment-variables)
  - [Backend Setup](#backend-setup)
  - [Frontend Setup](#frontend-setup)
- [Testing & Quality Assurance](#-testing--quality-assurance)
- [Project Structure](#-project-structure)
- [License](#-license)

---

## 🌟 Overview

**CinemaPulse** bridges the gap between external entertainment APIs and high-integrity booking transactions:
- It aggregates current in-cinema releases, recent theatrical titles, and upcoming movies directly from TMDB's Indian release feeds.
- It calculates genuine Haversine distances to discover multiplexes and local cinemas within user-selected radii (10 km to 50 km) or across 36+ major Indian cities (including Mumbai, Delhi NCR, Bengaluru, Hyderabad, Pune, Indore, Rau, and more).
- It handles seat reservations with zero tolerance for race conditions through pessimistic database locking and relational unique constraints.

---

## ✨ Key Features

### 1. Real-Time Movie Intelligence (TMDB Integration)
- **Now Showing**: Displays movies currently playing with verified, active, bookable shows in the selected location.
- **Latest Releases**: Strictly filters Indian theatrical titles released within a configurable rolling window (default 30 days), sorted newest first.
- **Coming Soon**: Strictly displays future Indian release dates with pre-booking alert notifications.
- **Star Cast & Media**: Live cast photos, synopses, genres, runtimes, and high-resolution posters mapped by verified TMDB IDs with fallback SVG graphics.

### 2. Location-Based Cinema Discovery
- **GPS Auto-Detect**: Direct browser geolocation with locality reverse-geocoding.
- **City & Locality Selector**: Searchable catalog of 36+ major Indian cities and suburban entertainment hubs (e.g., Rau, Mhow, Thane, Navi Mumbai).
- **Haversine Distance Sorting**: Dynamic distance calculation from user coordinates, sorted nearest-first within configurable search radii (10 km, 20 km, 30 km, 50 km).
- **Hybrid Provider Architecture**: Integrates local multiplex databases with Open Cinema Platform and Google Places fallbacks.

### 3. Realistic Showtime Scheduling & Tiered Pricing
- **Staggered Showtimes**: Cinema-specific start offsets (e.g., 09:30 AM, 12:45 PM, 04:15 PM, 07:30 PM, 10:45 PM) avoiding cloned show timings.
- **Tiered Seat Pricing**: Transparent seat tiers per screen:
  - **Regular (Rows A–D)**: Base multiplex rate
  - **Premium (Rows E–G)**: Enhanced audio-visual tier
  - **VIP Recliner (Rows H–J)**: Luxury reclining seats

### 4. Interactive Seat Layout & Concurrency Protection
- Visual 86-seat auditorium grid color-coded by tier (Available, Selected, Booked).
- Live seat availability check prior to confirmation.
- **Two-Layer Concurrency Defense**: Eliminates double-booking even under aggressive multi-threaded checkout requests.

### 5. Booking Management & Instant Cancellation
- Immediate booking confirmation with unique reference code, seat summaries, and show details.
- **Self-Service Cancellation**: Allows users to cancel tickets up to 1 hour before showtime.
- **Atomic Inventory Release**: Seats are immediately unlocked and restored to the available pool upon cancellation.
- User Bookings Wallet (`/bookings`) displaying upcoming and cancelled ticket histories.

### 6. Security & Authentication
- Stateless **JWT (JSON Web Token)** bearer authentication via Spring Security 6.
- **BCrypt** password hashing (10 rounds) with zero plaintext credential persistence.
- Role-Based Access Control (`ROLE_USER`, `ROLE_ADMIN`).

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      React 18 Frontend                      │
│     (Vite + React Router v7 + Axios + CSS Design System)     │
└──────────────────────────────┬──────────────────────────────┘
                               │  HTTPS / REST JSON
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 Spring Boot 3 Backend API                   │
│                                                             │
│   ┌────────────────────┐          ┌──────────────────────┐  │
│   │ Security & Auth    │          │ Global Exceptions    │  │
│   │ (JWT + BCrypt)     │          │ (@ControllerAdvice)  │  │
│   └─────────┬──────────┘          └──────────┬───────────┘  │
│             │                                │              │
│   ┌─────────▼────────────────────────────────▼───────────┐  │
│   │                    REST Controllers                  │  │
│   │  (Movie, Cinema, Show, Booking, Auth, Theatre)       │  │
│   └─────────────────────────┬────────────────────────────┘  │
│                             │                               │
│   ┌─────────────────────────▼────────────────────────────┐  │
│   │                    Service Layer                     │  │
│   │  • TmdbMovieService (External HTTP Caching)          │  │
│   │  • ShowService (Haversine Distance + Schedules)      │  │
│   │  • BookingService (Pessimistic Lock Transactions)    │  │
│   │  • GooglePlacesService / OpenCinemaService           │  │
│   └─────────────────────────┬────────────────────────────┘  │
│                             │                               │
│   ┌─────────────────────────▼────────────────────────────┐  │
│   │                 Spring Data JPA Repositories         │  │
│   └─────────────────────────┬────────────────────────────┘  │
└──────────────────────────────┼──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                     MySQL 8.0 / H2 Engine                   │
│   (Pessimistic Write Locks + Composite Unique Constraints)   │
└─────────────────────────────────────────────────────────────┘
```

---

## 🗄️ Database Schema & ER Design

```
[Users] (1) ──────── (N) [Bookings] (1) ──────── (N) [BookingSeats] (N) ──── (1) [Seats]
                             │                                                   │
                             │ (N)                                               │ (N)
                             ▼                                                   ▼
                          [Shows] (N) ─────── (1) [Screens] (N) ───────── (1) [Theatres]
                             │
                             │ (N)
                             ▼
                          [Movies]
```

### Table Definitions

| Table | Responsibility | Key Constraints & Indexes |
|---|---|---|
| `users` | Accounts, hashed credentials, roles | `UNIQUE(username)`, `UNIQUE(email)` |
| `theatres` | Physical cinema complexes with GPS coordinates | `latitude`, `longitude`, `city` |
| `screens` | Auditoriums within a theatre (IMAX, Atmos) | `FK(theatre_id)` |
| `seats` | Numbered seats with tier classifications | `UNIQUE(screen_id, seat_number)` |
| `movies` | Theatrical metadata, TMDB ID, release date | `tmdb_id`, `is_active`, `is_upcoming` |
| `shows` | Scheduled showtimes with screen & movie | `FK(movie_id)`, `FK(screen_id)`, `show_time` |
| `bookings` | Customer ticket orders | `UNIQUE(booking_reference)`, `status` |
| `booking_seats` | Seat allocations per show | **`UNIQUE(seat_id, show_id)`** |

---

## 🔒 Concurrency Defense Strategy

Preventing simultaneous double-booking of high-demand seats is handled across **two separate enforcement layers**:

```
Request 1 (User A) ───► [ SELECT ... FOR UPDATE ] ───► Acquired Lock ───► Seat Reserved ──► COMMIT
                                                                                               │
Request 2 (User B) ───► [ SELECT ... FOR UPDATE ] ───► WAITING... ─────────────────────────────┘
                                                             │
                                                             ▼
                                                    Lock Released to User B
                                                             │
                                                             ▼
                                              Availability Check Fails:
                                          Throws SeatAlreadyBookedException
                                                (HTTP 409 Conflict)
```

1. **Application Layer (Pessimistic Write Lock)**:
   - When reserving seats, `SeatRepository.findByIdInWithPessimisticLock()` issues a SQL query with `FOR UPDATE`.
   - The database locks the matched seat rows for the duration of the `@Transactional` boundary.
   - Competing transactions attempting to book the same seats block until the first transaction commits or rolls back, after which they re-check availability and are rejected with `SeatAlreadyBookedException` (HTTP 409 Conflict).

2. **Storage Engine Layer (Composite Unique Constraint)**:
   - The `booking_seats` table enforces a hard composite unique constraint: `UNIQUE(seat_id, show_id)`.
   - Even if concurrent code were to bypass application-level validation, the storage engine guarantees that duplicate seat-show records cannot physically be inserted.

---

## 💻 Technology Stack

### Backend
- **Language**: Java 21 / 25
- **Framework**: Spring Boot 3.4.5
- **Persistence**: Spring Data JPA, Hibernate 6
- **Security**: Spring Security 6, JJWT (io.jsonwebtoken 0.12.6)
- **HTTP Client**: Spring 6 `RestClient` with JDK 11+ HttpClient
- **API Documentation**: SpringDoc OpenAPI / Swagger 3.0
- **Build Tool**: Apache Maven (Wrapper included)

### Frontend
- **Framework**: React 18 (SPA)
- **Routing**: React Router DOM v7
- **Build Tool**: Vite 5
- **HTTP Client**: Axios with Bearer token interceptors
- **Styling**: Pure CSS Design System (dark multiplex aesthetic, mobile responsive)

### Database & Testing
- **Production DB**: MySQL 8.0+
- **In-Memory / Test DB**: H2 Database (MySQL compatibility mode)
- **Test Framework**: JUnit 5, Mockito, Spring Boot Test, MockMvc

---

## 📡 REST API Reference

Interactive Swagger documentation is accessible at `http://localhost:8080/swagger-ui.html` when running the backend.

### Authentication
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/auth/register` | Register a new user account | None |
| `POST` | `/api/auth/login` | Authenticate and obtain JWT Bearer token | None |

### Movies & Discovery
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/movies/now-playing` | Active movies currently playing in cinemas | None |
| `GET` | `/api/movies/latest-releases`| Recent Indian theatrical releases (past 30 days) | None |
| `GET` | `/api/movies/upcoming` | Strictly future upcoming releases | None |
| `GET` | `/api/movies/popular` | Popular theatrical titles | None |
| `GET` | `/api/movies/trending` | Trending movies (day / week window) | None |
| `GET` | `/api/movies/search` | Search movies by title keyword | None |
| `GET` | `/api/movies/{id}` | Movie details, runtime, genre, rating | None |
| `GET` | `/api/movies/{id}/cast` | Star cast list with profile photos | None |

### Theatres & Showtimes
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/shows/nearby` | Theatres & shows filtered by city/GPS/date | None |
| `GET` | `/api/shows/scan-nearby` | Scan cinemas within GPS radius (sorted nearest) | None |
| `GET` | `/api/shows/{id}` | Specific show details & pricing tiers | None |
| `GET` | `/api/shows/{id}/seats` | Real-time seat map and availability | None |
| `GET` | `/api/theatres/cities` | List of supported Indian cities | None |

### Bookings (User Protected)
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/bookings` | Reserve seats and generate booking | `USER` / `ADMIN` |
| `GET` | `/api/bookings/{id}` | View booking confirmation details | `USER` / `ADMIN` |
| `GET` | `/api/bookings/my-bookings` | View logged-in user's ticket wallet | `USER` / `ADMIN` |
| `POST` | `/api/bookings/{id}/cancel` | Cancel booking and release seats | `USER` / `ADMIN` |

---

## 🚀 Getting Started

### Prerequisites
- **Java Development Kit (JDK)**: Version 17, 21, or 25
- **Node.js**: Version 18+ and `npm`
- **MySQL Server** (Optional for local testing; H2 in-memory is enabled by default for zero-config startup)

---

### Environment Variables
Copy `.env.example` to `.env` in the project root:

```bash
cp .env.example .env
```

Configure your API keys:

```ini
# TMDB API Read Access Token (Free from https://www.themoviedb.org/settings/api)
TMDB_ACCESS_TOKEN=your_tmdb_read_access_token_here

# Optional: Google Places API Key (For live place search)
GOOGLE_PLACES_API_KEY=your_google_places_key_here

# Optional: Open Cinema Platform Key
OPEN_CINEMA_API_KEY=your_open_cinema_key_here

# JWT Secret Key (Minimum 32 characters)
JWT_SECRET=super_secret_jwt_key_that_is_at_least_32_characters_long!
```

---

### Backend Setup

1. Navigate to the `backend/` directory:
   ```bash
   cd backend
   ```

2. Run the test suite:
   ```bash
   ./mvnw clean test
   ```

3. Start the Spring Boot backend server:
   ```bash
   ./mvnw spring-boot:run
   ```
   *The backend starts at `http://localhost:8080`.*

#### Pre-Seeded Default Accounts:
- **Regular User**: Username: `john_doe` | Password: `User@123`
- **Administrator**: Username: `admin` | Password: `Admin@123`

---

### Frontend Setup

1. In a separate terminal, navigate to the `frontend/` directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Launch the Vite development server:
   ```bash
   npm run dev
   ```
   *The frontend starts at `http://localhost:3000`.*

---

## 🧪 Testing & Quality Assurance

The backend includes a comprehensive suite of automated unit, integration, and security tests:

```bash
cd backend
./mvnw test
```

### Key Test Suites:
- **`BookingConcurrencyTest`**: Simulates concurrent threads contending for the same seat using `CountDownLatch` and `ExecutorService` to verify that exactly one reservation succeeds and all others are safely rejected.
- **`BookingServiceTest`**: Validates happy-path booking, seat availability checks, cancellation window rules (> 1 hour before showtime), and access control.
- **`TmdbMovieServiceTest`**: Tests date-cutoff algorithms, region filtering, and fallback mechanisms when external APIs are unreachable.
- **`AdminSecurityTest`**: Ensures role-based authorization constraints are enforced across admin and user endpoints.

---

## 📁 Project Structure

```
Movie ticket booking Project/
├── .env.example                     # Environment template (safe for version control)
├── .gitignore                       # Git ignore configuration
├── README.md                        # Project documentation
├── backend/                         # Spring Boot application
│   ├── pom.xml                      # Maven configuration
│   ├── mvnw.cmd / mvnw              # Maven wrapper executables
│   └── src/
│       ├── main/
│       │   ├── java/com/moviebooking/
│       │   │   ├── config/          # Security, CORS, DataInitializer, Swagger
│       │   │   ├── controller/      # REST API Endpoints
│       │   │   ├── dto/             # Request & Response Data Transfer Objects
│       │   │   ├── entity/          # JPA Domain Entities
│       │   │   ├── exception/       # Global Exception Handler & Custom Errors
│       │   │   ├── repository/      # Spring Data JPA Repositories
│       │   │   ├── security/        # JWT Authentication Filter & Token Provider
│       │   │   ├── service/         # Business Logic & External Integrations
│       │   │   └── util/            # Pricing, Scheduling & Coordinate Helpers
│       │   └── resources/
│       │       ├── application.properties
│       │       ├── application-mysql.properties
│       │       └── application-test.properties
│       └── test/                    # Unit, Concurrency & Security Tests
├── frontend/                        # React + Vite application
│   ├── package.json                 # Dependencies & scripts
│   ├── vite.config.js               # Vite bundler configuration
│   └── src/
│       ├── components/              # Navbar, CitySelectorModal, CinemaTracker
│       ├── context/                 # AuthContext (JWT management)
│       ├── pages/                   # HomePage, Search, Movies, MovieDetail,
│       │                            # SeatSelection, BookingConfirmation, Bookings
│       ├── services/                # Axios API client & Location service
│       └── utils/                   # Poster fallbacks & helpers
└── postman/                         # Postman API Collection
    └── MovieBooking.postman_collection.json
```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE). Feel free to use, modify, and distribute this codebase for educational and personal projects.
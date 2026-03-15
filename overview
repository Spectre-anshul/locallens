# LocalLens — System Architecture Overview

## Platform Vision

**LocalLens** is a two-sided hyperlocal travel marketplace that replaces the fragmented stack of Google Maps + Booking.com + TripAdvisor + Notes + Flight apps with a single AI-powered dashboard. It surfaces hyperlocal experiences from verified local creators and dynamically replans itineraries in real-time based on weather, traffic, and venue status.

---

## High-Level Architecture Diagram

```mermaid
graph TB
    subgraph "Client Layer"
        RWA["React 18 SPA<br/>(Bootstrap 5 + Deck.gl)"]
        PWA["PWA / Mobile"]
    end

    subgraph "API Gateway & Auth"
        GW["Spring Cloud Gateway"]
        AUTH["AuthService<br/>(JWT + OAuth2)"]
    end

    subgraph "Core Services (Spring Boot 3.x — Modular Monolith)"
        TRIP["TripService"]
        AI["ItineraryAIService"]
        REPLAN["ReplanningService"]
        MKT["MarketplaceService"]
        PAY["PaymentService"]
        MAP["MapService"]
        EVT["EventAggregatorService"]
        NOTIF["NotificationService"]
        ANALYTICS["AnalyticsService"]
    end

    subgraph "Real-Time Layer"
        WS["WebSocket Broker<br/>(STOMP/SockJS)"]
        FCM["Firebase Cloud Messaging"]
    end

    subgraph "Data Layer"
        MONGO[("MongoDB Atlas<br/>(Primary Store)")]
        REDIS[("Redis<br/>(Cache + Pub/Sub)")]
    end

    subgraph "External APIs"
        CLAUDE["Claude API<br/>(Anthropic)"]
        OWM["OpenWeatherMap"]
        TOMTOM["TomTom Traffic"]
        GPLACES["Google Places"]
        EBRITE["Eventbrite API"]
        STRIPE["Stripe Connect"]
        SGRID["SendGrid"]
        MAPBOX["Mapbox / MapLibre Tiles"]
    end

    RWA -->|HTTPS + WS| GW
    PWA -->|HTTPS + WS| GW
    GW --> AUTH
    GW --> TRIP
    GW --> AI
    GW --> MKT
    GW --> MAP
    GW --> EVT
    GW --> NOTIF

    TRIP --> MONGO
    AI --> CLAUDE
    AI --> MONGO
    REPLAN --> OWM
    REPLAN --> TOMTOM
    REPLAN --> GPLACES
    REPLAN --> AI
    REPLAN --> WS
    MKT --> MONGO
    MKT --> PAY
    PAY --> STRIPE
    MAP --> MONGO
    MAP --> MAPBOX
    MAP --> TOMTOM
    EVT --> EBRITE
    EVT --> MONGO
    NOTIF --> WS
    NOTIF --> SGRID
    NOTIF --> FCM
    ANALYTICS --> MONGO
    ANALYTICS --> REDIS

    TRIP --> REDIS
    MAP --> REDIS
```

---

## Service Communication Pattern

```mermaid
sequenceDiagram
    participant U as React Client
    participant GW as API Gateway
    participant TS as TripService
    participant AIS as ItineraryAIService
    participant MS as MarketplaceService
    participant Claude as Claude API
    participant WS as WebSocket Broker
    participant DB as MongoDB

    U->>GW: POST /api/trips (trip form data)
    GW->>TS: Create trip document
    TS->>DB: Insert trip
    TS->>AIS: Generate itinerary request
    AIS->>MS: Fetch matching hyperlocal experiences
    MS->>DB: Query experiences by destination/dates
    MS-->>AIS: Ranked experience list
    AIS->>Claude: Prompt with profile + experiences + events
    Claude-->>AIS: Structured itinerary JSON
    AIS->>DB: Store parsed itinerary
    AIS->>WS: Publish ITINERARY_READY event
    WS-->>U: Push itinerary to client
    U->>U: Render full dashboard
```

---

## Dynamic Replanning Flow

```mermaid
sequenceDiagram
    participant SCHED as WeatherScheduler
    participant OWM as OpenWeatherMap API
    participant RS as ReplanningService
    participant DB as MongoDB
    participant AIS as ItineraryAIService
    participant Claude as Claude API
    participant WS as WebSocket Broker
    participant U as React Client

    SCHED->>OWM: Poll weather for active trip destinations
    OWM-->>SCHED: Storm alert for Kyoto
    SCHED->>RS: Trigger replan (tripId, reason=WEATHER)
    RS->>DB: Fetch affected itinerary days
    RS->>AIS: Request replan with weather context
    AIS->>Claude: Replan prompt (current plan + weather + alternatives)
    Claude-->>AIS: Updated day plan JSON
    AIS-->>RS: Parsed new itinerary slots
    RS->>RS: Compute diff (old vs new)
    RS->>DB: Store replan log with reason code
    RS->>DB: Update itinerary
    RS->>WS: Push REPLAN_TRIGGERED + diff payload
    WS-->>U: Toast: "Storm Alert — Day 3 updated"
    U->>U: Show diff modal (old vs new side-by-side)
```

---

## Deployment Architecture

```mermaid
graph LR
    subgraph "GitHub"
        REPO["Monorepo"]
        GHA["GitHub Actions CI/CD"]
    end

    subgraph "Container Registry"
        GHCR["GitHub Container Registry"]
    end

    subgraph "Production (Railway / Render)"
        FE["Frontend Container<br/>(nginx + React build)"]
        BE["Backend Container<br/>(Spring Boot JAR)"]
        REDIS_P["Redis Container"]
    end

    subgraph "Managed Services"
        ATLAS["MongoDB Atlas"]
        CF["Cloudflare CDN"]
    end

    REPO --> GHA
    GHA --> GHCR
    GHCR --> FE
    GHCR --> BE
    GHCR --> REDIS_P
    FE --> CF
    BE --> ATLAS
    BE --> REDIS_P
```

---

## Technology Decision Matrix

| Layer | Choice | Rationale |
|-------|--------|-----------|
| **Frontend** | React 18 + Bootstrap 5 | Component ecosystem + responsive grid, SSR-ready with Next.js migration path |
| **State** | Zustand + React Query | Lightweight client state + automatic server-state caching/invalidation |
| **Maps** | Deck.gl + MapLibre GL JS | Open-source 3D visualization, no Mapbox vendor lock-in, WebGL-powered |
| **Backend** | Spring Boot 3.x (Java 17) | Enterprise-grade, excellent MongoDB support, built-in WebSocket/STOMP |
| **Database** | MongoDB Atlas | Flexible schemas, GeoJSON native, GridFS for documents, TTL indexes |
| **Cache** | Redis | Sub-ms reads for heatmap data, session store, pub/sub for cross-service events |
| **AI** | Claude API (Anthropic) | Superior structured output, large context window for complex itineraries |
| **Payments** | Stripe Connect | Two-sided marketplace payouts, managed KYC, configurable commission |
| **Email** | SendGrid | Transactional email at scale, template engine, delivery tracking |
| **Real-time** | STOMP/SockJS + Firebase | WebSocket for web, FCM for mobile push, fallback to long-polling |
| **CI/CD** | GitHub Actions + Docker | Industry standard, container-native, free tier for open-source |

---

## Security Architecture

| Concern | Implementation |
|---------|----------------|
| **Authentication** | Spring Security + JWT (access 15min / refresh 7d) + OAuth2 (Google, Apple) |
| **Authorization** | Role-based: `TRAVELER`, `CREATOR`, `ADMIN` with method-level `@PreAuthorize` |
| **API Security** | Rate limiting (Bucket4j), CORS whitelist, CSRF for non-API routes |
| **Data Encryption** | TLS 1.3 in transit, MongoDB field-level encryption for PII |
| **Secrets** | Spring Cloud Vault or environment variables (never in code) |
| **Input Validation** | Bean Validation (JSR-380) on all DTOs, sanitized before Claude prompts |
| **File Upload** | MongoDB GridFS with virus scanning, max 10MB, allowed MIME types only |

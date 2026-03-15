# LocalLens — End-to-End Flows & Project Structure

---

## End-to-End Flow: Scenario A — New Trip Creation

```mermaid
sequenceDiagram
    participant U as User (React)
    participant TW as TripWizard
    participant API as Axios Client
    participant GW as Backend (Gateway)
    participant AS as AuthService
    participant TS as TripService
    participant AIS as ItineraryAIService
    participant MS as MarketplaceService
    participant ES as EventAggregatorService
    participant Claude as Claude API
    participant DB as MongoDB
    participant WS as WebSocket Broker
    participant RQ as React Query

    Note over U,TW: Step 1: User fills trip wizard
    U->>TW: Fill 4-step form (destination, dates, budget, interests, style, group)
    TW->>TW: Client-side validation
    TW->>API: POST /api/trips (trip payload)
    API->>GW: Forward with JWT
    GW->>AS: Validate JWT token
    AS-->>GW: User authenticated (TRAVELER role)
    GW->>TS: createTrip(userId, request)
    TS->>DB: Insert trip document (status: DRAFT)
    TS-->>GW: Return trip with ID
    GW-->>API: 201 Created { tripId, status: DRAFT }
    API-->>TW: Trip created

    Note over TW,AIS: Step 2: Trigger itinerary generation
    TW->>API: POST /api/itinerary/generate { tripId }
    API->>GW: Forward
    GW->>AIS: generateItinerary(tripId)
    AIS-->>GW: 202 Accepted { status: GENERATING }
    GW-->>API: 202 Accepted
    API-->>TW: Show "Generating your itinerary..." spinner

    Note over AIS,Claude: Step 3: AI itinerary generation (async)
    AIS->>DB: Fetch trip document
    AIS->>MS: ExperienceMatchingService.match(profile, destination, dates)
    MS->>DB: Query experiences (city, dates, hyperlocal=true)
    MS->>MS: Score & rank by interest, budget, style, rating
    MS-->>AIS: Top 15 ranked experiences

    AIS->>ES: getEventsForItinerary(city, startDate, endDate)
    ES->>DB: Query events by city + date range
    ES-->>AIS: Matching events list

    AIS->>AIS: buildSystemPrompt(trip, experiences, events)
    AIS->>AIS: buildGenerationPrompt(request)
    AIS->>Claude: messages.create(system + user prompt)
    Note right of Claude: ~10-20 seconds
    Claude-->>AIS: Structured JSON response

    AIS->>AIS: parseItineraryResponse(JSON, tripId)
    AIS->>DB: Insert itinerary_days documents (7 days)
    AIS->>DB: Update trip (status: PLANNED, itineraryVersion: 1)

    Note over AIS,U: Step 4: Push result to client
    AIS->>WS: Publish to /topic/itinerary/{tripId}/replan
    Note right of WS: Payload: { type: ITINERARY_READY, tripId }
    WS-->>U: WebSocket message received

    Note over U,RQ: Step 5: Render dashboard
    U->>U: useReplan hook receives ITINERARY_READY
    U->>RQ: Invalidate ['itinerary', tripId] query
    RQ->>API: GET /api/itinerary/{tripId}/days
    API->>GW: Forward
    GW->>AIS: getItineraryDays(tripId)
    AIS->>DB: Fetch all itinerary_days for tripId
    AIS-->>GW: 7 day documents with slots
    GW-->>API: ItineraryDayResponse[]
    API-->>RQ: Cache result
    RQ-->>U: Data available

    U->>U: Navigate to DashboardPage
    U->>U: Render TripOverviewCard + ItineraryTimeline + Map
    Note over U: Dashboard shows full itinerary with\n[LOCAL PICK] badges on hyperlocal activities
```

### Technical Details — Scenario A

| Step | Latency | Notes |
|------|---------|-------|
| Trip creation (POST) | ~100ms | Simple MongoDB insert |
| Generate trigger (POST) | ~50ms | Returns 202 immediately, spawns async task |
| Experience matching | ~200ms | MongoDB geo-query + in-memory scoring |
| Event fetching | ~100ms | MongoDB query, Redis-cached if available |
| Claude API call | 10–20s | Main bottleneck, depends on trip length |
| Parse + store | ~300ms | JSON parse + 7 MongoDB inserts |
| WebSocket push | ~10ms | STOMP message to subscribed client |
| Dashboard render | ~500ms | React Query fetch + Deck.gl map init |
| **Total** | **~12–22s** | User sees spinner, then full dashboard |

---

## End-to-End Flow: Scenario B — Dynamic Replan

```mermaid
sequenceDiagram
    participant SCHED as ReplanningScheduler
    participant OWM as OpenWeatherMap
    participant RS as ReplanningService
    participant DB as MongoDB
    participant AIS as ItineraryAIService
    participant MS as MarketplaceService
    participant Claude as Claude API
    participant WS as WebSocket Broker
    participant U as React Client

    Note over SCHED,OWM: Step 1: Scheduled weather check (every 30 min)
    SCHED->>SCHED: @Scheduled(fixedRate=1800000)
    SCHED->>DB: Query trips WHERE status=ACTIVE AND startDate<=today AND endDate>=today
    DB-->>SCHED: Active trip list (e.g., 25 trips)

    loop For each active trip destination (deduplicated)
        SCHED->>OWM: GET /data/2.5/forecast?lat={lat}&lon={lng}
        OWM-->>SCHED: Weather forecast (5-day, 3-hour intervals)
        SCHED->>SCHED: isAdverseWeather(forecast)?
    end

    Note over SCHED,RS: Step 2: Trigger replan for affected trips
    SCHED->>RS: triggerReplan(tripId="trip-42", dayNumber=3, trigger={WEATHER, "Heavy rain 2PM-6PM"})

    RS->>DB: Fetch itinerary_days for trip-42, day 3
    RS->>RS: Identify affected slots (outdoor activities during 2PM-6PM)
    Note right of RS: Slots: "Bamboo Grove Walk" (14:00),\n"Monkey Park" (14:30)

    Note over RS,Claude: Step 3: Claude replan
    RS->>MS: Get available alternative experiences (indoor, same area)
    MS->>DB: Query indoor experiences in Kyoto, available on this date
    MS-->>RS: Alternative list

    RS->>AIS: replanDay(tripId, dayNumber=3, context)
    AIS->>AIS: buildReplanPrompt(currentDay, weatherContext, alternatives)
    AIS->>Claude: messages.create(replan prompt)
    Note right of Claude: ~5-10 seconds
    Claude-->>AIS: Updated day JSON + replan summary

    AIS->>AIS: parseResponse → new ItineraryDayDocument
    AIS-->>RS: ReplanResult (updated day + summary)

    Note over RS,DB: Step 4: Diff, log, store
    RS->>RS: computeDiff(oldDay, newDay)
    Note right of RS: Diff: removed "Bamboo Grove",\nadded "Matcha Ceremony (indoor)"
    RS->>DB: Insert replan_log { tripId, reason: WEATHER, diff, tokens, timing }
    RS->>DB: Update itinerary_day (version: 2, new slots)
    RS->>DB: Update trip.itineraryVersion = 2

    Note over RS,U: Step 5: Push to client
    RS->>WS: Send to /topic/itinerary/trip-42/replan
    Note right of WS: Payload: {type: REPLAN_TRIGGERED, diff, reason}

    WS-->>U: Message received by useReplan hook
    U->>U: Show ReplanToast: "⛈️ Storm Alert: Day 3 updated"
    U->>U: Open ReplanModal with side-by-side diff

    Note over U: User reviews changes
    alt User Accepts
        U->>U: Click "Accept Changes"
        U->>U: POST /api/itinerary/trip-42/replan/accept
        U->>U: React Query invalidates & refetches itinerary
        U->>U: Dashboard shows updated Day 3
    else User Rejects
        U->>U: Click "Keep Original"
        U->>U: POST /api/itinerary/trip-42/replan/reject
        U->>U: UI restores original plan
    end
```

---

## Complete Project Structure

```
locallens/                                    # Monorepo root
├── .github/
│   └── workflows/
│       └── ci-cd.yml                         # GitHub Actions pipeline
├── docker/
│   └── mongo-init.js                         # MongoDB initialization script
├── docker-compose.yml                        # Local development
├── docker-compose.prod.yml                   # Production overrides
├── .env.example                              # Environment variables template
├── README.md                                 # Project overview
│
├── locallens-backend/                        # Spring Boot Application
│   ├── Dockerfile
│   ├── build.gradle                          # Gradle build config
│   ├── settings.gradle
│   ├── gradlew / gradlew.bat
│   ├── gradle/
│   │   └── wrapper/
│   └── src/
│       ├── main/
│       │   ├── java/com/locallens/
│       │   │   ├── LocalLensApplication.java
│       │   │   │
│       │   │   ├── config/
│       │   │   │   ├── SecurityConfig.java
│       │   │   │   ├── WebSocketConfig.java
│       │   │   │   ├── MongoConfig.java
│       │   │   │   ├── RedisConfig.java
│       │   │   │   ├── StripeConfig.java
│       │   │   │   ├── CorsConfig.java
│       │   │   │   └── SwaggerConfig.java
│       │   │   │
│       │   │   ├── common/
│       │   │   │   ├── dto/
│       │   │   │   │   ├── ApiResponse.java
│       │   │   │   │   ├── PageResponse.java
│       │   │   │   │   └── GeoPoint.java
│       │   │   │   ├── exception/
│       │   │   │   │   ├── GlobalExceptionHandler.java
│       │   │   │   │   ├── ResourceNotFoundException.java
│       │   │   │   │   ├── UnauthorizedException.java
│       │   │   │   │   └── BadRequestException.java
│       │   │   │   └── util/
│       │   │   │       ├── JwtTokenProvider.java
│       │   │   │       └── CurrencyUtils.java
│       │   │   │
│       │   │   ├── auth/
│       │   │   │   ├── controller/AuthController.java
│       │   │   │   ├── service/AuthService.java
│       │   │   │   ├── service/OAuth2Service.java
│       │   │   │   ├── security/JwtAuthenticationFilter.java
│       │   │   │   ├── security/UserPrincipal.java
│       │   │   │   ├── dto/RegisterRequest.java
│       │   │   │   ├── dto/LoginRequest.java
│       │   │   │   ├── dto/AuthResponse.java
│       │   │   │   ├── dto/UserProfileResponse.java
│       │   │   │   ├── repository/UserRepository.java
│       │   │   │   └── model/UserDocument.java
│       │   │   │
│       │   │   ├── trip/
│       │   │   │   ├── controller/TripController.java
│       │   │   │   ├── service/TripService.java
│       │   │   │   ├── dto/ (CreateTripRequest, TripResponse, ...)
│       │   │   │   ├── repository/TripRepository.java
│       │   │   │   └── model/TripDocument.java
│       │   │   │
│       │   │   ├── itinerary/
│       │   │   │   ├── controller/ItineraryController.java
│       │   │   │   ├── service/ItineraryAIService.java
│       │   │   │   ├── service/ClaudeApiClient.java
│       │   │   │   ├── service/PromptBuilder.java
│       │   │   │   ├── service/ItineraryParser.java
│       │   │   │   ├── dto/ (GenerateRequest, ItineraryDayResponse, ...)
│       │   │   │   ├── repository/ItineraryDayRepository.java
│       │   │   │   └── model/ItineraryDayDocument.java
│       │   │   │
│       │   │   ├── replan/
│       │   │   │   ├── service/ReplanningService.java
│       │   │   │   ├── service/ReplanningScheduler.java
│       │   │   │   ├── client/WeatherClient.java
│       │   │   │   ├── client/TrafficClient.java
│       │   │   │   ├── client/VenueStatusClient.java
│       │   │   │   ├── dto/ (ReplanContext, DiffPayload, ...)
│       │   │   │   ├── repository/ReplanLogRepository.java
│       │   │   │   └── model/ReplanLogDocument.java
│       │   │   │
│       │   │   ├── marketplace/
│       │   │   │   ├── controller/ExperienceController.java
│       │   │   │   ├── controller/CreatorController.java
│       │   │   │   ├── service/MarketplaceService.java
│       │   │   │   ├── service/ExperienceMatchingService.java
│       │   │   │   ├── dto/ (ExperienceResponse, BookingRequest, ...)
│       │   │   │   ├── repository/ExperienceRepository.java
│       │   │   │   ├── repository/BookingRepository.java
│       │   │   │   ├── repository/ReviewRepository.java
│       │   │   │   ├── model/ExperienceDocument.java
│       │   │   │   ├── model/BookingDocument.java
│       │   │   │   └── model/ReviewDocument.java
│       │   │   │
│       │   │   ├── payment/
│       │   │   │   ├── controller/PaymentController.java
│       │   │   │   ├── service/PaymentService.java
│       │   │   │   └── dto/ (PaymentIntentResponse, ...)
│       │   │   │
│       │   │   ├── map/
│       │   │   │   ├── controller/MapController.java
│       │   │   │   ├── service/FootfallService.java
│       │   │   │   ├── service/HeatmapScheduler.java
│       │   │   │   ├── dto/ (HeatmapResponse, POIResponse, ...)
│       │   │   │   ├── repository/FootfallGridRepository.java
│       │   │   │   └── model/FootfallGridDocument.java
│       │   │   │
│       │   │   ├── event/
│       │   │   │   ├── controller/EventController.java
│       │   │   │   ├── service/EventAggregatorService.java
│       │   │   │   ├── client/EventbriteClient.java
│       │   │   │   ├── dto/ (EventResponse, ...)
│       │   │   │   ├── repository/EventRepository.java
│       │   │   │   └── model/EventDocument.java
│       │   │   │
│       │   │   ├── notification/
│       │   │   │   ├── controller/NotificationController.java
│       │   │   │   ├── service/NotificationService.java
│       │   │   │   ├── client/SendGridClient.java
│       │   │   │   ├── client/FirebaseClient.java
│       │   │   │   ├── dto/ (NotificationResponse, ...)
│       │   │   │   ├── repository/NotificationRepository.java
│       │   │   │   └── model/NotificationDocument.java
│       │   │   │
│       │   │   ├── analytics/
│       │   │   │   ├── controller/AnalyticsController.java
│       │   │   │   ├── service/AnalyticsService.java
│       │   │   │   └── dto/ (CreatorVisibilityResponse, ...)
│       │   │   │
│       │   │   └── messaging/
│       │   │       ├── controller/MessageController.java
│       │   │       ├── service/MessageService.java
│       │   │       ├── repository/MessageRepository.java
│       │   │       └── model/MessageDocument.java
│       │   │
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-docker.yml
│       │       ├── application-test.yml
│       │       └── prompts/
│       │           ├── itinerary-system.txt
│       │           ├── itinerary-user.txt
│       │           └── replan-user.txt
│       │
│       └── test/
│           └── java/com/locallens/
│               ├── auth/service/AuthServiceTest.java
│               ├── trip/service/TripServiceTest.java
│               ├── itinerary/service/ItineraryAIServiceTest.java
│               ├── replan/service/ReplanningServiceTest.java
│               ├── marketplace/service/ExperienceMatchingServiceTest.java
│               └── integration/
│                   ├── TripIntegrationTest.java
│                   └── ItineraryIntegrationTest.java
│
├── locallens-frontend/                       # React Application
│   ├── Dockerfile
│   ├── Dockerfile.dev
│   ├── docker/nginx.conf
│   ├── package.json
│   ├── vite.config.js
│   ├── .env.example
│   ├── public/
│   │   ├── index.html
│   │   ├── favicon.ico
│   │   └── manifest.json
│   └── src/
│       ├── index.js
│       ├── App.jsx
│       ├── components/
│       │   ├── common/     (17 shared UI components)
│       │   ├── auth/       (4 auth components)
│       │   ├── trip/       (6 trip wizard components)
│       │   ├── dashboard/  (13 dashboard components)
│       │   ├── map/        (10 map components)
│       │   ├── marketplace/ (8 marketplace components)
│       │   ├── creator/    (8 creator components)
│       │   └── messaging/  (3 messaging components)
│       ├── pages/          (13 page components)
│       ├── hooks/          (14 custom hooks)
│       ├── services/       (12 API service modules)
│       ├── store/          (5 Zustand stores)
│       ├── utils/          (4 utility modules)
│       └── styles/         (6 CSS files)
│
└── docs/
    ├── architecture.md
    ├── api-docs.md
    ├── creator-guide.md
    └── deployment.md
```

### Component Count Summary

| Area | Files |
|------|-------|
| Backend Java classes | ~85 |
| Frontend React components | ~69 |
| Hooks | 14 |
| Services | 12 |
| Stores | 5 |
| CSS files | 6 |
| Config/Docker | ~12 |
| Tests (backend) | ~20 |
| **Total** | **~223 files** |

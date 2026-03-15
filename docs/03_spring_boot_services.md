# LocalLens — Spring Boot Services

> **Architecture**: Modular monolith (v1) using Spring Boot 3.x on Java 17+. Each service is a separate package within a single deployable JAR. Services marked with `🔀` are candidates for extraction into separate microservices at scale.

---

## Package Structure

```
com.locallens
├── config/                     # Spring configuration classes
│   ├── SecurityConfig.java
│   ├── WebSocketConfig.java
│   ├── MongoConfig.java
│   ├── RedisConfig.java
│   ├── StripeConfig.java
│   └── CorsConfig.java
├── auth/                       # AuthService
│   ├── controller/
│   ├── service/
│   ├── dto/
│   ├── repository/
│   └── security/
├── trip/                       # TripService
├── itinerary/                  # ItineraryAIService
├── replan/                     # ReplanningService 🔀
├── marketplace/                # MarketplaceService
├── payment/                    # PaymentService 🔀
├── map/                        # MapService
├── event/                      # EventAggregatorService
├── notification/               # NotificationService 🔀
├── analytics/                  # AnalyticsService
└── common/                     # Shared DTOs, exceptions, utils
    ├── exception/
    ├── dto/
    └── util/
```

---

## 1. AuthService

**Package**: `com.locallens.auth`

### Controller — `AuthController.java`

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // Local registration
    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req);

    // Local login (email + password)
    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req);

    // OAuth2 callback handler (Google / Apple)
    @PostMapping("/oauth2/{provider}")
    ResponseEntity<AuthResponse> oauthLogin(
        @PathVariable String provider,
        @RequestBody OAuth2TokenRequest req
    );

    // Refresh JWT access token
    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest req);

    // Logout (invalidate refresh token)
    @PostMapping("/logout")
    ResponseEntity<Void> logout(@AuthenticationPrincipal UserPrincipal user);

    // Get current user profile
    @GetMapping("/me")
    ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal user);

    // Update user preferences
    @PatchMapping("/me/preferences")
    ResponseEntity<UserProfileResponse> updatePreferences(
        @AuthenticationPrincipal UserPrincipal user,
        @Valid @RequestBody UpdatePreferencesRequest req
    );
}
```

### Service — `AuthService.java`

```java
@Service
public class AuthService {
    UserDocument register(RegisterRequest req);
    AuthResponse authenticate(String email, String password);
    AuthResponse authenticateOAuth2(String provider, String idToken);
    AuthResponse refreshAccessToken(String refreshToken);
    void logout(String userId);
    UserDocument getUserById(String userId);
    UserDocument updatePreferences(String userId, UpdatePreferencesRequest req);
}
```

### DTOs

```java
record RegisterRequest(String email, String password, String firstName, String lastName, String role) {}
record LoginRequest(String email, String password) {}
record AuthResponse(String accessToken, String refreshToken, UserProfileResponse user) {}
record OAuth2TokenRequest(String idToken) {}
record RefreshTokenRequest(String refreshToken) {}
record UpdatePreferencesRequest(List<String> interests, String travelStyle,
    List<String> accessibilityNeeds, Boolean darkMode) {}
record UserProfileResponse(String id, String email, String firstName, String lastName,
    String role, String avatarUrl, UserPreferences preferences, CreatorProfileDTO creatorProfile) {}
```

### Security

- `JwtTokenProvider.java` — generate/validate JWT, 15min access / 7d refresh
- `JwtAuthenticationFilter.java` — extracts JWT from `Authorization: Bearer` header
- `OAuth2Service.java` — verifies Google/Apple ID tokens via provider APIs
- `UserPrincipal.java` — implements `UserDetails`

---

## 2. TripService

**Package**: `com.locallens.trip`

### Controller — `TripController.java`

```java
@RestController
@RequestMapping("/api/trips")
@PreAuthorize("hasRole('TRAVELER')")
public class TripController {

    @PostMapping
    ResponseEntity<TripResponse> createTrip(@Valid @RequestBody CreateTripRequest req,
        @AuthenticationPrincipal UserPrincipal user);

    @GetMapping
    ResponseEntity<List<TripSummaryResponse>> getUserTrips(@AuthenticationPrincipal UserPrincipal user);

    @GetMapping("/{tripId}")
    ResponseEntity<TripDetailResponse> getTripById(@PathVariable String tripId);

    @PatchMapping("/{tripId}")
    ResponseEntity<TripResponse> updateTrip(@PathVariable String tripId,
        @Valid @RequestBody UpdateTripRequest req);

    @DeleteMapping("/{tripId}")
    ResponseEntity<Void> deleteTrip(@PathVariable String tripId);

    // Flight management
    @PostMapping("/{tripId}/flights")
    ResponseEntity<TripResponse> addFlight(@PathVariable String tripId,
        @Valid @RequestBody FlightRequest req);

    @DeleteMapping("/{tripId}/flights/{flightIdx}")
    ResponseEntity<TripResponse> removeFlight(@PathVariable String tripId, @PathVariable int flightIdx);

    // Accommodation management
    @PostMapping("/{tripId}/accommodation")
    ResponseEntity<TripResponse> addAccommodation(@PathVariable String tripId,
        @Valid @RequestBody AccommodationRequest req);

    // Document upload (GridFS)
    @PostMapping("/{tripId}/documents")
    ResponseEntity<TripResponse> uploadDocument(@PathVariable String tripId,
        @RequestParam("file") MultipartFile file,
        @RequestParam("type") String documentType);

    @GetMapping("/{tripId}/documents/{docId}")
    ResponseEntity<Resource> downloadDocument(@PathVariable String tripId, @PathVariable String docId);

    // Budget tracking
    @GetMapping("/{tripId}/budget")
    ResponseEntity<BudgetBreakdownResponse> getBudgetBreakdown(@PathVariable String tripId);

    @PostMapping("/{tripId}/budget/expense")
    ResponseEntity<BudgetBreakdownResponse> logExpense(@PathVariable String tripId,
        @Valid @RequestBody ExpenseRequest req);
}
```

### Service — `TripService.java`

```java
@Service
public class TripService {
    TripDocument createTrip(String userId, CreateTripRequest req);
    List<TripDocument> getUserTrips(String userId);
    TripDocument getTripById(String tripId);
    TripDocument updateTrip(String tripId, UpdateTripRequest req);
    void deleteTrip(String tripId);
    TripDocument addFlight(String tripId, FlightRequest req);
    TripDocument addAccommodation(String tripId, AccommodationRequest req);
    String uploadDocument(String tripId, MultipartFile file, String type);
    GridFsResource downloadDocument(String docId);
    BudgetBreakdown computeBudgetBreakdown(String tripId);
    void logExpense(String tripId, ExpenseRequest req);
    List<TripDocument> getActiveTripsForDestination(String city, LocalDate date);
}
```

**MongoDB Repos**: `TripRepository`, GridFS via `GridFsTemplate`
**External APIs**: None directly
**Scheduled Jobs**: None

---

## 3. ItineraryAIService 🔀

**Package**: `com.locallens.itinerary`

### Controller — `ItineraryController.java`

```java
@RestController
@RequestMapping("/api/itinerary")
public class ItineraryController {

    @PostMapping("/generate")
    ResponseEntity<ItineraryResponse> generateItinerary(
        @Valid @RequestBody GenerateItineraryRequest req,
        @AuthenticationPrincipal UserPrincipal user);

    @PostMapping("/{itineraryDayId}/replan")
    ResponseEntity<ReplanResponse> requestReplan(
        @PathVariable String itineraryDayId,
        @Valid @RequestBody ManualReplanRequest req);

    @GetMapping("/{tripId}/days")
    ResponseEntity<List<ItineraryDayResponse>> getItineraryDays(@PathVariable String tripId);

    @GetMapping("/{tripId}/days/{dayNumber}")
    ResponseEntity<ItineraryDayResponse> getItineraryDay(
        @PathVariable String tripId, @PathVariable int dayNumber);

    @PatchMapping("/{tripId}/days/{dayNumber}/slots/reorder")
    ResponseEntity<ItineraryDayResponse> reorderSlots(
        @PathVariable String tripId, @PathVariable int dayNumber,
        @Valid @RequestBody ReorderSlotsRequest req);

    @GetMapping("/{tripId}/status")
    ResponseEntity<ItineraryStatusResponse> getItineraryStatus(@PathVariable String tripId);
}
```

### Service — `ItineraryAIService.java`

```java
@Service
public class ItineraryAIService {

    // Assembles prompt with user profile, hyperlocal experiences, events, and constraints
    // Calls Claude API, parses structured JSON response, stores in MongoDB
    ItineraryResult generateItinerary(String tripId, GenerateItineraryRequest req);

    // Builds replan prompt with current plan + context (weather/traffic/closure)
    // Returns updated day plan + diff
    ReplanResult replanDay(String tripId, int dayNumber, ReplanContext context);

    // Internal: constructs the Claude system prompt
    String buildSystemPrompt(TripDocument trip, List<ExperienceDocument> experiences,
        List<EventDocument> events);

    // Internal: constructs the user prompt for itinerary generation
    String buildGenerationPrompt(GenerateItineraryRequest req);

    // Internal: constructs the user prompt for replanning
    String buildReplanPrompt(ItineraryDayDocument currentDay, ReplanContext context);

    // Internal: parses Claude's JSON response into ItineraryDay documents
    List<ItineraryDayDocument> parseItineraryResponse(String claudeResponse, String tripId);

    // Internal: calls Claude API with retry + error handling
    String callClaudeAPI(String systemPrompt, String userPrompt, int maxTokens);
}
```

### DTOs

```java
record GenerateItineraryRequest(String tripId) {}
record ManualReplanRequest(String reason, List<String> slotIds) {}
record ReplanContext(String triggeredBy, String reasonCode, String reasonDetail,
    WeatherData weather, TrafficData traffic, VenueStatus venueStatus) {}
record ItineraryResult(String tripId, List<ItineraryDayDocument> days, int totalTokensUsed) {}
record ReplanResult(String tripId, int dayNumber, ItineraryDayDocument updatedDay,
    DiffPayload diff) {}
record DiffPayload(List<SlotDiff> removed, List<SlotDiff> added, List<FieldDiff> modified) {}
record ReorderSlotsRequest(List<String> slotIdsInOrder) {}
```

**MongoDB Repos**: `ItineraryDayRepository`
**External APIs**: Claude API (Anthropic)
**Dependencies**: `MarketplaceService` (for experience matching), `EventAggregatorService` (for events)

---

## 4. ReplanningService 🔀

**Package**: `com.locallens.replan`

### Service — `ReplanningService.java`

```java
@Service
public class ReplanningService {

    // Master replan orchestrator
    void triggerReplan(String tripId, int dayNumber, ReplanTrigger trigger);

    // Checks weather for all active trips, triggers replans if needed
    void checkWeatherAndReplan();

    // Checks traffic for all active trips with upcoming activities
    void checkTrafficAndReplan();

    // Checks venue status for upcoming activities
    void checkVenueStatusAndReplan();

    // Computes diff between old and new itinerary day
    DiffPayload computeDiff(ItineraryDayDocument oldDay, ItineraryDayDocument newDay);

    // Logs replan decision to MongoDB
    void logReplanEvent(String tripId, int dayNumber, ReplanTrigger trigger,
        DiffPayload diff, int promptTokens, int responseTokens, long processingTimeMs);

    // User accepts or rejects a replan
    void handleReplanDecision(String tripId, String replanLogId, boolean accepted);
}
```

### Scheduled Jobs

```java
@Component
public class ReplanningScheduler {

    @Scheduled(fixedRate = 1800000) // every 30 minutes
    void weatherCheckJob();

    @Scheduled(fixedRate = 900000)  // every 15 minutes
    void trafficCheckJob();

    @Scheduled(fixedRate = 3600000) // every 60 minutes
    void venueStatusCheckJob();
}
```

### External API Clients

```java
@Service
public class WeatherClient {
    WeatherData getCurrentWeather(double lat, double lng);
    WeatherForecast getForecast(double lat, double lng, int days);
    boolean isAdverseWeather(WeatherData data); // rain > 70%, wind > 60km/h, storm alerts
}

@Service
public class TrafficClient {
    TrafficData getTrafficFlow(double fromLat, double fromLng, double toLat, double toLng);
    boolean isSevereCongestion(TrafficData data); // freeFlowSpeed/currentSpeed ratio > 2.0
    RouteData getAlternativeRoute(double fromLat, double fromLng, double toLat, double toLng);
}

@Service
public class VenueStatusClient {
    VenueStatus checkVenueStatus(String placeId);
    boolean isClosed(VenueStatus status);
}
```

**MongoDB Repos**: `ReplanLogRepository`, `ItineraryDayRepository`, `TripRepository`
**External APIs**: OpenWeatherMap, TomTom Traffic, Google Places
**WebSocket**: Pushes `REPLAN_TRIGGERED` events via `SimpMessagingTemplate`

---

## 5. MarketplaceService

**Package**: `com.locallens.marketplace`

### Controllers

```java
// --- Traveler-facing ---
@RestController
@RequestMapping("/api/experiences")
public class ExperienceController {

    @GetMapping
    ResponseEntity<Page<ExperienceResponse>> searchExperiences(
        @RequestParam String city,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) Double minPrice,
        @RequestParam(required = false) Double maxPrice,
        @RequestParam(required = false) Double lat,
        @RequestParam(required = false) Double lng,
        @RequestParam(required = false) Double radiusKm,
        @RequestParam(required = false) String sortBy, // rating, price, distance
        Pageable pageable);

    @GetMapping("/{id}")
    ResponseEntity<ExperienceDetailResponse> getExperience(@PathVariable String id);

    @GetMapping("/{id}/reviews")
    ResponseEntity<Page<ReviewResponse>> getExperienceReviews(
        @PathVariable String id, Pageable pageable);

    @PostMapping("/{id}/book")
    ResponseEntity<BookingResponse> bookExperience(@PathVariable String id,
        @Valid @RequestBody BookingRequest req,
        @AuthenticationPrincipal UserPrincipal user);

    @PostMapping("/bookings/{bookingId}/review")
    ResponseEntity<ReviewResponse> leaveReview(@PathVariable String bookingId,
        @Valid @RequestBody ReviewRequest req,
        @AuthenticationPrincipal UserPrincipal user);
}

// --- Creator-facing ---
@RestController
@RequestMapping("/api/creator")
@PreAuthorize("hasRole('CREATOR')")
public class CreatorController {

    @PostMapping("/experiences")
    ResponseEntity<ExperienceResponse> createExperience(
        @Valid @RequestBody CreateExperienceRequest req,
        @AuthenticationPrincipal UserPrincipal user);

    @PatchMapping("/experiences/{id}")
    ResponseEntity<ExperienceResponse> updateExperience(@PathVariable String id,
        @Valid @RequestBody UpdateExperienceRequest req);

    @DeleteMapping("/experiences/{id}")
    ResponseEntity<Void> archiveExperience(@PathVariable String id);

    @GetMapping("/experiences")
    ResponseEntity<List<ExperienceResponse>> getMyExperiences(
        @AuthenticationPrincipal UserPrincipal user);

    @GetMapping("/bookings")
    ResponseEntity<Page<BookingResponse>> getMyBookings(
        @AuthenticationPrincipal UserPrincipal user,
        @RequestParam(required = false) String status,
        Pageable pageable);

    @PatchMapping("/bookings/{bookingId}/accept")
    ResponseEntity<BookingResponse> acceptBooking(@PathVariable String bookingId);

    @PatchMapping("/bookings/{bookingId}/decline")
    ResponseEntity<BookingResponse> declineBooking(@PathVariable String bookingId,
        @RequestBody DeclineReasonRequest req);

    @GetMapping("/dashboard")
    ResponseEntity<CreatorDashboardResponse> getDashboard(
        @AuthenticationPrincipal UserPrincipal user);

    @GetMapping("/earnings")
    ResponseEntity<EarningsResponse> getEarnings(
        @AuthenticationPrincipal UserPrincipal user,
        @RequestParam String period); // WEEK, MONTH, YEAR, ALL

    @GetMapping("/analytics/ai-injections")
    ResponseEntity<List<AIInjectionStat>> getAIInjectionStats(
        @AuthenticationPrincipal UserPrincipal user);
}
```

### Service — `ExperienceMatchingService.java`

```java
@Service
public class ExperienceMatchingService {

    /**
     * Core matching algorithm for AI itinerary injection.
     * Scores each experience on weighted criteria and returns ranked list.
     */
    List<RankedExperience> match(UserPreferences travelerProfile,
                                  String destinationCity,
                                  LocalDate startDate,
                                  LocalDate endDate,
                                  int groupSize) {
        // Scoring weights:
        // 1. Interest tag overlap        → weight 0.25
        // 2. Budget fit                  → weight 0.20
        // 3. Travel style compatibility  → weight 0.15
        // 4. Availability on dates       → weight 0.15 (binary: available or not)
        // 5. Rating × log(bookingCount)  → weight 0.15
        // 6. Hyperlocal bonus            → weight 0.10 (1.5x multiplier if hyperlocal=true)
    }

    double scoreInterestOverlap(List<String> travelerInterests, List<String> experienceTags);
    double scoreBudgetFit(long budgetCents, long experiencePriceCents, String travelStyle);
    double scoreStyleCompatibility(String travelStyle, String experienceCategory, long price);
    boolean checkAvailability(List<Availability> slots, LocalDate start, LocalDate end);
    double scorePopularity(double rating, int bookingCount);
}
```

**MongoDB Repos**: `ExperienceRepository`, `BookingRepository`, `ReviewRepository`
**External APIs**: None directly
**Dependencies**: `PaymentService`, `NotificationService`

---

## 6. PaymentService 🔀

**Package**: `com.locallens.payment`

### Controller — `PaymentController.java`

```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @PostMapping("/create-intent")
    ResponseEntity<PaymentIntentResponse> createPaymentIntent(
        @Valid @RequestBody CreatePaymentIntentRequest req,
        @AuthenticationPrincipal UserPrincipal user);

    @PostMapping("/webhook")
    ResponseEntity<Void> handleStripeWebhook(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String sigHeader);

    @GetMapping("/creator/payouts")
    @PreAuthorize("hasRole('CREATOR')")
    ResponseEntity<List<PayoutResponse>> getPayouts(@AuthenticationPrincipal UserPrincipal user);

    @PostMapping("/creator/onboard")
    @PreAuthorize("hasRole('CREATOR')")
    ResponseEntity<StripeOnboardingResponse> initiateStripeOnboarding(
        @AuthenticationPrincipal UserPrincipal user);
}
```

### Service — `PaymentService.java`

```java
@Service
public class PaymentService {
    PaymentIntent createPaymentIntent(String travelerId, String experienceId,
        int groupSize, LocalDate date);
    void handlePaymentSuccess(String paymentIntentId);
    void handlePaymentFailure(String paymentIntentId);
    void processCreatorPayout(String bookingId);
    void processRefund(String bookingId, long refundAmountCents);
    String createStripeConnectAccount(String creatorId, String email);
    String getStripeOnboardingUrl(String stripeAccountId);
    long calculatePlatformFee(long amountCents, double commissionRate);
}
```

**MongoDB Repos**: `BookingRepository`
**External APIs**: Stripe (Payments + Connect)

---

## 7. MapService

**Package**: `com.locallens.map`

### Controller — `MapController.java`

```java
@RestController
@RequestMapping("/api/map")
public class MapController {

    @GetMapping("/heatmap")
    ResponseEntity<HeatmapResponse> getHeatmap(
        @RequestParam double lat,
        @RequestParam double lng,
        @RequestParam double radiusKm,
        @RequestParam(required = false, defaultValue = "-1") int hour,
        @RequestParam(required = false) String dayOfWeek);

    @GetMapping("/pois")
    ResponseEntity<List<POIResponse>> getPOIs(
        @RequestParam double lat,
        @RequestParam double lng,
        @RequestParam double radiusKm,
        @RequestParam(required = false) String category);

    @GetMapping("/pois/{id}")
    ResponseEntity<POIDetailResponse> getPOIDetail(@PathVariable String id);

    @GetMapping("/route")
    ResponseEntity<RouteResponse> getRouteGeoJSON(
        @RequestParam String tripId,
        @RequestParam int dayNumber);

    @GetMapping("/traffic")
    ResponseEntity<TrafficOverlayResponse> getTrafficOverlay(
        @RequestParam double lat,
        @RequestParam double lng,
        @RequestParam double radiusKm);

    @PostMapping("/checkin")
    ResponseEntity<Void> recordCheckIn(
        @Valid @RequestBody CheckInRequest req,
        @AuthenticationPrincipal UserPrincipal user);
}
```

### Service — `FootfallService.java`

```java
@Service
public class FootfallService {

    // Aggregates anonymous visit logs + creator check-in data
    void recordVisit(double lat, double lng, String userId, Instant timestamp);

    // Precomputes heatmap grid for a city, stores as GeoJSON in MongoDB
    void computeHeatmapGrid(String city);

    // Retrieves cached heatmap data for a region
    List<FootfallGridDocument> getHeatmapData(double lat, double lng,
        double radiusKm, int hour);

    // Determines crowd level from footfall score
    CrowdLevel classifyCrowdLevel(double footfallScore);

    // Generates best-time-to-visit recommendation
    String recommendBestTime(String poiId);
}
```

### Scheduled Jobs

```java
@Scheduled(fixedRate = 1800000) // every 30 minutes
void recomputeHeatmaps();
```

**MongoDB Repos**: `FootfallGridRepository`, `ExperienceRepository` (for POIs)
**External APIs**: TomTom Traffic (for traffic overlay), Mapbox/MapLibre tile URLs
**Cache**: Redis for heatmap grid data (30min TTL)

---

## 8. EventAggregatorService

**Package**: `com.locallens.event`

### Controller — `EventController.java`

```java
@RestController
@RequestMapping("/api/events")
public class EventController {

    @GetMapping
    ResponseEntity<Page<EventResponse>> getEvents(
        @RequestParam String city,
        @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) List<String> tags,
        @RequestParam(required = false) Boolean familyFriendly,
        Pageable pageable);

    @GetMapping("/tonight")
    ResponseEntity<List<EventResponse>> getTonightEvents(@RequestParam String city);

    @PostMapping
    @PreAuthorize("hasRole('CREATOR')")
    ResponseEntity<EventResponse> submitCreatorEvent(
        @Valid @RequestBody CreateEventRequest req,
        @AuthenticationPrincipal UserPrincipal user);
}
```

### Service — `EventAggregatorService.java`

```java
@Service
public class EventAggregatorService {

    // Nightly batch: pull events from all external sources for tracked cities
    void aggregateAllEvents();

    // Pull Eventbrite events for a city + date range
    List<EventDocument> fetchEventbriteEvents(String city, LocalDate start, LocalDate end);

    // Deduplicate events across sources (by title + location + time similarity)
    List<EventDocument> deduplicateEvents(List<EventDocument> events);

    // Query events for AI prompt injection
    List<EventDocument> getEventsForItinerary(String city, LocalDate start, LocalDate end);

    // Creator submits a manual event
    EventDocument createCreatorEvent(String creatorId, CreateEventRequest req);
}
```

### Scheduled Jobs

```java
@Scheduled(cron = "0 0 2 * * *") // 2 AM daily
void nightlyEventAggregation();

@Scheduled(fixedRate = 3600000) // hourly during daytime
void realTimeEventRefresh();
```

**MongoDB Repos**: `EventRepository`
**External APIs**: Eventbrite API
**Cache**: Redis for tonight's events (15min TTL)

---

## 9. NotificationService 🔀

**Package**: `com.locallens.notification`

### Controller — `NotificationController.java`

```java
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @GetMapping
    ResponseEntity<Page<NotificationResponse>> getNotifications(
        @AuthenticationPrincipal UserPrincipal user,
        @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
        Pageable pageable);

    @PatchMapping("/{id}/read")
    ResponseEntity<Void> markAsRead(@PathVariable String id);

    @PatchMapping("/read-all")
    ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserPrincipal user);

    @GetMapping("/unread-count")
    ResponseEntity<UnreadCountResponse> getUnreadCount(@AuthenticationPrincipal UserPrincipal user);
}
```

### Service — `NotificationService.java`

```java
@Service
public class NotificationService {

    // Unified dispatch: creates notification + sends via configured channels
    void send(String userId, NotificationType type, String title, String body,
        Map<String, String> data, Set<NotificationChannel> channels);

    // WebSocket push
    void pushWebSocket(String userId, NotificationPayload payload);

    // Email via SendGrid
    void sendEmail(String toEmail, String templateId, Map<String, Object> templateData);

    // Push notification via Firebase Cloud Messaging
    void sendPushNotification(String userId, String title, String body, Map<String, String> data);

    // Convenience methods for common notification types
    void notifyBookingConfirmed(BookingDocument booking);
    void notifyBookingDeclined(BookingDocument booking, String reason);
    void notifyReplanTriggered(String tripId, int dayNumber, DiffPayload diff);
    void notifyReviewReceived(ReviewDocument review);
    void notifyPayoutProcessed(String creatorId, long amountCents);
    void notifyNewMessage(MessageDocument message);
}
```

**MongoDB Repos**: `NotificationRepository`
**External APIs**: SendGrid, Firebase Cloud Messaging
**WebSocket**: `SimpMessagingTemplate` for real-time push

---

## 10. AnalyticsService

**Package**: `com.locallens.analytics`

### Controller — `AnalyticsController.java`

```java
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @GetMapping("/creator/{creatorId}/visibility")
    @PreAuthorize("hasRole('CREATOR') or hasRole('ADMIN')")
    ResponseEntity<CreatorVisibilityResponse> getCreatorVisibility(@PathVariable String creatorId);

    @GetMapping("/trip/{tripId}/stats")
    ResponseEntity<TripAnalyticsResponse> getTripAnalytics(@PathVariable String tripId);

    @GetMapping("/admin/platform")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<PlatformAnalyticsResponse> getPlatformAnalytics(
        @RequestParam String period);
}
```

### Service — `AnalyticsService.java`

```java
@Service
public class AnalyticsService {

    // How many AI itineraries included this creator's experiences
    CreatorVisibilityStats getCreatorAIInjectionStats(String creatorId, String period);

    // Footfall logging for heatmap data
    void logFootfall(String userId, double lat, double lng);

    // Trip-level analytics: budget adherence, activities completed, replans
    TripAnalytics computeTripAnalytics(String tripId);

    // Platform-wide: total bookings, GMV, active creators, popular destinations
    PlatformAnalytics computePlatformAnalytics(String period);
}
```

**MongoDB Repos**: `ReplanLogRepository`, `BookingRepository`, `FootfallGridRepository`
**External APIs**: None
**Scheduled Jobs**: Nightly aggregation of analytics snapshots

---

## Messaging (Cross-cutting)

### Controller — `MessageController.java`

```java
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @GetMapping("/conversations")
    ResponseEntity<List<ConversationSummary>> getConversations(
        @AuthenticationPrincipal UserPrincipal user);

    @GetMapping("/conversations/{conversationId}")
    ResponseEntity<Page<MessageResponse>> getMessages(
        @PathVariable String conversationId, Pageable pageable);

    @PostMapping("/send")
    ResponseEntity<MessageResponse> sendMessage(
        @Valid @RequestBody SendMessageRequest req,
        @AuthenticationPrincipal UserPrincipal user);

    @PatchMapping("/{messageId}/read")
    ResponseEntity<Void> markMessageRead(@PathVariable String messageId);
}
```

**MongoDB Repos**: `MessageRepository`
**WebSocket**: Messages pushed to `/queue/messages/{userId}` in real-time

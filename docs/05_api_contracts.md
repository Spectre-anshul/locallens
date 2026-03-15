# LocalLens — API Contracts

> All endpoints require `Authorization: Bearer <JWT>` unless marked 🔓 (public).  
> Pagination uses `?page=0&size=20&sort=createdAt,desc`.  
> All monetary values are in **cents** (integer).  
> Error responses follow `{ "error": "CODE", "message": "Human-readable", "timestamp": "ISO" }`.

---

## Authentication

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| 🔓 POST | `/api/auth/register` | — | Register new user |
| 🔓 POST | `/api/auth/login` | — | Login with email/password |
| 🔓 POST | `/api/auth/oauth2/{provider}` | — | OAuth2 login (google/apple) |
| 🔓 POST | `/api/auth/refresh` | — | Refresh access token |
| POST | `/api/auth/logout` | JWT | Invalidate refresh token |
| GET | `/api/auth/me` | JWT | Get current user profile |
| PATCH | `/api/auth/me/preferences` | JWT | Update user preferences |

### POST `/api/auth/register`
```json
// Request
{ "email": "user@example.com", "password": "SecureP@ss1", "firstName": "Aiko", "lastName": "Tanaka", "role": "TRAVELER" }

// Response 201
{ "accessToken": "eyJ...", "refreshToken": "abc...", "user": { "id": "6601...", "email": "user@example.com", "firstName": "Aiko", "lastName": "Tanaka", "role": "TRAVELER", "preferences": null } }
```

### POST `/api/auth/login`
```json
// Request
{ "email": "user@example.com", "password": "SecureP@ss1" }

// Response 200 — same shape as register response

// Error 401
{ "error": "INVALID_CREDENTIALS", "message": "Invalid email or password" }
```

### POST `/api/auth/oauth2/google`
```json
// Request
{ "idToken": "eyJ... (Google ID token)" }

// Response 200 — same shape as register (creates account if first login)
```

---

## Trips

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/trips` | TRAVELER | Create new trip |
| GET | `/api/trips` | TRAVELER | List user's trips |
| GET | `/api/trips/{tripId}` | TRAVELER | Get trip detail |
| PATCH | `/api/trips/{tripId}` | TRAVELER | Update trip |
| DELETE | `/api/trips/{tripId}` | TRAVELER | Delete trip |
| POST | `/api/trips/{tripId}/flights` | TRAVELER | Add flight |
| DELETE | `/api/trips/{tripId}/flights/{idx}` | TRAVELER | Remove flight |
| POST | `/api/trips/{tripId}/accommodation` | TRAVELER | Add accommodation |
| POST | `/api/trips/{tripId}/documents` | TRAVELER | Upload document (multipart) |
| GET | `/api/trips/{tripId}/documents/{docId}` | TRAVELER | Download document |
| GET | `/api/trips/{tripId}/budget` | TRAVELER | Get budget breakdown |
| POST | `/api/trips/{tripId}/budget/expense` | TRAVELER | Log expense |

### POST `/api/trips`
```json
// Request
{
  "title": "Kyoto Adventure 2026",
  "destination": { "city": "Kyoto", "country": "Japan", "lat": 35.0116, "lng": 135.7681 },
  "startDate": "2026-04-15",
  "endDate": "2026-04-22",
  "groupSize": 2,
  "travelStyle": "COMFORT",
  "interests": ["Food", "Culture", "Nature"],
  "accessibilityNeeds": [],
  "budget": { "total": 350000, "currency": "USD" }
}

// Response 201
{
  "id": "6601abc...",
  "title": "Kyoto Adventure 2026",
  "destination": { "city": "Kyoto", "country": "Japan" },
  "startDate": "2026-04-15",
  "endDate": "2026-04-22",
  "durationDays": 7,
  "status": "DRAFT",
  "budget": { "total": 350000, "spent": 0, "currency": "USD" },
  "createdAt": "2026-03-14T16:00:00Z"
}
```

### GET `/api/trips/{tripId}/budget`
```json
// Response 200
{
  "total": 350000,
  "spent": 127500,
  "remaining": 222500,
  "currency": "USD",
  "breakdown": {
    "accommodation": { "budgeted": 150000, "spent": 89000, "percentage": 59 },
    "food": { "budgeted": 70000, "spent": 23000, "percentage": 33 },
    "transport": { "budgeted": 50000, "spent": 12500, "percentage": 25 },
    "activities": { "budgeted": 60000, "spent": 3000, "percentage": 5 },
    "shopping": { "budgeted": 15000, "spent": 0, "percentage": 0 },
    "miscellaneous": { "budgeted": 5000, "spent": 0, "percentage": 0 }
  }
}
```

---

## Itinerary

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/itinerary/generate` | TRAVELER | Generate AI itinerary |
| GET | `/api/itinerary/{tripId}/days` | TRAVELER | Get all itinerary days |
| GET | `/api/itinerary/{tripId}/days/{dayNumber}` | TRAVELER | Get single day |
| PATCH | `/api/itinerary/{tripId}/days/{dayNumber}/slots/reorder` | TRAVELER | Reorder slots |
| POST | `/api/itinerary/{tripId}/replan` | TRAVELER | Request manual replan |
| GET | `/api/itinerary/{tripId}/status` | TRAVELER | Get generation status |

### POST `/api/itinerary/generate`
```json
// Request
{ "tripId": "6601abc..." }

// Response 202 (Accepted — generation is async)
{ "tripId": "6601abc...", "status": "GENERATING", "estimatedSeconds": 15 }
```

### GET `/api/itinerary/{tripId}/days`
```json
// Response 200
[
  {
    "id": "6602abc...",
    "tripId": "6601abc...",
    "dayNumber": 1,
    "date": "2026-04-15",
    "theme": "Temple Trail & Tea Ceremony",
    "weatherForecast": { "condition": "Partly Cloudy", "tempHighC": 22, "tempLowC": 14, "precipitation": 10 },
    "slots": [
      {
        "slotId": "s1-uuid",
        "startTime": "09:00",
        "endTime": "11:00",
        "activity": {
          "type": "ATTRACTION",
          "title": "Fushimi Inari Shrine Hike",
          "description": "Walk through thousands of vermillion torii gates...",
          "location": { "name": "Fushimi Inari Taisha", "address": "68 Fukakusa...", "coordinates": { "type": "Point", "coordinates": [135.7727, 34.9671] } },
          "duration": 120,
          "cost": { "amount": 0, "currency": "JPY" },
          "isHyperlocal": false,
          "tags": ["Nature", "Culture"]
        },
        "transport": { "mode": "METRO", "from": "Kyoto Station", "to": "Inari Station", "duration": 5, "cost": 150, "notes": "JR Nara Line, 2 stops" },
        "status": "SCHEDULED"
      },
      {
        "slotId": "s2-uuid",
        "startTime": "11:30",
        "endTime": "13:00",
        "activity": {
          "type": "EXPERIENCE",
          "title": "Local Ramen Tasting with Chef Hiro",
          "experienceId": "exp-123",
          "isHyperlocal": true,
          "tags": ["Food", "Culture"]
        },
        "status": "SCHEDULED"
      }
    ],
    "version": 1
  }
]
```

### POST `/api/itinerary/{tripId}/replan`
```json
// Request
{ "reason": "I want to skip the museum and do something outdoors instead", "slotIds": ["s3-uuid", "s4-uuid"] }

// Response 200
{
  "tripId": "6601abc...",
  "dayNumber": 2,
  "diff": {
    "removed": [{ "slotId": "s3-uuid", "title": "National Museum", "startTime": "14:00" }],
    "added": [{ "slotId": "s3-new", "title": "Arashiyama Bamboo Grove", "startTime": "14:00" }],
    "modified": [{ "slotId": "s4-uuid", "field": "startTime", "oldValue": "16:00", "newValue": "16:30" }]
  }
}
```

---

## Experiences (Marketplace)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| 🔓 GET | `/api/experiences` | — | Search experiences |
| 🔓 GET | `/api/experiences/{id}` | — | Get detail |
| 🔓 GET | `/api/experiences/{id}/reviews` | — | Get reviews |
| POST | `/api/experiences/{id}/book` | TRAVELER | Book experience |
| POST | `/api/experiences/bookings/{id}/review` | TRAVELER | Leave review |

### GET `/api/experiences?city=Kyoto&category=FOOD&maxPrice=5000&sortBy=rating`
```json
// Response 200
{
  "content": [
    {
      "id": "exp-123",
      "title": "Local Ramen Tasting with Chef Hiro",
      "category": "FOOD",
      "price": { "amount": 2800, "currency": "JPY", "pricingType": "PER_PERSON" },
      "duration": 90,
      "rating": 4.8,
      "reviewCount": 47,
      "location": { "city": "Kyoto", "coordinates": { "type": "Point", "coordinates": [135.77, 35.01] } },
      "creator": { "id": "creator-1", "name": "Chef Hiro", "verified": true },
      "primaryImage": "https://...",
      "hyperlocal": true
    }
  ],
  "totalElements": 34,
  "page": 0,
  "size": 20
}
```

### POST `/api/experiences/{id}/book`
```json
// Request
{ "date": "2026-04-16", "timeSlot": "19:00", "groupSize": 2, "specialRequests": "Vegetarian option for one person", "tripId": "6601abc..." }

// Response 201
{
  "bookingId": "bk-456",
  "status": "PENDING",
  "totalAmount": 5600,
  "currency": "JPY",
  "stripeClientSecret": "pi_xxx_secret_xxx"
}
```

---

## Creator

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/creator/experiences` | CREATOR | Create experience |
| PATCH | `/api/creator/experiences/{id}` | CREATOR | Update experience |
| DELETE | `/api/creator/experiences/{id}` | CREATOR | Archive experience |
| GET | `/api/creator/experiences` | CREATOR | List my experiences |
| GET | `/api/creator/bookings` | CREATOR | List my bookings |
| PATCH | `/api/creator/bookings/{id}/accept` | CREATOR | Accept booking |
| PATCH | `/api/creator/bookings/{id}/decline` | CREATOR | Decline booking |
| GET | `/api/creator/dashboard` | CREATOR | Dashboard stats |
| GET | `/api/creator/earnings?period=MONTH` | CREATOR | Earnings data |
| GET | `/api/creator/analytics/ai-injections` | CREATOR | AI injection stats |

### GET `/api/creator/dashboard`
```json
// Response 200
{
  "upcomingBookings": 5,
  "pendingBookings": 2,
  "totalEarnings": 1250000,
  "thisMonthEarnings": 340000,
  "activeExperiences": 3,
  "averageRating": 4.7,
  "aiInjections": { "thisMonth": 23, "total": 187 },
  "currency": "JPY"
}
```

---

## Map

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| 🔓 GET | `/api/map/heatmap` | — | Get heatmap grid |
| 🔓 GET | `/api/map/pois` | — | Get POIs in radius |
| 🔓 GET | `/api/map/pois/{id}` | — | POI detail |
| GET | `/api/map/route` | JWT | Route GeoJSON for trip day |
| 🔓 GET | `/api/map/traffic` | — | Traffic overlay data |
| POST | `/api/map/checkin` | JWT | Record anonymous check-in |

### GET `/api/map/heatmap?lat=35.01&lng=135.77&radiusKm=5&hour=14`
```json
// Response 200
{
  "gridCells": [
    { "centroid": [135.768, 35.012], "footfallScore": 87, "crowdLevel": "VERY_HIGH", "bestTimeToVisit": "Before 9 AM or after 4 PM" },
    { "centroid": [135.772, 35.008], "footfallScore": 23, "crowdLevel": "LOW", "bestTimeToVisit": "Any time" }
  ],
  "computedAt": "2026-04-15T14:30:00Z",
  "filterHour": 14
}
```

---

## Events

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| 🔓 GET | `/api/events` | — | Search events |
| 🔓 GET | `/api/events/tonight` | — | Tonight's events |
| POST | `/api/events` | CREATOR | Submit creator event |

### GET `/api/events/tonight?city=Kyoto`
```json
// Response 200
[
  { "id": "evt-1", "source": "CREATOR", "title": "Underground Jazz at Bar Pigmalion", "startTime": "2026-04-15T21:00:00+09:00", "location": { "venueName": "Bar Pigmalion", "city": "Kyoto" }, "price": { "amount": 1500, "currency": "JPY" }, "ticketsRemaining": 15, "tags": ["Music", "Nightlife"] },
  { "id": "evt-2", "source": "EVENTBRITE", "title": "Neighborhood Lantern Festival", "startTime": "2026-04-15T18:00:00+09:00", "location": { "venueName": "Gion District", "city": "Kyoto" }, "price": { "amount": 0, "currency": "JPY" }, "familyFriendly": true, "tags": ["Culture", "Festival"] }
]
```

---

## Payments

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/payments/create-intent` | TRAVELER | Create Stripe PaymentIntent |
| POST | `/api/payments/webhook` | — (Stripe sig) | Stripe webhook handler |
| GET | `/api/payments/creator/payouts` | CREATOR | Payout history |
| POST | `/api/payments/creator/onboard` | CREATOR | Start Stripe Connect onboarding |

---

## Notifications & Messages

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/notifications` | JWT | Get notifications |
| PATCH | `/api/notifications/{id}/read` | JWT | Mark as read |
| PATCH | `/api/notifications/read-all` | JWT | Mark all read |
| GET | `/api/notifications/unread-count` | JWT | Unread count |
| GET | `/api/messages/conversations` | JWT | List conversations |
| GET | `/api/messages/conversations/{id}` | JWT | Get messages |
| POST | `/api/messages/send` | JWT | Send message |

---

## Analytics

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/analytics/creator/{id}/visibility` | CREATOR/ADMIN | Creator AI injection stats |
| GET | `/api/analytics/trip/{id}/stats` | TRAVELER | Trip analytics |
| GET | `/api/analytics/admin/platform` | ADMIN | Platform analytics |

---

## WebSocket STOMP Topics

| Destination | Direction | Payload | Description |
|------------|-----------|---------|-------------|
| `/topic/itinerary/{tripId}/replan` | Server → Client | `ReplanEvent` | Replan triggered (weather/traffic/closure) |
| `/topic/booking/{userId}/updates` | Server → Client | `BookingUpdate` | Booking confirmed/declined/cancelled |
| `/topic/map/{city}/crowd` | Server → Client | `CrowdUpdate` | Live crowd data every 5 min |
| `/queue/messages/{userId}` | Server → Client | `MessagePayload` | New DM received |
| `/app/checkin` | Client → Server | `CheckInRequest` | User check-in at location |

### WebSocket Connect
```
URL: wss://api.locallens.com/ws
Headers: { Authorization: "Bearer eyJ..." }
Protocol: STOMP over SockJS
Heartbeat: 10000ms send / 10000ms receive
```

### Replan Event Payload
```json
{
  "type": "REPLAN_TRIGGERED",
  "tripId": "6601abc...",
  "dayNumber": 3,
  "triggeredBy": "WEATHER",
  "reasonCode": "STORM_ALERT",
  "reasonDetail": "Heavy rain forecasted from 2 PM to 6 PM in Kyoto",
  "diff": {
    "removed": [{ "slotId": "s7", "title": "Arashiyama Bamboo Grove Walk", "startTime": "14:00" }],
    "added": [{ "slotId": "s7-new", "title": "Nishiki Market Food Tour (Indoor)", "startTime": "14:00" }],
    "modified": []
  },
  "timestamp": "2026-04-17T12:30:00Z"
}
```

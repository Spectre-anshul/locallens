# LocalLens — MongoDB Schemas

> All collections use MongoDB Atlas with `2dsphere` geospatial indexes on location fields. TTL indexes on session/temp data. All `_id` fields are auto-generated `ObjectId` unless noted.

---

## 1. `users` Collection

Unified collection with `role` discriminator for travelers and creators.

```json
{
  "_id": "ObjectId",
  "email": "string (unique, indexed)",
  "passwordHash": "string (null if OAuth-only)",
  "firstName": "string",
  "lastName": "string",
  "avatarUrl": "string",
  "role": "enum: TRAVELER | CREATOR | ADMIN",
  "authProvider": "enum: LOCAL | GOOGLE | APPLE",
  "oauthProviderId": "string (nullable)",
  "phone": "string (nullable)",
  "currency": "string (ISO 4217, default: USD)",
  "language": "string (ISO 639-1, default: en)",
  "preferences": {
    "interests": ["Food", "Adventure", "Museums"],
    "travelStyle": "enum: LUXURY | COMFORT | BACKPACKER | FAMILY | SOLO | COUPLE",
    "accessibilityNeeds": ["string"],
    "dietaryRestrictions": ["string"],
    "darkMode": "boolean"
  },
  "creatorProfile": {
    "city": "string",
    "expertise": ["Food", "Culture", "Nightlife"],
    "bio": "string (max 500 chars)",
    "verified": "boolean (default: false)",
    "idVerificationStatus": "enum: PENDING | VERIFIED | REJECTED",
    "stripeAccountId": "string (Stripe Connect)",
    "totalEarnings": "number (cents)",
    "rating": "number (0-5, computed)",
    "reviewCount": "number",
    "totalBookings": "number",
    "location": {
      "type": "Point",
      "coordinates": ["longitude", "latitude"]
    }
  },
  "refreshTokenHash": "string (nullable)",
  "lastLoginAt": "ISODate",
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

**Indexes:**
- `{ email: 1 }` — unique
- `{ role: 1 }`
- `{ "creatorProfile.city": 1, "creatorProfile.expertise": 1 }`
- `{ "creatorProfile.location": "2dsphere" }`

---

## 2. `trips` Collection

Master trip document containing metadata and budget tracking.

```json
{
  "_id": "ObjectId",
  "userId": "ObjectId (ref: users)",
  "title": "string (e.g., 'Kyoto Adventure 2026')",
  "destination": {
    "city": "string",
    "country": "string",
    "location": {
      "type": "Point",
      "coordinates": ["longitude", "latitude"]
    }
  },
  "startDate": "ISODate",
  "endDate": "ISODate",
  "durationDays": "number",
  "groupSize": "number",
  "travelStyle": "enum: LUXURY | COMFORT | BACKPACKER | FAMILY | SOLO | COUPLE",
  "interests": ["Food", "Adventure", "Museums"],
  "accessibilityNeeds": ["string"],
  "budget": {
    "total": "number (cents)",
    "currency": "string (ISO 4217)",
    "spent": "number (cents, computed)",
    "breakdown": {
      "accommodation": "number (cents)",
      "food": "number (cents)",
      "transport": "number (cents)",
      "activities": "number (cents)",
      "shopping": "number (cents)",
      "miscellaneous": "number (cents)"
    }
  },
  "flights": [{
    "type": "enum: OUTBOUND | RETURN | LAYOVER",
    "airline": "string",
    "flightNumber": "string",
    "departure": {
      "airport": "string (IATA)",
      "terminal": "string",
      "gate": "string (nullable)",
      "time": "ISODate"
    },
    "arrival": {
      "airport": "string (IATA)",
      "terminal": "string",
      "time": "ISODate"
    },
    "bookingRef": "string",
    "seatNumber": "string (nullable)"
  }],
  "accommodation": [{
    "name": "string",
    "type": "enum: HOTEL | HOSTEL | AIRBNB | RESORT",
    "address": "string",
    "location": {
      "type": "Point",
      "coordinates": ["longitude", "latitude"]
    },
    "checkIn": "ISODate",
    "checkOut": "ISODate",
    "bookingRef": "string",
    "pricePerNight": "number (cents)",
    "totalPrice": "number (cents)",
    "contactPhone": "string"
  }],
  "documents": [{
    "type": "enum: PASSPORT | VISA | INSURANCE | TICKET | OTHER",
    "fileName": "string",
    "gridFsFileId": "ObjectId (ref: GridFS)",
    "uploadedAt": "ISODate"
  }],
  "status": "enum: DRAFT | PLANNED | ACTIVE | COMPLETED | CANCELLED",
  "itineraryVersion": "number (incremented on replan)",
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

**Indexes:**
- `{ userId: 1, status: 1 }`
- `{ "destination.location": "2dsphere" }`
- `{ startDate: 1, endDate: 1 }`
- `{ status: 1, startDate: 1 }` — for scheduler queries

---

## 3. `itinerary_days` Collection

Day-by-day breakdown with time-slotted activity cards.

```json
{
  "_id": "ObjectId",
  "tripId": "ObjectId (ref: trips)",
  "dayNumber": "number (1-indexed)",
  "date": "ISODate",
  "theme": "string (e.g., 'Temple Trail & Tea Ceremony')",
  "slots": [{
    "slotId": "string (UUID)",
    "startTime": "string (HH:mm, 24h)",
    "endTime": "string (HH:mm, 24h)",
    "activity": {
      "type": "enum: ATTRACTION | EXPERIENCE | MEAL | TRANSPORT | FREE_TIME | CHECK_IN | CHECK_OUT",
      "title": "string",
      "description": "string",
      "location": {
        "name": "string",
        "address": "string",
        "coordinates": {
          "type": "Point",
          "coordinates": ["longitude", "latitude"]
        }
      },
      "duration": "number (minutes)",
      "cost": {
        "amount": "number (cents)",
        "currency": "string"
      },
      "bookingUrl": "string (nullable)",
      "experienceId": "ObjectId (nullable, ref: experiences)",
      "isHyperlocal": "boolean",
      "tags": ["string"],
      "imageUrl": "string (nullable)"
    },
    "transport": {
      "mode": "enum: WALK | METRO | BUS | TAXI | RENTAL | BIKE | FERRY",
      "from": "string",
      "to": "string",
      "duration": "number (minutes)",
      "cost": "number (cents)",
      "notes": "string (e.g., 'Take Karasuma Line to Gojo Station')"
    },
    "status": "enum: SCHEDULED | IN_PROGRESS | COMPLETED | SKIPPED | REPLANNED",
    "replanNote": "string (nullable, e.g., 'Moved indoors due to rain')"
  }],
  "weatherForecast": {
    "condition": "string",
    "tempHighC": "number",
    "tempLowC": "number",
    "precipitation": "number (percentage)",
    "icon": "string"
  },
  "version": "number",
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

**Indexes:**
- `{ tripId: 1, dayNumber: 1 }` — compound unique
- `{ date: 1 }`
- `{ "slots.activity.location.coordinates": "2dsphere" }`

---

## 4. `experiences` Collection

Creator-listed hyperlocal experiences for the marketplace.

```json
{
  "_id": "ObjectId",
  "creatorId": "ObjectId (ref: users)",
  "title": "string",
  "description": "string (max 2000 chars)",
  "category": "enum: FOOD | CULTURE | ADVENTURE | NIGHTLIFE | WELLNESS | NATURE | SHOPPING",
  "subcategory": "string (nullable)",
  "price": {
    "amount": "number (cents)",
    "currency": "string (ISO 4217)",
    "pricingType": "enum: PER_PERSON | PER_GROUP | FREE"
  },
  "duration": "number (minutes)",
  "maxGroupSize": "number",
  "minGroupSize": "number (default: 1)",
  "location": {
    "city": "string",
    "address": "string",
    "meetingPoint": "string",
    "coordinates": {
      "type": "Point",
      "coordinates": ["longitude", "latitude"]
    }
  },
  "availability": [{
    "type": "enum: RECURRING | ONE_OFF",
    "dayOfWeek": "number (0=Sun, nullable for ONE_OFF)",
    "startTime": "string (HH:mm)",
    "endTime": "string (HH:mm)",
    "specificDate": "ISODate (nullable, for ONE_OFF)",
    "maxBookings": "number",
    "currentBookings": "number"
  }],
  "media": [{
    "type": "enum: IMAGE | VIDEO",
    "url": "string",
    "gridFsId": "ObjectId (nullable)",
    "caption": "string",
    "isPrimary": "boolean"
  }],
  "tags": ["string"],
  "hyperlocal": "boolean (default: true)",
  "languages": ["string (ISO 639-1)"],
  "accessibilityInfo": "string (nullable)",
  "cancellationPolicy": "enum: FLEXIBLE | MODERATE | STRICT",
  "rating": "number (0-5, computed)",
  "reviewCount": "number",
  "bookingCount": "number",
  "status": "enum: DRAFT | ACTIVE | PAUSED | ARCHIVED",
  "commissionRate": "number (percentage, platform default or custom)",
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

**Indexes:**
- `{ creatorId: 1 }`
- `{ "location.coordinates": "2dsphere" }`
- `{ category: 1, "location.city": 1, status: 1 }`
- `{ hyperlocal: 1, status: 1, rating: -1 }`
- `{ tags: 1 }`

---

## 5. `events` Collection

Aggregated events from all sources.

```json
{
  "_id": "ObjectId",
  "source": "enum: CREATOR | EVENTBRITE | FACEBOOK | INSTAGRAM",
  "externalId": "string (nullable, source's event ID)",
  "title": "string",
  "description": "string",
  "category": "string",
  "startTime": "ISODate",
  "endTime": "ISODate (nullable)",
  "location": {
    "city": "string",
    "venueName": "string",
    "address": "string",
    "coordinates": {
      "type": "Point",
      "coordinates": ["longitude", "latitude"]
    }
  },
  "price": {
    "amount": "number (cents, 0 = free)",
    "currency": "string"
  },
  "capacity": "number (nullable)",
  "ticketsRemaining": "number (nullable)",
  "tags": ["string"],
  "externalUrl": "string (nullable)",
  "imageUrl": "string (nullable)",
  "creatorId": "ObjectId (nullable, ref: users)",
  "familyFriendly": "boolean",
  "ageRestriction": "number (nullable, minimum age)",
  "lastSyncedAt": "ISODate (for external sources)",
  "status": "enum: UPCOMING | ONGOING | PAST | CANCELLED",
  "createdAt": "ISODate"
}
```

**Indexes:**
- `{ "location.city": 1, startTime: 1, status: 1 }`
- `{ "location.coordinates": "2dsphere" }`
- `{ source: 1, externalId: 1 }` — compound unique for deduplication
- `{ startTime: 1 }` — TTL index to auto-delete events older than 30 days

---

## 6. `bookings` Collection

```json
{
  "_id": "ObjectId",
  "travelerId": "ObjectId (ref: users)",
  "creatorId": "ObjectId (ref: users)",
  "experienceId": "ObjectId (ref: experiences)",
  "tripId": "ObjectId (nullable, ref: trips)",
  "date": "ISODate",
  "timeSlot": {
    "start": "string (HH:mm)",
    "end": "string (HH:mm)"
  },
  "groupSize": "number",
  "totalAmount": "number (cents)",
  "platformFee": "number (cents)",
  "creatorPayout": "number (cents)",
  "currency": "string",
  "status": "enum: PENDING | CONFIRMED | DECLINED | CANCELLED | COMPLETED | REFUNDED | NO_SHOW",
  "payment": {
    "stripePaymentIntentId": "string",
    "stripeTransferId": "string (nullable, for payout)",
    "status": "enum: PENDING | SUCCEEDED | FAILED | REFUNDED",
    "paidAt": "ISODate (nullable)",
    "refundedAt": "ISODate (nullable)"
  },
  "cancellation": {
    "cancelledBy": "enum: TRAVELER | CREATOR | SYSTEM (nullable)",
    "reason": "string (nullable)",
    "cancelledAt": "ISODate (nullable)",
    "refundAmount": "number (cents, nullable)"
  },
  "specialRequests": "string (nullable)",
  "injectedByAI": "boolean (true if booked from AI-generated itinerary)",
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

**Indexes:**
- `{ travelerId: 1, status: 1 }`
- `{ creatorId: 1, date: 1 }`
- `{ experienceId: 1, date: 1 }`
- `{ status: 1, date: 1 }`

---

## 7. `reviews` Collection

```json
{
  "_id": "ObjectId",
  "bookingId": "ObjectId (ref: bookings, unique)",
  "reviewerId": "ObjectId (ref: users)",
  "experienceId": "ObjectId (ref: experiences)",
  "creatorId": "ObjectId (ref: users)",
  "rating": "number (1-5)",
  "title": "string (max 100 chars)",
  "body": "string (max 1000 chars)",
  "photos": ["string (URLs)"],
  "helpfulCount": "number",
  "response": {
    "body": "string (creator reply, nullable)",
    "respondedAt": "ISODate (nullable)"
  },
  "verified": "boolean (true if booking was completed)",
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

**Indexes:**
- `{ experienceId: 1, createdAt: -1 }`
- `{ creatorId: 1, rating: 1 }`
- `{ bookingId: 1 }` — unique

---

## 8. `footfall_grid` Collection

Precomputed heatmap data for crowd intelligence.

```json
{
  "_id": "ObjectId",
  "city": "string",
  "gridCell": {
    "type": "Polygon",
    "coordinates": [["array of [lng, lat] forming polygon"]]
  },
  "centroid": {
    "type": "Point",
    "coordinates": ["longitude", "latitude"]
  },
  "hour": "number (0-23)",
  "dayOfWeek": "number (0=Sun)",
  "footfallScore": "number (0-100, normalized)",
  "crowdLevel": "enum: LOW | MODERATE | HIGH | VERY_HIGH",
  "dataPoints": "number (sample size)",
  "bestTimeToVisit": "string (e.g., 'Early morning or after 5 PM')",
  "lastComputedAt": "ISODate",
  "expiresAt": "ISODate (TTL, 1 hour)"
}
```

**Indexes:**
- `{ city: 1, hour: 1 }`
- `{ gridCell: "2dsphere" }`
- `{ centroid: "2dsphere" }`
- `{ expiresAt: 1 }` — TTL index (auto-delete expired grids)

---

## 9. `messages` Collection

Creator ↔ traveler direct messaging.

```json
{
  "_id": "ObjectId",
  "conversationId": "string (sorted pair: min(userId1,userId2)_max(userId1,userId2))",
  "senderId": "ObjectId (ref: users)",
  "recipientId": "ObjectId (ref: users)",
  "body": "string (max 2000 chars)",
  "attachments": [{
    "type": "enum: IMAGE | FILE",
    "url": "string",
    "fileName": "string"
  }],
  "readAt": "ISODate (nullable)",
  "createdAt": "ISODate"
}
```

**Indexes:**
- `{ conversationId: 1, createdAt: -1 }`
- `{ recipientId: 1, readAt: 1 }`

---

## 10. `notifications` Collection

```json
{
  "_id": "ObjectId",
  "userId": "ObjectId (ref: users)",
  "type": "enum: BOOKING_CONFIRMED | BOOKING_DECLINED | REPLAN_ALERT | REVIEW_RECEIVED | MESSAGE | PAYOUT | SYSTEM",
  "title": "string",
  "body": "string",
  "data": {
    "tripId": "ObjectId (nullable)",
    "bookingId": "ObjectId (nullable)",
    "experienceId": "ObjectId (nullable)",
    "link": "string (in-app deep link)"
  },
  "channels": {
    "inApp": "boolean",
    "email": "boolean",
    "push": "boolean"
  },
  "read": "boolean (default: false)",
  "emailSentAt": "ISODate (nullable)",
  "pushSentAt": "ISODate (nullable)",
  "createdAt": "ISODate",
  "expiresAt": "ISODate (TTL, 90 days)"
}
```

**Indexes:**
- `{ userId: 1, read: 1, createdAt: -1 }`
- `{ expiresAt: 1 }` — TTL index

---

## 11. `replan_logs` Collection

Audit trail for all dynamic replanning decisions.

```json
{
  "_id": "ObjectId",
  "tripId": "ObjectId (ref: trips)",
  "dayNumber": "number",
  "triggeredBy": "enum: WEATHER | TRAFFIC | VENUE_CLOSURE | USER_REQUEST | SYSTEM",
  "reasonCode": "string (e.g., 'STORM_ALERT', 'HEAVY_CONGESTION', 'VENUE_CLOSED')",
  "reasonDetail": "string (human-readable explanation)",
  "affectedSlots": ["string (slotIds)"],
  "diff": {
    "removed": [{
      "slotId": "string",
      "title": "string",
      "startTime": "string",
      "endTime": "string"
    }],
    "added": [{
      "slotId": "string",
      "title": "string",
      "startTime": "string",
      "endTime": "string"
    }],
    "modified": [{
      "slotId": "string",
      "field": "string",
      "oldValue": "string",
      "newValue": "string"
    }]
  },
  "previousVersion": "number",
  "newVersion": "number",
  "claudePromptTokens": "number",
  "claudeResponseTokens": "number",
  "processingTimeMs": "number",
  "accepted": "boolean (nullable, user accepted/rejected replan)",
  "createdAt": "ISODate"
}
```

**Indexes:**
- `{ tripId: 1, createdAt: -1 }`
- `{ triggeredBy: 1, createdAt: -1 }` — analytics

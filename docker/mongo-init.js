db = db.getSiblingDB('locallens');

db.createCollection('users');
db.createCollection('trips');
db.createCollection('itinerary_days');
db.createCollection('experiences');
db.createCollection('events');
db.createCollection('bookings');
db.createCollection('reviews');
db.createCollection('footfall_grid');
db.createCollection('messages');
db.createCollection('notifications');
db.createCollection('replan_logs');

// Geospatial indexes
db.users.createIndex({ "creatorProfile.location": "2dsphere" });
db.trips.createIndex({ "destination.location": "2dsphere" });
db.itinerary_days.createIndex({ "slots.activity.location.coordinates": "2dsphere" });
db.experiences.createIndex({ "location.coordinates": "2dsphere" });
db.events.createIndex({ "location.coordinates": "2dsphere" });
db.footfall_grid.createIndex({ "gridCell": "2dsphere" });
db.footfall_grid.createIndex({ "centroid": "2dsphere" });

// Unique indexes
db.users.createIndex({ "email": 1 }, { unique: true });
db.events.createIndex({ "source": 1, "externalId": 1 }, { unique: true, sparse: true });
db.reviews.createIndex({ "bookingId": 1 }, { unique: true });
db.itinerary_days.createIndex({ "tripId": 1, "dayNumber": 1 }, { unique: true });

// TTL indexes
db.footfall_grid.createIndex({ "expiresAt": 1 }, { expireAfterSeconds: 0 });
db.notifications.createIndex({ "expiresAt": 1 }, { expireAfterSeconds: 0 });

// Performance indexes
db.trips.createIndex({ "userId": 1, "status": 1 });
db.trips.createIndex({ "status": 1, "startDate": 1 });
db.bookings.createIndex({ "travelerId": 1, "status": 1 });
db.bookings.createIndex({ "creatorId": 1, "date": 1 });
db.experiences.createIndex({ "category": 1, "location.city": 1, "status": 1 });
db.experiences.createIndex({ "hyperlocal": 1, "status": 1, "rating": -1 });
db.notifications.createIndex({ "userId": 1, "read": 1, "createdAt": -1 });
db.messages.createIndex({ "conversationId": 1, "createdAt": -1 });

print('LocalLens MongoDB initialized with collections and indexes.');

# LocalLens — Docker & CI/CD Configuration

---

## Docker Compose (Local Development)

```yaml
# docker-compose.yml
version: '3.9'

services:
  # ─── MongoDB ────────────────────────────────────────────
  mongodb:
    image: mongo:7.0
    container_name: locallens-mongo
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: locallens_admin
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD:-devpassword}
      MONGO_INITDB_DATABASE: locallens
    volumes:
      - mongodb_data:/data/db
      - ./docker/mongo-init.js:/docker-entrypoint-initdb.d/init.js:ro
    healthcheck:
      test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - locallens-net

  # ─── Redis ──────────────────────────────────────────────
  redis:
    image: redis:7-alpine
    container_name: locallens-redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD:-devredispass}
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD:-devredispass}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - locallens-net

  # ─── Spring Boot Backend ────────────────────────────────
  backend:
    build:
      context: ./locallens-backend
      dockerfile: Dockerfile
    container_name: locallens-backend
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATA_MONGODB_URI: mongodb://locallens_admin:${MONGO_PASSWORD:-devpassword}@mongodb:27017/locallens?authSource=admin
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
      SPRING_DATA_REDIS_PASSWORD: ${REDIS_PASSWORD:-devredispass}
      JWT_SECRET: ${JWT_SECRET:-dev-jwt-secret-change-in-production}
      JWT_EXPIRATION_MS: 900000
      JWT_REFRESH_EXPIRATION_MS: 604800000
      CLAUDE_API_KEY: ${CLAUDE_API_KEY}
      OPENWEATHERMAP_API_KEY: ${OPENWEATHERMAP_API_KEY}
      TOMTOM_API_KEY: ${TOMTOM_API_KEY}
      GOOGLE_PLACES_API_KEY: ${GOOGLE_PLACES_API_KEY}
      EVENTBRITE_API_KEY: ${EVENTBRITE_API_KEY}
      STRIPE_SECRET_KEY: ${STRIPE_SECRET_KEY}
      STRIPE_WEBHOOK_SECRET: ${STRIPE_WEBHOOK_SECRET}
      STRIPE_PLATFORM_COMMISSION: 0.15
      SENDGRID_API_KEY: ${SENDGRID_API_KEY}
      GOOGLE_OAUTH_CLIENT_ID: ${GOOGLE_OAUTH_CLIENT_ID}
      GOOGLE_OAUTH_CLIENT_SECRET: ${GOOGLE_OAUTH_CLIENT_SECRET}
      CORS_ALLOWED_ORIGINS: http://localhost:5173,http://localhost:3000
    depends_on:
      mongodb:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    networks:
      - locallens-net

  # ─── React Frontend (Dev) ──────────────────────────────
  frontend:
    build:
      context: ./locallens-frontend
      dockerfile: Dockerfile.dev
    container_name: locallens-frontend
    ports:
      - "5173:5173"
    environment:
      VITE_API_BASE_URL: http://localhost:8080
      VITE_WS_BASE_URL: ws://localhost:8080/ws
      VITE_MAPLIBRE_STYLE: https://demotiles.maplibre.org/style.json
      VITE_STRIPE_PUBLISHABLE_KEY: ${STRIPE_PUBLISHABLE_KEY}
    volumes:
      - ./locallens-frontend/src:/app/src
      - ./locallens-frontend/public:/app/public
    depends_on:
      - backend
    networks:
      - locallens-net

volumes:
  mongodb_data:
  redis_data:

networks:
  locallens-net:
    driver: bridge
```

---

## Production Docker Compose Override

```yaml
# docker-compose.prod.yml
version: '3.9'

services:
  backend:
    build:
      context: ./locallens-backend
      dockerfile: Dockerfile
      target: production
    restart: always
    deploy:
      resources:
        limits:
          memory: 1G
          cpus: '1.0'

  frontend:
    build:
      context: ./locallens-frontend
      dockerfile: Dockerfile
      target: production
    restart: always
    ports:
      - "80:80"
      - "443:443"
```

---

## Backend Dockerfile

```dockerfile
# locallens-backend/Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew .
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine AS production
WORKDIR /app
RUN addgroup -S locallens && adduser -S locallens -G locallens
COPY --from=build /app/build/libs/*.jar app.jar
RUN chown -R locallens:locallens /app
USER locallens
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget --spider -q http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

## Frontend Dockerfile

```dockerfile
# locallens-frontend/Dockerfile
# --- Dev stage ---
FROM node:20-alpine AS dev
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host"]

# --- Build stage ---
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# --- Production stage ---
FROM nginx:alpine AS production
COPY --from=build /app/dist /usr/share/nginx/html
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## Nginx Config

```nginx
# locallens-frontend/docker/nginx.conf
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /ws {
        proxy_pass http://backend:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }

    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml;
    gzip_min_length 1000;
}
```

---

## MongoDB Init Script

```javascript
// docker/mongo-init.js
db = db.getSiblingDB('locallens');

// Create collections with validation
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
```

---

## GitHub Actions CI/CD Pipeline

```yaml
# .github/workflows/ci-cd.yml
name: LocalLens CI/CD

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  BACKEND_IMAGE: ghcr.io/${{ github.repository }}/backend
  FRONTEND_IMAGE: ghcr.io/${{ github.repository }}/frontend

jobs:
  # ─── Backend Tests ──────────────────────────────────────
  test-backend:
    runs-on: ubuntu-latest
    services:
      mongodb:
        image: mongo:7.0
        ports: ['27017:27017']
        options: >-
          --health-cmd "mongosh --eval 'db.adminCommand(\"ping\")'"
          --health-interval 10s --health-timeout 5s --health-retries 5
      redis:
        image: redis:7-alpine
        ports: ['6379:6379']
        options: >-
          --health-cmd "redis-cli ping" --health-interval 10s

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Run backend tests
        working-directory: ./locallens-backend
        run: ./gradlew test --no-daemon
        env:
          SPRING_DATA_MONGODB_URI: mongodb://localhost:27017/locallens_test
          SPRING_DATA_REDIS_HOST: localhost

      - name: Upload test reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: backend-test-reports
          path: locallens-backend/build/reports/tests/

  # ─── Frontend Tests ─────────────────────────────────────
  test-frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: locallens-frontend/package-lock.json

      - name: Install dependencies
        working-directory: ./locallens-frontend
        run: npm ci

      - name: Run linter
        working-directory: ./locallens-frontend
        run: npm run lint

      - name: Run tests
        working-directory: ./locallens-frontend
        run: npm test -- --coverage --watchAll=false

      - name: Build
        working-directory: ./locallens-frontend
        run: npm run build

  # ─── Build & Push Docker Images ─────────────────────────
  build-and-push:
    needs: [test-backend, test-frontend]
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build & push backend
        uses: docker/build-push-action@v5
        with:
          context: ./locallens-backend
          push: true
          tags: |
            ${{ env.BACKEND_IMAGE }}:latest
            ${{ env.BACKEND_IMAGE }}:${{ github.sha }}

      - name: Build & push frontend
        uses: docker/build-push-action@v5
        with:
          context: ./locallens-frontend
          target: production
          push: true
          tags: |
            ${{ env.FRONTEND_IMAGE }}:latest
            ${{ env.FRONTEND_IMAGE }}:${{ github.sha }}

  # ─── Deploy to Railway ──────────────────────────────────
  deploy:
    needs: [build-and-push]
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Deploy backend to Railway
        uses: bervProject/railway-deploy@main
        with:
          railway_token: ${{ secrets.RAILWAY_TOKEN }}
          service: locallens-backend

      - name: Deploy frontend to Railway
        uses: bervProject/railway-deploy@main
        with:
          railway_token: ${{ secrets.RAILWAY_TOKEN }}
          service: locallens-frontend
```

---

## Environment Variables Reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `MONGO_PASSWORD` | Yes | `devpassword` | MongoDB root password |
| `REDIS_PASSWORD` | Yes | `devredispass` | Redis auth password |
| `JWT_SECRET` | Yes | — | HMAC key for JWT signing (min 256-bit) |
| `CLAUDE_API_KEY` | Yes | — | Anthropic API key |
| `OPENWEATHERMAP_API_KEY` | Yes | — | OpenWeatherMap API key |
| `TOMTOM_API_KEY` | Yes | — | TomTom Traffic API key |
| `GOOGLE_PLACES_API_KEY` | Yes | — | Google Places API key |
| `EVENTBRITE_API_KEY` | Yes | — | Eventbrite OAuth token |
| `STRIPE_SECRET_KEY` | Yes | — | Stripe secret key |
| `STRIPE_PUBLISHABLE_KEY` | Yes | — | Stripe publishable key (frontend) |
| `STRIPE_WEBHOOK_SECRET` | Yes | — | Stripe webhook signing secret |
| `SENDGRID_API_KEY` | Phase 2 | — | SendGrid API key |
| `GOOGLE_OAUTH_CLIENT_ID` | Yes | — | Google OAuth 2.0 client ID |
| `GOOGLE_OAUTH_CLIENT_SECRET` | Yes | — | Google OAuth 2.0 client secret |
| `RAILWAY_TOKEN` | Deploy | — | Railway deployment token |

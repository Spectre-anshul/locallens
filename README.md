# 🌍 LocalLens — Hyperlocal Travel Intelligence Platform

A two-sided hyperlocal travel marketplace powered by AI, real-time data, and local creator networks. Replaces Google Maps + Booking.com + TripAdvisor + Notes + Flight apps with one unified dashboard.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18 + Bootstrap 5 + Deck.gl + MapLibre GL JS |
| Backend | Spring Boot 3.x (Java 17+) |
| Database | MongoDB Atlas |
| Maps | Deck.gl + MapLibre GL JS |
| Real-time | WebSockets (STOMP/SockJS) + Firebase Push |
| AI/LLM | Claude API (Anthropic) |
| Payments | Stripe Connect |
| DevOps | Docker + GitHub Actions |

## Quick Start

### Prerequisites
- Java 17+
- Node.js 20+
- Docker & Docker Compose
- MongoDB Atlas account (or local Docker)

### Run with Docker Compose
```bash
cp .env.example .env
# Fill in your API keys in .env
docker compose up -d
```

### Run Locally (Development)

**Backend:**
```bash
cd locallens-backend
./gradlew bootRun
```

**Frontend:**
```bash
cd locallens-frontend
npm install
npm run dev
```

### Access
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- API Docs: http://localhost:8080/swagger-ui.html

## Project Structure
```
locallens/
├── locallens-backend/      # Spring Boot API
├── locallens-frontend/     # React SPA
├── docker/                 # Docker support files
├── docs/                   # Architecture documentation
├── .github/workflows/      # CI/CD pipeline
├── docker-compose.yml
└── .env.example
```

## Documentation
See the `docs/` folder for complete architecture documentation:
- Architecture Overview & Diagrams
- MongoDB Schemas
- Spring Boot Services
- React Component Tree
- API Contracts
- Claude Prompt Templates
- Development Roadmap

## License
MIT

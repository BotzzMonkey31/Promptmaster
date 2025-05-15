# backend

This is the backend API server for Promptmaster, built with Spring Boot (Java 21).

## Overview
- REST API and WebSocket server for coding puzzles and versus mode
- Handles user management, matchmaking, scoring, and game state

## Project Structure
- `src/main/java/info/sup/proj/backend/`
  - `controllers/`: REST API endpoints
  - `websocket/`: WebSocket controllers (real-time game events)
  - `services/`: Business logic (matchmaking, scoring, AI, etc.)
  - `model/`: Data models (User, Game, Player, etc.)
  - `repositories/`: Spring Data JPA repositories
  - `config/`: WebSocket and app configuration
- `src/main/resources/`: Application config files

## Database Schema
See the [main README](../README.md#database-schema-summary) for a summary. Main entities:
- **User**: id, email, name, username, picture, country, elo, createdAt, updatedAt
- **Puzzle**: id, name, description, type, difficulty
- **PuzzleSession**: id, puzzle (FK), user (FK), currentCode, createdAt, lastUpdatedAt, etc.
- **SessionInteraction**: session (FK), userInput, aiTextResponse, aiCodeResponse, interaction_order

## Configuration
- Database connection and app settings: [`src/main/resources/application-prod.yml`](./src/main/resources/application-prod.yml)
- Uses SQL Server by default, can be configured for MySQL
- JPA auto-migration enabled (`ddl-auto: update`)

## Deployment
Promptmaster backend is deployed to Azure Container Apps via GitHub Actions. See the [main README](../README.md#deployment) and [`.github/workflows/azuze-deploy.yml`](../.github/workflows/azuze-deploy.yml) for details.

## Versus Mode
- Real-time head-to-head matches via WebSockets
- Matchmaking and challenge logic: `services/MatchmakingService.java`
- Scoring logic: `services/ScoreService.java`
- WebSocket endpoints: `websocket/GameWebSocketController.java`
- See [frontend/src/views/README.versus.md](../frontend/src/views/README.versus.md) for full versus mode documentation

## Setup

### Prerequisites
- Java 21+
- Maven
- SQL Server or MySQL (see config)

### Running

```sh
mvn spring-boot:run
```

Or build and run the JAR:

```sh
mvn package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

## Testing

```sh
mvn test
```

## See Also
- [Frontend README](../frontend/README.md)
- [Versus Mode](../frontend/src/views/README.versus.md) 
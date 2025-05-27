# Promptmaster

Promptmaster is an interactive, AI-powered coding game platform where users solve programming puzzles, compete in real-time versus matches, and improve their skills through engaging challenges.

## Features

- Solve AI-driven programming puzzles
- Real-time versus mode: compete head-to-head with other users
- ELO-based ranking and progression
- Live chat and collaborative features
- Modern web UI (Vue 3 + Vite) and robust backend (Spring Boot)

## Project Structure

- [`frontend/`](./frontend): Vue 3 + Vite web client ([see frontend README](./frontend/README.md))
- [`backend/`](./backend): Spring Boot Java API server ([see backend README](./backend/README.md))
- [`frontend/src/views/README.versus.md`](./frontend/src/views/README.versus.md): Versus mode documentation

## Database Schema (Summary)

Promptmaster uses a relational database (SQL Server or MySQL). Main tables/entities:

| Table                | Fields (Key)                                                                      | Description                 |
| -------------------- | --------------------------------------------------------------------------------- | --------------------------- |
| users                | id (PK), email, name, username, picture, country, elo, createdAt, updatedAt       | Registered users            |
| puzzles              | id (PK), name, description, type, difficulty                                      | Coding puzzles              |
| puzzle_sessions      | id (PK), puzzle_id (FK), user_id (FK), currentCode, createdAt, lastUpdatedAt, ... | User's progress on a puzzle |
| session_interactions | session_id (FK), userInput, aiTextResponse, aiCodeResponse, interaction_order     | Steps in a puzzle session   |

Other entities (not always persisted): Game, Player, PlayerStatus, ChatMessage, Rank.

## Deployment

Promptmaster is continuously deployed to Azure Container Apps using GitHub Actions. On every push to `main`:

- Backend is built with Maven, frontend with npm
- Docker images are built and pushed to Azure Container Registry
- Azure CLI updates the running containers for both frontend and backend

See [`.github/workflows/azuze-deploy.yml`](.github/workflows/azuze-deploy.yml) for details.

## Configuration

- Backend: See [`backend/src/main/resources/application-prod.yml`](./backend/src/main/resources/application-prod.yml) for DB and app config
- Frontend: See [`frontend/README.md`](./frontend/README.md) for environment and build info

### Manual Setup

1. **Backend**
   - See [backend/README.md](./backend/README.md)
2. **Frontend**
   - See [frontend/README.md](./frontend/README.md)

## Versus Mode

Promptmaster features a real-time versus mode for head-to-head coding battles. See [`frontend/src/views/README.versus.md`](./frontend/src/views/README.versus.md) for details.

## Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you would like to change.

## FAQ / Troubleshooting

- See backend/frontend READMEs for troubleshooting tips

## License

[MIT](LICENSE)

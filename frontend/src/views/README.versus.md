# Versus Mode (Head-to-Head)

Promptmaster's Versus Mode lets two players compete in real-time coding battles. This document explains how it works for users and developers.

---

## Features
- **Real-time matchmaking**: Find random opponents or challenge friends
- **Lobby system**: See available players, send/accept challenges
- **Head-to-head gameplay**: Compete on the same puzzle, see live progress
- **Scoring & ELO**: Win, lose, or draw and gain/lose ELO points

---

## How It Works

### 1. Lobby & Matchmaking
- Enter the lobby (see `Lobby.vue`)
- Find a random opponent or challenge a specific player
- Accept or decline incoming challenges
- When matched, both players are sent to the game

### 2. Game Flow
- Both players receive the same puzzle
- Each submits code and interacts with the AI assistant
- Progress and scores are updated live
- When both finish (or time runs out), scores are compared
- Winner gains ELO, loser loses ELO, draw is possible

### 3. Scoring
- Based on:
  - Time to solve
  - Code efficiency
  - Token usage
  - AI-evaluated correctness & quality
- See backend `ScoreService.java` for details

---

## Frontend Components
- `Lobby.vue`: Player list, matchmaking, challenge system
- `Game.vue`: Versus gameplay UI, code editor, live scores, result popup
- Uses Pinia store (`stores/game`), WebSockets for real-time updates

## Backend Components
- `MatchmakingService.java`: Handles lobby, matchmaking, challenges
- `GameWebSocketController.java`: WebSocket endpoints for game events
- `ScoreService.java`: Calculates scores
- `User`, `Player`, `Game` models: Represent users and game state

---

## Developer Notes
- WebSocket endpoints are under `/game` (see backend config)
- ELO and rank thresholds are in both frontend and backend
- Extendable for more than 2 players or custom rules
- For testing, use two browser windows or accounts

---

## See Also
- [Frontend README](../../README.md)
- [Backend README](../../../backend/README.md) 
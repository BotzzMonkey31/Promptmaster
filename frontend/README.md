# frontend

This is the web client for Promptmaster, built with Vue 3 and Vite.

## Features
- Modern, responsive UI for coding puzzles and versus mode
- Real-time matchmaking and gameplay using WebSockets
- ELO-based ranking and user profiles
- Integrated code editor and AI assistant

## Project Structure
- `src/views/Lobby.vue`: Game lobby, matchmaking, and challenges
- `src/views/Game.vue`: Versus gameplay (head-to-head mode)
- `src/views/README.versus.md`: [Versus mode documentation](./src/views/README.versus.md)
- `src/components/`: Shared UI components
- `src/services/`: API and WebSocket clients
- `src/stores/`: Pinia state management
- `src/types/`: TypeScript types
- `src/router/`: Vue Router configuration
- `src/assets/`: Static assets

## Database Schema & Backend
Promptmaster uses a relational database for users, puzzles, sessions, and more. See the [main README](../README.md#database-schema-summary) for a summary and the [backend README](../backend/README.md) for details.

## Deployment
Promptmaster is deployed to Azure Container Apps using GitHub Actions. See the [main README](../README.md#deployment) for details.

## Versus Mode
Promptmaster features a real-time versus mode for head-to-head coding battles. See [`src/views/README.versus.md`](./src/views/README.versus.md) for details on gameplay, matchmaking, and scoring.

## Recommended IDE Setup

[VSCode](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

## Type Support for `.vue` Imports in TS

TypeScript cannot handle type information for `.vue` imports by default, so we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we need [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) to make the TypeScript language service aware of `.vue` types.

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

### Type-Check, Compile and Minify for Production

```sh
npm run build
```

### Run Unit Tests with [Vitest](https://vitest.dev/)

```sh
npm run test:unit
```

### Run End-to-End Tests with [Playwright](https://playwright.dev)

```sh
# Install browsers for the first run
npx playwright install

# When testing on CI, must build the project first
npm run build

# Runs the end-to-end tests
npm run test:e2e
# Runs the tests only on Chromium
npm run test:e2e -- --project=chromium
# Runs the tests of a specific file
npm run test:e2e -- tests/example.spec.ts
# Runs the tests in debug mode
npm run test:e2e -- --debug
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```

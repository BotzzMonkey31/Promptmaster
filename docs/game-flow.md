# Complete Game Flow: Puzzle Submission to Next Round

## Overview
This document outlines the complete flow from when a user submits a puzzle solution to the start of the next round in the versus mode, covering both frontend and backend interactions.

## 1. Frontend Puzzle Submission Flow

### Initial Submission (`handleSubmit`)
1. User clicks "Submit Solution" triggering `handleSubmit()`
2. Initial checks:
   - Verifies submission is not already in progress (`isSubmitting`)
   - Confirms editor exists and game state is valid
   - Ensures puzzle and current player data exists

### Frontend Connection Handling
1. Checks WebSocket connection status
2. If disconnected:
   - Attempts reconnection via `gameStore.reconnectToGame()`
   - Shows appropriate status messages to user
3. Uses a dual submission strategy:
   - Primary: WebSocket submission
   - Fallback: REST API submission (if enabled)

## 2. Backend Submission Processing

### WebSocket Controller (`GameController`)
1. Receives submission through `/game/{gameId}/submit` endpoint
2. Validates request parameters:
   - Code
   - Player ID
   - Game ID
3. Retrieves game instance and verifies player participation

### Solution Evaluation (`GameService`)
1. Updates current code in game state
2. Generates AI evaluation prompt with:
   - Puzzle name
   - Description
   - Submitted code
3. Calls AI service for code evaluation
4. Calculates final score based on multiple factors:
   ```java
   totalScore = (
       correctnessScore * 0.4 +
       qualityScore * 0.3 +
       timeBonus * 0.3
   )
   ```

### AI Evaluation (`AiService`)
1. Evaluates code using AI model with specific criteria:
   - Correctness (0-100)
   - Code quality (0-100)
2. Returns evaluation in JSON format
3. Includes special handling for different puzzle types (e.g., BY_PASS puzzles)

### Score Calculation (`ScoreService`)
1. Time Bonus Calculation:
   ```java
   if (timeInSeconds <= perfectTime) return 100;
   else if (timeInSeconds <= goodTime) return 90;
   else if (timeInSeconds <= okayTime) return 80;
   else if (timeInSeconds <= maxTime) return 70;
   ```
2. Quality Score Components:
   - Code correctness (40%)
   - Code quality (30%)
   - Time bonus (30%)

## 3. Frontend Score Processing

### Score Display (`handleScoreUpdate`)
1. Receives score data and creates score details:
   ```typescript
   scoreDetails = {
       totalScore: score,
       timeScore: timeBonus,
       efficiencyScore: qualityScore,
       tokenScore: correctnessScore,
       timeSeconds: 300 - timeRemaining,
       hasFailed: false
   }
   ```
2. Shows score popup after 300ms delay
3. Displays detailed breakdown of scoring components

## 4. Backend Round Completion

### Round Management (`Game`)
1. `markPlayerCompleted`:
   - Updates player completion status
   - Checks if all players have completed
2. `startNextRoundWithExplicitNumber`:
   - Validates round number
   - Updates current round
   - Resets round timer
   - Resets player states

### Game State Updates
1. Updates player scores in game state
2. Broadcasts game state updates to all players
3. Handles round transitions and game completion

## 5. Backend Round Progression

### Next Round Request Handling (`GameService`)
1. Validates game and player state:
   ```java
   if (!game.hasPlayer(playerId)) {
       throw new IllegalArgumentException("Player is not part of this game");
   }
   ```

### Round Synchronization
1. Uses round-specific keys to prevent race conditions:
   ```java
   String roundKey = gameId + ":" + currentRound;
   String playerRequestKey = roundKey + ":" + playerId;
   ```
2. Tracks player readiness:
   - Records player's next round request
   - Prevents duplicate round advancements

### Round Advancement Logic
1. Round number validation:
   ```java
   if (currentRound >= totalRounds) {
       return game;
   }
   ```
2. Puzzle selection:
   - Gets new random puzzle via `getRandomPuzzle()`
   - Validates puzzle availability
3. Round initialization:
   ```java
   game.startNextRoundWithExplicitNumber(nextPuzzle, nextRound);
   ```

### Game State Reset
1. Player state reset:
   ```java
   private void resetPlayersForNewRound() {
       // Reset completion status
       playerStatus.values().forEach(status -> 
           status.setHasCompleted(false)
       );
       
       // Reset player code
       players.forEach(player -> 
           playerCode.put(player.getId(), "")
       );
       
       // Switch starting player (round-robin)
       currentTurn = players.get(
           (currentRound - 1) % players.size()
       ).getId();
   }
   ```
2. Round timer reset:
   - Updates `roundStartTime` to current timestamp
   - Resets all player completion flags

### Concurrency Protection
1. Uses synchronized methods for critical sections
2. Implements cleanup after round advancement:
   ```java
   try {
       // Round advancement logic
   } finally {
       roundAdvancingMap.remove(roundKey);
   }
   ```

## 6. Frontend Round Transition

### Round Completion (`closeScorePopup`)
1. Closes score popup
2. Marks current puzzle as completed
3. Checks for more rounds:
   ```typescript
   currentRound < totalRounds
   ```

### Next Round Preparation
1. If rounds remain:
   - Resets timer to 300 seconds
   - Clears editor content
   - Resets text bubble
   - Clears existing timer intervals

### Starting Next Round
1. Calls `startNextRound()` from game store
2. Updates game notification
3. Initializes new timer after 500ms delay
4. Sets up new round environment

## 7. WebSocket Communication

### Message Types
1. Game State Updates:
   ```typescript
   {
       type: "GAME_STATE",
       payload: Game
   }
   ```
2. Score Updates:
   ```typescript
   {
       type: "SCORE_UPDATE",
       payload: {
           score: number,
           timeBonus: number,
           qualityScore: number
       }
   }
   ```
3. Round Completion:
   ```typescript
   {
       type: "ROUND_COMPLETE",
       payload: {
           roundNumber: number,
           nextPuzzle: Puzzle
       }
   }
   ```

## 8. Error Handling & Recovery

### Frontend Recovery
1. Connection failures:
   - Automatic reconnection attempts
   - Fallback to REST API
2. Score verification:
   - Multiple watchers for score updates
   - Timeout-based verification

### Backend Recovery
1. Transaction management for state updates
2. Fallback evaluation mechanisms
3. Round synchronization protection

## Notes
- All timeouts and intervals are cleared on component unmount
- Multiple safeguards ensure score updates aren't missed
- Fallback systems ensure game progression even with connection issues
- Round transitions are protected against race conditions
- Score calculations include multiple factors for fairness 
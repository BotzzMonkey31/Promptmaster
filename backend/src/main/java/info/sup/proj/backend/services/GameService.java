package info.sup.proj.backend.services;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import info.sup.proj.backend.model.Game;
import info.sup.proj.backend.model.Player;
import info.sup.proj.backend.model.Puzzle;
import info.sup.proj.backend.repositories.PuzzleRepository;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

@Service
public class GameService {
    private final Map<String, Game> activeGames = new ConcurrentHashMap<>();
    
    @Autowired
    private PuzzleRepository puzzleRepository;

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private AiService aiService;

    public Game createGame(Player player1, Player player2) {
        // Get a random puzzle for the first round
        var puzzle = getRandomPuzzle();
        if (puzzle == null) {
            throw new IllegalStateException("No puzzles available");
        }

        var game = new Game(
            UUID.randomUUID().toString(),
            List.of(player1, player2),
            puzzle,
            3 // Total rounds
        );

        activeGames.put(game.getId(), game);
        return game;
    }

    public Game getGame(String gameId) {
        return activeGames.get(gameId);
    }

    public Map<String, Object> submitSolution(String playerId, String code) {
        // Find the game this player is in
        var game = findGameByPlayerId(playerId);
        if (game == null) {
            throw new IllegalStateException("Player not in any active game");
        }

        // Update the player's code in the game
        game.updateCurrentCode(playerId, code);

        // Get the current puzzle
        Puzzle currentPuzzle = game.getCurrentPuzzle();
        
        // Create evaluation prompt
        String evaluationPrompt = String.format(
            "Please evaluate this code solution for the following puzzle:\n" +
            "Puzzle: %s\n" +
            "Description: %s\n\n" +
            "Evaluate the following aspects on a scale from 0-100:\n" +
            "1. Correctness: Does the code correctly solve the problem as described?\n" +
            "2. Code quality: Is the code well-structured, efficient, and following best practices?\n\n" +
            "Respond in JSON format: {\"correctness\": X, \"quality\": Y} where X and Y are scores from 0-100.",
            currentPuzzle.getName(),
            currentPuzzle.getDescription()
        );

        // Get AI evaluation
        String evaluationResponse = aiService.getCodeEvaluation(evaluationPrompt, code, currentPuzzle.getType());
        Map<String, Integer> scores = parseAiEvaluation(evaluationResponse);

        // Calculate total score
        int correctnessScore = scores.get("correctness");
        int qualityScore = scores.get("quality");
        int timeBonus = calculateTimeBonus(game.getRoundStartTime());
        
        int totalScore = (int) Math.round(
            correctnessScore * 0.4 +
            qualityScore * 0.3 +
            timeBonus * 0.3
        );

        // Update player's score
        game.updatePlayerScore(playerId, totalScore);

        var result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("score", totalScore);
        result.put("correctnessScore", correctnessScore);
        result.put("qualityScore", qualityScore);
        result.put("timeBonus", timeBonus);
        result.put("playerId", playerId);
        
        // Update game status and broadcast it
        Map<String, Object> gameState = new HashMap<>();
        gameState.put("type", "GAME_STATE");
        gameState.put("payload", game);
        
        return result;
    }

    private int calculateTimeBonus(long startTime) {
        long timeTaken = System.currentTimeMillis() - startTime;
        long timeInSeconds = timeTaken / 1000;
        
        // Base time limits (in seconds) for different scores
        final int perfectTime = 60;  // 1 minute
        final int goodTime = 120;    // 2 minutes
        final int okayTime = 180;    // 3 minutes
        final int maxTime = 300;     // 5 minutes
        
        if (timeInSeconds <= perfectTime) {
            return 100;
        } else if (timeInSeconds <= goodTime) {
            return 90;
        } else if (timeInSeconds <= okayTime) {
            return 80;
        } else if (timeInSeconds <= maxTime) {
            return 70;
        } else {
            return Math.max(40, 70 - (int)((timeInSeconds - maxTime) / 30));
        }
    }

    private Map<String, Integer> parseAiEvaluation(String evaluationResponse) {
        Map<String, Integer> result = new HashMap<>();
        
        try {
            if (evaluationResponse.contains("\"correctness\":")) {
                int correctnessIndex = evaluationResponse.indexOf("\"correctness\":");
                int commaIndex = evaluationResponse.indexOf(",", correctnessIndex);
                String correctnessStr = evaluationResponse.substring(correctnessIndex + 14, commaIndex).trim();
                result.put("correctness", Integer.parseInt(correctnessStr));
            }
            
            if (evaluationResponse.contains("\"quality\":")) {
                int qualityIndex = evaluationResponse.indexOf("\"quality\":");
                int endIndex = evaluationResponse.indexOf("}", qualityIndex);
                String qualityStr = evaluationResponse.substring(qualityIndex + 10, endIndex).trim();
                result.put("quality", Integer.parseInt(qualityStr));
            }
        } catch (Exception e) {
            result.put("correctness", 75);
            result.put("quality", 70);
        }
        
        if (!result.containsKey("correctness")) result.put("correctness", 75);
        if (!result.containsKey("quality")) result.put("quality", 70);
        
        return result;
    }

    public Game completePuzzle(String playerId) {
        Game game = findGameByPlayerId(playerId);
        if (game == null) {
            throw new IllegalStateException("Player not in any active game");
        }
        
        // Mark this specific player as completed, but don't affect other players
        game.markPlayerCompleted(playerId);
        
        // Check if all players have completed their puzzles
        if (game.allPlayersCompleted()) {
            // If this was the final round, end the game
            if (game.getCurrentRound() >= game.getTotalRounds()) {
                game.endGame();
            }
        }
        
        return game;
    }

    public Game forfeitGame(String playerId) {
        var game = findGameByPlayerId(playerId);
        if (game == null) {
            throw new IllegalStateException("Player not in any active game");
        }

        game.forfeit(playerId);
        return game;
    }

    // Make this method public so it can be called from the controller
    public Game findGameByPlayerId(String playerId) {
        return activeGames.values().stream()
            .filter(game -> game.hasPlayer(playerId))
            .findFirst()
            .orElse(null);
    }

    // Add new method to list all active games
    public List<Game> listAllGames() {
        return new ArrayList<>(activeGames.values());
    }

    // A map to track games that are currently in the process of advancing rounds
    private final Map<String, Boolean> roundAdvancingMap = new ConcurrentHashMap<>();
    
    // A map to track which players have requested next round for a game
    private final Map<String, Boolean> playerNextRoundRequestMap = new ConcurrentHashMap<>();
    
    /**
     * Clean, simplified approach to round advancement that ensures each round is only advanced once.
     */
    public synchronized Game startNextRound(String gameId, String playerId) {
        // Get the game from the active games map
        Game game = activeGames.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        
        // Check if the player is part of this game
        if (!game.hasPlayer(playerId)) {
            throw new IllegalArgumentException("Player is not part of this game");
        }
        
        int currentRound = game.getCurrentRound();
        int totalRounds = game.getTotalRounds();
        
        // Enforce valid round progression
        if (currentRound <= 0) {
            currentRound = 1;
        }
        
        if (currentRound >= totalRounds) {
            return game;
        }
        
        // Generate a unique key for this game and round
        String roundKey = gameId + ":" + currentRound;
        
        // Record this player's request
        String playerRequestKey = roundKey + ":" + playerId;
        playerNextRoundRequestMap.put(playerRequestKey, true);
        
        // Check if this round is already being advanced
        if (Boolean.TRUE.equals(roundAdvancingMap.get(roundKey))) {
            return game;
        }
        
        // Mark this round as being advanced
        roundAdvancingMap.put(roundKey, true);
        
        try {
            // Ensure all players are marked as completed for this round
            if (!game.allPlayersCompleted()) {
                for (Player player : game.getPlayers()) {
                    game.markPlayerCompleted(player.getId());
                }
            }

            // Get the next puzzle
            Puzzle nextPuzzle = getRandomPuzzle();
            if (nextPuzzle == null) {
                throw new IllegalStateException("No puzzles available");
            }
            
            // Calculate the next round number explicitly 
            int nextRound = currentRound + 1;
            
            // Validate the next round number
            if (nextRound > totalRounds) {
                nextRound = totalRounds;
            }
            
            // Start the next round with an explicit round number to prevent any skipping
            game.startNextRoundWithExplicitNumber(nextPuzzle, nextRound);
            
            return game;
        } finally {
            // Clean up the tracking maps to prevent memory leaks
            roundAdvancingMap.remove(roundKey);
            // We keep the player request map entries for auditing purposes
        }
    }

    private Puzzle getRandomPuzzle() {
        var puzzles = puzzleRepository.findByType(Puzzle.Type.Multi_Step);
        if (puzzles.isEmpty()) {
            return null;
        }
        
        // Find the File Word Counter puzzle
        return puzzles.stream()
            .filter(puzzle -> puzzle.getName().equals("File Word Counter"))
            .findFirst()
            .orElse(null);
    }
} 
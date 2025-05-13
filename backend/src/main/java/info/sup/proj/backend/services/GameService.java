package info.sup.proj.backend.services;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import info.sup.proj.backend.model.Game;
import info.sup.proj.backend.model.Player;
import info.sup.proj.backend.model.Puzzle;
import info.sup.proj.backend.model.PlayerStatus;
import info.sup.proj.backend.dto.GamePuzzleDTO;
import info.sup.proj.backend.repositories.PuzzleRepository;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Random;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
        result.put("totalScore", totalScore);

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
        var game = findGameByPlayerId(playerId);
        if (game == null) {
            throw new IllegalStateException("Player not in any active game");
        }

        game.markPlayerCompleted(playerId);

        // If all players completed, advance to next round
        if (game.allPlayersCompleted()) {
            advanceToNextRound(game);
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

    private Game findGameByPlayerId(String playerId) {
        return activeGames.values().stream()
            .filter(game -> game.hasPlayer(playerId))
            .findFirst()
            .orElse(null);
    }

    private void advanceToNextRound(Game game) {
        if (game.getCurrentRound() >= game.getTotalRounds()) {
            game.endGame();
            return;
        }

        var puzzle = getRandomPuzzle();
        if (puzzle == null) {
            throw new IllegalStateException("No puzzles available");
        }

        game.startNextRound(puzzle);
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
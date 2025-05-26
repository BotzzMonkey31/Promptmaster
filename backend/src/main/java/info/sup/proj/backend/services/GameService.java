package info.sup.proj.backend.services;

import org.springframework.stereotype.Service;
import info.sup.proj.backend.model.Game;
import info.sup.proj.backend.model.Player;
import info.sup.proj.backend.model.Puzzle;
import info.sup.proj.backend.repositories.PuzzleRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {
    private final Map<String, Game> activeGames = new ConcurrentHashMap<>();

    private final PuzzleRepository puzzleRepository;

    private final AiService aiService;

    private static final String CORRECTNESS = "correctness";
    private static final String QUALITY = "quality";
    private final Random r = new Random();

    public GameService(PuzzleRepository puzzleRepository, AiService aiService) {
        this.puzzleRepository = puzzleRepository;
        this.aiService = aiService;
    }

    public Game createGame(Player player1, Player player2) {
        var puzzle = getRandomPuzzle();
        if (puzzle == null) {
            throw new IllegalStateException("No puzzles available");
        }

        var game = new Game(
            UUID.randomUUID().toString(),
            List.of(player1, player2),
            puzzle,
            3
        );

        activeGames.put(game.getId(), game);
        return game;
    }

    public Game getGame(String gameId) {
        return activeGames.get(gameId);
    }

    public Map<String, Object> submitSolution(String playerId, String code) {
        var game = findGameByPlayerId(playerId);
        if (game == null) {
            throw new IllegalStateException("Player not in any active game");
        }

        game.updateCurrentCode(playerId, code);

        Puzzle currentPuzzle = game.getCurrentPuzzle();
        
        String evaluationPrompt = String.format(
            """
            Please evaluate this code solution for the following puzzle:
            Puzzle: %s
            "Description: %s +
            "Evaluate the following aspects on a scale from 0-100:
            "1. Correctness: Does the code correctly solve the problem as described?
            "2. Code quality: Is the code well-structured, efficient, and following best practices?
            "Respond in JSON format: {correctness: X, quality: Y} where X and Y are scores from 0-100.""",
            currentPuzzle.getName(),
            currentPuzzle.getDescription()
        );

        String evaluationResponse = aiService.getCodeEvaluation(evaluationPrompt, code, currentPuzzle.getType());
        Map<String, Integer> scores = parseAiEvaluation(evaluationResponse);

        int correctnessScore = scores.get(CORRECTNESS);
        int qualityScore = scores.get(QUALITY);
        int timeBonus = calculateTimeBonus(game.getRoundStartTime());
        
        int totalScore = (int) Math.round(
            correctnessScore * 0.4 +
            qualityScore * 0.3 +
            timeBonus * 0.3
        );

        game.updatePlayerScore(playerId, totalScore);

        var result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("score", totalScore);
        result.put("correctnessScore", correctnessScore);
        result.put("qualityScore", qualityScore);
        result.put("timeBonus", timeBonus);
        result.put("playerId", playerId);
        
        Map<String, Object> gameState = new HashMap<>();
        gameState.put("type", "GAME_STATE");
        gameState.put("payload", game);
        
        return result;
    }

    private int calculateTimeBonus(long startTime) {
        long timeTaken = System.currentTimeMillis() - startTime;
        long timeInSeconds = timeTaken / 1000;
        
        final int perfectTime = 60;
        final int goodTime = 120;
        final int okayTime = 180;
        final int maxTime = 300;
        
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
                result.put(CORRECTNESS, Integer.parseInt(correctnessStr));
            }
            
            if (evaluationResponse.contains("\"quality\":")) {
                int qualityIndex = evaluationResponse.indexOf("\"quality\":");
                int endIndex = evaluationResponse.indexOf("}", qualityIndex);
                String qualityStr = evaluationResponse.substring(qualityIndex + 10, endIndex).trim();
                result.put(QUALITY, Integer.parseInt(qualityStr));
            }
        } catch (Exception e) {
            result.put(CORRECTNESS, 75);
            result.put(QUALITY, 70);
        }
        
        result.computeIfAbsent(CORRECTNESS, k -> 75);
        result.computeIfAbsent(QUALITY, k -> 70);
        
        return result;
    }

    public Game completePuzzle(String playerId) {
        Game game = findGameByPlayerId(playerId);
        if (game == null) {
            throw new IllegalStateException("Player not in any active game");
        }
        
        game.markPlayerCompleted(playerId);
            if (game.allPlayersCompleted() && game.getCurrentRound() >= game.getTotalRounds()) {
                game.endGame();
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

    public Game findGameByPlayerId(String playerId) {
        return activeGames.values().stream()
            .filter(game -> game.hasPlayer(playerId))
            .findFirst()
            .orElse(null);
    }

    public List<Game> listAllGames() {
        return new ArrayList<>(activeGames.values());
    }

    private final Map<String, Boolean> roundAdvancingMap = new ConcurrentHashMap<>();
    
    private final Map<String, Boolean> playerNextRoundRequestMap = new ConcurrentHashMap<>();
    
    public synchronized Game startNextRound(String gameId, String playerId) {
        Game game = activeGames.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        
        if (!game.hasPlayer(playerId)) {
            throw new IllegalArgumentException("Player is not part of this game");
        }
        
        int currentRound = game.getCurrentRound();
        int totalRounds = game.getTotalRounds();
        
        if (currentRound <= 0) {
            currentRound = 1;
        }
        
        if (currentRound >= totalRounds) {
            return game;
        }
        
        String roundKey = gameId + ":" + currentRound;
        
        String playerRequestKey = roundKey + ":" + playerId;
        playerNextRoundRequestMap.put(playerRequestKey, true);
        
        if (Boolean.TRUE.equals(roundAdvancingMap.get(roundKey))) {
            return game;
        }
        
        roundAdvancingMap.put(roundKey, true);
        
        try {
            if (!game.allPlayersCompleted()) {
                for (Player player : game.getPlayers()) {
                    game.markPlayerCompleted(player.getId());
                }
            }

            Puzzle nextPuzzle = getRandomPuzzle();
            if (nextPuzzle == null) {
                throw new IllegalStateException("No puzzles available");
            }
            
            int nextRound = currentRound + 1;
            
            if (nextRound > totalRounds) {
                nextRound = totalRounds;
            }
            
            game.startNextRoundWithExplicitNumber(nextPuzzle, nextRound);
            
            return game;
        } finally {
            roundAdvancingMap.remove(roundKey);
        }
    }

    private Puzzle getRandomPuzzle() {
        var puzzles = puzzleRepository.findAll();
        if (puzzles.isEmpty()) {
            return null;
        }
        int randomIndex = (this.r.nextInt(puzzles.size()-1));
        return puzzles.get(randomIndex);
    }
} 
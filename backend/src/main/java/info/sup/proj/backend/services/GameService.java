package info.sup.proj.backend.services;

import org.springframework.stereotype.Service;
import info.sup.proj.backend.model.Game;
import info.sup.proj.backend.model.Player;
import info.sup.proj.backend.model.Puzzle;
import info.sup.proj.backend.repositories.PuzzleRepository;
import info.sup.proj.backend.events.GameStateChangeEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import info.sup.proj.backend.model.User;
import info.sup.proj.backend.repositories.UserRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class GameService {
    private final Map<String, Game> activeGames = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> gameTimers = new ConcurrentHashMap<>();
    private final PuzzleRepository puzzleRepository;
    private final AiService aiService;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final ScheduledExecutorService scheduler;
    private UserRepository userRepository;

    private static final String CORRECTNESS = "correctness";
    private static final String QUALITY = "quality";
    private static final int ROUND_TIME_LIMIT = 300; // 5 minutes
    private final Random r = new Random();

    public GameService(
        PuzzleRepository puzzleRepository, 
        AiService aiService,
        ApplicationEventPublisher eventPublisher,
        SimpMessagingTemplate messagingTemplate,
        ScheduledExecutorService scheduler,
        UserRepository userRepository
    ) {
        this.puzzleRepository = puzzleRepository;
        this.aiService = aiService;
        this.eventPublisher = eventPublisher;
        this.messagingTemplate = messagingTemplate;
        this.scheduler = scheduler;
        this.userRepository = userRepository;
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
        startRoundTimer(game.getId());
        publishGameState(game);
        return game;
    }

    private void startRoundTimer(String gameId) {
        stopRoundTimer(gameId);
        
        ScheduledFuture<?> timer = scheduler.schedule(() -> {
            Game game = getGame(gameId);
            if (game != null && !game.isEnded()) {
                handleRoundTimeout(game);
            }
        }, ROUND_TIME_LIMIT, TimeUnit.SECONDS);
        
        gameTimers.put(gameId, timer);
    }

    private void stopRoundTimer(String gameId) {
        ScheduledFuture<?> timer = gameTimers.remove(gameId);
        if (timer != null) {
            timer.cancel(false);
        }
    }

    private void handleRoundTimeout(Game game) {
        game.getPlayers().forEach(player -> {
            if (!game.hasPlayerCompleted(player.getId())) {
                completePuzzle(player.getId());
            }
        });
        
        if (game.getCurrentRound() < game.getTotalRounds()) {
            startNextRound(game.getId());
        } else {
            endGame(game);
        }
    }

    private Puzzle getNextDifferentPuzzle(Game game) {
        Puzzle currentPuzzle = game.getCurrentPuzzle();
        Puzzle nextPuzzle;
        int attempts = 0;
        do {
            nextPuzzle = getRandomPuzzle();
            attempts++;
        } while (nextPuzzle != null && 
                 currentPuzzle != null && 
                 nextPuzzle.getId().equals(currentPuzzle.getId()) && 
                 attempts < 5);
        
        if (nextPuzzle == null || (currentPuzzle != null && nextPuzzle.getId().equals(currentPuzzle.getId()))) {
            throw new IllegalStateException("Could not find a different puzzle");
        }
        return nextPuzzle;
    }

    public void startNextRound(String gameId) {
        Game game = getGame(gameId);
        if (game == null || game.isEnded()) {
            return;
        }

        Puzzle nextPuzzle = getNextDifferentPuzzle(game);
        game.startNextRound(nextPuzzle);
        startRoundTimer(gameId);
        publishGameState(game);
    }

    public Game startNextRound(String gameId, String playerId) {
        Game game = getGame(gameId);
        if (game == null || !game.hasPlayer(playerId)) {
            throw new IllegalStateException("Game not found or player not in game");
        }

        if (game.isEnded()) {
            return game;
        }

        Puzzle nextPuzzle = getNextDifferentPuzzle(game);
        game.startNextRound(nextPuzzle);
        startRoundTimer(gameId);
        publishGameState(game);
        return game;
    }

    private void endGame(Game game) {
        game.endGame();
        stopRoundTimer(game.getId());
        updatePlayerElo(game);
        publishGameState(game);

        scheduler.schedule(() -> activeGames.remove(game.getId()), 5, TimeUnit.MINUTES);
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
        publishGameState(game);

        Puzzle currentPuzzle = game.getCurrentPuzzle();
        
        String evaluationPrompt = String.format(
            """
            Please evaluate this code solution for the following puzzle:
            Puzzle: %s
            Description: %s
            Evaluate the following aspects on a scale from 0-100:
            1. Correctness: Does the code correctly solve the problem as described?
            2. Code quality: Is the code well-structured, efficient, and following best practices?
            Respond in JSON format: {correctness: X, quality: Y} where X and Y are scores from 0-100.""",
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
        publishGameState(game);

        var result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("score", totalScore);
        result.put("correctnessScore", correctnessScore);
        result.put("qualityScore", qualityScore);
        result.put("timeBonus", timeBonus);
        result.put("playerId", playerId);
        
        // Send individual score update to player
        messagingTemplate.convertAndSendToUser(
            playerId,
            "/queue/game",
            result
        );
        
        return result;
    }

    private void publishGameState(Game game) {
        GameStateChangeEvent event = new GameStateChangeEvent(this, game);
        eventPublisher.publishEvent(event);
        
        // Broadcast game state to all players
        messagingTemplate.convertAndSend(
            "/topic/game/" + game.getId(),
            Map.of("type", "GAME_STATE", "payload", game)
        );
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
        
        // Send completion status to all players
        Map<String, Object> completionStatus = new HashMap<>();
        completionStatus.put("type", "PLAYER_COMPLETION");
        completionStatus.put("playerId", playerId);
        completionStatus.put("allCompleted", game.allPlayersCompleted());
        
        messagingTemplate.convertAndSend(
            "/topic/game/" + game.getId(),
            completionStatus
        );
        
        publishGameState(game);
        
        if (game.allPlayersCompleted()) {
            if (game.getCurrentRound() >= game.getTotalRounds()) {
                endGame(game);
            } else {
                // Add a small delay before starting the next round to ensure proper synchronization
                scheduler.schedule(() -> {
                    Puzzle nextPuzzle = getNextDifferentPuzzle(game);
                    if (nextPuzzle != null) {
                        game.startNextRoundWithExplicitNumber(nextPuzzle, game.getCurrentRound() + 1);
                        startRoundTimer(game.getId());
                        publishGameState(game);
                    }
                }, 2, TimeUnit.SECONDS);
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
        endGame(game);
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

    private Puzzle getRandomPuzzle() {
        var puzzles = puzzleRepository.findAll();
        if (puzzles.isEmpty()) {
            return null;
        }
        int randomIndex = (this.r.nextInt(puzzles.size()));
        return puzzles.get(randomIndex);
    }

    public Game initializeGameWithPuzzle(String gameId) {
        Game game = getGame(gameId);
        if (game == null) {
            throw new IllegalStateException("Game not found: " + gameId);
        }

        if (game.getCurrentPuzzle() == null) {
            Puzzle puzzle = getRandomPuzzle();
            game.startNextRound(puzzle);
        }

        return game;
    }

    private void updatePlayerElo(Game game) {
        if (game.getPlayers().size() != 2) return;
        
        Player player1 = game.getPlayers().get(0);
        Player player2 = game.getPlayers().get(1);
        
        int score1 = game.getPlayerStatus().get(player1.getId()).getScore();
        int score2 = game.getPlayerStatus().get(player2.getId()).getScore();
        
        User user1 = userRepository.findById(Long.parseLong(player1.getId()))
            .orElseThrow(() -> new IllegalStateException("User not found: " + player1.getId()));
        User user2 = userRepository.findById(Long.parseLong(player2.getId()))
            .orElseThrow(() -> new IllegalStateException("User not found: " + player2.getId()));
        
        int eloChange;
        if (score1 > score2) {
            eloChange = 25;
            user1.setElo(user1.getElo() + eloChange);
            user2.setElo(user2.getElo() - 15);
        } else if (score2 > score1) {
            eloChange = 25;
            user2.setElo(user2.getElo() + eloChange);
            user1.setElo(user1.getElo() - 15);
        }
        
        userRepository.save(user1);
        userRepository.save(user2);
        
        // Notify players of ELO changes
        for (Player player : game.getPlayers()) {
            User user = player.getId().equals(user1.getId()) ? user1 : user2;
            Map<String, Object> eloUpdate = new HashMap<>();
            eloUpdate.put("type", "ELO_UPDATE");
            eloUpdate.put("newElo", user.getElo());
            
            messagingTemplate.convertAndSendToUser(
                player.getId(),
                "/queue/game",
                eloUpdate
            );
        }
    }
} 
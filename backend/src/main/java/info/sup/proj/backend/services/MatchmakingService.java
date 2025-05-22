package info.sup.proj.backend.services;

import info.sup.proj.backend.model.Player;
import info.sup.proj.backend.model.User;
import info.sup.proj.backend.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class MatchmakingService {
    private final Map<String, Player> availablePlayers = new ConcurrentHashMap<>();
    private final Map<String, Player> searchingPlayers = new ConcurrentHashMap<>();
    private final Map<String, String> playerChallenges = new ConcurrentHashMap<>();
    private final GameService gameService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String USERNF = "User not found";
    private static final String QUEUE_GAME = "/queue/game";

    private final Logger logger = LoggerFactory.getLogger(MatchmakingService.class);

    public MatchmakingService(GameService gameService, UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public void addPlayerToLobby(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(USERNF));

        Player player = new Player(
            userId.toString(),
            user.getUsername(),
            user.getPicture()
        );

        availablePlayers.put(userId.toString(), player);
        broadcastLobbyUpdate();
    }

    public void removePlayerFromLobby(String userId) {
        availablePlayers.remove(userId);
        searchingPlayers.remove(userId);
        playerChallenges.remove(userId);
        broadcastLobbyUpdate();
    }

    public void startSearchingForOpponent(String userId, Map<String, Object> preferences) {
        Player player = availablePlayers.get(userId);
        if (player == null) {
            throw new IllegalStateException("Player not in lobby");
        }

        searchingPlayers.put(userId, player);
        findMatch(userId, preferences);
    }

    public void stopSearchingForOpponent(String userId) {
        searchingPlayers.remove(userId);
    }

    public void challengePlayer(String challengerId, String targetId) {
        Player challenger = availablePlayers.get(challengerId);
        Player target = availablePlayers.get(targetId);

        if (challenger == null || target == null) {
            throw new IllegalStateException("One or both players not in lobby");
        }

        playerChallenges.put(targetId, challengerId);

        User challengerUser = userRepository.findById(Long.parseLong(challengerId))
                .orElseThrow(() -> new IllegalArgumentException("Challenger not found"));

        Map<String, Object> challengeInfo = new HashMap<>();
        challengeInfo.put("type", "CHALLENGE_RECEIVED");
        challengeInfo.put("challengerId", challengerId);
        challengeInfo.put("challengerName", challengerUser.getUsername());
        challengeInfo.put("challengerPicture", challengerUser.getPicture());
        challengeInfo.put("challengerElo", challengerUser.getElo());

        messagingTemplate.convertAndSendToUser(
            targetId,
            QUEUE_GAME,
            challengeInfo
        );
    }

    public void acceptChallenge(String targetId) {
        String challengerId = playerChallenges.get(targetId);
        if (challengerId == null) {
            throw new IllegalStateException("No active challenge found");
        }

        Player challenger = availablePlayers.get(challengerId);
        Player target = availablePlayers.get(targetId);

        if (challenger == null || target == null) {
            throw new IllegalStateException("One or both players not in lobby");
        }

        try {
            createAndStartGame(challenger, target);

            playerChallenges.remove(targetId);
            removePlayerFromLobby(challengerId);
            removePlayerFromLobby(targetId);
        } catch (Exception e) {
            logger.error("Error creating game: {}" , e.getMessage());
        }
    }

    public void rejectChallenge(String targetId) {
        String challengerId = playerChallenges.get(targetId);
        if (challengerId == null) {
            throw new IllegalStateException("No active challenge found");
        }
        Map<String, Object> rejectInfo = new HashMap<>();
        rejectInfo.put("type", "CHALLENGE_REJECTED");
        rejectInfo.put("content", "Challenge was rejected");

        messagingTemplate.convertAndSendToUser(
            challengerId,
            QUEUE_GAME,
            rejectInfo
        );

        playerChallenges.remove(targetId);
    }

    private void findMatch(String userId, Map<String, Object> preferences) {
        Player searchingPlayer = searchingPlayers.get(userId);
        if (searchingPlayer == null) return;

        User searchingUser = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new IllegalArgumentException(USERNF));

        int searchingElo = searchingUser.getElo();
        int eloRange = preferences != null && preferences.get("eloRange") != null 
            ? (Integer) preferences.get("eloRange") 
            : 1000;

        Optional<Player> opponent = searchingPlayers.entrySet().stream()
            .filter(entry -> !entry.getKey().equals(userId))
            .map(Map.Entry::getValue)
            .filter(player -> {
                User potentialOpponent = userRepository.findById(Long.parseLong(player.getId()))
                        .orElse(null);
                if (potentialOpponent == null) return false;

                int eloDiff = Math.abs(searchingElo - potentialOpponent.getElo());
                return eloDiff <= eloRange;
            })
            .findFirst();

        if (opponent.isPresent()) {
            Player opponentPlayer = opponent.get();
            
            try {
                createAndStartGame(searchingPlayer, opponentPlayer);

                stopSearchingForOpponent(userId);
                stopSearchingForOpponent(opponentPlayer.getId());
                removePlayerFromLobby(userId);
                removePlayerFromLobby(opponentPlayer.getId());
            } catch (Exception e) {
                logger.error("Error creating game: {}" , e.getMessage());
            }
        }
    }

    private void createAndStartGame(Player player1, Player player2) {
        var game = gameService.createGame(player1, player2);

        for (Player player : List.of(player1, player2)) {
            Player opponent = player.getId().equals(player1.getId()) ? player2 : player1;
            User opponentUser = userRepository.findById(Long.parseLong(opponent.getId()))
                    .orElseThrow(() -> new IllegalArgumentException("Opponent not found"));

            Map<String, Object> gameStartInfo = new HashMap<>();
            gameStartInfo.put("type", "GAME_STARTED");
            gameStartInfo.put("gameId", game.getId());
            gameStartInfo.put("opponentId", opponent.getId());
            gameStartInfo.put("opponentName", opponent.getUsername());
            gameStartInfo.put("opponentPicture", opponent.getPicture());
            gameStartInfo.put("opponentElo", opponentUser.getElo());
            gameStartInfo.put("rounds", game.getTotalRounds());
            gameStartInfo.put("currentRound", game.getCurrentRound());
            gameStartInfo.put("currentPuzzleId", game.getPuzzle().getId());

            try {
                messagingTemplate.convertAndSendToUser(
                    player.getId(),
                    QUEUE_GAME,
                    gameStartInfo
                );
            } catch (Exception e) {
                logger.error("Error sending game start info to {}: {}" ,player.getUsername(), e.getMessage());
            }
        }
    }

    private void broadcastLobbyUpdate() {
        List<Map<String, Object>> lobbyPlayers = availablePlayers.values().stream()
            .filter(player -> !searchingPlayers.containsKey(player.getId()))
            .map(player -> {
                User user = userRepository.findById(Long.parseLong(player.getId()))
                        .orElseThrow(() -> new IllegalArgumentException(USERNF));
                
                Map<String, Object> playerInfo = new HashMap<>();
                playerInfo.put("userId", Long.parseLong(player.getId()));
                playerInfo.put("username", player.getUsername());
                playerInfo.put("picture", player.getPicture());
                playerInfo.put("elo", user.getElo());
                return playerInfo;
            })
            .toList();

        messagingTemplate.convertAndSend("/topic/lobby", lobbyPlayers);
    }
} 
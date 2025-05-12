package info.sup.proj.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.sup.proj.backend.model.User;
import info.sup.proj.backend.model.Puzzle;
import info.sup.proj.backend.model.GameMessage;
import info.sup.proj.backend.services.PuzzleService;
import info.sup.proj.backend.services.UserService;
import info.sup.proj.backend.services.PuzzleSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicBoolean;

interface GameTimerHandler {
    void handleRoundTimeout(String gameId, int currentRound) throws IOException;
}

@Component
public class GameWebSocketHandler extends TextWebSocketHandler implements GameTimerHandler, ApplicationListener<ContextClosedEvent> {
    private static final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, GameSession> gameSessions = new ConcurrentHashMap<>();
    private static final Map<Long, String> userToSessionMap = new HashMap<>();
    private static final List<GamePlayer> waitingPlayers = new ArrayList<>();
    private final Set<Long> activelySearching = new HashSet<>();
    private final AtomicBoolean applicationShuttingDown = new AtomicBoolean(false);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PuzzleService puzzleService;

    @Autowired
    private UserService userService;

    @SuppressWarnings("unused")
    @Autowired
    private PuzzleSessionService puzzleSessionService;
    
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        applicationShuttingDown.set(true);
        System.out.println("Application shutdown detected in GameWebSocketHandler");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        System.out.println("Game WebSocket connection established: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        GameMessage gameMessage = objectMapper.readValue(message.getPayload(), GameMessage.class);

        switch (gameMessage.getType()) {
            case "JOIN_LOBBY":
                handleJoinLobby(session, gameMessage);
                break;
            case "LEAVE_LOBBY":
                handleLeaveLobby(session, gameMessage);
                break;
            case "FIND_OPPONENT":
                handleFindOpponent(session, gameMessage);
                break;
            case "CHALLENGE_PLAYER":
                handleChallengePlayer(session, gameMessage);
                break;
            case "ACCEPT_CHALLENGE":
                handleAcceptChallenge(session, gameMessage);
                break;
            case "REJECT_CHALLENGE":
                handleRejectChallenge(session, gameMessage);
                break;
            case "SUBMIT_SOLUTION":
                handleSubmitSolution(session, gameMessage);
                break;
            case "GAME_ACTION":
                handleGameAction(session, gameMessage);
                break;
            default:
                sendMessageToSession(session, new GameMessage("ERROR", "Unknown message type", gameMessage.getUserId()));
        }
    }

    @Override
    public void handleRoundTimeout(String gameId, int currentRound) throws IOException {
        GameSession gameSession = gameSessions.get(gameId);
        if (gameSession != null) {
            if (gameSession.bothPlayersSubmitted()) {
                return;
            }

            if (gameSession.getPlayer1Scores().size() < currentRound) {
                gameSession.addPlayer1Score(0);
            }

            if (gameSession.getPlayer2Scores().size() < currentRound) {
                gameSession.addPlayer2Score(0);
            }

            if (currentRound < 3) {
                gameSession.nextRound();
                notifyRoundComplete(gameSession);
            } else {
                endGame(gameSession);
            }
        }
    }

    private void handleJoinLobby(WebSocketSession session, GameMessage message) throws IOException {
        Long userId = message.getUserId();

        if (userToSessionMap.containsKey(userId)) {
            String existingSessionId = userToSessionMap.get(userId);
            sessions.remove(existingSessionId);
        }

        userToSessionMap.put(userId, session.getId());

        boolean playerExists = waitingPlayers.stream()
            .anyMatch(player -> player.getUserId().equals(userId));

        if (!playerExists) {
            User user = userService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            GamePlayer player = new GamePlayer(
                userId,
                user.getUsername(),
                user.getPicture(),
                user.getElo(),
                session.getId()
            );
            waitingPlayers.add(player);
        }

        List<Map<String, Object>> availablePlayers = waitingPlayers.stream()
            .filter(player -> !player.getUserId().equals(userId))
            .map(GamePlayer::toMap)
            .toList();

        GameMessage responseMessage = new GameMessage(
            "LOBBY_UPDATE",
            objectMapper.writeValueAsString(availablePlayers),
            userId
        );

        sendMessageToSession(session, responseMessage);

        broadcastLobbyUpdate();
    }

    private void handleLeaveLobby(WebSocketSession session, GameMessage message) {
        Long userId = message.getUserId();
        waitingPlayers.removeIf(player -> player.getUserId().equals(userId));
        userToSessionMap.remove(userId);
        activelySearching.remove(userId);

        broadcastLobbyUpdate();
    }

    private void handleFindOpponent(WebSocketSession session, GameMessage message) throws IOException {
        Long userId = message.getUserId();
        User user = userService.getUserById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        activelySearching.add(userId);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            try {
                if (activelySearching.contains(userId)) {
                    activelySearching.remove(userId);
                    GameMessage timeoutMessage = new GameMessage(
                        "NO_OPPONENT",
                        "No suitable opponent found within the time limit. Please try again.",
                        userId
                    );
                    sendMessageToSession(session, timeoutMessage);
                    System.out.println("Search timeout for user: " + userId);
                }
            } catch (Exception e) {
                System.err.println("Error in search timeout handler: " + e.getMessage());
                e.printStackTrace();
            } finally {
                scheduler.shutdown();
            }
        }, 14, TimeUnit.SECONDS);

        try {
            Map<String, Object> preferences = new HashMap<>();
            if (!message.getContent().isEmpty()) {
                preferences = objectMapper.readValue(message.getContent(), Map.class);
            }

            int eloRange = preferences.containsKey("eloRange") ?
                           (int) preferences.get("eloRange") : 200;

            boolean strictMatching = preferences.containsKey("strictMatching") ?
                                   (boolean) preferences.get("strictMatching") : false;

            int userElo = user.getElo();
            GamePlayer opponent = null;

            if (strictMatching) {
                Optional<GamePlayer> potentialOpponent = waitingPlayers.stream()
                    .filter(player -> !player.getUserId().equals(userId) &&
                                     Math.abs(player.getElo() - userElo) <= eloRange &&
                                     !player.isInGame() &&
                                     activelySearching.contains(player.getUserId()))
                    .findFirst();

                if (potentialOpponent.isPresent()) {
                    opponent = potentialOpponent.get();
                }
            } else {
                int currentEloRange = eloRange;
                while (currentEloRange <= 1000 && opponent == null) {
                    int finalEloRange = currentEloRange;
                    Optional<GamePlayer> potentialOpponent = waitingPlayers.stream()
                        .filter(player -> !player.getUserId().equals(userId) &&
                                         Math.abs(player.getElo() - userElo) <= finalEloRange &&
                                         !player.isInGame() &&
                                         activelySearching.contains(player.getUserId()))
                        .findFirst();

                    if (potentialOpponent.isPresent()) {
                        opponent = potentialOpponent.get();
                        break;
                    }

                    currentEloRange += eloRange;
                }
            }

            if (opponent != null) {
                startGame(userId, opponent.getUserId());

                activelySearching.remove(userId);
                activelySearching.remove(opponent.getUserId());

                scheduler.shutdownNow();

                System.out.println("Match found: " + user.getUsername() + " (ELO: " + userElo +
                                  ") vs " + opponent.getUsername() + " (ELO: " + opponent.getElo() + ")");
            }
        } catch (Exception e) {
            System.err.println("Error in find opponent logic: " + e.getMessage());
            e.printStackTrace();

            activelySearching.remove(userId);
            scheduler.shutdownNow();

            GameMessage errorMessage = new GameMessage(
                "ERROR",
                "An error occurred while finding an opponent. Please try again.",
                userId
            );
            sendMessageToSession(session, errorMessage);
        }
    }

    private void findOpponentSimple(WebSocketSession session, Long userId, int userElo) throws IOException {
        GamePlayer opponent = null;
        int eloRange = 200;

        while (eloRange <= 1000 && opponent == null) {
            int finalEloRange = eloRange;
            Optional<GamePlayer> potentialOpponent = waitingPlayers.stream()
                .filter(player -> !player.getUserId().equals(userId) &&
                                 Math.abs(player.getElo() - userElo) <= finalEloRange &&
                                 !player.isInGame() &&
                                 activelySearching.contains(player.getUserId()))
                .findFirst();

            if (potentialOpponent.isPresent()) {
                opponent = potentialOpponent.get();
                break;
            }

            eloRange += 200;
        }

        if (opponent != null) {
            startGame(userId, opponent.getUserId());

            activelySearching.remove(userId);
            activelySearching.remove(opponent.getUserId());
        } else {
            GameMessage responseMessage = new GameMessage(
                "NO_OPPONENT",
                "No suitable opponent found. Please try again later.",
                userId
            );
            sendMessageToSession(session, responseMessage);
        }
    }

    private void handleChallengePlayer(WebSocketSession session, GameMessage message) throws IOException {
        Long challengerId = message.getUserId();
        Long challengedId = Long.parseLong(message.getContent());

        String challengedSessionId = userToSessionMap.get(challengedId);
        if (challengedSessionId != null) {
            WebSocketSession challengedSession = sessions.get(challengedSessionId);

            if (challengedSession != null && challengedSession.isOpen()) {
                User challenger = userService.getUserById(challengerId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

                Map<String, Object> challengeInfo = new HashMap<>();
                challengeInfo.put("challengerId", challengerId);
                challengeInfo.put("challengerName", challenger.getUsername());
                challengeInfo.put("challengerPicture", challenger.getPicture());
                challengeInfo.put("challengerElo", challenger.getElo());

                GameMessage challengeMessage = new GameMessage(
                    "CHALLENGE_RECEIVED",
                    objectMapper.writeValueAsString(challengeInfo),
                    challengedId
                );

                sendMessageToSession(challengedSession, challengeMessage);

                GameMessage confirmMessage = new GameMessage(
                    "CHALLENGE_SENT",
                    "Challenge sent successfully",
                    challengerId
                );

                sendMessageToSession(session, confirmMessage);
                return;
            }
        }

        GameMessage errorMessage = new GameMessage(
            "ERROR",
            "The player you challenged is no longer available",
            challengerId
        );

        sendMessageToSession(session, errorMessage);
    }

    private void handleAcceptChallenge(WebSocketSession session, GameMessage message) throws IOException {
        Long accepterId = message.getUserId();
        Long challengerId = Long.parseLong(message.getContent());

        System.out.println("Challenge accepted: User " + accepterId + " accepted challenge from " + challengerId);

        String challengerSessionId = userToSessionMap.get(challengerId);
        String accepterSessionId = userToSessionMap.get(accepterId);

        if (challengerSessionId == null || accepterSessionId == null) {
            System.err.println("Error accepting challenge: One of the players is not available");
            GameMessage errorMessage = new GameMessage(
                "ERROR",
                "Unable to start game: one of the players is no longer available",
                accepterId
            );
            sendMessageToSession(session, errorMessage);
            return;
        }

        WebSocketSession challengerSession = sessions.get(challengerSessionId);
        if (challengerSession == null || !challengerSession.isOpen()) {
            System.err.println("Error accepting challenge: Challenger's session is not available");
            GameMessage errorMessage = new GameMessage(
                "ERROR",
                "Unable to start game: the challenger is no longer connected",
                accepterId
            );
            sendMessageToSession(session, errorMessage);
            return;
        }

        try {
            startGame(challengerId, accepterId);
        } catch (Exception e) {
            System.err.println("Error starting game after challenge acceptance: " + e.getMessage());
            e.printStackTrace();

            GameMessage errorMessage = new GameMessage(
                "ERROR",
                "Failed to start game: " + e.getMessage(),
                accepterId
            );
            sendMessageToSession(session, errorMessage);

            GameMessage challengerErrorMessage = new GameMessage(
                "ERROR",
                "Failed to start game: " + e.getMessage(),
                challengerId
            );
            sendMessageToSession(challengerSession, challengerErrorMessage);
        }
    }

    private void handleRejectChallenge(WebSocketSession session, GameMessage message) throws IOException {
        Long rejecterId = message.getUserId();
        Long challengerId = Long.parseLong(message.getContent());

        String challengerSessionId = userToSessionMap.get(challengerId);
        if (challengerSessionId != null) {
            WebSocketSession challengerSession = sessions.get(challengerSessionId);

            if (challengerSession != null && challengerSession.isOpen()) {
                User rejector = userService.getUserById(rejecterId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

                GameMessage rejectionMessage = new GameMessage(
                    "CHALLENGE_REJECTED",
                    rejector.getUsername() + " declined your challenge",
                    challengerId
                );

                sendMessageToSession(challengerSession, rejectionMessage);
            }
        }
    }

    private void startGame(Long player1Id, Long player2Id) throws IOException {
        String gameId = UUID.randomUUID().toString();

        waitingPlayers.stream()
            .filter(p -> p.getUserId().equals(player1Id) || p.getUserId().equals(player2Id))
            .forEach(p -> p.setInGame(true));

        List<Puzzle> allPuzzles = puzzleService.getPuzzlesByType(Puzzle.Type.Multi_Step);
        List<Puzzle> selectedPuzzles = new ArrayList<>();

        if (allPuzzles.size() >= 3) {
            Collections.shuffle(allPuzzles);
            selectedPuzzles = allPuzzles.subList(0, 3);
        } else {
            while (selectedPuzzles.size() < 3) {
                selectedPuzzles.addAll(allPuzzles);
            }
            if (selectedPuzzles.size() > 3) {
                selectedPuzzles = selectedPuzzles.subList(0, 3);
            }
        }

        GameSession gameSession = new GameSession(
            gameId,
            player1Id,
            player2Id,
            selectedPuzzles.stream().map(Puzzle::getId).toList(),
            this
        );

        gameSessions.put(gameId, gameSession);

        String player1SessionId = userToSessionMap.get(player1Id);
        String player2SessionId = userToSessionMap.get(player2Id);

        WebSocketSession player1Session = sessions.get(player1SessionId);
        WebSocketSession player2Session = sessions.get(player2SessionId);

        User player1 = userService.getUserById(player1Id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        User player2 = userService.getUserById(player2Id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Map<String, Object> player1Info = new HashMap<>();
        player1Info.put("gameId", gameId);
        player1Info.put("opponentId", player2Id);
        player1Info.put("opponentName", player2.getUsername());
        player1Info.put("opponentElo", player2.getElo());
        player1Info.put("opponentPicture", player2.getPicture());
        player1Info.put("rounds", 3);
        player1Info.put("currentRound", 1);
        player1Info.put("currentPuzzleId", gameSession.getCurrentPuzzleId());
        player1Info.put("timePerRound", 300);

        Map<String, Object> player2Info = new HashMap<>();
        player2Info.put("gameId", gameId);
        player2Info.put("opponentId", player1Id);
        player2Info.put("opponentName", player1.getUsername());
        player2Info.put("opponentElo", player1.getElo());
        player2Info.put("opponentPicture", player1.getPicture());
        player2Info.put("rounds", 3);
        player2Info.put("currentRound", 1);
        player2Info.put("currentPuzzleId", gameSession.getCurrentPuzzleId());
        player2Info.put("timePerRound", 300);

        GameMessage player1Message = new GameMessage(
            "GAME_STARTED",
            objectMapper.writeValueAsString(player1Info),
            player1Id
        );

        GameMessage player2Message = new GameMessage(
            "GAME_STARTED",
            objectMapper.writeValueAsString(player2Info),
            player2Id
        );

        if (player1Session != null && player1Session.isOpen()) {
            sendMessageToSession(player1Session, player1Message);
        }

        if (player2Session != null && player2Session.isOpen()) {
            sendMessageToSession(player2Session, player2Message);
        }

        gameSession.startRound();

        broadcastLobbyUpdate();
    }

    private void handleSubmitSolution(WebSocketSession session, GameMessage message) throws IOException {
        Long userId = message.getUserId();
        @SuppressWarnings("unchecked")
        Map<String, Object> solutionData = objectMapper.readValue(message.getContent(), Map.class);

        String gameId = (String) solutionData.get("gameId");
        String solution = (String) solutionData.get("solution");

        GameSession gameSession = gameSessions.get(gameId);

        if (gameSession == null) {
            sendMessageToSession(session, new GameMessage("ERROR", "Game not found", userId));
            return;
        }

        int score = calculateScore(solution, gameSession.getCurrentPuzzleId(), userId);

        if (userId.equals(gameSession.getPlayer1Id())) {
            gameSession.addPlayer1Score(score);
        } else if (userId.equals(gameSession.getPlayer2Id())) {
            gameSession.addPlayer2Score(score);
        }

        if (gameSession.bothPlayersSubmitted()) {
            if (gameSession.getCurrentRound() < 3) {
                gameSession.nextRound();

                notifyRoundComplete(gameSession);
            } else {
                endGame(gameSession);
            }
        } else {
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("score", score);
            resultData.put("message", "Solution submitted. Waiting for opponent...");

            GameMessage resultMessage = new GameMessage(
                "SOLUTION_SUBMITTED",
                objectMapper.writeValueAsString(resultData),
                userId
            );

            sendMessageToSession(session, resultMessage);
        }
    }

    private int calculateScore(String solution, int puzzleId, Long userId) {
        int baseScore = 50;

        int qualityScore = solution.length() > 10 ? 30 : 10;

        int timeScore = 20;

        return baseScore + qualityScore + timeScore;
    }

    private void notifyRoundComplete(GameSession gameSession) throws IOException {
        Map<String, Object> player1Data = new HashMap<>();
        player1Data.put("gameId", gameSession.getGameId());
        player1Data.put("currentRound", gameSession.getCurrentRound());
        player1Data.put("yourScore", gameSession.getPlayer1TotalScore());
        player1Data.put("opponentScore", gameSession.getPlayer2TotalScore());
        player1Data.put("nextPuzzleId", gameSession.getCurrentPuzzleId());

        Map<String, Object> player2Data = new HashMap<>();
        player2Data.put("gameId", gameSession.getGameId());
        player2Data.put("currentRound", gameSession.getCurrentRound());
        player2Data.put("yourScore", gameSession.getPlayer2TotalScore());
        player2Data.put("opponentScore", gameSession.getPlayer1TotalScore());
        player2Data.put("nextPuzzleId", gameSession.getCurrentPuzzleId());

        GameMessage player1Message = new GameMessage(
            "ROUND_COMPLETE",
            objectMapper.writeValueAsString(player1Data),
            gameSession.getPlayer1Id()
        );

        GameMessage player2Message = new GameMessage(
            "ROUND_COMPLETE",
            objectMapper.writeValueAsString(player2Data),
            gameSession.getPlayer2Id()
        );

        sendMessageToUser(gameSession.getPlayer1Id(), player1Message);
        sendMessageToUser(gameSession.getPlayer2Id(), player2Message);

        gameSession.startRound();
    }

    private void endGame(GameSession gameSession) throws IOException {
        int player1Score = gameSession.getPlayer1TotalScore();
        int player2Score = gameSession.getPlayer2TotalScore();

        Long winnerId = null;
        Long loserId = null;
        boolean isDraw = false;

        if (player1Score > player2Score) {
            winnerId = gameSession.getPlayer1Id();
            loserId = gameSession.getPlayer2Id();
        } else if (player2Score > player1Score) {
            winnerId = gameSession.getPlayer2Id();
            loserId = gameSession.getPlayer1Id();
        } else {
            isDraw = true;
        }

        int eloChange = 0;
        if (!isDraw) {
            User winner = userService.getUserById(winnerId).orElse(null);
            User loser = userService.getUserById(loserId).orElse(null);

            if (winner != null && loser != null) {
                int winnerElo = winner.getElo();
                int loserElo = loser.getElo();

                int expectedOutcome = 1 / (1 + (int)Math.pow(10, (loserElo - winnerElo) / 400.0));
                eloChange = 32 * (1 - expectedOutcome);

                userService.updateUserElo(winnerId, eloChange);
                userService.updateUserElo(loserId, -eloChange);
            }
        }

        Map<String, Object> player1Data = new HashMap<>();
        player1Data.put("gameId", gameSession.getGameId());
        player1Data.put("yourScore", player1Score);
        player1Data.put("opponentScore", player2Score);
        player1Data.put("eloChange", player1Score > player2Score ? eloChange : (player1Score < player2Score ? -eloChange : 0));
        player1Data.put("result", player1Score > player2Score ? "WIN" : (player1Score < player2Score ? "LOSS" : "DRAW"));

        Map<String, Object> player2Data = new HashMap<>();
        player2Data.put("gameId", gameSession.getGameId());
        player2Data.put("yourScore", player2Score);
        player2Data.put("opponentScore", player1Score);
        player2Data.put("eloChange", player2Score > player1Score ? eloChange : (player2Score < player1Score ? -eloChange : 0));
        player2Data.put("result", player2Score > player1Score ? "WIN" : (player2Score < player1Score ? "LOSS" : "DRAW"));

        GameMessage player1Message = new GameMessage(
            "GAME_OVER",
            objectMapper.writeValueAsString(player1Data),
            gameSession.getPlayer1Id()
        );

        GameMessage player2Message = new GameMessage(
            "GAME_OVER",
            objectMapper.writeValueAsString(player2Data),
            gameSession.getPlayer2Id()
        );

        sendMessageToUser(gameSession.getPlayer1Id(), player1Message);
        sendMessageToUser(gameSession.getPlayer2Id(), player2Message);

        gameSessions.remove(gameSession.getGameId());

        waitingPlayers.stream()
            .filter(p -> p.getUserId().equals(gameSession.getPlayer1Id()) ||
                        p.getUserId().equals(gameSession.getPlayer2Id()))
            .forEach(p -> p.setInGame(false));

        broadcastLobbyUpdate();
    }

    private void handleGameAction(WebSocketSession session, GameMessage message) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> actionData = objectMapper.readValue(message.getContent(), Map.class);

        String gameId = (String) actionData.get("gameId");
        String action = (String) actionData.get("action");

        GameSession gameSession = gameSessions.get(gameId);

        if (gameSession == null) {
            sendMessageToSession(session, new GameMessage("ERROR", "Game not found", message.getUserId()));
            return;
        }

        switch (action) {
            case "FORFEIT":
                handleForfeit(gameSession, message.getUserId());
                break;
        }
    }

    private void handleForfeit(GameSession gameSession, Long forfeittingUserId) throws IOException {
        Long winnerId = gameSession.getPlayer1Id().equals(forfeittingUserId) ?
            gameSession.getPlayer2Id() : gameSession.getPlayer1Id();

        userService.updateUserElo(winnerId, 25);
        userService.updateUserElo(forfeittingUserId, -25);

        Map<String, Object> winnerData = new HashMap<>();
        winnerData.put("gameId", gameSession.getGameId());
        winnerData.put("result", "WIN_BY_FORFEIT");
        winnerData.put("eloChange", 25);

        Map<String, Object> loserData = new HashMap<>();
        loserData.put("gameId", gameSession.getGameId());
        loserData.put("result", "FORFEIT");
        loserData.put("eloChange", -25);

        GameMessage winnerMessage = new GameMessage(
            "GAME_OVER",
            objectMapper.writeValueAsString(winnerData),
            winnerId
        );

        GameMessage loserMessage = new GameMessage(
            "GAME_OVER",
            objectMapper.writeValueAsString(loserData),
            forfeittingUserId
        );

        sendMessageToUser(winnerId, winnerMessage);
        sendMessageToUser(forfeittingUserId, loserMessage);

        gameSessions.remove(gameSession.getGameId());

        waitingPlayers.stream()
            .filter(p -> p.getUserId().equals(gameSession.getPlayer1Id()) ||
                       p.getUserId().equals(gameSession.getPlayer2Id()))
            .forEach(p -> p.setInGame(false));

        broadcastLobbyUpdate();
    }

    private void broadcastLobbyUpdate() {
        List<Map<String, Object>> availablePlayers = waitingPlayers.stream()
            .filter(player -> !player.isInGame())
            .map(GamePlayer::toMap)
            .toList();

        for (GamePlayer player : waitingPlayers) {
            if (!player.isInGame()) {
                List<Map<String, Object>> otherPlayers = availablePlayers.stream()
                    .filter(p -> ((Number)p.get("userId")).longValue() != player.getUserId())
                    .toList();

                try {
                    GameMessage updateMessage = new GameMessage(
                        "LOBBY_UPDATE",
                        objectMapper.writeValueAsString(otherPlayers),
                        player.getUserId()
                    );

                    sendMessageToUser(player.getUserId(), updateMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendMessageToUser(Long userId, GameMessage message) throws IOException {
        String sessionId = userToSessionMap.get(userId);
        if (sessionId != null) {
            WebSocketSession session = sessions.get(sessionId);
            if (session != null && session.isOpen()) {
                sendMessageToSession(session, message);
            }
        }
    }

    private void sendMessageToSession(WebSocketSession session, GameMessage message) throws IOException {
        if (session.isOpen()) {
            String sessionId = session.getId();
            
            sessionLocks.putIfAbsent(sessionId, new ReentrantLock());
            ReentrantLock lock = sessionLocks.get(sessionId);
            
            lock.lock();
            try {
                String jsonMessage = objectMapper.writeValueAsString(message);
                synchronized (session) {
                    session.sendMessage(new TextMessage(jsonMessage));
                }
            } catch (IOException e) {
                System.err.println("Error sending message to session " + sessionId + ": " + e.getMessage());
                throw e;
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        sessionLocks.remove(sessionId);
        
        if (applicationShuttingDown.get()) {
            System.out.println("Skipping database operations during shutdown for session: " + sessionId);
            return;
        }
        
        Long userId = null;
        for (Map.Entry<Long, String> entry : userToSessionMap.entrySet()) {
            if (entry.getValue().equals(sessionId)) {
                userId = entry.getKey();
                userToSessionMap.remove(userId);
                break;
            }
        }
        
        Optional.ofNullable(userId).ifPresent(uid -> {
            synchronized (waitingPlayers) {
                waitingPlayers.removeIf(player -> player.getUserId().equals(uid));
            }
            
            activelySearching.remove(uid);
            
            Optional<Map.Entry<String, GameSession>> gameEntry = gameSessions.entrySet().stream()
                    .filter(entry -> 
                        entry.getValue().getPlayer1Id().equals(uid) || 
                        entry.getValue().getPlayer2Id().equals(uid))
                    .findFirst();
            
            gameEntry.ifPresent(entry -> {
                String gameId = entry.getKey();
                GameSession game = entry.getValue();
                
                try {
                    handleForfeit(game, uid);
                } catch (Exception e) {
                    System.err.println("Error handling forfeit during connection close: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });
    }
}

class GamePlayer {
    private final Long userId;
    private final String username;
    private final String picture;
    private final int elo;
    private final String sessionId;
    private boolean inGame = false;

    public GamePlayer(Long userId, String username, String picture, int elo, String sessionId) {
        this.userId = userId;
        this.username = username;
        this.picture = picture;
        this.elo = elo;
        this.sessionId = sessionId;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPicture() { return picture; }
    public int getElo() { return elo; }
    public String getSessionId() { return sessionId; }
    public boolean isInGame() { return inGame; }
    public void setInGame(boolean inGame) { this.inGame = inGame; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("username", username);
        map.put("picture", picture);
        map.put("elo", elo);
        return map;
    }
}

class GameSession {
    private final String gameId;
    private final Long player1Id;
    private final Long player2Id;
    private final List<Integer> puzzleIds;
    private int currentRound = 0;
    private final List<Integer> player1Scores = new ArrayList<>();
    private final List<Integer> player2Scores = new ArrayList<>();
    private Timer timer;
    private final GameTimerHandler timerHandler;

    public GameSession(String gameId, Long player1Id, Long player2Id, List<Integer> puzzleIds, GameTimerHandler timerHandler) {
        this.gameId = gameId;
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.puzzleIds = puzzleIds;
        this.timerHandler = timerHandler;
    }

    public String getGameId() { return gameId; }
    public Long getPlayer1Id() { return player1Id; }
    public Long getPlayer2Id() { return player2Id; }
    public int getCurrentRound() { return currentRound; }
    public List<Integer> getPlayer1Scores() { return player1Scores; }
    public List<Integer> getPlayer2Scores() { return player2Scores; }

    public int getCurrentPuzzleId() {
        if (currentRound <= 0 && !puzzleIds.isEmpty()) {
            return puzzleIds.get(0);
        }
        return puzzleIds.get(currentRound - 1);
    }

    public void startRound() {
        if (currentRound < 3) {
            currentRound++;
        }

        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer();
        final int roundNumber = currentRound;
        final String gameSessionId = gameId;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    timerHandler.handleRoundTimeout(gameSessionId, roundNumber);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 300000);
    }

    public void nextRound() {
        if (timer != null) {
            timer.cancel();
        }

        if (currentRound < 3) {
            startRound();
        }
    }

    public void addPlayer1Score(int score) {
        while (player1Scores.size() < currentRound - 1) {
            player1Scores.add(0);
        }

        if (player1Scores.size() < currentRound) {
            player1Scores.add(score);
        } else {
            player1Scores.set(currentRound - 1, score);
        }
    }

    public void addPlayer2Score(int score) {
        while (player2Scores.size() < currentRound - 1) {
            player2Scores.add(0);
        }

        if (player2Scores.size() < currentRound) {
            player2Scores.add(score);
        } else {
            player2Scores.set(currentRound - 1, score);
        }
    }

    public boolean bothPlayersSubmitted() {
        return player1Scores.size() >= currentRound && player2Scores.size() >= currentRound;
    }

    public int getPlayer1TotalScore() {
        return player1Scores.stream().mapToInt(Integer::intValue).sum();
    }

    public int getPlayer2TotalScore() {
        return player2Scores.stream().mapToInt(Integer::intValue).sum();
    }
}
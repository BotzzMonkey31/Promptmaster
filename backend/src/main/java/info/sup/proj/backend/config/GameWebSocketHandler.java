package info.sup.proj.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.sup.proj.backend.model.User;
import info.sup.proj.backend.model.Puzzle;
import info.sup.proj.backend.model.GameMessage;
import info.sup.proj.backend.services.PuzzleService;
import info.sup.proj.backend.services.UserService;
import info.sup.proj.backend.services.PuzzleSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interface for handling game timer events
 */
interface GameTimerHandler {
    void handleRoundTimeout(String gameId, int currentRound) throws IOException;
}

@Component
public class GameWebSocketHandler extends TextWebSocketHandler implements GameTimerHandler {
    private static final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, GameSession> gameSessions = new ConcurrentHashMap<>();
    private static final Map<Long, String> userToSessionMap = new HashMap<>();
    private static final List<GamePlayer> waitingPlayers = new ArrayList<>();
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private PuzzleService puzzleService;
    
    @Autowired
    private UserService userService;
    
    // This might be used in future functionality
    @SuppressWarnings("unused")
    @Autowired
    private PuzzleSessionService puzzleSessionService;

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

    // Implement the GameTimerHandler interface
    @Override
    public void handleRoundTimeout(String gameId, int currentRound) throws IOException {
        GameSession gameSession = gameSessions.get(gameId);
        if (gameSession != null) {
            // Check if both players have submitted for this round
            if (gameSession.bothPlayersSubmitted()) {
                // Already handled by submission logic
                return;
            }
            
            // Force add zero scores for players who haven't submitted
            if (gameSession.getPlayer1Scores().size() < currentRound) {
                gameSession.addPlayer1Score(0);
            }
            
            if (gameSession.getPlayer2Scores().size() < currentRound) {
                gameSession.addPlayer2Score(0);
            }
            
            // Move to next round or end the game
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
        
        // Add user to waiting players list if not already there
        if (userToSessionMap.containsKey(userId)) {
            // Already connected, update the session
            String existingSessionId = userToSessionMap.get(userId);
            sessions.remove(existingSessionId);
        }
        
        userToSessionMap.put(userId, session.getId());
        
        // Check if player is already in waiting list
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
        
        // Send the current list of available players to the user
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
        
        // Notify other players that a new player joined
        broadcastLobbyUpdate();
    }

    private void handleLeaveLobby(WebSocketSession session, GameMessage message) {
        Long userId = message.getUserId();
        waitingPlayers.removeIf(player -> player.getUserId().equals(userId));
        userToSessionMap.remove(userId);
        
        // Notify other players
        broadcastLobbyUpdate();
    }

    private void handleFindOpponent(WebSocketSession session, GameMessage message) throws IOException {
        Long userId = message.getUserId();
        User user = userService.getUserById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Enhanced opponent matching with more parameters
        try {
            Map<String, Object> preferences = new HashMap<>();
            if (!message.getContent().isEmpty()) {
                preferences = objectMapper.readValue(message.getContent(), Map.class);
            }
            
            // Get matching preferences or use defaults
            int eloRange = preferences.containsKey("eloRange") ? 
                           (int) preferences.get("eloRange") : 200;
            
            boolean strictMatching = preferences.containsKey("strictMatching") ? 
                                   (boolean) preferences.get("strictMatching") : false;
            
            // Find suitable opponent based on preferences
            int userElo = user.getElo();
            GamePlayer opponent = null;
            
            if (strictMatching) {
                // Strict matching - use exact preferences
                Optional<GamePlayer> potentialOpponent = waitingPlayers.stream()
                    .filter(player -> !player.getUserId().equals(userId) &&
                                     Math.abs(player.getElo() - userElo) <= eloRange &&
                                     !player.isInGame())
                    .findFirst();
                    
                if (potentialOpponent.isPresent()) {
                    opponent = potentialOpponent.get();
                }
            } else {
                // Progressive matching - gradually increase range
                int currentEloRange = eloRange;
                while (currentEloRange <= 1000 && opponent == null) {
                    int finalEloRange = currentEloRange;
                    Optional<GamePlayer> potentialOpponent = waitingPlayers.stream()
                        .filter(player -> !player.getUserId().equals(userId) &&
                                         Math.abs(player.getElo() - userElo) <= finalEloRange &&
                                         !player.isInGame())
                        .findFirst();
                        
                    if (potentialOpponent.isPresent()) {
                        opponent = potentialOpponent.get();
                        break;
                    }
                    
                    currentEloRange += eloRange; // Gradually increase the range
                }
            }
            
            if (opponent != null) {
                // Found an opponent, start a game
                startGame(userId, opponent.getUserId());
                
                // Log match details
                System.out.println("Match found: " + user.getUsername() + " (ELO: " + userElo + 
                                  ") vs " + opponent.getUsername() + " (ELO: " + opponent.getElo() + ")");
            } else {
                // No opponent found
                GameMessage responseMessage = new GameMessage(
                    "NO_OPPONENT", 
                    "No suitable opponent found. Please try again later.",
                    userId
                );
                sendMessageToSession(session, responseMessage);
            }
        } catch (Exception e) {
            System.err.println("Error in find opponent logic: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to simple matching on error
            findOpponentSimple(session, userId, user.getElo());
        }
    }
    
    // Simple fallback method for finding opponents
    private void findOpponentSimple(WebSocketSession session, Long userId, int userElo) throws IOException {
        GamePlayer opponent = null;
        int eloRange = 200;
        
        while (eloRange <= 1000 && opponent == null) {
            int finalEloRange = eloRange;
            Optional<GamePlayer> potentialOpponent = waitingPlayers.stream()
                .filter(player -> !player.getUserId().equals(userId) &&
                                 Math.abs(player.getElo() - userElo) <= finalEloRange &&
                                 !player.isInGame())
                .findFirst();
                
            if (potentialOpponent.isPresent()) {
                opponent = potentialOpponent.get();
                break;
            }
            
            eloRange += 200;
        }
        
        if (opponent != null) {
            // Found an opponent, start a game
            startGame(userId, opponent.getUserId());
        } else {
            // No opponent found
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
        
        // Get the challenged player's session
        String challengedSessionId = userToSessionMap.get(challengedId);
        if (challengedSessionId != null) {
            WebSocketSession challengedSession = sessions.get(challengedSessionId);
            
            if (challengedSession != null && challengedSession.isOpen()) {
                // Get challenger info
                User challenger = userService.getUserById(challengerId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
                // Send challenge to challenged player
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
                
                // Send confirmation to challenger
                GameMessage confirmMessage = new GameMessage(
                    "CHALLENGE_SENT", 
                    "Challenge sent successfully",
                    challengerId
                );
                
                sendMessageToSession(session, confirmMessage);
                return;
            }
        }
        
        // If we get here, the challenged player is not available
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
        
        // Validate both players are available
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
        
        // Start a game between these two players
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
            
            // Also notify the challenger
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
        
        // Get challenger session
        String challengerSessionId = userToSessionMap.get(challengerId);
        if (challengerSessionId != null) {
            WebSocketSession challengerSession = sessions.get(challengerSessionId);
            
            if (challengerSession != null && challengerSession.isOpen()) {
                // Get rejector info
                User rejector = userService.getUserById(rejecterId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
                // Send rejection notification
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
        // Create a new game session
        String gameId = UUID.randomUUID().toString();
        
        // Mark players as in-game
        waitingPlayers.stream()
            .filter(p -> p.getUserId().equals(player1Id) || p.getUserId().equals(player2Id))
            .forEach(p -> p.setInGame(true));
        
        // Select 3 random Multi_Step puzzles
        List<Puzzle> allPuzzles = puzzleService.getPuzzlesByType(Puzzle.Type.Multi_Step);
        List<Puzzle> selectedPuzzles = new ArrayList<>();
        
        if (allPuzzles.size() >= 3) {
            Collections.shuffle(allPuzzles);
            selectedPuzzles = allPuzzles.subList(0, 3);
        } else {
            // Not enough puzzles, use what we have and possibly repeat
            while (selectedPuzzles.size() < 3) {
                selectedPuzzles.addAll(allPuzzles);
            }
            if (selectedPuzzles.size() > 3) {
                selectedPuzzles = selectedPuzzles.subList(0, 3);
            }
        }
        
        // Create game session
        GameSession gameSession = new GameSession(
            gameId,
            player1Id,
            player2Id,
            selectedPuzzles.stream().map(Puzzle::getId).toList(),
            this
        );
        
        gameSessions.put(gameId, gameSession);
        
        // Get player sessions
        String player1SessionId = userToSessionMap.get(player1Id);
        String player2SessionId = userToSessionMap.get(player2Id);
        
        WebSocketSession player1Session = sessions.get(player1SessionId);
        WebSocketSession player2Session = sessions.get(player2SessionId);
        
        // Get user info
        User player1 = userService.getUserById(player1Id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        User player2 = userService.getUserById(player2Id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Send game start messages to both players with opponent info
        Map<String, Object> player1Info = new HashMap<>();
        player1Info.put("gameId", gameId);
        player1Info.put("opponentId", player2Id);
        player1Info.put("opponentName", player2.getUsername());
        player1Info.put("opponentElo", player2.getElo());
        player1Info.put("opponentPicture", player2.getPicture());
        player1Info.put("rounds", 3);
        player1Info.put("currentRound", 1);
        player1Info.put("currentPuzzleId", gameSession.getCurrentPuzzleId());
        player1Info.put("timePerRound", 300); // 5 minutes per puzzle
        
        Map<String, Object> player2Info = new HashMap<>();
        player2Info.put("gameId", gameId);
        player2Info.put("opponentId", player1Id);
        player2Info.put("opponentName", player1.getUsername());
        player2Info.put("opponentElo", player1.getElo());
        player2Info.put("opponentPicture", player1.getPicture());
        player2Info.put("rounds", 3);
        player2Info.put("currentRound", 1);
        player2Info.put("currentPuzzleId", gameSession.getCurrentPuzzleId());
        player2Info.put("timePerRound", 300); // 5 minutes per puzzle
        
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
        
        // Start countdown for both players
        gameSession.startRound();
        
        // Broadcast lobby update since players are now in a game
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
        
        // Calculate score based on solution quality and time taken
        int score = calculateScore(solution, gameSession.getCurrentPuzzleId(), userId);
        
        // Record the score for this round
        if (userId.equals(gameSession.getPlayer1Id())) {
            gameSession.addPlayer1Score(score);
        } else if (userId.equals(gameSession.getPlayer2Id())) {
            gameSession.addPlayer2Score(score);
        }
        
        // Check if both players have submitted for this round
        if (gameSession.bothPlayersSubmitted()) {
            // Move to next round or end the game
            if (gameSession.getCurrentRound() < 3) {
                // Next round
                gameSession.nextRound();
                
                // Notify both players
                notifyRoundComplete(gameSession);
            } else {
                // Game complete
                endGame(gameSession);
            }
        } else {
            // Just notify the current player of their submission
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
        // Base score calculation - in a real implementation, you'd have more sophisticated scoring
        int baseScore = 50; // Base score for submitting something
        
        // Calculate additional score based on solution quality
        // This is a simple placeholder - in a real implementation, you'd analyze the solution
        int qualityScore = solution.length() > 10 ? 30 : 10;
        
        // Time-based score (faster = more points)
        int timeScore = 20; // Default time score
        
        return baseScore + qualityScore + timeScore;
    }
    
    private void notifyRoundComplete(GameSession gameSession) throws IOException {
        // Get the current scores
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
        
        // Start the next round
        gameSession.startRound();
    }
    
    private void endGame(GameSession gameSession) throws IOException {
        // Determine winner and update ELO
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
        
        // Calculate ELO changes
        int eloChange = 0;
        if (!isDraw) {
            User winner = userService.getUserById(winnerId).orElse(null);
            User loser = userService.getUserById(loserId).orElse(null);
            
            if (winner != null && loser != null) {
                int winnerElo = winner.getElo();
                int loserElo = loser.getElo();
                
                // Simple ELO calculation
                int expectedOutcome = 1 / (1 + (int)Math.pow(10, (loserElo - winnerElo) / 400.0));
                eloChange = 32 * (1 - expectedOutcome);
                
                // Update ELOs
                userService.updateUserElo(winnerId, eloChange);
                userService.updateUserElo(loserId, -eloChange);
            }
        }
        
        // Prepare end game messages
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
        
        // Clean up game resources
        gameSessions.remove(gameSession.getGameId());
        
        // Update player statuses to not in game
        waitingPlayers.stream()
            .filter(p -> p.getUserId().equals(gameSession.getPlayer1Id()) || 
                        p.getUserId().equals(gameSession.getPlayer2Id()))
            .forEach(p -> p.setInGame(false));
        
        // Broadcast lobby update
        broadcastLobbyUpdate();
    }

    private void handleGameAction(WebSocketSession session, GameMessage message) throws IOException {
        // Handle in-game actions like giving up, requesting time, etc.
        @SuppressWarnings("unchecked")
        Map<String, Object> actionData = objectMapper.readValue(message.getContent(), Map.class);
        
        String gameId = (String) actionData.get("gameId");
        String action = (String) actionData.get("action");
        
        GameSession gameSession = gameSessions.get(gameId);
        
        if (gameSession == null) {
            sendMessageToSession(session, new GameMessage("ERROR", "Game not found", message.getUserId()));
            return;
        }
        
        // Handle different actions
        switch (action) {
            case "FORFEIT":
                handleForfeit(gameSession, message.getUserId());
                break;
            // Add other actions as needed
        }
    }
    
    private void handleForfeit(GameSession gameSession, Long forfeittingUserId) throws IOException {
        // Determine the winner
        Long winnerId = gameSession.getPlayer1Id().equals(forfeittingUserId) ? 
            gameSession.getPlayer2Id() : gameSession.getPlayer1Id();
        
        // Update ELO
        userService.updateUserElo(winnerId, 25);
        userService.updateUserElo(forfeittingUserId, -25);
        
        // Send messages to both players
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
        
        // Clean up game resources
        gameSessions.remove(gameSession.getGameId());
        
        // Update player statuses
        waitingPlayers.stream()
            .filter(p -> p.getUserId().equals(gameSession.getPlayer1Id()) || 
                       p.getUserId().equals(gameSession.getPlayer2Id()))
            .forEach(p -> p.setInGame(false));
        
        // Broadcast lobby update
        broadcastLobbyUpdate();
    }

    private void broadcastLobbyUpdate() {
        // Send an updated player list to everyone in the lobby
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
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // Clean up session
        String sessionId = session.getId();
        sessions.remove(sessionId);
        
        // Find and remove the user from waiting list
        Optional<GamePlayer> player = waitingPlayers.stream()
            .filter(p -> p.getSessionId().equals(sessionId))
            .findFirst();
            
        player.ifPresent(p -> {
            waitingPlayers.remove(p);
            userToSessionMap.remove(p.getUserId());
            
            // If player was in a game, handle forfeit
            for (GameSession gameSession : gameSessions.values()) {
                if (gameSession.getPlayer1Id().equals(p.getUserId()) || 
                    gameSession.getPlayer2Id().equals(p.getUserId())) {
                    try {
                        handleForfeit(gameSession, p.getUserId());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
            
            // Broadcast lobby update
            broadcastLobbyUpdate();
        });
    }
}

// Separate static classes
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
        // If currentRound is 0, return the first puzzle
        if (currentRound <= 0 && !puzzleIds.isEmpty()) {
            return puzzleIds.get(0);
        }
        return puzzleIds.get(currentRound - 1);
    }
    
    public void startRound() {
        if (currentRound < 3) {
            currentRound++;
        }
        
        // Reset previous timer if exists
        if (timer != null) {
            timer.cancel();
        }
        
        // Set up a new timer for this round - 5 minutes
        timer = new Timer();
        final int roundNumber = currentRound;
        final String gameSessionId = gameId;
        
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // Time's up for this round
                try {
                    timerHandler.handleRoundTimeout(gameSessionId, roundNumber);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 300000); // 300000 ms = 5 minutes
    }
    
    public void nextRound() {
        if (timer != null) {
            timer.cancel();
        }
        
        // Start the next round
        if (currentRound < 3) {
            startRound();
        }
    }
    
    public void addPlayer1Score(int score) {
        while (player1Scores.size() < currentRound - 1) {
            player1Scores.add(0); // Fill any missing previous scores with 0
        }
        
        if (player1Scores.size() < currentRound) {
            player1Scores.add(score);
        } else {
            // Replace the score for the current round
            player1Scores.set(currentRound - 1, score);
        }
    }
    
    public void addPlayer2Score(int score) {
        while (player2Scores.size() < currentRound - 1) {
            player2Scores.add(0); // Fill any missing previous scores with 0
        }
        
        if (player2Scores.size() < currentRound) {
            player2Scores.add(score);
        } else {
            // Replace the score for the current round
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
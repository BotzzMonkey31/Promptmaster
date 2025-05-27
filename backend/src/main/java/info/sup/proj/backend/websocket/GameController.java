package info.sup.proj.backend.websocket;

import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import info.sup.proj.backend.services.GameService;
import info.sup.proj.backend.services.AiService;
import info.sup.proj.backend.model.Game;
import info.sup.proj.backend.services.AiService.ChatResponse;
import java.util.Map;
import java.util.HashMap;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.slf4j.Logger;

@Controller
public class GameController {

    private final GameService gameService;
    private final AiService aiService;
    private final SimpMessagingTemplate messagingTemplate;
    private static final Logger logger = LoggerFactory.getLogger(GameController.class);

    private static final String GAMESTATE = "GAME_STATE";
    private static final String PAYLOAD = "payload";
    private static final String TOPICGAME = "/topic/game/";
    private static final String QUEUEGAME = "/queue/game";
    private static final String ERROR = "ERROR";
    private static final String MESSAGE = "message";
    private static final String GAMEID = "gameId";
    private static final String PLAYERID = "playerId";

    public GameController(GameService gameService, AiService aiService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.aiService = aiService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/game/join")
    public void handleJoin(Map<String, Object> message) {
        String gameId = message.get(GAMEID).toString();
        String playerId = message.get(PLAYERID).toString();
        
        logger.info("Player {} joining game {}", playerId, gameId);
        
        Game game = gameService.getGame(gameId);
        if (game != null) {
            if (game.hasPlayer(playerId)) {
                // Ensure puzzle is initialized
                if (game.getCurrentPuzzle() == null) {
                    logger.info("Initializing game {} with puzzle for player {}", gameId, playerId);
                    game = gameService.initializeGameWithPuzzle(gameId);
                }

                Map<String, Object> gameState = new HashMap<>();
                gameState.put("type", GAMESTATE);
                gameState.put(PAYLOAD, game);

                logger.info("Sending game state to player {} with puzzle: {}", playerId, game.getCurrentPuzzle() != null ? game.getCurrentPuzzle().getName() : "null");

                // First send to the joining player directly
                messagingTemplate.convertAndSendToUser(
                    playerId,
                    QUEUEGAME,
                    gameState
                );
                
                // Then broadcast to all players
                messagingTemplate.convertAndSend(TOPICGAME + gameId, gameState);
                
                logger.info("Game state sent to player {} and broadcast to game {}", playerId, gameId);
            } else {
                logger.warn("Player {} attempted to join game {} but is not a participant", playerId, gameId);
                
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("type", ERROR);
                Map<String, String> payload = new HashMap<>();
                payload.put(MESSAGE, "You are not part of this game");
                errorResponse.put(PAYLOAD, payload);
                
                messagingTemplate.convertAndSendToUser(
                    playerId,
                    QUEUEGAME,
                    errorResponse
                );
            }
        } else {
            logger.warn("Game {} not found for player {}", gameId, playerId);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", ERROR);
            Map<String, String> payload = new HashMap<>();
            payload.put(MESSAGE, "Game not found");
            errorResponse.put(PAYLOAD, payload);
            
            messagingTemplate.convertAndSendToUser(
                playerId,
                QUEUEGAME,
                errorResponse
            );
        }
    }

    @MessageMapping("/game/prompt")
    public void handlePrompt(Map<String, Object> message) {
        String gameId = message.get(GAMEID).toString();
        String playerId = message.get(PLAYERID).toString();
        String prompt = message.get("prompt").toString();

        Game game = gameService.getGame(gameId);
        if (game != null) {
            if (game.hasPlayer(playerId)) {
                String currentCode = game.getPlayerCode(playerId);
                if (currentCode == null || currentCode.isEmpty()) {
                    currentCode = "";
                }
                
                try {
                    ChatResponse aiResponse = aiService.generateResponse(
                        prompt,
                        currentCode,
                        game.getCurrentPuzzle().getType()
                    );

                    Map<String, Object> response = new HashMap<>();
                    response.put("type", "AI_RESPONSE");
                    response.put("text", aiResponse.getText());
                    response.put("code", aiResponse.getCode());

                    messagingTemplate.convertAndSendToUser(
                        playerId,
                        QUEUEGAME,
                        response
                    );
                } catch (Exception e) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("type", "AI_RESPONSE");
                    errorResponse.put("text", "Sorry, I encountered an error while generating code. Please try a different prompt.");
                    errorResponse.put("code", "// Error generating code\n// Please try a different prompt");
                    
                    messagingTemplate.convertAndSendToUser(
                        playerId,
                        QUEUEGAME,
                        errorResponse
                    );
                }
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("type", ERROR);
                Map<String, String> payload = new HashMap<>();
                payload.put(MESSAGE, "You are not part of this game");
                errorResponse.put(PAYLOAD, payload);
                
                messagingTemplate.convertAndSendToUser(
                    playerId,
                    QUEUEGAME,
                    errorResponse
                );
            }
        } else {
            logger.info("GAME CONTROLLER ERROR: Game {} not found for prompt from player {}", gameId, playerId);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", ERROR);
            Map<String, String> payload = new HashMap<>();
            payload.put(MESSAGE, "Game not found");
            errorResponse.put(PAYLOAD, payload);
            
            messagingTemplate.convertAndSendToUser(
                playerId,
                QUEUEGAME,
                errorResponse
            );
        }
    }

    @MessageMapping("/game/{gameId}/submit")
    public void handleSubmission(@Payload Map<String, Object> message, @DestinationVariable String gameId) {
        String code = (String) message.get("code");
        String playerId = (String) message.get(PLAYERID);
        
        if (code == null || playerId == null) {
            throw new IllegalArgumentException("Missing required parameters: code or playerId");
        }
        
        Game game = gameService.getGame(gameId);
        if (game != null && game.hasPlayer(playerId)) {
            Map<String, Object> result = gameService.submitSolution(playerId, code);
            
            result.put(GAMEID, gameId);
            
            messagingTemplate.convertAndSendToUser(
                playerId,
                QUEUEGAME,
                result
            );
            
            Map<String, Object> gameState = new HashMap<>();
            gameState.put("type", GAMESTATE);
            gameState.put(PAYLOAD, game);
            
            messagingTemplate.convertAndSend(TOPICGAME + gameId, gameState);
        } else {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", ERROR);
            Map<String, String> payload = new HashMap<>();
            payload.put(MESSAGE, "Game not found or player not in game");
            errorResponse.put(PAYLOAD, payload);
            
            messagingTemplate.convertAndSendToUser(
                playerId,
                QUEUEGAME,
                errorResponse
            );
        }
    }

    @MessageMapping("/game/{gameId}/complete")
    @SendTo("/topic/game/{gameId}")
    public Game handlePuzzleCompletion(@Payload Map<String, Object> message, @DestinationVariable String gameId) {
        String playerId = (String) message.get(PLAYERID);
        return gameService.completePuzzle(playerId);
    }

    @MessageMapping("/game/{gameId}/forfeit")
    @SendTo("/topic/game/{gameId}")
    public Game handleForfeit(@Payload Map<String, Object> message, @DestinationVariable String gameId) {
        String playerId = (String) message.get(PLAYERID);
        return gameService.forfeitGame(playerId);
    }

    @MessageMapping("/game/{gameId}/code")
    public void handleCodeUpdate(@Payload Map<String, Object> message, @DestinationVariable String gameId) {
        String playerId = (String) message.get(PLAYERID);
        String code = (String) message.get("code");
        
        if (playerId == null || code == null) {
            throw new IllegalArgumentException("Missing required parameters: playerId or code");
        }
        
        Game game = gameService.getGame(gameId);
        if (game != null && game.hasPlayer(playerId)) {
            game.updateCurrentCode(playerId, code);
            
            Map<String, Object> gameState = new HashMap<>();
            gameState.put("type", GAMESTATE);
            gameState.put(PAYLOAD, game);
            
            messagingTemplate.convertAndSend(TOPICGAME + gameId, gameState);
        }
    }

    @MessageMapping("/game/{gameId}/next-round")
    @SendTo("/topic/game/{gameId}")
    public Game handleNextRound(@Payload Map<String, Object> message, @DestinationVariable String gameId) {
        String playerId = (String) message.get(PLAYERID);
        
        if (playerId == null) {
            throw new IllegalArgumentException("Missing required parameter: playerId");
        }
        
        Game game = gameService.getGame(gameId);
        if (game == null) {
            logger.info("REQUEST ERROR: Game not found: {}" , gameId);
            throw new IllegalArgumentException("Game not found");
        }
        
        if (!game.hasPlayer(playerId)) {
            logger.info("REQUEST ERROR: Player {} is not part of game {}" ,playerId,gameId);
            throw new IllegalArgumentException("Player is not part of this game");
        }
        
        try {
            return gameService.startNextRound(gameId, playerId);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", ERROR);
            Map<String, String> payload = new HashMap<>();
            payload.put(MESSAGE, "Failed to advance round: " + e.getMessage());
            errorResponse.put(PAYLOAD, payload);
            
            messagingTemplate.convertAndSendToUser(
                playerId,
                QUEUEGAME,
                errorResponse
            );
            return null;
        }
    }
} 
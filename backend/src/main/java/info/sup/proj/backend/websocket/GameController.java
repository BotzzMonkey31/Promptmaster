package info.sup.proj.backend.websocket;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import info.sup.proj.backend.services.GameService;
import info.sup.proj.backend.services.AiService;
import info.sup.proj.backend.model.Game;
import info.sup.proj.backend.services.AiService.ChatResponse;
import java.util.Map;
import java.util.HashMap;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.DestinationVariable;

@Controller
public class GameController {

    @Autowired
    private GameService gameService;

    @Autowired
    private AiService aiService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game/join")
    public void handleJoin(Map<String, Object> message) {
        String gameId = message.get("gameId").toString();
        String playerId = message.get("playerId").toString();
        String username = message.get("username").toString();
        String picture = message.get("picture") != null ? message.get("picture").toString() : null;
        
        Game game = gameService.getGame(gameId);
        if (game != null) {
            Map<String, Object> gameState = new HashMap<>();
            gameState.put("type", "GAME_STATE");
            gameState.put("payload", game);

            messagingTemplate.convertAndSend("/topic/game/" + gameId, gameState);
        } else {
            gameService.listAllGames().forEach(g -> {
            });
        }
    }

    @MessageMapping("/game/prompt")
    public void handlePrompt(Map<String, Object> message) {
        String gameId = message.get("gameId").toString();
        String playerId = message.get("playerId").toString();
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
                        game.getPuzzle().getType()
                    );

                    Map<String, Object> response = new HashMap<>();
                    response.put("type", "AI_RESPONSE");
                    response.put("text", aiResponse.getText());
                    response.put("code", aiResponse.getCode());

                    messagingTemplate.convertAndSendToUser(
                        playerId,
                        "/queue/game",
                        response
                    );
                } catch (Exception e) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("type", "AI_RESPONSE");
                    errorResponse.put("text", "Sorry, I encountered an error while generating code. Please try a different prompt.");
                    errorResponse.put("code", "// Error generating code\n// Please try a different prompt");
                    
                    messagingTemplate.convertAndSendToUser(
                        playerId,
                        "/queue/game",
                        errorResponse
                    );
                }
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("type", "ERROR");
                Map<String, String> payload = new HashMap<>();
                payload.put("message", "You are not part of this game");
                errorResponse.put("payload", payload);
                
                messagingTemplate.convertAndSendToUser(
                    playerId,
                    "/queue/game",
                    errorResponse
                );
            }
        } else {
            System.out.println("GAME CONTROLLER ERROR: Game " + gameId + " not found for prompt from player " + playerId);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", "ERROR");
            Map<String, String> payload = new HashMap<>();
            payload.put("message", "Game not found");
            errorResponse.put("payload", payload);
            
            messagingTemplate.convertAndSendToUser(
                playerId,
                "/queue/game",
                errorResponse
            );
        }
    }

    @MessageMapping("/game/{gameId}/submit")
    public void handleSubmission(@Payload Map<String, Object> message, @DestinationVariable String gameId) {
        String code = (String) message.get("code");
        String playerId = (String) message.get("playerId");
        
        if (code == null || playerId == null) {
            throw new IllegalArgumentException("Missing required parameters: code or playerId");
        }
        
        Game game = gameService.getGame(gameId);
        if (game != null && game.hasPlayer(playerId)) {
            Map<String, Object> result = gameService.submitSolution(playerId, code);
            
            result.put("gameId", gameId);
            
            messagingTemplate.convertAndSendToUser(
                playerId,
                "/queue/game",
                result
            );
            
            Map<String, Object> gameState = new HashMap<>();
            gameState.put("type", "GAME_STATE");
            gameState.put("payload", game);
            
            messagingTemplate.convertAndSend("/topic/game/" + gameId, gameState);
        } else {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", "ERROR");
            Map<String, String> payload = new HashMap<>();
            payload.put("message", "Game not found or player not in game");
            errorResponse.put("payload", payload);
            
            messagingTemplate.convertAndSendToUser(
                playerId,
                "/queue/game",
                errorResponse
            );
        }
    }

    @MessageMapping("/game/{gameId}/complete")
    @SendTo("/topic/game/{gameId}")
    public Game handlePuzzleCompletion(@Payload Map<String, Object> message, @DestinationVariable String gameId) {
        String playerId = (String) message.get("playerId");
        
        Game game = gameService.completePuzzle(playerId);
        
        if (game.getState() == Game.GameState.ENDED) {
            Map<String, Object> gameState = new HashMap<>();
            gameState.put("type", "GAME_STATE");
            gameState.put("payload", game);
            
            messagingTemplate.convertAndSend("/topic/game/" + gameId, gameState);
        }
        
        return game;
    }

    @MessageMapping("/game/{gameId}/forfeit")
    @SendTo("/topic/game/{gameId}")
    public Game handleForfeit(@Payload Map<String, Object> message, @DestinationVariable String gameId) {
        String playerId = (String) message.get("playerId");
        
        return gameService.forfeitGame(playerId);
    }

    @MessageMapping("/game/{gameId}/code")
    public void handleCodeUpdate(@Payload Map<String, Object> message, @DestinationVariable String gameId) {
        String playerId = (String) message.get("playerId");
        String code = (String) message.get("code");
        
        if (playerId == null || code == null) {
            throw new IllegalArgumentException("Missing required parameters: playerId or code");
        }
        
        Game game = gameService.getGame(gameId);
        if (game != null) {
            if (game.hasPlayer(playerId)) {
                game.updateCurrentCode(playerId, code);
                
                Map<String, Object> gameState = new HashMap<>();
                gameState.put("type", "GAME_STATE");
                gameState.put("payload", game);
                
                messagingTemplate.convertAndSend("/topic/game/" + gameId, gameState);
            }
        }
    }

    @MessageMapping("/game/{gameId}/next-round")
    @SendTo("/topic/game/{gameId}")
    public Game handleNextRound(@Payload Map<String, Object> message, @DestinationVariable String gameId) {
        String playerId = (String) message.get("playerId");
        Integer currentRound = (Integer) message.get("currentRound");
        Integer expectedNextRound = (Integer) message.get("expectedNextRound");
        
        if (currentRound != null && expectedNextRound != null) {
        }
        
        if (playerId == null) {
            throw new IllegalArgumentException("Missing required parameter: playerId");
        }
        
        Game game = gameService.getGame(gameId);
        if (game == null) {
            System.out.println("REQUEST ERROR: Game not found: " + gameId);
            throw new IllegalArgumentException("Game not found");
        }
        
        if (!game.hasPlayer(playerId)) {
            System.out.println("REQUEST ERROR: Player " + playerId + " is not part of game " + gameId);
            throw new IllegalArgumentException("Player is not part of this game");
        }
        
        if (currentRound != null && game.getCurrentRound() != currentRound) {
        }
        
        try {
            Game updatedGame = gameService.startNextRound(gameId, playerId);
            
            if (expectedNextRound != null && updatedGame.getCurrentRound() != expectedNextRound) {
            }
            
            return updatedGame;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", "ERROR");
            Map<String, String> payload = new HashMap<>();
            payload.put("message", "Failed to advance round: " + e.getMessage());
            errorResponse.put("payload", payload);
            
            messagingTemplate.convertAndSendToUser(
                playerId,
                "/queue/game",
                errorResponse
            );
            return null;
        }
    }
} 
package info.sup.proj.backend.websocket;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import info.sup.proj.backend.services.GameService;
import info.sup.proj.backend.services.AiService;
import info.sup.proj.backend.model.Game;
import info.sup.proj.backend.model.Player;
import info.sup.proj.backend.services.AiService.ChatResponse;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
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
        
        System.out.println("GAME CONTROLLER: Player " + playerId + " joined game " + gameId);

        Game game = gameService.getGame(gameId);
        if (game != null) {
            System.out.println("GAME CONTROLLER: Found game " + gameId + " for player " + playerId);
            
            // Ensure player is properly registered in this game
            if (!game.hasPlayer(playerId)) {
                System.out.println("GAME CONTROLLER WARNING: Player " + playerId + " not in game " + gameId + ", players: " + 
                    game.getPlayers().stream().map(Player::getId).collect(Collectors.toList()));
            }
            
            Map<String, Object> gameState = new HashMap<>();
            gameState.put("type", "GAME_STATE");
            gameState.put("payload", game);

            // Send game state to all subscribers of this game's topic
            messagingTemplate.convertAndSend("/topic/game/" + gameId, gameState);
        } else {
            System.out.println("GAME CONTROLLER ERROR: Game " + gameId + " not found for player " + playerId);
            // Log all active games for debugging
            System.out.println("GAME CONTROLLER: Active games:");
            gameService.listAllGames().forEach(g -> {
                System.out.println("Game " + g.getId() + " has players: " + 
                    g.getPlayers().stream().map(Player::getId).collect(Collectors.toList()));
            });
        }
    }

    @MessageMapping("/game/prompt")
    public void handlePrompt(Map<String, Object> message) {
        String gameId = message.get("gameId").toString();
        String playerId = message.get("playerId").toString();
        String prompt = message.get("prompt").toString();

        System.out.println("GAME CONTROLLER: Received prompt from player " + playerId + " for game " + gameId);
        System.out.println("GAME CONTROLLER: Prompt content: \"" + prompt + "\"");

        try {
            Game game = gameService.getGame(gameId);
            if (game != null) {
                // Check if the player is part of this game
                if (game.hasPlayer(playerId)) {
                    // Get player-specific code context instead of current turn player's code
                    String currentCode = game.getPlayerCode(playerId);
                    if (currentCode == null || currentCode.isEmpty()) {
                        // If player has no code yet, use empty string
                        currentCode = "";
                    }
                    
                    System.out.println("GAME CONTROLLER: Generating AI response for player " + playerId);
                    
                    try {
                        // Generate response using AI service
                        ChatResponse aiResponse = aiService.generateResponse(
                            prompt,
                            currentCode,
                            game.getPuzzle().getType()
                        );

                        System.out.println("GAME CONTROLLER: AI response generated successfully");
                        System.out.println("GAME CONTROLLER: Response text length: " + 
                                          (aiResponse.getText() != null ? aiResponse.getText().length() : 0));
                        System.out.println("GAME CONTROLLER: Response code length: " + 
                                          (aiResponse.getCode() != null ? aiResponse.getCode().length() : 0));

                        // Create response message
                        Map<String, Object> response = new HashMap<>();
                        response.put("type", "AI_RESPONSE");
                        response.put("text", aiResponse.getText());
                        response.put("code", aiResponse.getCode());

                        // Send response only to the requesting player
                        messagingTemplate.convertAndSendToUser(
                            playerId,
                            "/queue/game",
                            response
                        );
                        System.out.println("GAME CONTROLLER: AI response sent to player " + playerId);
                    } catch (Exception e) {
                        System.err.println("GAME CONTROLLER ERROR: Failed to generate AI response: " + e.getMessage());
                        e.printStackTrace();
                        
                        // Send error response to player
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
                    System.out.println("GAME CONTROLLER ERROR: Player " + playerId + " is not part of game " + gameId);
                    
                    // Send error response to player
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
                
                // Send error response to player
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
        } catch (Exception e) {
            System.err.println("GAME CONTROLLER ERROR: Unhandled exception in handlePrompt: " + e.getMessage());
            e.printStackTrace();
            
            // Send a fallback response to ensure the client isn't left hanging
            try {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("type", "AI_RESPONSE");
                errorResponse.put("text", "Sorry, I encountered an error processing your request.");
                errorResponse.put("code", "// Error occurred\n// Please try again");
                
                messagingTemplate.convertAndSendToUser(
                    playerId,
                    "/queue/game",
                    errorResponse
                );
            } catch (Exception ex) {
                System.err.println("GAME CONTROLLER ERROR: Failed to send error response: " + ex.getMessage());
            }
        }
    }

    @MessageMapping("/game/{gameId}/submit")
    public void handleSubmission(@Payload Map<String, Object> message, @DestinationVariable String gameId) {
        String code = (String) message.get("code");
        String playerId = (String) message.get("playerId");
        
        // Debug logging
        System.out.println("Handling solution submission for game: " + gameId + ", player: " + playerId);
        
        if (code == null || playerId == null) {
            throw new IllegalArgumentException("Missing required parameters: code or playerId");
        }
        
        Game game = gameService.getGame(gameId);
        if (game != null && game.hasPlayer(playerId)) {
            // Get submission result - allowing any player to submit regardless of turn
            Map<String, Object> result = gameService.submitSolution(playerId, code);
            
            // Add game ID for context
            result.put("gameId", gameId);
            
            // Send the score directly to the player who submitted
            messagingTemplate.convertAndSendToUser(
                playerId,
                "/queue/game",
                result
            );
            
            // Also update game state for all players
            Map<String, Object> gameState = new HashMap<>();
            gameState.put("type", "GAME_STATE");
            gameState.put("payload", game);
            
            messagingTemplate.convertAndSend("/topic/game/" + gameId, gameState);
        } else {
            System.out.println("Game not found or player not in game: " + gameId + ", player: " + playerId);
            // Send error response to player
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", "ERROR");
            Map<String, String> payload = new HashMap<>();
            payload.put("message", "Game not found or you are not part of this game");
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
        
        // Debug logging
        System.out.println("Handling puzzle completion for game: " + gameId + ", player: " + playerId);
        
        if (playerId == null) {
            throw new IllegalArgumentException("Missing required parameter: playerId");
        }
        
        return gameService.completePuzzle(playerId);
    }

    @MessageMapping("/game/{gameId}/forfeit")
    @SendTo("/topic/game/{gameId}")
    public Game handleForfeit(@Payload Map<String, Object> message, @DestinationVariable String gameId) {
        String playerId = (String) message.get("playerId");
        
        // Debug logging
        System.out.println("Handling forfeit for game: " + gameId + ", player: " + playerId);
        
        if (playerId == null) {
            throw new IllegalArgumentException("Missing required parameter: playerId");
        }
        
        return gameService.forfeitGame(playerId);
    }

    @MessageMapping("/game/{gameId}/code")
    public void handleCodeUpdate(@Payload Map<String, Object> message, @DestinationVariable String gameId) {
        String playerId = (String) message.get("playerId");
        String code = (String) message.get("code");
        
        // Debug logging
        System.out.println("Handling code update for game: " + gameId + ", player: " + playerId);
        
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
            } else {
                System.out.println("Player " + playerId + " not found in game: " + gameId);
            }
        } else {
            System.out.println("Game not found: " + gameId);
        }
    }

    @MessageMapping("/game/{gameId}/next-round")
    @SendTo("/topic/game/{gameId}")
    public Game handleNextRound(@Payload Map<String, Object> message, @DestinationVariable String gameId) {
        String playerId = (String) message.get("playerId");
        Integer currentRound = (Integer) message.get("currentRound");
        Integer expectedNextRound = (Integer) message.get("expectedNextRound");
        
        // Debug logging
        System.out.println("REQUEST: Next round for game: " + gameId + ", player: " + playerId);
        if (currentRound != null && expectedNextRound != null) {
            System.out.println("REQUEST: Current round: " + currentRound + ", expected next: " + expectedNextRound);
        }
        
        if (playerId == null) {
            throw new IllegalArgumentException("Missing required parameter: playerId");
        }
        
        // Find the game first to verify player membership
        Game game = gameService.getGame(gameId);
        if (game == null) {
            System.out.println("REQUEST ERROR: Game not found: " + gameId);
            throw new IllegalArgumentException("Game not found");
        }
        
        if (!game.hasPlayer(playerId)) {
            System.out.println("REQUEST ERROR: Player " + playerId + " is not part of game " + gameId);
            throw new IllegalArgumentException("Player is not part of this game");
        }
        
        // Verify the round number if provided
        if (currentRound != null && game.getCurrentRound() != currentRound) {
            System.out.println("REQUEST WARNING: Current round mismatch - client: " + 
                              currentRound + ", server: " + game.getCurrentRound());
        }
        
        // Call service method with both gameId and playerId
        try {
            Game updatedGame = gameService.startNextRound(gameId, playerId);
            
            // Verify the round advancement worked as expected
            if (expectedNextRound != null && updatedGame.getCurrentRound() != expectedNextRound) {
                System.out.println("REQUEST WARNING: Round advancement unexpected - expected: " + 
                                  expectedNextRound + ", actual: " + updatedGame.getCurrentRound());
            }
            
            return updatedGame;
        } catch (Exception e) {
            System.out.println("REQUEST ERROR: Failed to advance round: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
} 
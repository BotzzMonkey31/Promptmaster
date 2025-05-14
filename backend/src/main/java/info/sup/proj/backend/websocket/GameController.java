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

        Game game = gameService.getGame(gameId);
        if (game != null && game.getCurrentTurn().equals(playerId)) {
            // Get current code context
            String currentCode = game.getCurrentCode();

            // Generate response using AI service
            ChatResponse aiResponse = aiService.generateResponse(
                prompt,
                currentCode,
                game.getPuzzle().getType()
            );

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
        
        // Get submission result
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
        Game game = gameService.getGame(gameId);
        if (game != null) {
            Map<String, Object> gameState = new HashMap<>();
            gameState.put("type", "GAME_STATE");
            gameState.put("payload", game);
            
            messagingTemplate.convertAndSend("/topic/game/" + gameId, gameState);
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
            game.updateCurrentCode(playerId, code);
            
            // Broadcast updated game state
            Map<String, Object> gameState = new HashMap<>();
            gameState.put("type", "GAME_STATE");
            gameState.put("payload", game);
            
            messagingTemplate.convertAndSend("/topic/game/" + gameId, gameState);
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
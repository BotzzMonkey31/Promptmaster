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

            // Send game state to all subscribers of this game's topic
            messagingTemplate.convertAndSend("/topic/game/" + gameId, gameState);
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
    @SendTo("/topic/game/{gameId}")
    public Map<String, Object> handleSubmission(String code, String gameId) {
        return gameService.submitSolution(gameId, code);
    }

    @MessageMapping("/game/{gameId}/complete")
    @SendTo("/topic/game/{gameId}")
    public Game handlePuzzleCompletion(String gameId) {
        return gameService.completePuzzle(gameId);
    }

    @MessageMapping("/game/{gameId}/forfeit")
    @SendTo("/topic/game/{gameId}")
    public Game handleForfeit(String gameId) {
        return gameService.forfeitGame(gameId);
    }
} 
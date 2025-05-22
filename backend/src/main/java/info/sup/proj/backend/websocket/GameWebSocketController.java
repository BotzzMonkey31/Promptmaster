package info.sup.proj.backend.websocket;

import info.sup.proj.backend.services.MatchmakingService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class GameWebSocketController {
    private final MatchmakingService matchmakingService;
    private static final String USERID = "userId";

    public GameWebSocketController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @MessageMapping("/game/join-lobby")
    public void joinLobby(@Payload Map<String, Object> message) {
        Long userId = Long.parseLong(message.get(USERID).toString());
        matchmakingService.addPlayerToLobby(userId);
    }

    @MessageMapping("/game/leave-lobby")
    public void leaveLobby(@Payload Map<String, Object> message) {
        String userId = message.get(USERID).toString();
        matchmakingService.removePlayerFromLobby(userId);
    }

    @MessageMapping("/game/find-opponent")
    public void findOpponent(@Payload Map<String, Object> message) {
        String userId = message.get(USERID).toString();
        @SuppressWarnings("unchecked")
        Map<String, Object> preferences = (Map<String, Object>) message.get("preferences");
        matchmakingService.startSearchingForOpponent(userId, preferences);
    }

    @MessageMapping("/game/stop-searching")
    public void stopSearching(@Payload Map<String, Object> message) {
        String userId = message.get(USERID).toString();
        matchmakingService.stopSearchingForOpponent(userId);
    }

    @MessageMapping("/game/challenge-player")
    public void challengePlayer(@Payload Map<String, Object> message) {
        String challengerId = message.get(USERID).toString();
        String targetId = message.get("targetId").toString();
        matchmakingService.challengePlayer(challengerId, targetId);
    }

    @MessageMapping("/game/accept-challenge")
    public void acceptChallenge(@Payload Map<String, Object> message) {
        String userId = message.get(USERID).toString();
        matchmakingService.acceptChallenge(userId);
    }

    @MessageMapping("/game/reject-challenge")
    public void rejectChallenge(@Payload Map<String, Object> message) {
        String userId = message.get(USERID).toString();
        matchmakingService.rejectChallenge(userId);
    }
} 
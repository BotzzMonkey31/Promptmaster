package info.sup.proj.backend.websocket;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import info.sup.proj.backend.services.MatchmakingService;
import java.util.Map;

@Controller
public class MatchmakingController {
    private final MatchmakingService matchmakingService;

    public MatchmakingController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @MessageMapping("/game/join-lobby")
    public void joinLobby(@Payload Map<String, Object> message) {
        Long userId = ((Number) message.get("userId")).longValue();
        matchmakingService.addPlayerToLobby(userId);
    }

    @MessageMapping("/game/leave-lobby")
    public void leaveLobby(@Payload Map<String, Object> message) {
        String userId = message.get("userId").toString();
        matchmakingService.removePlayerFromLobby(userId);
    }

    @MessageMapping("/game/find-opponent")
    public void findOpponent(@Payload Map<String, Object> message) {
        String userId = message.get("userId").toString();
        @SuppressWarnings("unchecked")
        Map<String, Object> preferences = (Map<String, Object>) message.get("preferences");
        matchmakingService.startSearchingForOpponent(userId, preferences);
    }

    @MessageMapping("/game/stop-searching")
    public void stopSearching(@Payload Map<String, Object> message) {
        String userId = message.get("userId").toString();
        matchmakingService.stopSearchingForOpponent(userId);
    }

    @MessageMapping("/game/challenge-player")
    public void challengePlayer(@Payload Map<String, Object> message) {
        String userId = message.get("userId").toString();
        String targetId = message.get("targetId").toString();
        matchmakingService.challengePlayer(userId, targetId);
    }

    @MessageMapping("/game/accept-challenge")
    public void acceptChallenge(@Payload Map<String, Object> message) {
        String userId = message.get("userId").toString();
        matchmakingService.acceptChallenge(userId);
    }

    @MessageMapping("/game/reject-challenge")
    public void rejectChallenge(@Payload Map<String, Object> message) {
        String userId = message.get("userId").toString();
        matchmakingService.rejectChallenge(userId);
    }
} 
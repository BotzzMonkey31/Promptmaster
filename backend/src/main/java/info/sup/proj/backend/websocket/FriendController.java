package info.sup.proj.backend.websocket;

import info.sup.proj.backend.services.FriendshipService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class FriendController {

    private final FriendshipService friendshipService;

    public FriendController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @MessageMapping("/friend/request")
    public void sendFriendRequest(@Payload Map<String, Object> message) {
        Long userId = Long.parseLong(message.get("userId").toString());
        Long friendId = Long.parseLong(message.get("friendId").toString());
        
        friendshipService.sendFriendRequest(userId, friendId);
    }

    @MessageMapping("/friend/accept")
    public void acceptFriendRequest(@Payload Map<String, Object> message) {
        Long userId = Long.parseLong(message.get("userId").toString());
        Long friendshipId = Long.parseLong(message.get("friendshipId").toString());
        
        friendshipService.acceptFriendRequest(friendshipId, userId);
    }

    @MessageMapping("/friend/decline")
    public void declineFriendRequest(@Payload Map<String, Object> message) {
        Long userId = Long.parseLong(message.get("userId").toString());
        Long friendshipId = Long.parseLong(message.get("friendshipId").toString());
        
        friendshipService.declineFriendRequest(friendshipId, userId);
    }

    @MessageMapping("/friend/remove")
    public void removeFriend(@Payload Map<String, Object> message) {
        Long userId = Long.parseLong(message.get("userId").toString());
        Long friendId = Long.parseLong(message.get("friendId").toString());
        
        friendshipService.removeFriend(userId, friendId);
    }
}

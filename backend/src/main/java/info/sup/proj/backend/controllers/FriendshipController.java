package info.sup.proj.backend.controllers;

import info.sup.proj.backend.dto.ApiResponse;
import info.sup.proj.backend.model.Friendship;
import info.sup.proj.backend.model.User;
import info.sup.proj.backend.services.FriendshipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/friendships")
public class FriendshipController {

    private final FriendshipService friendshipService;
    private final Logger logger = LoggerFactory.getLogger(FriendshipController.class);

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<Friendship>> sendFriendRequest(@RequestBody Map<String, Long> request) {
        try {
            Long userId = request.get("userId");
            Long friendId = request.get("friendId");
            
            if (userId == null || friendId == null) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Both userId and friendId are required")
                );
            }
            
            Friendship friendship = friendshipService.sendFriendRequest(userId, friendId);
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Friend request sent successfully", friendship)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Error sending friend request", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "An error occurred while sending friend request"));
        }
    }

    @PostMapping("/{friendshipId}/accept")
    public ResponseEntity<ApiResponse<Friendship>> acceptFriendRequest(
            @PathVariable Long friendshipId,
            @RequestBody Map<String, Long> request) {
        try {
            Long userId = request.get("userId");
            
            if (userId == null) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "userId is required")
                );
            }
            
            Friendship friendship = friendshipService.acceptFriendRequest(friendshipId, userId);
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Friend request accepted", friendship)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Error accepting friend request", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "An error occurred while accepting friend request"));
        }
    }

    @PostMapping("/{friendshipId}/decline")
    public ResponseEntity<ApiResponse<Friendship>> declineFriendRequest(
            @PathVariable Long friendshipId,
            @RequestBody Map<String, Long> request) {
        try {
            Long userId = request.get("userId");
            
            if (userId == null) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "userId is required")
                );
            }
            
            Friendship friendship = friendshipService.declineFriendRequest(friendshipId, userId);
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Friend request declined", friendship)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Error declining friend request", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "An error occurred while declining friend request"));
        }
    }

    @DeleteMapping("/{userId}/remove/{friendId}")
    public ResponseEntity<ApiResponse<Void>> removeFriend(
            @PathVariable Long userId,
            @PathVariable Long friendId) {
        try {
            friendshipService.removeFriend(userId, friendId);
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Friend removed successfully")
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Error removing friend", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "An error occurred while removing friend"));
        }
    }

    @GetMapping("/{userId}/friends")
    public ResponseEntity<ApiResponse<List<User>>> getFriends(@PathVariable Long userId) {
        try {
            List<User> friends = friendshipService.getFriends(userId);
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Friends retrieved successfully", friends)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Error getting friends", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "An error occurred while fetching friends"));
        }
    }

    @GetMapping("/{userId}/requests")
    public ResponseEntity<ApiResponse<List<Friendship>>> getPendingFriendRequests(@PathVariable Long userId) {
        try {
            List<Friendship> requests = friendshipService.getPendingFriendRequests(userId);
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Friend requests retrieved successfully", requests)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Error getting friend requests", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "An error occurred while fetching friend requests"));
        }
    }

    @GetMapping("/{userId}/sent-requests")
    public ResponseEntity<ApiResponse<List<Friendship>>> getSentFriendRequests(@PathVariable Long userId) {
        try {
            List<Friendship> requests = friendshipService.getSentFriendRequests(userId);
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Sent friend requests retrieved successfully", requests)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Error getting sent friend requests", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "An error occurred while fetching sent friend requests"));
        }
    }
    
    @GetMapping("/{userId1}/check/{userId2}")
    public ResponseEntity<ApiResponse<Boolean>> checkFriendship(
            @PathVariable Long userId1,
            @PathVariable Long userId2) {
        try {
            boolean areFriends = friendshipService.areFriends(userId1, userId2);
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Friendship check completed", areFriends)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Error checking friendship status", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "An error occurred while checking friendship status"));
        }
    }
}

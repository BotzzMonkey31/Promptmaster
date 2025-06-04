package info.sup.proj.backend.services;

import info.sup.proj.backend.model.Friendship;
import info.sup.proj.backend.model.User;
import info.sup.proj.backend.repositories.FriendshipRepository;
import info.sup.proj.backend.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class FriendshipService {
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private static final Logger logger = LoggerFactory.getLogger(FriendshipService.class);

    public static final String USERNF = "User not found";
    public static final String TIMES = "timestamp";
    public static final String QUEUEF = "/queue/friend";


    public FriendshipService(FriendshipRepository friendshipRepository, UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Friendship sendFriendRequest(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("Cannot send a friend request to yourself");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(USERNF));
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("Friend not found"));

        // Check if a friendship already exists in either direction
        if (friendshipRepository.existsByUsers(user, friend)) {
            throw new IllegalArgumentException("A friendship already exists between these users");
        }

        Friendship friendship = new Friendship();
        friendship.setUser(user);
        friendship.setFriend(friend);
        friendship.setStatus(Friendship.Status.PENDING);
        
        Friendship savedFriendship = friendshipRepository.save(friendship);
        
        // Send notification to the recipient
        notifyFriendRequest(savedFriendship);
        
        return savedFriendship;
    }

    @Transactional
    public Friendship acceptFriendRequest(Long friendshipId, Long userId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        // Verify the user is the recipient of this request
        if (!friendship.getFriend().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot accept this friend request");
        }

        friendship.setStatus(Friendship.Status.ACCEPTED);
        Friendship acceptedFriendship = friendshipRepository.save(friendship);
        
        // Notify both users about the accepted request
        notifyFriendRequestAccepted(acceptedFriendship);
        
        return acceptedFriendship;
    }

    @Transactional
    public Friendship declineFriendRequest(Long friendshipId, Long userId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        // Verify the user is the recipient
        if (!friendship.getFriend().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot decline this friend request");
        }

        friendship.setStatus(Friendship.Status.DECLINED);
        return friendshipRepository.save(friendship);
    }    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(USERNF));
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("Friend not found"));

        // Find friendship in either direction
        Optional<Friendship> friendship = friendshipRepository.findByUserAndFriend(user, friend);
        if (friendship.isEmpty()) {
            friendship = friendshipRepository.findByUserAndFriend(friend, user);
        }        friendship.ifPresent(f -> {
            // Delete the friendship
            friendshipRepository.delete(f);
            
            // Notify the other user about the removal
            notifyFriendRemoval(userId, user, friend);
        });
    }
    
    private void notifyFriendRemoval(Long initiatorId, User initiator, User friend) {
        try {
            if (initiator == null || friend == null) {
                logger.error("Cannot send friend removal notification - invalid user data");
                return;
            }
            
            // Only notify the friend who didn't initiate the removal
            if (!friend.getId().equals(initiatorId)) {
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "FRIEND_REMOVED");
                notification.put("userId", initiator.getId());
                notification.put("username", initiator.getUsername());
                notification.put(TIMES, System.currentTimeMillis());
                
                messagingTemplate.convertAndSendToUser(
                    friend.getId().toString(),
                    QUEUEF,
                    notification
                );
                
                logger.info("Friend removal notification sent from {} to {}", 
                    initiator.getUsername(), friend.getUsername());
            }
        } catch (Exception e) {
            logger.error("Error sending friend removal notification", e);
        }
    }

    public List<User> getFriends(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(USERNF));

        List<Friendship> friendships = friendshipRepository.findAcceptedFriendships(user);
        
        return friendships.stream()
                .map(f -> f.getUser().getId().equals(userId) ? f.getFriend() : f.getUser())
                .toList();
    }

    public List<Friendship> getPendingFriendRequests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(USERNF));

        return friendshipRepository.findPendingFriendRequestsReceived(user);
    }

    public List<Friendship> getSentFriendRequests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(USERNF));

        return friendshipRepository.findPendingFriendRequestsSent(user);
    }
      public boolean areFriends(Long userId1, Long userId2) {
        User user1 = userRepository.findById(userId1)
                .orElseThrow(() -> new IllegalArgumentException(USERNF));
        // Verify user2 exists but we don't need the variable
        userRepository.findById(userId2)
                .orElseThrow(() -> new IllegalArgumentException(USERNF));
                
        List<Friendship> friendships = friendshipRepository.findAcceptedFriendships(user1);
        
        return friendships.stream()
                .anyMatch(f -> 
                    (f.getUser().getId().equals(userId2) || f.getFriend().getId().equals(userId2)) 
                    && f.getStatus() == Friendship.Status.ACCEPTED);
    }private void notifyFriendRequest(Friendship friendship) {
        try {
            if (friendship == null || friendship.getUser() == null || friendship.getFriend() == null) {
                logger.error("Cannot send friend request notification - invalid friendship data");
                return;
            }
            
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "FRIEND_REQUEST");
            notification.put("friendshipId", friendship.getId());
            notification.put("userId", friendship.getUser().getId());
            notification.put("username", friendship.getUser().getUsername());
            notification.put("userPicture", friendship.getUser().getPicture());
            notification.put(TIMES, System.currentTimeMillis());

            messagingTemplate.convertAndSendToUser(
                friendship.getFriend().getId().toString(),
                QUEUEF,
                notification
            );
            
            logger.info("Friend request notification sent from {} to {}", 
                friendship.getUser().getUsername(), 
                friendship.getFriend().getUsername());
        } catch (Exception e) {
            logger.error("Error sending friend request notification", e);
        }
    }
      private void notifyFriendRequestAccepted(Friendship friendship) {
        try {
            if (friendship == null || friendship.getUser() == null || friendship.getFriend() == null) {
                logger.error("Cannot send friend acceptance notification - invalid friendship data");
                return;
            }

            // Notification for the original requester
            Map<String, Object> requesterNotification = new HashMap<>();
            requesterNotification.put("type", "FRIEND_REQUEST_ACCEPTED");
            requesterNotification.put("friendId", friendship.getFriend().getId());
            requesterNotification.put("friendUsername", friendship.getFriend().getUsername());
            requesterNotification.put("friendPicture", friendship.getFriend().getPicture());
            requesterNotification.put(TIMES, System.currentTimeMillis());

            messagingTemplate.convertAndSendToUser(
                friendship.getUser().getId().toString(),
                QUEUEF,
                requesterNotification
            );
            
            // We could also notify the recipient if needed
            Map<String, Object> recipientNotification = new HashMap<>();
            recipientNotification.put("type", "FRIEND_ADDED");
            recipientNotification.put("friendId", friendship.getUser().getId());
            recipientNotification.put("friendUsername", friendship.getUser().getUsername());
            recipientNotification.put("friendPicture", friendship.getUser().getPicture());
            recipientNotification.put(TIMES, System.currentTimeMillis());
            
            messagingTemplate.convertAndSendToUser(
                friendship.getFriend().getId().toString(),
                QUEUEF,
                recipientNotification
            );
            
            logger.info("Friend acceptance notifications sent between {} and {}", 
                friendship.getUser().getUsername(), 
                friendship.getFriend().getUsername());
        } catch (Exception e) {
            logger.error("Error sending friend acceptance notification", e);
        }
    }
}

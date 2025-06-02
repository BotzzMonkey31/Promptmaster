package info.sup.proj.backend.repositories;

import info.sup.proj.backend.model.Friendship;
import info.sup.proj.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    // Find all friendships where the user is either the initiator or the recipient
    @Query("SELECT f FROM Friendship f WHERE (f.user = :user OR f.friend = :user) AND f.status = :status")
    List<Friendship> findAllFriendshipsByUserAndStatus(@Param("user") User user, @Param("status") Friendship.Status status);
    
    // Find all accepted friendships for a user
    @Query("SELECT f FROM Friendship f WHERE (f.user = :user OR f.friend = :user) AND f.status = 'ACCEPTED'")
    List<Friendship> findAcceptedFriendships(@Param("user") User user);
    
    // Find all pending friend requests received by the user
    @Query("SELECT f FROM Friendship f WHERE f.friend = :user AND f.status = 'PENDING'")
    List<Friendship> findPendingFriendRequestsReceived(@Param("user") User user);
    
    // Find all pending friend requests sent by the user
    @Query("SELECT f FROM Friendship f WHERE f.user = :user AND f.status = 'PENDING'")
    List<Friendship> findPendingFriendRequestsSent(@Param("user") User user);
    
    // Find a specific friendship between two users
    Optional<Friendship> findByUserAndFriend(User user, User friend);
    
    // Check if a friendship already exists between two users (regardless of who initiated it)
    @Query("SELECT COUNT(f) > 0 FROM Friendship f WHERE (f.user = :user1 AND f.friend = :user2) OR (f.user = :user2 AND f.friend = :user1)")
    boolean existsByUsers(@Param("user1") User user1, @Param("user2") User user2);
}

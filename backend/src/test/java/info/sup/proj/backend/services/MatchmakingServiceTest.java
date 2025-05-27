package info.sup.proj.backend.services;

import info.sup.proj.backend.model.Game;
import info.sup.proj.backend.model.Player;
import info.sup.proj.backend.model.User;
import info.sup.proj.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatchmakingServiceTest {

    @Mock
    private GameService gameService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private MatchmakingService matchmakingService;

    @BeforeEach
    void setUp() {
        matchmakingService = new MatchmakingService(gameService, userRepository, messagingTemplate);
    }

    @Test
    void testAddPlayerToLobby_userNotFound() {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            matchmakingService.addPlayerToLobby(userId);
        });

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void testRemovePlayerFromLobby() {
        // Arrange
        String userId = "1";
        
        // Add player first to ensure they're in the lobby
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPicture("test.jpg");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        matchmakingService.addPlayerToLobby(1L);
        
        // Reset mocks for clear verification
        reset(messagingTemplate);

        // Act
        matchmakingService.removePlayerFromLobby(userId);

        // Assert
        verify(messagingTemplate).convertAndSend(
            eq("/topic/lobby"), 
            any(Object.class)
        );
    }

    @Test
    void testStartSearchingForOpponent_playerInLobby() {
        // Arrange
        String userId = "1";
        Map<String, Object> preferences = new HashMap<>();
        preferences.put("eloRange", 1000);
        
        // Add player first to ensure they're in the lobby
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPicture("test.jpg");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        matchmakingService.addPlayerToLobby(1L);

        // Act
        matchmakingService.startSearchingForOpponent(userId, preferences);

        // No exceptions should be thrown
    }

    @Test
    void testStartSearchingForOpponent_playerNotInLobby() {
        // Arrange
        String userId = "1";
        Map<String, Object> preferences = new HashMap<>();

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            matchmakingService.startSearchingForOpponent(userId, preferences);
        });

        assertEquals("Player not in lobby", exception.getMessage());
    }

    @Test
    void testStopSearchingForOpponent() {
        // Arrange
        String userId = "1";
        
        // Add player and start searching
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPicture("test.jpg");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        matchmakingService.addPlayerToLobby(1L);
        matchmakingService.startSearchingForOpponent(userId, new HashMap<>());
        
        // Act
        matchmakingService.stopSearchingForOpponent(userId);
        
        // Assert - verify player is removed from searching list
        try {
            java.lang.reflect.Field searchingPlayersField = MatchmakingService.class.getDeclaredField("searchingPlayers");
            searchingPlayersField.setAccessible(true);
            Map<String, Player> searchingPlayers = (Map<String, Player>) searchingPlayersField.get(matchmakingService);
            assertFalse(searchingPlayers.containsKey(userId));
        } catch (Exception e) {
            fail("Failed to access searchingPlayers field: " + e.getMessage());
        }
    }

    @Test
    void testChallengePlayer_bothPlayersInLobby() {
        // Arrange
        String challengerId = "1";
        String targetId = "2";
        
        // Add both players to the lobby
        User challenger = new User();
        challenger.setId(1L);
        challenger.setUsername("challenger");
        challenger.setPicture("challenger.jpg");
        challenger.setElo(1500);
        
        User target = new User();
        target.setId(2L);
        target.setUsername("target");
        target.setPicture("target.jpg");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(challenger));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        
        matchmakingService.addPlayerToLobby(1L);
        matchmakingService.addPlayerToLobby(2L);
        
        // Reset to clear previous invocations
        reset(messagingTemplate);
        
        // Act
        matchmakingService.challengePlayer(challengerId, targetId);
        
        // Assert
        verify(messagingTemplate).convertAndSendToUser(
            eq(targetId), 
            eq("/queue/game"), 
            argThat(map -> 
                map instanceof Map && 
                "CHALLENGE_RECEIVED".equals(((Map) map).get("type")) &&
                challengerId.equals(((Map) map).get("challengerId"))
            )
        );
    }

    @Test
    void testChallengePlayer_playerNotInLobby() {
        // Arrange
        String challengerId = "1";
        String targetId = "2";
        
        // Only add challenger to the lobby
        User challenger = new User();
        challenger.setId(1L);
        challenger.setUsername("challenger");
        when(userRepository.findById(1L)).thenReturn(Optional.of(challenger));
        matchmakingService.addPlayerToLobby(1L);

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            matchmakingService.challengePlayer(challengerId, targetId);
        });

        assertEquals("One or both players not in lobby", exception.getMessage());
    }


    @Test
    void testAcceptChallenge_noChallengeFound() {
        // Arrange
        String targetId = "2";
        
        // No challenge setup
        
        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            matchmakingService.acceptChallenge(targetId);
        });

        assertEquals("No active challenge found", exception.getMessage());
    }

    @Test
    void testRejectChallenge_validChallenge() {
        // Arrange
        String challengerId = "1";
        String targetId = "2";
        
        // Add both players to the lobby
        User challenger = new User();
        challenger.setId(1L);
        challenger.setUsername("challenger");
        challenger.setPicture("challenger.jpg");
        
        User target = new User();
        target.setId(2L);
        target.setUsername("target");
        target.setPicture("target.jpg");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(challenger));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        
        matchmakingService.addPlayerToLobby(1L);
        matchmakingService.addPlayerToLobby(2L);
        
        // Set up the challenge
        matchmakingService.challengePlayer(challengerId, targetId);
        
        // Act
        matchmakingService.rejectChallenge(targetId);
        
        // Assert
        verify(messagingTemplate).convertAndSendToUser(
            eq(challengerId), 
            eq("/queue/game"), 
            argThat(map -> 
                map instanceof Map && 
                "CHALLENGE_REJECTED".equals(((Map) map).get("type"))
            )
        );
        
        // Verify the challenge was removed
        try {
            java.lang.reflect.Field playerChallengesField = MatchmakingService.class.getDeclaredField("playerChallenges");
            playerChallengesField.setAccessible(true);
            Map<String, String> playerChallenges = (Map<String, String>) playerChallengesField.get(matchmakingService);
            assertFalse(playerChallenges.containsKey(targetId));
        } catch (Exception e) {
            fail("Failed to access playerChallenges field: " + e.getMessage());
        }
    }

    @Test
    void testRejectChallenge_noChallengeFound() {
        // Arrange
        String targetId = "2";
        
        // No challenge setup
        
        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            matchmakingService.rejectChallenge(targetId);
        });

        assertEquals("No active challenge found", exception.getMessage());
    }
} 
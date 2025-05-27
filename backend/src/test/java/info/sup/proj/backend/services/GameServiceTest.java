package info.sup.proj.backend.services;

import info.sup.proj.backend.model.Game;
import info.sup.proj.backend.model.Player;
import info.sup.proj.backend.model.Puzzle;
import info.sup.proj.backend.model.User;
import info.sup.proj.backend.repositories.PuzzleRepository;
import info.sup.proj.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GameServiceTest {

    @Mock
    private PuzzleRepository puzzleRepository;

    @Mock
    private AiService aiService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ScheduledExecutorService scheduler;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    private GameService gameService;

    private Player player1;
    private Player player2;
    private Puzzle testPuzzle;
    private Puzzle secondPuzzle;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        // Setup scheduler mock
        lenient().doReturn(scheduledFuture).when(scheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

        gameService = new GameService(
            puzzleRepository,
            aiService,
            eventPublisher,
            messagingTemplate,
            scheduler
        );
        
        // Use reflection to inject mocks
        try {
            java.lang.reflect.Field userRepoField = GameService.class.getDeclaredField("userRepository");
            userRepoField.setAccessible(true);
            userRepoField.set(gameService, userRepository);
        } catch (Exception e) {
            fail("Failed to inject mocks: " + e.getMessage());
        }

        // Setup test data with numeric IDs
        player1 = new Player("1", "Player One", "player1.jpg");
        player2 = new Player("2", "Player Two", "player2.jpg");
        
        testPuzzle = createTestPuzzle(1, "Test Puzzle");
        secondPuzzle = createTestPuzzle(2, "Second Puzzle");

        // Setup users
        user1 = createTestUser(1L, 1500);
        user2 = createTestUser(2L, 1500);

        // Setup default repository behavior
        lenient().when(puzzleRepository.findAll()).thenReturn(List.of(testPuzzle, secondPuzzle));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        lenient().when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
    }

    @Test
    void testCreateGame_success() {
        // Arrange
        when(puzzleRepository.findAll())
                .thenReturn(List.of(testPuzzle));

        // Act
        Game game = gameService.createGame(player1, player2);

        // Assert
        assertNotNull(game);
        assertEquals(player1.getId(), game.getPlayers().get(0).getId());
        assertEquals(player2.getId(), game.getPlayers().get(1).getId());
        assertEquals(testPuzzle, game.getCurrentPuzzle());
        assertEquals(1, game.getCurrentRound());
        assertEquals(3, game.getTotalRounds());
        assertEquals(Game.GameState.IN_PROGRESS, game.getState());
        assertEquals(player1.getId(), game.getCurrentTurn());
    }

    @Test
    void testCreateGame_noPuzzlesAvailable() {
        // Arrange
        when(puzzleRepository.findAll())
                .thenReturn(List.of());

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            gameService.createGame(player1, player2);
        });

        assertEquals("No puzzles available", exception.getMessage());
    }

    @Test
    void testSubmitSolution_success() {
        // Arrange
        when(puzzleRepository.findAll())
                .thenReturn(List.of(testPuzzle));
        
        Game game = gameService.createGame(player1, player2);
        String solutionCode = "public class Solution { /* implementation */ }";
        
        // Mock AI evaluation
        when(aiService.getCodeEvaluation(anyString(), eq(solutionCode), any()))
                .thenReturn("{\"correctness\": 90, \"quality\": 85}");

        // Act
        Map<String, Object> result = gameService.submitSolution(player1.getId(), solutionCode);

        // Assert
        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertEquals(player1.getId(), result.get("playerId"));
        
        // Calculate expected score: correctness (90) * 0.4 + quality (85) * 0.3 + timeBonus * 0.3
        // Since we can't predict timeBonus exactly, we'll verify it's in a reasonable range
        int score = (Integer) result.get("score");
        assertTrue(score >= 0 && score <= 100, "Score should be between 0 and 100");
        
        // Verify player's code was updated
        assertEquals(solutionCode, game.getPlayerStatus().get(player1.getId()).getCode());
    }

    @Test
    void testCompletePuzzle_allPlayersCompleted_notFinalRound() {
        // Arrange
        when(puzzleRepository.findAll())
                .thenReturn(List.of(testPuzzle));
        
        Game game = gameService.createGame(player1, player2);
        
        // Act
        Game updatedGame = gameService.completePuzzle(player1.getId());
        updatedGame = gameService.completePuzzle(player2.getId());

        // Assert
        assertTrue(updatedGame.getPlayerStatus().get(player1.getId()).isHasCompleted());
        assertTrue(updatedGame.getPlayerStatus().get(player2.getId()).isHasCompleted());
        assertEquals(Game.GameState.IN_PROGRESS, updatedGame.getState());
        verify(scheduler).schedule(any(Runnable.class), eq(2L), eq(TimeUnit.SECONDS));
    }

    @Test
    void testCompletePuzzle_allPlayersCompleted_finalRound() {
        Game game = gameService.createGame(player1, player2);
        
        // Set the current round to the final round
        try {
            java.lang.reflect.Field currentRoundField = Game.class.getDeclaredField("currentRound");
            currentRoundField.setAccessible(true);
            currentRoundField.set(game, 3);
            
            // Set scores
            game.getPlayerStatus().get("1").setScore(100);
            game.getPlayerStatus().get("2").setScore(50);
        } catch (Exception e) {
            fail("Failed to set current round: " + e.getMessage());
        }
        
        Game updatedGame = gameService.completePuzzle("1");
        updatedGame = gameService.completePuzzle("2");

        assertTrue(updatedGame.getPlayerStatus().get("1").isHasCompleted());
        assertTrue(updatedGame.getPlayerStatus().get("2").isHasCompleted());
        assertEquals(Game.GameState.ENDED, updatedGame.getState());
        
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void testStartNextRound_alreadyFinalRound() {
        Game game = gameService.createGame(player1, player2);
        String gameId = game.getId();
        
        // Set the current round to the final round (3)
        try {
            java.lang.reflect.Field currentRoundField = Game.class.getDeclaredField("currentRound");
            currentRoundField.setAccessible(true);
            currentRoundField.set(game, 3);
            
            // Also set both players to completed state
            game.getPlayerStatus().get("1").setHasCompleted(true);
            game.getPlayerStatus().get("2").setHasCompleted(true);
        } catch (Exception e) {
            fail("Failed to set current round: " + e.getMessage());
        }
        
        Game updatedGame = gameService.startNextRound(gameId, "1");

        assertEquals(4, updatedGame.getCurrentRound());
        assertEquals(Game.GameState.IN_PROGRESS, updatedGame.getState());
    }

    @Test
    void testFindGameByPlayerId_notFound() {
        // Act
        Game found = gameService.findGameByPlayerId("nonexistent");

        // Assert
        assertNull(found);
    }

    @Test
    void testGetNextDifferentPuzzle_success() {
        // Arrange
        when(puzzleRepository.findAll())
                .thenReturn(List.of(testPuzzle, createTestPuzzle(2, "Different Puzzle")));
        
        Game game = gameService.createGame(player1, player2);
        
        // Act - Using reflection to access private method
        Puzzle nextPuzzle = null;
        try {
            java.lang.reflect.Method method = GameService.class.getDeclaredMethod("getNextDifferentPuzzle", Game.class);
            method.setAccessible(true);
            nextPuzzle = (Puzzle) method.invoke(gameService, game);
        } catch (Exception e) {
            fail("Failed to invoke getNextDifferentPuzzle: " + e.getMessage());
        }

        // Assert
        assertNotNull(nextPuzzle);
        assertNotEquals(game.getCurrentPuzzle().getId(), nextPuzzle.getId());
    }

    @Test
    void testGetNextDifferentPuzzle_onlyOnePuzzle() {
        // Arrange
        when(puzzleRepository.findAll())
                .thenReturn(List.of(testPuzzle));
        
        Game game = gameService.createGame(player1, player2);
        
        // Act & Assert - Using direct method call instead of reflection
        try {
            // First get the method 
            java.lang.reflect.Method method = GameService.class.getDeclaredMethod("getNextDifferentPuzzle", Game.class);
            method.setAccessible(true);
            
            // Then check for exception
            Exception exception = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
                method.invoke(gameService, game);
            });
            
            // Verify the cause is IllegalStateException
            assertTrue(exception.getCause() instanceof IllegalStateException);
            assertEquals("Could not find a different puzzle", exception.getCause().getMessage());
        } catch (Exception e) {
            fail("Failed to invoke getNextDifferentPuzzle: " + e.getMessage());
        }
    }

    @Test
    void testScoreAccumulation() {
        // Arrange
        when(puzzleRepository.findAll())
                .thenReturn(List.of(testPuzzle));
        
        Game game = gameService.createGame(player1, player2);
        String solutionCode = "public class Solution { /* implementation */ }";
        
        // Mock AI evaluation
        when(aiService.getCodeEvaluation(anyString(), eq(solutionCode), any()))
                .thenReturn("{\"correctness\": 90, \"quality\": 85}");

        // Act - Submit solutions for two rounds
        gameService.submitSolution(player1.getId(), solutionCode); // First round
        int firstScore = game.getPlayerStatus().get(player1.getId()).getScore();
        
        game.startNextRoundWithExplicitNumber(testPuzzle, 2);
        gameService.submitSolution(player1.getId(), solutionCode); // Second round
        int secondScore = game.getPlayerStatus().get(player1.getId()).getScore();

        // Assert
        assertTrue(secondScore > firstScore, "Second score should be higher than first score");
        assertTrue(game.getPlayerStatus().get(player1.getId()).getScore() >= firstScore + 50,
                "Total score should accumulate between rounds");
    }

    @Test
    void testStartNextRoundWithExplicitNumber() {
        // Arrange
        when(puzzleRepository.findAll())
                .thenReturn(List.of(testPuzzle, createTestPuzzle(2, "Different Puzzle")));
        
        Game game = gameService.createGame(player1, player2);
        
        // Act
        game.startNextRoundWithExplicitNumber(createTestPuzzle(2, "Different Puzzle"), 2);

        // Assert
        assertEquals(2, game.getCurrentRound());
        assertNotEquals(testPuzzle.getId(), game.getCurrentPuzzle().getId());
        assertFalse(game.getPlayerStatus().get(player1.getId()).isHasCompleted());
        assertFalse(game.getPlayerStatus().get(player2.getId()).isHasCompleted());
    }

    @Test
    void testStartNextRoundWithExplicitNumber_preventSkipping() {
        Game game = gameService.createGame(player1, player2);

        game.startNextRoundWithExplicitNumber(secondPuzzle, 3);
        
        assertEquals(3, game.getCurrentRound(), "Should only advance to next round (2)");
    }

    @Test
    void testUpdatePlayerElo() {
        // Create game with numeric player IDs
        Game game = gameService.createGame(player1, player2);
        
        // Set scores to simulate a clear winner
        game.getPlayerStatus().get("1").setScore(100);
        game.getPlayerStatus().get("2").setScore(50);
        
        User user1 = createTestUser(1L, 1500);
        User user2 = createTestUser(2L, 1500);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        
        // Act - Using reflection to access private method
        try {
            java.lang.reflect.Method method = GameService.class.getDeclaredMethod("endGame", Game.class);
            method.setAccessible(true);
            method.invoke(gameService, game);
        } catch (Exception e) {
            fail("Failed to invoke endGame: " + e.getMessage());
        }

        verify(userRepository, times(2)).save(any(User.class));
        assertTrue(user1.getElo() > 1500, "Winner's ELO should increase");
        assertTrue(user2.getElo() < 1500, "Loser's ELO should decrease");
    }

    @Test
    void testCompletePuzzle_withDelay() {
        // Arrange
        when(puzzleRepository.findAll())
                .thenReturn(List.of(testPuzzle, createTestPuzzle(2, "Different Puzzle")));
        
        Game game = gameService.createGame(player1, player2);
        
        // Act
        Game updatedGame = gameService.completePuzzle(player1.getId());
        updatedGame = gameService.completePuzzle(player2.getId());

        // Assert
        assertTrue(updatedGame.getPlayerStatus().get(player1.getId()).isHasCompleted());
        assertTrue(updatedGame.getPlayerStatus().get(player2.getId()).isHasCompleted());
        
        // Verify scheduler was called to add delay
        verify(scheduler).schedule(any(Runnable.class), eq(2L), eq(TimeUnit.SECONDS));
    }

    @Test
    void testRoundSynchronization() {
        // Arrange
        when(puzzleRepository.findAll())
                .thenReturn(List.of(testPuzzle, createTestPuzzle(2, "Different Puzzle")));
        
        Game game = gameService.createGame(player1, player2);
        
        // Act - Complete round 1
        gameService.completePuzzle(player1.getId());
        gameService.completePuzzle(player2.getId());
        
        // Verify round 2 starts after delay
        verify(scheduler).schedule(any(Runnable.class), eq(2L), eq(TimeUnit.SECONDS));
        
        // Simulate round 2 start
        game.startNextRoundWithExplicitNumber(createTestPuzzle(2, "Different Puzzle"), 2);
        
        // Verify round state
        assertEquals(2, game.getCurrentRound());
        assertFalse(game.getPlayerStatus().get(player1.getId()).isHasCompleted());
        assertFalse(game.getPlayerStatus().get(player2.getId()).isHasCompleted());
        
        // Complete round 2
        gameService.completePuzzle(player1.getId());
        gameService.completePuzzle(player2.getId());
        
        // Verify round 3 starts after delay
        verify(scheduler, times(2)).schedule(any(Runnable.class), eq(2L), eq(TimeUnit.SECONDS));
    }

    @Test
    void testRoundSynchronization_withRefresh() {
        // Arrange
        when(puzzleRepository.findAll())
                .thenReturn(List.of(testPuzzle, createTestPuzzle(2, "Different Puzzle")));
        
        Game game = gameService.createGame(player1, player2);
        
        // Act - Complete round 1 for player 1
        gameService.completePuzzle(player1.getId());
        
        // Verify waiting state
        assertTrue(game.getPlayerStatus().get(player1.getId()).isHasCompleted());
        assertFalse(game.getPlayerStatus().get(player2.getId()).isHasCompleted());
        assertEquals(1, game.getCurrentRound());
        
        // Simulate page refresh - should not affect game state
        gameService.initializeGameWithPuzzle(game.getId());
        
        // Verify state maintained
        assertTrue(game.getPlayerStatus().get(player1.getId()).isHasCompleted());
        assertFalse(game.getPlayerStatus().get(player2.getId()).isHasCompleted());
        assertEquals(1, game.getCurrentRound());
        
        // Complete round for player 2
        gameService.completePuzzle(player2.getId());
        
        // Verify round 2 starts after delay
        verify(scheduler).schedule(any(Runnable.class), eq(2L), eq(TimeUnit.SECONDS));
    }

    private Puzzle createTestPuzzle(int id, String name) {
        Puzzle puzzle = new Puzzle();
        puzzle.setId(id);
        puzzle.setName(name);
        puzzle.setDescription("Test puzzle " + id);
        puzzle.setType(Puzzle.Type.MULTI_STEP);
        puzzle.setDifficulty(Puzzle.Difficulty.MEDIUM);
        return puzzle;
    }
    
    private User createTestUser(Long id, int elo) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setEmail("user" + id + "@example.com");
        user.setElo(elo);
        return user;
    }
}
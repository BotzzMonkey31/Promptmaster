package info.sup.proj.backend.services;

import info.sup.proj.backend.model.Game;
import info.sup.proj.backend.model.Player;
import info.sup.proj.backend.model.Puzzle;
import info.sup.proj.backend.repositories.PuzzleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GameServiceTest {

    @Mock
    private PuzzleRepository puzzleRepository;

    @Mock
    private AiService aiService;

    private GameService gameService;

    private Player player1;
    private Player player2;
    private Puzzle testPuzzle;

    @BeforeEach
    void setUp() {
        gameService = new GameService(puzzleRepository, aiService);
        
        // Use reflection to inject mocks
        try {
            java.lang.reflect.Field puzzleRepoField = GameService.class.getDeclaredField("puzzleRepository");
            puzzleRepoField.setAccessible(true);
            puzzleRepoField.set(gameService, puzzleRepository);

            java.lang.reflect.Field aiServiceField = GameService.class.getDeclaredField("aiService");
            aiServiceField.setAccessible(true);
            aiServiceField.set(gameService, aiService);
        } catch (Exception e) {
            fail("Failed to inject mocks: " + e.getMessage());
        }

        // Setup test data
        player1 = new Player("player1", "Player One", "player1.jpg");
        player2 = new Player("player2", "Player Two", "player2.jpg");
        
        testPuzzle = new Puzzle();
        testPuzzle.setId(1);
        testPuzzle.setName("File Word Counter");
        testPuzzle.setDescription("Count words in file");
        testPuzzle.setType(Puzzle.Type.MULTI_STEP);
        testPuzzle.setDifficulty(Puzzle.Difficulty.MEDIUM);
    }

    @Test
    void testCreateGame_success() {
        // Arrange
        when(puzzleRepository.findByType(Puzzle.Type.MULTI_STEP))
                .thenReturn(List.of(testPuzzle));

        // Act
        Game game = gameService.createGame(player1, player2);

        // Assert
        assertNotNull(game);
        assertEquals(player1.getId(), game.getPlayers().get(0).getId());
        assertEquals(player2.getId(), game.getPlayers().get(1).getId());
        assertEquals(testPuzzle, game.getPuzzle());
        assertEquals(1, game.getCurrentRound());
        assertEquals(3, game.getTotalRounds());
        assertEquals(Game.GameState.IN_PROGRESS, game.getState());
        assertEquals(player1.getId(), game.getCurrentTurn());
    }

    @Test
    void testCreateGame_noPuzzlesAvailable() {
        // Arrange
        when(puzzleRepository.findByType(Puzzle.Type.MULTI_STEP))
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
        when(puzzleRepository.findByType(Puzzle.Type.MULTI_STEP))
                .thenReturn(List.of(testPuzzle));
        
        Game game = gameService.createGame(player1, player2);
        String solutionCode = "public class Solution { /* implementation */ }";
        
        // Mock AI evaluation
        when(aiService.getCodeEvaluation(anyString(), eq(solutionCode), eq(Puzzle.Type.MULTI_STEP)))
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
        assertEquals(solutionCode, game.getPlayerCode(player1.getId()));
    }

    @Test
    void testCompletePuzzle_allPlayersCompleted_notFinalRound() {
        // Arrange
        when(puzzleRepository.findByType(Puzzle.Type.MULTI_STEP))
                .thenReturn(List.of(testPuzzle));
        
        Game game = gameService.createGame(player1, player2);
        
        // Act
        Game updatedGame = gameService.completePuzzle(player1.getId());
        updatedGame = gameService.completePuzzle(player2.getId());

        // Assert
        assertTrue(updatedGame.getPlayerStatus().get(player1.getId()).isHasCompleted());
        assertTrue(updatedGame.getPlayerStatus().get(player2.getId()).isHasCompleted());
        assertEquals(Game.GameState.IN_PROGRESS, updatedGame.getState());
    }

    @Test
    void testCompletePuzzle_allPlayersCompleted_finalRound() {
        // Arrange
        when(puzzleRepository.findByType(Puzzle.Type.MULTI_STEP))
                .thenReturn(List.of(testPuzzle));
        
        Game game = gameService.createGame(player1, player2);
        
        // Set the current round to the final round
        try {
            java.lang.reflect.Field currentRoundField = Game.class.getDeclaredField("currentRound");
            currentRoundField.setAccessible(true);
            currentRoundField.set(game, game.getTotalRounds());
        } catch (Exception e) {
            fail("Failed to set current round: " + e.getMessage());
        }
        
        // Act
        Game updatedGame = gameService.completePuzzle(player1.getId());
        updatedGame = gameService.completePuzzle(player2.getId());

        // Assert
        assertTrue(updatedGame.getPlayerStatus().get(player1.getId()).isHasCompleted());
        assertTrue(updatedGame.getPlayerStatus().get(player2.getId()).isHasCompleted());
        assertEquals(Game.GameState.ENDED, updatedGame.getState());
    }

    @Test
    void testForfeitGame() {
        // Arrange
        when(puzzleRepository.findByType(Puzzle.Type.MULTI_STEP))
                .thenReturn(List.of(testPuzzle));
        
        Game game = gameService.createGame(player1, player2);
        
        // Act
        Game updatedGame = gameService.forfeitGame(player1.getId());

        // Assert
        assertEquals(Game.GameState.ENDED, updatedGame.getState());
        assertTrue(updatedGame.getPlayerStatus().get(player1.getId()).isHasCompleted());
        assertEquals(0, updatedGame.getPlayerStatus().get(player1.getId()).getScore());
    }

    @Test
    void testStartNextRound_success() {
        // Arrange
        when(puzzleRepository.findByType(Puzzle.Type.MULTI_STEP))
                .thenReturn(List.of(testPuzzle));
        
        Game game = gameService.createGame(player1, player2);
        String gameId = game.getId();
        
        // Mark both players as completed
        gameService.completePuzzle(player1.getId());
        gameService.completePuzzle(player2.getId());
        
        // Act
        Game updatedGame = gameService.startNextRound(gameId, player1.getId());

        // Assert
        assertEquals(2, updatedGame.getCurrentRound());
        assertEquals(testPuzzle, updatedGame.getPuzzle());
        assertFalse(updatedGame.getPlayerStatus().get(player1.getId()).isHasCompleted());
        assertFalse(updatedGame.getPlayerStatus().get(player2.getId()).isHasCompleted());
        assertEquals("", updatedGame.getPlayerCode(player1.getId()));
        assertEquals("", updatedGame.getPlayerCode(player2.getId()));
    }

    @Test
    void testStartNextRound_alreadyFinalRound() {
        // Arrange
        when(puzzleRepository.findByType(Puzzle.Type.MULTI_STEP))
                .thenReturn(List.of(testPuzzle));
        
        Game game = gameService.createGame(player1, player2);
        String gameId = game.getId();
        
        // Set the current round to the final round
        try {
            java.lang.reflect.Field currentRoundField = Game.class.getDeclaredField("currentRound");
            currentRoundField.setAccessible(true);
            currentRoundField.set(game, game.getTotalRounds());
        } catch (Exception e) {
            fail("Failed to set current round: " + e.getMessage());
        }
        
        // Act
        Game updatedGame = gameService.startNextRound(gameId, player1.getId());

        // Assert
        assertEquals(game.getTotalRounds(), updatedGame.getCurrentRound());
        assertEquals(game, updatedGame); // Should return the same game object without changes
    }

    @Test
    void testFindGameByPlayerId_found() {
        // Arrange
        when(puzzleRepository.findByType(Puzzle.Type.MULTI_STEP))
                .thenReturn(List.of(testPuzzle));
        
        Game game = gameService.createGame(player1, player2);
        
        // Act
        Game found = gameService.findGameByPlayerId(player1.getId());

        // Assert
        assertNotNull(found);
        assertEquals(game.getId(), found.getId());
    }

    @Test
    void testFindGameByPlayerId_notFound() {
        // Act
        Game found = gameService.findGameByPlayerId("nonexistent");

        // Assert
        assertNull(found);
    }
} 
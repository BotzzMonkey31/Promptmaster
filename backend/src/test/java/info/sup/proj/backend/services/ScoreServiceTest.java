package info.sup.proj.backend.services;

import info.sup.proj.backend.model.Puzzle;
import info.sup.proj.backend.model.PuzzleSession;
import info.sup.proj.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScoreServiceTest {

    @Mock
    private AiService aiService;

    @InjectMocks
    private ScoreService scoreService;

    private PuzzleSession session;
    private Puzzle puzzle;
    private User user;
    private LocalDateTime now;
    private LocalDateTime tenMinutesAgo;

    @BeforeEach
    void setUp() {
        // Setup time values first
        now = LocalDateTime.now();
        tenMinutesAgo = now.minus(10, ChronoUnit.MINUTES);
        
        // Setup user
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        // Setup puzzle with correct structure
        puzzle = new Puzzle();
        puzzle.setId(1);
        puzzle.setName("Test Puzzle");
        puzzle.setType(Puzzle.Type.MULTI_STEP);
        puzzle.setDescription("A multi-step test puzzle");
        puzzle.setDifficulty(Puzzle.Difficulty.MEDIUM);

        // Setup session with accessible fields
        session = new PuzzleSession();
        session.setId(1L);
        session.setUser(user);
        session.setPuzzle(puzzle);
        session.setCurrentCode("public class Solution { public void solve() { /* code */ } }");
        
        // Use reflection to set protected datetime fields
        try {
            java.lang.reflect.Field createdAtField = PuzzleSession.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(session, tenMinutesAgo);
            
            java.lang.reflect.Field lastUpdatedAtField = PuzzleSession.class.getDeclaredField("lastUpdatedAt");
            lastUpdatedAtField.setAccessible(true);
            lastUpdatedAtField.set(session, now);
        } catch (Exception e) {
            // If reflection fails, we'll use the getter methods in our assertions to validate time
            System.err.println("Failed to set datetime fields via reflection: " + e.getMessage());
        }
    }

    @Test
    void testCalculateScore_GoodSubmission() {
        // Arrange
        // Add interactions to session
        session.addInteraction("How do I read a file?", "You can use FileInputStream", "import java.io.*;");
        session.addInteraction("How to parse CSV?", "Use a library", "import csv.*;");
        session.addInteraction("How to sort data?", "Use Collections.sort", "Collections.sort(list);");
        
        // Mock AI evaluation
        when(aiService.getCodeEvaluation(anyString(), anyString(), eq(Puzzle.Type.MULTI_STEP)))
            .thenReturn("{\"correctness\": 90, \"quality\": 85}");
        
        // Act
        Map<String, Object> scoreDetails = scoreService.calculateScore(session);
        
        // Assert
        assertNotNull(scoreDetails);
        assertFalse((Boolean) scoreDetails.get("hasFailed"));
        assertTrue((Integer) scoreDetails.get("totalScore") > 0);
        assertEquals(90, scoreDetails.get("correctnessScore"));
        assertEquals(85, scoreDetails.get("codeQualityScore"));
        assertEquals(3, scoreDetails.get("interactionCount"));
        
        // Verify time calculation - allow for minor differences due to test execution time
        long timeSeconds = (Long) scoreDetails.get("timeSeconds");
        assertTrue(timeSeconds >= 590 && timeSeconds <= 610, "Time should be approximately 600 seconds");
        
        // Verify AI interaction
        verify(aiService).getCodeEvaluation(anyString(), eq(session.getCurrentCode()), eq(Puzzle.Type.MULTI_STEP));
    }
    
    @Test
    void testCalculateScore_InsufficientInteractions() {
        // Arrange - only one interaction, which is below the minimum for Medium difficulty
        session.addInteraction("How do I solve this?", "Break it down", "");
        
        // Act
        Map<String, Object> scoreDetails = scoreService.calculateScore(session);
        
        // Assert
        assertNotNull(scoreDetails);
        assertEquals(0, scoreDetails.get("totalScore")); // Should get zero score due to insufficient interactions
        
        // Verify no AI evaluation call was made
        verifyNoInteractions(aiService);
    }
    
    @Test
    void testCalculateScore_EmptyCode() {
        // Arrange - sufficient interactions but no code
        session.addInteraction("How do I read a file?", "You can use FileInputStream", "");
        session.addInteraction("How to parse CSV?", "Use a library", "");
        session.addInteraction("How to sort data?", "Use Collections.sort", "");
        session.setCurrentCode("");
        
        // Act
        Map<String, Object> scoreDetails = scoreService.calculateScore(session);
        
        // Assert
        assertNotNull(scoreDetails);
        assertEquals(0, scoreDetails.get("totalScore")); // Should get zero score due to empty code
        
        // Verify no AI evaluation call was made
        verifyNoInteractions(aiService);
    }
    
    @Test
    void testCalculateScore_ExceptionalPerformance() {
        // Arrange - very few interactions and quick completion
        session.addInteraction("How do I solve this efficiently?", "Here's an approach", "import java.util.*;");
        session.addInteraction("How to optimize?", "Try this algorithm", "public void optimizedSolution() {}");
        
        // Set completion in just 2 minutes
        LocalDateTime twoMinutesAgo = now.minus(2, ChronoUnit.MINUTES);
        try {
            java.lang.reflect.Field createdAtField = PuzzleSession.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(session, twoMinutesAgo);
        } catch (Exception e) {
            System.err.println("Failed to update createdAt field: " + e.getMessage());
        }
        
        // Mock AI evaluation with high scores
        when(aiService.getCodeEvaluation(anyString(), anyString(), eq(Puzzle.Type.MULTI_STEP)))
            .thenReturn("{\"correctness\": 100, \"quality\": 95}");
        
        // Act
        Map<String, Object> scoreDetails = scoreService.calculateScore(session);
        
        // Assert
        assertNotNull(scoreDetails);
        assertFalse((Boolean) scoreDetails.get("hasFailed"));
        assertTrue((Integer) scoreDetails.get("totalScore") >= 90); // Should get very high score
        assertEquals(100, scoreDetails.get("correctnessScore"));
        assertEquals(95, scoreDetails.get("codeQualityScore"));
        assertEquals(2, scoreDetails.get("interactionCount"));
        
        // Verify time calculation with some flexibility
        long timeSeconds = (Long) scoreDetails.get("timeSeconds");
        assertTrue(timeSeconds >= 110 && timeSeconds <= 130, "Time should be approximately 120 seconds");
        
        // Verify AI interaction
        verify(aiService).getCodeEvaluation(anyString(), eq(session.getCurrentCode()), eq(Puzzle.Type.MULTI_STEP));
    }
    
    @Test
    void testCalculateScore_AIEvaluationFailure() {
        // Arrange
        session.addInteraction("How do I read a file?", "You can use FileInputStream", "import java.io.*;");
        session.addInteraction("How to parse CSV?", "Use a library", "import csv.*;");
        session.addInteraction("How to sort data?", "Use Collections.sort", "Collections.sort(list);");
        
        // Mock AI evaluation to throw exception
        when(aiService.getCodeEvaluation(anyString(), anyString(), eq(Puzzle.Type.MULTI_STEP)))
            .thenThrow(new RuntimeException("AI evaluation failed"));
        
        // Act
        Map<String, Object> scoreDetails = scoreService.calculateScore(session);
        
        // Assert
        assertNotNull(scoreDetails);
        assertFalse((Boolean) scoreDetails.get("hasFailed"));
        assertTrue((Integer) scoreDetails.get("totalScore") > 0);
        assertEquals(75, scoreDetails.get("correctnessScore")); // Default values when AI fails
        assertEquals(70, scoreDetails.get("codeQualityScore")); // Default values when AI fails
        
        // Verify AI interaction
        verify(aiService).getCodeEvaluation(anyString(), eq(session.getCurrentCode()), eq(Puzzle.Type.MULTI_STEP));
    }
    
    @Test
    void testCalculateScore_DifficultLevelImpact() {
        // Test with Easy difficulty
        puzzle.setDifficulty(Puzzle.Difficulty.EASY);
        testScoreWithDifficulty();
        
        // Test with Medium difficulty
        puzzle.setDifficulty(Puzzle.Difficulty.MEDIUM);
        testScoreWithDifficulty();
        
        // Test with Hard difficulty
        puzzle.setDifficulty(Puzzle.Difficulty.HARD);
        testScoreWithDifficulty();
    }
    
    private void testScoreWithDifficulty() {
        // Clear existing interactions first
        try {
            java.lang.reflect.Field interactionsField = PuzzleSession.class.getDeclaredField("interactions");
            interactionsField.setAccessible(true);
            interactionsField.set(session, new java.util.ArrayList<>());
        } catch (Exception e) {
            System.err.println("Failed to clear interactions: " + e.getMessage());
        }
        
        // Add appropriate number of interactions based on difficulty
        switch (puzzle.getDifficulty()) {
            case EASY:
                // For Easy, add 3 interactions (minimum is 3)
                session.addInteraction("How do I solve this?", "Try this approach", "some code;");
                session.addInteraction("How to optimize?", "Use this algorithm", "optimized code;");
                session.addInteraction("Final question", "Final answer", "final code;");
                break;
            case MEDIUM:
            case HARD:
                // For Medium and Hard, add 2 interactions (minimum is 2)
                session.addInteraction("How do I solve this?", "Try this approach", "some code;");
                session.addInteraction("How to optimize?", "Use this algorithm", "optimized code;");
                break;
        }
        
        // Setup session timing based on difficulty - use times that will result in good scores
        LocalDateTime startTime;
        switch (puzzle.getDifficulty()) {
            case EASY:
                // 5 minutes for Easy (expected time is 10 minutes)
                startTime = now.minus(5, ChronoUnit.MINUTES);
                break;
            case MEDIUM:
                // 4 minutes for Medium (expected time is 7.5 minutes)
                startTime = now.minus(4, ChronoUnit.MINUTES);
                break;
            case HARD:
            default:
                // 3 minutes for Hard (expected time is 5 minutes)
                startTime = now.minus(3, ChronoUnit.MINUTES);
                break;
        }
        
        try {
            java.lang.reflect.Field createdAtField = PuzzleSession.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(session, startTime);
        } catch (Exception e) {
            System.err.println("Failed to update createdAt field: " + e.getMessage());
        }
        
        // Mock AI evaluation with good scores to prevent failing
        when(aiService.getCodeEvaluation(anyString(), anyString(), any()))
            .thenReturn("{\"correctness\": 85, \"quality\": 80}");
        
        // Act
        Map<String, Object> scoreDetails = scoreService.calculateScore(session);
        
        // Assert
        assertNotNull(scoreDetails);
        assertFalse((Boolean) scoreDetails.get("hasFailed"));
        assertTrue((Integer) scoreDetails.get("totalScore") > 0);
        
        // Reset mocks for next test
        reset(aiService);
    }
}
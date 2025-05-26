package info.sup.proj.backend.services;

import info.sup.proj.backend.model.Puzzle;
import info.sup.proj.backend.model.PuzzleSession;
import info.sup.proj.backend.model.User;
import info.sup.proj.backend.dto.SessionMetricsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
        now = LocalDateTime.now();
        tenMinutesAgo = now.minus(10, ChronoUnit.MINUTES);
        
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        puzzle = new Puzzle();
        puzzle.setId(1);
        puzzle.setName("Test Puzzle");
        puzzle.setType(Puzzle.Type.MULTI_STEP);
        puzzle.setDescription("A multi-step test puzzle");
        puzzle.setDifficulty(Puzzle.Difficulty.MEDIUM);

        session = new PuzzleSession();
        session.setId(1L);
        session.setUser(user);
        session.setPuzzle(puzzle);
        session.setCurrentCode("public class Solution { public void solve() { /* code */ } }");
        
        try {
            java.lang.reflect.Field createdAtField = PuzzleSession.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(session, tenMinutesAgo);
            
            java.lang.reflect.Field lastUpdatedAtField = PuzzleSession.class.getDeclaredField("lastUpdatedAt");
            lastUpdatedAtField.setAccessible(true);
            lastUpdatedAtField.set(session, now);
        } catch (Exception e) {
            System.err.println("Failed to set datetime fields via reflection: " + e.getMessage());
        }
    }

    @Test
    void testCalculateScore_GoodSubmission() {
        session.addInteraction("How do I read a file?", "You can use FileInputStream", "import java.io.*;");
        session.addInteraction("How to parse CSV?", "Use a library", "import csv.*;");
        session.addInteraction("How to sort data?", "Use Collections.sort", "Collections.sort(list);");
        
        when(aiService.getCodeEvaluation(anyString(), anyString(), eq(Puzzle.Type.MULTI_STEP)))
            .thenReturn("{\"correctness\": 90, \"quality\": 85}");
        
        SessionMetricsDto scoreDetails = scoreService.calculateScore(session);
        
        assertNotNull(scoreDetails);
        assertFalse(scoreDetails.getHasFailed());
        assertTrue(scoreDetails.getTotalScore() > 0);
        assertEquals(90, scoreDetails.getCorrectnessScore());
        assertEquals(85, scoreDetails.getCodeQualityScore());
        assertEquals(3, scoreDetails.getInteractionCount());
        
        long timeSeconds = scoreDetails.getTimeSeconds();
        assertTrue(timeSeconds >= 590L && timeSeconds <= 610L, "Time should be approximately 600 seconds");
        
        verify(aiService).getCodeEvaluation(anyString(), eq(session.getCurrentCode()), eq(Puzzle.Type.MULTI_STEP));
    }
    
    @Test
    void testCalculateScore_EmptyCode() {
        session.addInteraction("How do I read a file?", "You can use FileInputStream", "");
        session.addInteraction("How to parse CSV?", "Use a library", "");
        session.addInteraction("How to sort data?", "Use Collections.sort", "");
        session.setCurrentCode("");
        
        SessionMetricsDto scoreDetails = scoreService.calculateScore(session);
        
        assertNotNull(scoreDetails);
        assertEquals(0, scoreDetails.getTotalScore());
        
        verifyNoInteractions(aiService);
    }
    
    @Test
    void testCalculateScore_ExceptionalPerformance() {
        session.addInteraction("How do I solve this efficiently?", "Here's an approach", "import java.util.*;");
        session.addInteraction("How to optimize?", "Try this algorithm", "public void optimizedSolution() {}");
        
        LocalDateTime twoMinutesAgo = now.minus(2, ChronoUnit.MINUTES);
        try {
            java.lang.reflect.Field createdAtField = PuzzleSession.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(session, twoMinutesAgo);
        } catch (Exception e) {
            System.err.println("Failed to update createdAt field: " + e.getMessage());
        }
        
        when(aiService.getCodeEvaluation(anyString(), anyString(), eq(Puzzle.Type.MULTI_STEP)))
            .thenReturn("{\"correctness\": 100, \"quality\": 95}");
        
        SessionMetricsDto scoreDetails = scoreService.calculateScore(session);
        
        assertNotNull(scoreDetails);
        assertFalse(scoreDetails.getHasFailed());
        assertTrue(scoreDetails.getTotalScore() >= 90);
        assertEquals(100, scoreDetails.getCorrectnessScore());
        assertEquals(95, scoreDetails.getCodeQualityScore());
        assertEquals(2, scoreDetails.getInteractionCount());
        
        long timeSeconds = scoreDetails.getTimeSeconds();
        assertTrue(timeSeconds >= 110L && timeSeconds <= 130L, "Time should be approximately 120 seconds");
        
        verify(aiService).getCodeEvaluation(anyString(), eq(session.getCurrentCode()), eq(Puzzle.Type.MULTI_STEP));
    }
    
    @Test
    void testCalculateScore_AIEvaluationFailure() {
        session.addInteraction("How do I read a file?", "You can use FileInputStream", "import java.io.*;");
        session.addInteraction("How to parse CSV?", "Use a library", "import csv.*;");
        session.addInteraction("How to sort data?", "Use Collections.sort", "Collections.sort(list);");
        
        when(aiService.getCodeEvaluation(anyString(), anyString(), eq(Puzzle.Type.MULTI_STEP)))
            .thenThrow(new RuntimeException("AI evaluation failed"));
        
        SessionMetricsDto scoreDetails = scoreService.calculateScore(session);
        
        assertNotNull(scoreDetails);
        assertFalse(scoreDetails.getHasFailed());
        assertTrue(scoreDetails.getTotalScore() > 0);
        assertEquals(75, scoreDetails.getCorrectnessScore());
        assertEquals(70, scoreDetails.getCodeQualityScore());
        
        verify(aiService).getCodeEvaluation(anyString(), eq(session.getCurrentCode()), eq(Puzzle.Type.MULTI_STEP));
    }
    
    @Test
    void testCalculateScore_DifficultLevelImpact() {
        puzzle.setDifficulty(Puzzle.Difficulty.EASY);
        testScoreWithDifficulty();
        
        puzzle.setDifficulty(Puzzle.Difficulty.MEDIUM);
        testScoreWithDifficulty();
        
        puzzle.setDifficulty(Puzzle.Difficulty.HARD);
        testScoreWithDifficulty();
    }
    
    private void testScoreWithDifficulty() {
        try {
            java.lang.reflect.Field interactionsField = PuzzleSession.class.getDeclaredField("interactions");
            interactionsField.setAccessible(true);
            interactionsField.set(session, new java.util.ArrayList<>());
        } catch (Exception e) {
            System.err.println("Failed to clear interactions: " + e.getMessage());
        }
        
        switch (puzzle.getDifficulty()) {
            case EASY:
                session.addInteraction("How do I solve this?", "Try this approach", "some code;");
                session.addInteraction("How to optimize?", "Use this algorithm", "optimized code;");
                session.addInteraction("Final question", "Final answer", "final code;");
                break;
            case MEDIUM:
            case HARD:
                session.addInteraction("How do I solve this?", "Try this approach", "some code;");
                session.addInteraction("How to optimize?", "Use this algorithm", "optimized code;");
                break;
        }
        
        LocalDateTime startTime;
        switch (puzzle.getDifficulty()) {
            case EASY:
                startTime = now.minus(5, ChronoUnit.MINUTES);
                break;
            case MEDIUM:
                startTime = now.minus(4, ChronoUnit.MINUTES);
                break;
            case HARD:
            default:
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
        
        when(aiService.getCodeEvaluation(anyString(), anyString(), eq(puzzle.getType())))
            .thenReturn("{\"correctness\": 95, \"quality\": 90}");
        
        SessionMetricsDto scoreDetails = scoreService.calculateScore(session);
        
        assertNotNull(scoreDetails);
        assertFalse(scoreDetails.getHasFailed());
        assertTrue(scoreDetails.getTotalScore() > 0);
        assertEquals(95, scoreDetails.getCorrectnessScore());
        assertEquals(90, scoreDetails.getCodeQualityScore());
        
        verify(aiService).getCodeEvaluation(anyString(), eq(session.getCurrentCode()), eq(puzzle.getType()));
    }
}
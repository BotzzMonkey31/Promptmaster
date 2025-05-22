package info.sup.proj.backend.services;

import info.sup.proj.backend.model.Puzzle;
import info.sup.proj.backend.model.PuzzleSession;
import info.sup.proj.backend.model.User;
import info.sup.proj.backend.repositories.PuzzleRepository;
import info.sup.proj.backend.repositories.PuzzleSessionRepository;
import info.sup.proj.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PuzzleSessionServiceTest {

    @Mock
    private PuzzleSessionRepository sessionRepository;

    @Mock
    private PuzzleRepository puzzleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ScoreService scoreService;

    @InjectMocks
    private PuzzleSessionService sessionService;

    private Puzzle testPuzzle;
    private User testUser;
    private PuzzleSession testSession;

    @BeforeEach
    void setUp() {
        // Setup test entities
        testPuzzle = new Puzzle();
        testPuzzle.setId(1);
        testPuzzle.setName("Test Puzzle"); // Changed from setTitle to setName
        testPuzzle.setType(Puzzle.Type.MULTI_STEP);
        testPuzzle.setDescription("A test puzzle");
        testPuzzle.setDifficulty(Puzzle.Difficulty.MEDIUM); // Changed from ordinal to enum
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        
        testSession = new PuzzleSession();
        testSession.setId(1L);
        testSession.setPuzzle(testPuzzle);
        testSession.setUser(testUser);
        testSession.setCurrentCode("public class Solution {}");
    }

    @Test
    void testGetOrCreateSession_ExistingSession() {
        // Arrange
        Integer puzzleId = 1;
        Long userId = 1L;
        when(sessionRepository.findByPuzzleIdAndUserId(puzzleId, userId))
            .thenReturn(Optional.of(testSession));
        
        // Act
        PuzzleSession result = sessionService.getOrCreateSession(puzzleId, userId);
        
        // Assert
        assertNotNull(result);
        assertEquals(testSession, result);
        verify(sessionRepository).findByPuzzleIdAndUserId(puzzleId, userId);
        // Verify we didn't try to create a new session
        verifyNoMoreInteractions(puzzleRepository, userRepository);
    }

    @Test
    void testGetOrCreateSession_NewSession() {
        // Arrange
        Integer puzzleId = 1;
        Long userId = 1L;
        when(sessionRepository.findByPuzzleIdAndUserId(puzzleId, userId))
            .thenReturn(Optional.empty());
        when(puzzleRepository.findById(puzzleId))
            .thenReturn(Optional.of(testPuzzle));
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(testUser));
        
        PuzzleSession newSession = new PuzzleSession();
        newSession.setPuzzle(testPuzzle);
        newSession.setUser(testUser);
        when(sessionRepository.save(any(PuzzleSession.class))).thenReturn(newSession);
        
        // Act
        PuzzleSession result = sessionService.getOrCreateSession(puzzleId, userId);
        
        // Assert
        assertNotNull(result);
        assertEquals(testPuzzle, result.getPuzzle());
        assertEquals(testUser, result.getUser());
        verify(sessionRepository).findByPuzzleIdAndUserId(puzzleId, userId);
        verify(puzzleRepository).findById(puzzleId);
        verify(userRepository).findById(userId);
        verify(sessionRepository).save(any(PuzzleSession.class));
    }

    @Test
    void testAddInteraction() {
        // Arrange
        Integer puzzleId = 1;
        Long userId = 1L;
        String userInput = "How do I read a file?";
        String aiTextResponse = "You can use FileInputStream";
        String aiCodeResponse = "import java.io.FileInputStream;";
        
        when(sessionRepository.findByPuzzleIdAndUserId(puzzleId, userId))
            .thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(PuzzleSession.class))).thenReturn(testSession);
        
        // Act
        PuzzleSession result = sessionService.addInteraction(puzzleId, userId, userInput, aiTextResponse, aiCodeResponse);
        
        // Assert
        assertNotNull(result);
        // Verify the session was updated with the code
        assertTrue(result.getCurrentCode().contains("import java.io.FileInputStream;"));
        verify(sessionRepository).findByPuzzleIdAndUserId(puzzleId, userId);
        verify(sessionRepository).save(testSession);
    }

    @Test
    void testResetSession_ExistingSession() {
        // Arrange
        Integer puzzleId = 1;
        Long userId = 1L;
        
        testSession.setAttemptCount(3);
        testSession.setBestInteractionCount(5);
        testSession.setBestTimeSeconds(300L);
        
        when(sessionRepository.findByPuzzleIdAndUserId(puzzleId, userId))
            .thenReturn(Optional.of(testSession));
        doNothing().when(sessionRepository).delete(testSession);
        doNothing().when(sessionRepository).flush();
        
        // Setup for creating new session
        when(puzzleRepository.findById(puzzleId))
            .thenReturn(Optional.of(testPuzzle));
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(testUser));
        
        PuzzleSession newSession = new PuzzleSession();
        newSession.setPuzzle(testPuzzle);
        newSession.setUser(testUser);
        when(sessionRepository.save(any(PuzzleSession.class))).thenReturn(newSession);
        
        // Act
        PuzzleSession result = sessionService.resetSession(puzzleId, userId);
        
        // Assert
        assertNotNull(result);
        assertEquals(4, result.getAttemptCount()); // Should be incremented
        assertEquals(5, result.getBestInteractionCount()); // Should be preserved
        assertEquals(300L, result.getBestTimeSeconds()); // Should be preserved
        verify(sessionRepository).findByPuzzleIdAndUserId(puzzleId, userId);
        verify(sessionRepository).delete(testSession);
        verify(sessionRepository).flush();
        verify(sessionRepository, times(2)).save(any(PuzzleSession.class)); // save is called twice
    }

    @Test
    void testGetCurrentCode() {
        // Arrange
        Integer puzzleId = 1;
        Long userId = 1L;
        when(sessionRepository.findByPuzzleIdAndUserId(puzzleId, userId))
            .thenReturn(Optional.of(testSession));
        
        // Act
        String result = sessionService.getCurrentCode(puzzleId, userId);
        
        // Assert
        assertEquals("public class Solution {}", result);
        verify(sessionRepository).findByPuzzleIdAndUserId(puzzleId, userId);
    }

    @Test
    void testMarkSessionCompleted() {
        // Arrange
        Integer puzzleId = 1;
        Long userId = 1L;
        
        when(sessionRepository.findByPuzzleIdAndUserId(puzzleId, userId))
            .thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(PuzzleSession.class))).thenReturn(testSession);
        
        Map<String, Object> scoreDetails = new HashMap<>();
        scoreDetails.put("score", 85);
        scoreDetails.put("codeQuality", 78);
        when(scoreService.calculateScore(testSession)).thenReturn(scoreDetails);
        
        // Act
        Map<String, Object> result = sessionService.markSessionCompleted(puzzleId, userId);
        
        // Assert
        assertTrue(testSession.getIsCompleted());
        assertTrue(result.containsKey("score"));
        assertEquals(85, result.get("score"));
        assertTrue(result.containsKey("codeQuality"));
        assertEquals(78, result.get("codeQuality"));
        
        // Verify with times(2) since getOrCreateSession is called twice:
        // once in markSessionCompleted and once in getSessionMetrics
        verify(sessionRepository, times(2)).findByPuzzleIdAndUserId(puzzleId, userId);
        verify(sessionRepository).save(testSession);
        verify(scoreService).calculateScore(testSession);
    }

    @Test
    void testGetSessionMetrics() {
        // Arrange
        Integer puzzleId = 1;
        Long userId = 1L;
        
        testSession.setAttemptCount(2);
        testSession.setBestInteractionCount(5);
        testSession.setBestTimeSeconds(300L);
        testSession.setIsCompleted(true);
        
        when(sessionRepository.findByPuzzleIdAndUserId(puzzleId, userId))
            .thenReturn(Optional.of(testSession));
        
        // Act
        Map<String, Object> metrics = sessionService.getSessionMetrics(puzzleId, userId);
        
        // Assert
        assertEquals(2, metrics.get("attemptCount"));
        assertEquals(5, metrics.get("bestInteractionCount"));
        assertEquals(300L, metrics.get("bestTimeSeconds"));
        assertEquals(true, metrics.get("isCompleted"));
        
        verify(sessionRepository).findByPuzzleIdAndUserId(puzzleId, userId);
    }
}
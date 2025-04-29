package info.sup.proj.backend.services;

import info.sup.proj.backend.model.PuzzleSession;
import info.sup.proj.backend.model.User;
import info.sup.proj.backend.repositories.PuzzleRepository;
import info.sup.proj.backend.repositories.PuzzleSessionRepository;
import info.sup.proj.backend.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PuzzleSessionService {
    private final PuzzleSessionRepository sessionRepository;
    private final PuzzleRepository puzzleRepository;
    private final UserRepository userRepository;
    private final ScoreService scoreService;

    public PuzzleSessionService(
            PuzzleSessionRepository sessionRepository, 
            PuzzleRepository puzzleRepository,
            UserRepository userRepository,
            ScoreService scoreService) {
        this.sessionRepository = sessionRepository;
        this.puzzleRepository = puzzleRepository;
        this.userRepository = userRepository;
        this.scoreService = scoreService;
    }

    @Transactional
    public PuzzleSession getOrCreateSession(Integer puzzleId, Long userId) {
        Optional<PuzzleSession> existingSession = sessionRepository.findByPuzzleIdAndUserId(puzzleId, userId);
        
        if (existingSession.isPresent()) {
            return existingSession.get();
        }
        
        return puzzleRepository.findById(puzzleId)
            .map(puzzle -> {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
                
                PuzzleSession newSession = new PuzzleSession();
                newSession.setPuzzle(puzzle);
                newSession.setUser(user);
                return sessionRepository.save(newSession);
            })
            .orElseThrow(() -> new IllegalArgumentException("Puzzle not found with ID: " + puzzleId));
    }

    @Transactional
    public PuzzleSession addInteraction(Integer puzzleId, Long userId, String userInput, String aiTextResponse, String aiCodeResponse) {
        PuzzleSession session = getOrCreateSession(puzzleId, userId);
        
        session.addInteraction(userInput, aiTextResponse, aiCodeResponse);
        
        if (aiCodeResponse != null && !aiCodeResponse.isEmpty()) {
            if (session.getCurrentCode() != null && !session.getCurrentCode().isEmpty()) {
                session.setCurrentCode(session.getCurrentCode() + "\n\n" + aiCodeResponse);
            } else {
                session.setCurrentCode(aiCodeResponse);
            }
        }
        
        return sessionRepository.save(session);
    }

    /**
     * Helper method to create a new session
     */
    private PuzzleSession createNewSession(Integer puzzleId, Long userId) {
        return puzzleRepository.findById(puzzleId)
            .map(puzzle -> {
                // Get the user
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
                
                // Create a fresh session
                PuzzleSession newSession = new PuzzleSession();
                newSession.setPuzzle(puzzle);
                newSession.setUser(user);
                return sessionRepository.save(newSession);
            })
            .orElseThrow(() -> new IllegalArgumentException("Puzzle not found with ID: " + puzzleId));
    }

    /**
     * Reset a puzzle session for a specific user-puzzle combination.
     * This allows users to start over when attempting to improve their score.
     */
    @Transactional
    public PuzzleSession resetSession(Integer puzzleId, Long userId) {
        // Find existing session
        Optional<PuzzleSession> existingSession = sessionRepository.findByPuzzleIdAndUserId(puzzleId, userId);
        
        Integer bestInteractionCount = null;
        Long bestTimeSeconds = null;
        Integer attemptCount = 1; // Default to 1 for new sessions
        
        if (existingSession.isPresent()) {
            PuzzleSession session = existingSession.get();
            
            // Save best metrics before clearing the session
            bestInteractionCount = session.getBestInteractionCount();
            bestTimeSeconds = session.getBestTimeSeconds();
            attemptCount = session.getAttemptCount() != null ? session.getAttemptCount() + 1 : 1;
            
            // Delete the existing session and flush to ensure the delete is committed
            sessionRepository.delete(session);
            sessionRepository.flush(); // This ensures the delete is committed before proceeding
        }
        
        // Create a new clean session
        PuzzleSession newSession = createNewSession(puzzleId, userId);
        
        // Transfer the metrics to the new session
        newSession.setAttemptCount(attemptCount);
        newSession.setBestInteractionCount(bestInteractionCount);
        newSession.setBestTimeSeconds(bestTimeSeconds);
        
        return sessionRepository.save(newSession);
    }

    public String getCurrentCode(Integer puzzleId, Long userId) {
        return getOrCreateSession(puzzleId, userId).getCurrentCode();
    }
    
    /**
     * Mark a puzzle session as completed and update metrics
     * @return Map containing session metrics and score details
     */
    @Transactional
    public Map<String, Object> markSessionCompleted(Integer puzzleId, Long userId) {
        PuzzleSession session = getOrCreateSession(puzzleId, userId);
        session.setIsCompleted(true);
        session.updateBestMetrics();
        sessionRepository.save(session);
        
        // Calculate score using the ScoreService
        Map<String, Object> scoreDetails = scoreService.calculateScore(session);
        
        // Combine metrics and score details
        Map<String, Object> result = getSessionMetrics(puzzleId, userId);
        result.putAll(scoreDetails);
        
        return result;
    }
    
    /**
     * Get session metrics for a user-puzzle combination
     */
    public Map<String, Object> getSessionMetrics(Integer puzzleId, Long userId) {
        PuzzleSession session = getOrCreateSession(puzzleId, userId);
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("attemptCount", session.getAttemptCount());
        metrics.put("bestInteractionCount", session.getBestInteractionCount());
        metrics.put("bestTimeSeconds", session.getBestTimeSeconds());
        metrics.put("isCompleted", session.getIsCompleted());
        metrics.put("currentInteractionCount", session.getInteractions().size());
        return metrics;
    }
}
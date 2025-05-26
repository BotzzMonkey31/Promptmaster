package info.sup.proj.backend.services;

import info.sup.proj.backend.model.PuzzleSession;
import info.sup.proj.backend.model.User;
import info.sup.proj.backend.repositories.PuzzleRepository;
import info.sup.proj.backend.repositories.PuzzleSessionRepository;
import info.sup.proj.backend.repositories.UserRepository;
import info.sup.proj.backend.dto.SessionMetricsDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.aop.framework.AopContext;

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

        return existingSession.orElseGet(() -> puzzleRepository.findById(puzzleId)
                .map(puzzle -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

                    PuzzleSession newSession = new PuzzleSession();
                    newSession.setPuzzle(puzzle);
                    newSession.setUser(user);
                    return sessionRepository.save(newSession);
                })
                .orElseThrow(() -> new IllegalArgumentException("Puzzle not found with ID: " + puzzleId)));

    }

    private PuzzleSessionService getProxy() {
        return (PuzzleSessionService) AopContext.currentProxy();
    }

    @Transactional
    public PuzzleSession addInteraction(Integer puzzleId, Long userId, String userInput, String aiTextResponse, String aiCodeResponse) {
        PuzzleSession session = getProxy().getOrCreateSession(puzzleId, userId);
        
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

    private PuzzleSession createNewSession(Integer puzzleId, Long userId) {
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
    public void resetSession(Integer puzzleId, Long userId) {
        Optional<PuzzleSession> existingSession = sessionRepository.findByPuzzleIdAndUserId(puzzleId, userId);
        
        Integer bestInteractionCount = null;
        Long bestTimeSeconds = null;
        int attemptCount = 1;
        
        if (existingSession.isPresent()) {
            PuzzleSession session = existingSession.get();

            bestInteractionCount = session.getBestInteractionCount();
            bestTimeSeconds = session.getBestTimeSeconds();
            attemptCount = session.getAttemptCount() != null ? session.getAttemptCount() + 1 : 1;

            sessionRepository.delete(session);
            sessionRepository.flush();
        }

        PuzzleSession newSession = createNewSession(puzzleId, userId);

        newSession.setAttemptCount(attemptCount);
        newSession.setBestInteractionCount(bestInteractionCount);
        newSession.setBestTimeSeconds(bestTimeSeconds);

        sessionRepository.save(newSession);
    }

    @Transactional
    public String getCurrentCode(Integer puzzleId, Long userId) {
        return getProxy().getOrCreateSession(puzzleId, userId).getCurrentCode();
    }
    
    @Transactional
    public SessionMetricsDto markSessionCompleted(Integer puzzleId, Long userId) {
        PuzzleSession session = getProxy().getOrCreateSession(puzzleId, userId);
        session.setIsCompleted(true);
        session.updateBestMetrics();
        sessionRepository.save(session);
        
        SessionMetricsDto scoreDetails = scoreService.calculateScore(session);
        SessionMetricsDto metrics = getProxy().getSessionMetrics(puzzleId, userId);
        
        return SessionMetricsDto.builder()
            .totalScore(scoreDetails.getTotalScore())
            .hasFailed(scoreDetails.getHasFailed())
            .timeScore(scoreDetails.getTimeScore())
            .efficiencyScore(scoreDetails.getEfficiencyScore())
            .tokenScore(scoreDetails.getTokenScore())
            .correctnessScore(scoreDetails.getCorrectnessScore())
            .codeQualityScore(scoreDetails.getCodeQualityScore())
            .timeSeconds(scoreDetails.getTimeSeconds())
            .interactionCount(scoreDetails.getInteractionCount())
            .attemptCount(metrics.getAttemptCount())
            .bestInteractionCount(metrics.getBestInteractionCount())
            .bestTimeSeconds(metrics.getBestTimeSeconds())
            .isCompleted(metrics.getIsCompleted())
            .currentInteractionCount(metrics.getCurrentInteractionCount())
            .build();
    }
    
    @Transactional
    public SessionMetricsDto getSessionMetrics(Integer puzzleId, Long userId) {
        PuzzleSession session = getProxy().getOrCreateSession(puzzleId, userId);
        return SessionMetricsDto.builder()
            .attemptCount(session.getAttemptCount())
            .bestInteractionCount(session.getBestInteractionCount())
            .bestTimeSeconds(session.getBestTimeSeconds())
            .isCompleted(session.getIsCompleted())
            .currentInteractionCount(session.getInteractions().size())
            .build();
    }
}
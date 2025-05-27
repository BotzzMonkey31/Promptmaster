package info.sup.proj.backend.services;

import info.sup.proj.backend.model.Puzzle;
import info.sup.proj.backend.model.PuzzleSession;
import info.sup.proj.backend.dto.SessionMetricsDto;
import info.sup.proj.backend.dto.CodeEvaluationDto;
import org.springframework.stereotype.Service;

@Service
public class ScoreService {

    private final AiService aiService;

    public ScoreService(AiService aiService) {
        this.aiService = aiService;
    }

    public SessionMetricsDto calculateScore(PuzzleSession session) {
        int interactionCount = session.getInteractions().size();
        long timeSeconds = session.getBestTimeSeconds() != null 
            ? session.getBestTimeSeconds() 
            : java.time.Duration.between(session.getCreatedAt(), session.getLastUpdatedAt()).getSeconds();
        String currentCode = session.getCurrentCode();
        Puzzle puzzle = session.getPuzzle();
        
        int minInteractions = 1;
        
        boolean isSerious = interactionCount >= minInteractions && currentCode != null && !currentCode.trim().isEmpty();
        
        int timeScore;
        int efficiencyScore;
        int tokenScore;
        int correctnessScore;
        int qualityScore;
        
        if (isSerious) {
            timeScore = calculateTimeScore(timeSeconds, puzzle.getDifficulty());
            efficiencyScore = calculateEfficiencyScore(interactionCount, puzzle.getDifficulty());
            
            CodeEvaluationDto aiEvaluation = evaluateCodeWithAi(currentCode, puzzle);
            correctnessScore = aiEvaluation.getCorrectness();
            qualityScore = aiEvaluation.getQuality();
            tokenScore = calculateTokenScore(interactionCount, puzzle.getDifficulty());
        } else {
            timeScore = 0;
            efficiencyScore = 0;
            tokenScore = 0;
            correctnessScore = 0;
            qualityScore = 0;
        }
        
        int calculatedScore = (int) Math.round(
            timeScore * 0.25 + 
            efficiencyScore * 0.20 + 
            tokenScore * 0.15 +
            correctnessScore * 0.25 + 
            qualityScore * 0.15
        );
        
        boolean hasFailed = timeScore < 40 || 
                           efficiencyScore < 40 || 
                           tokenScore < 40 || 
                           correctnessScore < 40 || 
                           qualityScore < 40;
        
        int totalScore = hasFailed ? 0 : calculatedScore;
        
        return SessionMetricsDto.builder()
            .totalScore(totalScore)
            .hasFailed(hasFailed)
            .timeScore(timeScore)
            .efficiencyScore(efficiencyScore)
            .tokenScore(tokenScore)
            .correctnessScore(correctnessScore)
            .codeQualityScore(qualityScore)
            .timeSeconds(timeSeconds)
            .interactionCount(interactionCount)
            .build();
    }
    
    private int calculateTimeScore(long seconds, Puzzle.Difficulty difficulty) {
        long expectedTime = switch (difficulty) {
            case EASY -> 600;
            case MEDIUM -> 450;
            case HARD -> 300;
        };

        if (seconds <= expectedTime / 4) {
            return 100;
        } else if (seconds <= expectedTime / 2) {
            return 95;
        } else if (seconds <= expectedTime * 0.75) {
            return 90;
        } else if (seconds <= expectedTime) {
            return 85;
        } else if (seconds <= expectedTime * 1.5) {
            return 75;
        } else if (seconds <= expectedTime * 2) {
            return 65;
        } else if (seconds <= expectedTime * 3) {
            return 55;
        } else {
            return Math.max(30, 50 - (int)((seconds - expectedTime * 3) / 120));
        }
    }
    
    private int calculateEfficiencyScore(int interactionCount, Puzzle.Difficulty difficulty) {
        int expectedInteractions = switch (difficulty) {
            case EASY -> 9;
            case MEDIUM -> 6;
            case HARD -> 4;
        };

        if (interactionCount <= expectedInteractions / 3) {
            return 100;
        } else if (interactionCount <= expectedInteractions / 2) {
            return 95;
        } else if (interactionCount <= expectedInteractions * 0.75) {
            return 90;
        } else if (interactionCount <= expectedInteractions) {
            return 85;
        } else if (interactionCount <= expectedInteractions * 1.5) {
            return 75;
        } else if (interactionCount <= expectedInteractions * 2) {
            return 65;
        } else if (interactionCount <= expectedInteractions * 3) {
            return 55;
        } else {
            return Math.max(35, 50 - (interactionCount - expectedInteractions * 3));
        }
    }

    private int calculateTokenScore(int interactionCount, Puzzle.Difficulty difficulty) {
        int expectedTokenUsage = switch (difficulty) {
            case EASY -> 12;
            case MEDIUM -> 8;
            case HARD -> 5;
        };

        if (interactionCount <= expectedTokenUsage / 3) {
            return 100;
        } else if (interactionCount <= expectedTokenUsage / 2) {
            return 95;
        } else if (interactionCount <= expectedTokenUsage * 0.75) {
            return 90;
        } else if (interactionCount <= expectedTokenUsage) {
            return 85;
        } else if (interactionCount <= expectedTokenUsage * 1.25) {
            return 80;
        } else if (interactionCount <= expectedTokenUsage * 1.5) {
            return 70;
        } else if (interactionCount <= expectedTokenUsage * 2) {
            return 60;
        } else if (interactionCount <= expectedTokenUsage * 3) {
            return 50;
        } else {
            return Math.max(30, 45 - (interactionCount - expectedTokenUsage * 3) * 2);
        }
    }
    
    private CodeEvaluationDto evaluateCodeWithAi(String code, Puzzle puzzle) {
        try {
            String evaluationPrompt = createEvaluationPrompt(code, puzzle);
            String evaluationResponse = aiService.getCodeEvaluation(evaluationPrompt, code, puzzle.getType());
            return parseAiEvaluation(evaluationResponse, puzzle.getType() == Puzzle.Type.BY_PASS);
        } catch (Exception e) {
            return getDefaultEvaluation(puzzle.getType() == Puzzle.Type.BY_PASS);
        }
    }
    
    private CodeEvaluationDto getDefaultEvaluation(boolean isByPassPuzzle) {
        return CodeEvaluationDto.builder()
            .correctness(isByPassPuzzle ? 85 : 75)
            .quality(isByPassPuzzle ? 80 : 70)
            .build();
    }
    
    private String createEvaluationPrompt(String code, Puzzle puzzle) {
        return String.format(
            """
            Please evaluate this code solution for the following puzzle:
            Puzzle: %s
            Description: %s
            Code: %s
            Evaluate the following aspects on a scale from 0-100:
            1. Correctness: Does the code correctly solve the problem as described?
            2. Code quality: Is the code well-structured, efficient, and following best practices?
            Respond in JSON format: {correctness: X, quality: Y} where X and Y are scores from 0-100.""",
            puzzle.getName(),
            puzzle.getDescription(),
            code
        );
    }
    
    private CodeEvaluationDto parseAiEvaluation(String evaluationResponse, boolean isByPassPuzzle) {
        try {
            int correctness = 75;
            int quality = 70;
            
            if (evaluationResponse.contains("\"correctness\":")) {
                int correctnessIndex = evaluationResponse.indexOf("\"correctness\":");
                int commaIndex = evaluationResponse.indexOf(",", correctnessIndex);
                String correctnessStr = evaluationResponse.substring(correctnessIndex + 14, commaIndex).trim();
                correctness = Integer.parseInt(correctnessStr);
            }
            
            if (evaluationResponse.contains("\"quality\":")) {
                int qualityIndex = evaluationResponse.indexOf("\"quality\":");
                int endIndex = evaluationResponse.indexOf("}", qualityIndex);
                String qualityStr = evaluationResponse.substring(qualityIndex + 10, endIndex).trim();
                quality = Integer.parseInt(qualityStr);
            }

            if (isByPassPuzzle) {
                correctness = Math.max(correctness, 85);
                quality = Math.max(quality, 80);
            }
            
            return CodeEvaluationDto.builder()
                .correctness(correctness)
                .quality(quality)
                .build();
        } catch (Exception e) {
            return getDefaultEvaluation(isByPassPuzzle);
        }
    }
}
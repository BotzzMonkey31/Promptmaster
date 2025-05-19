package info.sup.proj.backend.services;

import info.sup.proj.backend.model.Puzzle;
import info.sup.proj.backend.model.PuzzleSession;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ScoreService {

    private final AiService aiService;

    public ScoreService(AiService aiService) {
        this.aiService = aiService;
    }

    public Map<String, Object> calculateScore(PuzzleSession session) {
        Map<String, Object> scoreDetails = new HashMap<>();
        
        int interactionCount = session.getInteractions().size();
        long timeSeconds = session.getBestTimeSeconds() != null 
            ? session.getBestTimeSeconds() 
            : java.time.Duration.between(session.getCreatedAt(), session.getLastUpdatedAt()).getSeconds();
        String currentCode = session.getCurrentCode();
        Puzzle puzzle = session.getPuzzle();
        
        int minInteractions;
        if (puzzle.getType() == Puzzle.Type.BY_PASS) {
            minInteractions = 1; // BY_PASS puzzles only need 1 interaction
        } else {
            switch (puzzle.getDifficulty()) {
                case Easy:
                    minInteractions = 3;
                    break;
                case Medium:
                    minInteractions = 2;
                    break;
                case Hard:
                    minInteractions = 2;
                    break;
                default:
                    minInteractions = 2;
            }
        }
        
        boolean isSerious = interactionCount >= minInteractions && currentCode != null && !currentCode.trim().isEmpty();
        
        int timeScore, efficiencyScore, tokenScore, correctnessScore, qualityScore;
        
        if (isSerious) {
            timeScore = calculateTimeScore(timeSeconds, puzzle.getDifficulty());
            efficiencyScore = calculateEfficiencyScore(interactionCount, puzzle.getDifficulty());
            
            Map<String, Integer> aiEvaluation = evaluateCodeWithAi(currentCode, puzzle);
            correctnessScore = aiEvaluation.get("correctness");
            qualityScore = aiEvaluation.get("quality");
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
        
        scoreDetails.put("totalScore", totalScore);
        scoreDetails.put("hasFailed", hasFailed);
        scoreDetails.put("timeScore", timeScore);
        scoreDetails.put("efficiencyScore", efficiencyScore);
        scoreDetails.put("tokenScore", tokenScore);
        scoreDetails.put("correctnessScore", correctnessScore);
        scoreDetails.put("codeQualityScore", qualityScore);
        
        scoreDetails.put("timeSeconds", timeSeconds);
        scoreDetails.put("interactionCount", interactionCount);
        
        return scoreDetails;
    }
    
    private int calculateTimeScore(long seconds, Puzzle.Difficulty difficulty) {
        long expectedTime;
        switch (difficulty) {
            case Easy:
                expectedTime = 600;
                break;
            case Medium:
                expectedTime = 450;
                break;
            case Hard:
                expectedTime = 300;
                break;
            default:
                expectedTime = 450;
        }
        
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
        int expectedInteractions;
        switch (difficulty) {
            case Easy:
                expectedInteractions = 9;
                break;
            case Medium:
                expectedInteractions = 6;
                break;
            case Hard:
                expectedInteractions = 4;
                break;
            default:
                expectedInteractions = 6;
        }
        
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
        int expectedTokenUsage;
        switch (difficulty) {
            case Easy:
                expectedTokenUsage = 12;
                break;
            case Medium:
                expectedTokenUsage = 8;
                break;
            case Hard:
                expectedTokenUsage = 5;
                break;
            default:
                expectedTokenUsage = 8;
        }
        
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
    
    private Map<String, Integer> evaluateCodeWithAi(String code, Puzzle puzzle) {
        Map<String, Integer> scores = new HashMap<>();
        
        try {
            String evaluationPrompt = createEvaluationPrompt(code, puzzle);
            
            String evaluationResponse = aiService.getCodeEvaluation(evaluationPrompt, code, puzzle.getType());
            
            scores = parseAiEvaluation(evaluationResponse);
        } catch (Exception e) {
            // More lenient default scores for BY_PASS puzzles
            if (puzzle.getType() == Puzzle.Type.BY_PASS) {
                scores.put("correctness", 85);
                scores.put("quality", 80);
            } else {
                scores.put("correctness", 75);
                scores.put("quality", 70);
            }
        }
        
        // Ensure minimum scores for BY_PASS puzzles
        if (puzzle.getType() == Puzzle.Type.BY_PASS) {
            scores.put("correctness", Math.max(scores.getOrDefault("correctness", 85), 85));
            scores.put("quality", Math.max(scores.getOrDefault("quality", 80), 80));
        }
        
        return scores;
    }
    
    private String createEvaluationPrompt(String code, Puzzle puzzle) {
        return String.format(
            "Please evaluate this code solution for the following puzzle:\n" +
            "Puzzle: %s\n" +
            "Description: %s\n\n" +
            "Evaluate the following aspects on a scale from 0-100:\n" +
            "1. Correctness: Does the code correctly solve the problem as described?\n" +
            "2. Code quality: Is the code well-structured, efficient, and following best practices?\n\n" +
            "Respond in JSON format: {\"correctness\": X, \"quality\": Y} where X and Y are scores from 0-100.",
            puzzle.getName(),
            puzzle.getDescription()
        );
    }
    
    private Map<String, Integer> parseAiEvaluation(String evaluationResponse) {
        Map<String, Integer> result = new HashMap<>();
        
        try {
            if (evaluationResponse.contains("\"correctness\":")) {
                int correctnessIndex = evaluationResponse.indexOf("\"correctness\":");
                int commaIndex = evaluationResponse.indexOf(",", correctnessIndex);
                String correctnessStr = evaluationResponse.substring(correctnessIndex + 14, commaIndex).trim();
                result.put("correctness", Integer.parseInt(correctnessStr));
            }
            
            if (evaluationResponse.contains("\"quality\":")) {
                int qualityIndex = evaluationResponse.indexOf("\"quality\":");
                int endIndex = evaluationResponse.indexOf("}", qualityIndex);
                String qualityStr = evaluationResponse.substring(qualityIndex + 10, endIndex).trim();
                result.put("quality", Integer.parseInt(qualityStr));
            }
        } catch (Exception e) {
            result.put("correctness", 75);
            result.put("quality", 70);
        }
        
        if (!result.containsKey("correctness")) result.put("correctness", 75);
        if (!result.containsKey("quality")) result.put("quality", 70);
        
        return result;
    }
}
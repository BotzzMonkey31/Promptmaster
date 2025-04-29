package info.sup.proj.backend.services;

import info.sup.proj.backend.model.Puzzle;
import info.sup.proj.backend.model.PuzzleSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ScoreService {

    private final AiService aiService;

    @Autowired
    public ScoreService(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * Calculate a comprehensive score for a completed puzzle
     * @param session The puzzle session to evaluate
     * @return A map containing the score details
     */
    public Map<String, Object> calculateScore(PuzzleSession session) {
        Map<String, Object> scoreDetails = new HashMap<>();
        
        // Get the raw metrics
        int interactionCount = session.getInteractions().size();
        long timeSeconds = session.getBestTimeSeconds() != null 
            ? session.getBestTimeSeconds() 
            : java.time.Duration.between(session.getCreatedAt(), session.getLastUpdatedAt()).getSeconds();
        String currentCode = session.getCurrentCode();
        Puzzle puzzle = session.getPuzzle();
        
        // Calculate scores for each component (0-100)
        int timeScore = calculateTimeScore(timeSeconds, puzzle.getDifficulty());
        int efficiencyScore = calculateEfficiencyScore(interactionCount, puzzle.getDifficulty());
        
        // Get AI evaluation of code quality and correctness
        Map<String, Integer> aiEvaluation = evaluateCodeWithAi(currentCode, puzzle);
        int correctnessScore = aiEvaluation.get("correctness");
        int qualityScore = aiEvaluation.get("quality");
        int tokenScore = calculateTokenScore(interactionCount, puzzle.getDifficulty());
        
        // Calculate total score (weighted average)
        int totalScore = (int) Math.round(
            timeScore * 0.25 + 
            efficiencyScore * 0.20 + 
            tokenScore * 0.15 +
            correctnessScore * 0.25 + 
            qualityScore * 0.15
        );
        
        // Add all scores to the response
        scoreDetails.put("totalScore", totalScore);
        scoreDetails.put("timeScore", timeScore);
        scoreDetails.put("efficiencyScore", efficiencyScore);
        scoreDetails.put("tokenScore", tokenScore);
        scoreDetails.put("correctnessScore", correctnessScore);
        scoreDetails.put("codeQualityScore", qualityScore);
        
        // Add raw metrics for context
        scoreDetails.put("timeSeconds", timeSeconds);
        scoreDetails.put("interactionCount", interactionCount);
        
        return scoreDetails;
    }
    
    private int calculateTimeScore(long seconds, Puzzle.Difficulty difficulty) {
        // Base expected times for different difficulties (in seconds)
        long expectedTime;
        switch (difficulty) {
            case Easy:
                expectedTime = 300; // 5 minutes
                break;
            case Medium:
                expectedTime = 600; // 10 minutes
                break;
            case Hard:
                expectedTime = 1200; // 20 minutes
                break;
            default:
                expectedTime = 600;
        }
        
        // Calculate score based on time taken relative to expected time
        // Score decreases as time increases
        if (seconds <= expectedTime / 2) {
            return 100; // Excellent: Half the expected time or less
        } else if (seconds <= expectedTime) {
            return 90; // Great: Within expected time
        } else if (seconds <= expectedTime * 1.5) {
            return 80; // Good: Up to 50% longer than expected
        } else if (seconds <= expectedTime * 2) {
            return 70; // Average: Up to twice the expected time
        } else {
            return Math.max(50, 100 - (int)((seconds - expectedTime * 2) / 60)); // Decreasing score for longer times
        }
    }
    
    private int calculateEfficiencyScore(int interactionCount, Puzzle.Difficulty difficulty) {
        // Base expected interactions for different difficulties
        int expectedInteractions;
        switch (difficulty) {
            case Easy:
                expectedInteractions = 5;
                break;
            case Medium:
                expectedInteractions = 8;
                break;
            case Hard:
                expectedInteractions = 12;
                break;
            default:
                expectedInteractions = 8;
        }
        
        // Score decreases as interaction count increases beyond expectation
        if (interactionCount <= expectedInteractions / 2) {
            return 100; // Excellent: Half the expected interactions or fewer
        } else if (interactionCount <= expectedInteractions) {
            return 90; // Great: Within expected interactions
        } else if (interactionCount <= expectedInteractions * 1.5) {
            return 80; // Good: Up to 50% more than expected
        } else if (interactionCount <= expectedInteractions * 2) {
            return 70; // Average: Up to twice the expected
        } else {
            return Math.max(50, 100 - (interactionCount - expectedInteractions * 2) * 2); // Decrease for more interactions
        }
    }

    private int calculateTokenScore(int interactionCount, Puzzle.Difficulty difficulty) {
        // This is a proxy for token usage based on interaction count
        // Similar to efficiency score but with different thresholds
        int expectedTokenUsage;
        switch (difficulty) {
            case Easy:
                expectedTokenUsage = 6;
                break;
            case Medium:
                expectedTokenUsage = 10;
                break;
            case Hard:
                expectedTokenUsage = 15;
                break;
            default:
                expectedTokenUsage = 10;
        }
        
        if (interactionCount <= expectedTokenUsage / 2) {
            return 100;
        } else if (interactionCount <= expectedTokenUsage) {
            return 90;
        } else if (interactionCount <= expectedTokenUsage * 1.5) {
            return 75;
        } else if (interactionCount <= expectedTokenUsage * 2) {
            return 60;
        } else {
            return Math.max(40, 100 - (interactionCount - expectedTokenUsage * 2) * 3);
        }
    }
    
    private Map<String, Integer> evaluateCodeWithAi(String code, Puzzle puzzle) {
        Map<String, Integer> scores = new HashMap<>();
        
        try {
            // Create a prompt for evaluating correctness and quality
            String evaluationPrompt = createEvaluationPrompt(code, puzzle);
            
            // Call the AI service to evaluate the code
            String evaluationResponse = aiService.getCodeEvaluation(evaluationPrompt, code, puzzle.getType());
            
            // Parse the response to extract scores (assumed format: JSON-like string)
            scores = parseAiEvaluation(evaluationResponse);
        } catch (Exception e) {
            // Fallback if AI evaluation fails
            scores.put("correctness", 75);
            scores.put("quality", 70);
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
            // Extract scores using simple string parsing (as a fallback if JSON parsing fails)
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
            // Default values if parsing fails
            result.put("correctness", 75);
            result.put("quality", 70);
        }
        
        // Ensure both scores exist
        if (!result.containsKey("correctness")) result.put("correctness", 75);
        if (!result.containsKey("quality")) result.put("quality", 70);
        
        return result;
    }
}
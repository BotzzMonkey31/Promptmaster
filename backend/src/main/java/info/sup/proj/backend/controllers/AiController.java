package info.sup.proj.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import info.sup.proj.backend.services.AiService;
import info.sup.proj.backend.services.PuzzleService;
import info.sup.proj.backend.services.PuzzleSessionService;
import info.sup.proj.backend.services.AiService.ChatResponse;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {
    private final AiService aiService;
    private final PuzzleService puzzleService;
    private final PuzzleSessionService sessionService;

    public AiController(AiService aiService, PuzzleService puzzleService, PuzzleSessionService sessionService) {
        this.aiService = aiService;
        this.puzzleService = puzzleService;
        this.sessionService = sessionService;
    }

    @PostMapping("/solve")
    public ResponseEntity<?> solve(@RequestBody SolveRequest request) {
        return puzzleService.getPuzzleById(request.getPuzzleId())
            .map(puzzle -> {
                String currentCode = sessionService.getCurrentCode(
                    request.getPuzzleId(),
                    request.getUserId()
                );

                ChatResponse response = aiService.generateResponse(
                    request.getUserInput(),
                    currentCode,
                    puzzle.getType()
                );

                sessionService.addInteraction(
                    request.getPuzzleId(),
                    request.getUserId(),
                    request.getUserInput(),
                    response.getText(),
                    response.getCode()
                );

                return ResponseEntity.ok(Map.of(
                    "text", response.getText(),
                    "code", response.getCode(),
                    "completeCode", sessionService.getCurrentCode(request.getPuzzleId(), request.getUserId())
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetSession(@RequestBody ResetSessionRequest request) {
        try {
            // Reset the session
            sessionService.resetSession(request.getPuzzleId(), request.getUserId());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Session reset successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to reset session: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/complete")
    public ResponseEntity<?> markCompleted(@RequestBody SessionRequest request) {
        try {
            // The markSessionCompleted method now returns the complete metrics with score details
            Map<String, Object> results = sessionService.markSessionCompleted(request.getPuzzleId(), request.getUserId());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Puzzle marked as completed",
                "metrics", results,
                "scoreDetails", results
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to mark puzzle as completed: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/start-fresh")
    public ResponseEntity<?> startFreshPuzzle(@RequestBody SessionRequest request) {
        try {
            // Log the received request data
            System.out.println("Starting fresh puzzle with puzzleId: " + request.getPuzzleId() + ", userId: " + request.getUserId());

            // Check for null values
            if (request.getPuzzleId() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Missing puzzleId parameter"
                ));
            }

            if (request.getUserId() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Missing userId parameter"
                ));
            }

            // Reset the session to start a fresh puzzle
            sessionService.resetSession(request.getPuzzleId(), request.getUserId());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Fresh puzzle session started successfully"
            ));
        } catch (Exception e) {
            // Enhanced error logging
            e.printStackTrace();

            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to start fresh puzzle session: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/metrics/{puzzleId}/{userId}")
    public ResponseEntity<?> getSessionMetrics(@PathVariable Integer puzzleId, @PathVariable Long userId) {
        try {
            Map<String, Object> metrics = sessionService.getSessionMetrics(puzzleId, userId);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to get metrics: " + e.getMessage()
            ));
        }
    }

    public static class SolveRequest {
        private Integer puzzleId;
        private Long userId;
        private String userInput;
        private String code;

        public Integer getPuzzleId() { return puzzleId; }
        public void setPuzzleId(Integer puzzleId) { this.puzzleId = puzzleId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUserInput() { return userInput; }
        public void setUserInput(String userInput) { this.userInput = userInput; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class ResetSessionRequest {
        private Integer puzzleId;
        private Long userId;

        public Integer getPuzzleId() { return puzzleId; }
        public void setPuzzleId(Integer puzzleId) { this.puzzleId = puzzleId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    public static class SessionRequest {
        private Integer puzzleId;
        private Long userId;

        public Integer getPuzzleId() { return puzzleId; }
        public void setPuzzleId(Integer puzzleId) { this.puzzleId = puzzleId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }
}

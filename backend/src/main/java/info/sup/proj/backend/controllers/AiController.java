package info.sup.proj.backend.controllers;

import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import info.sup.proj.backend.services.AiService;
import info.sup.proj.backend.services.PuzzleService;
import info.sup.proj.backend.services.PuzzleSessionService;
import info.sup.proj.backend.services.AiService.ChatResponse;
import info.sup.proj.backend.dto.ApiResponse;
import info.sup.proj.backend.dto.SolveResponseDto;
import info.sup.proj.backend.dto.SessionMetricsDto;
import org.slf4j.Logger;

@RestController
@RequestMapping("/ai")
public class AiController {
    private final AiService aiService;
    private final PuzzleService puzzleService;
    private final PuzzleSessionService sessionService;
    private final Logger logger = LoggerFactory.getLogger(AiController.class);

    public AiController(AiService aiService, PuzzleService puzzleService, PuzzleSessionService sessionService) {
        this.aiService = aiService;
        this.puzzleService = puzzleService;
        this.sessionService = sessionService;
    }

    @PostMapping("/solve")
    public ResponseEntity<SolveResponseDto> solve(@RequestBody SolveRequest request) {
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

                SolveResponseDto solveResponse = new SolveResponseDto(
                    response.getText(),
                    response.getCode(),
                    sessionService.getCurrentCode(request.getPuzzleId(), request.getUserId())
                );

                return ResponseEntity.ok(solveResponse);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<Void>> resetSession(@RequestBody ResetSessionRequest request) {
        try {
            sessionService.resetSession(request.getPuzzleId(), request.getUserId());
            return ResponseEntity.ok(new ApiResponse<>(true, "Session reset successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Failed to reset session: " + e.getMessage()));
        }
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<SessionMetricsDto>> markCompleted(@RequestBody SessionRequest request) {
        try {
            SessionMetricsDto metrics = sessionService.markSessionCompleted(request.getPuzzleId(), request.getUserId());
            return ResponseEntity.ok(new ApiResponse<>(true, "Puzzle marked as completed", metrics));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Failed to mark puzzle as completed: " + e.getMessage()));
        }
    }

    @PostMapping("/start-fresh")
    public ResponseEntity<ApiResponse<Void>> startFreshPuzzle(@RequestBody SessionRequest request) {
        try {
            logger.info("Starting fresh puzzle [puzzleId={}, userId={}]", request.getPuzzleId(), request.getUserId());

            if (request.getPuzzleId() == null) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Missing puzzleId parameter"));
            }

            if (request.getUserId() == null) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Missing userId parameter"));
            }

            sessionService.resetSession(request.getPuzzleId(), request.getUserId());
            return ResponseEntity.ok(new ApiResponse<>(true, "Fresh puzzle session started successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Failed to start fresh puzzle session: " + e.getMessage()));
        }
    }

    @GetMapping("/metrics/{puzzleId}/{userId}")
    public ResponseEntity<ApiResponse<SessionMetricsDto>> getSessionMetrics(@PathVariable Integer puzzleId, @PathVariable Long userId) {
        try {
            SessionMetricsDto metrics = sessionService.getSessionMetrics(puzzleId, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Metrics retrieved successfully", metrics));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Failed to get metrics: " + e.getMessage()));
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
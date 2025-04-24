package info.sup.proj.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import info.sup.proj.backend.services.AiService;
import info.sup.proj.backend.services.PuzzleService;
import info.sup.proj.backend.services.AiService.ChatResponse;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {
    private final AiService aiService;
    private final PuzzleService puzzleService;

    public AiController(AiService aiService, PuzzleService puzzleService) {
        this.aiService = aiService;
        this.puzzleService = puzzleService;
    }

    @PostMapping("/solve")
    public ResponseEntity<?> solve(@RequestBody SolveRequest request) {
        return puzzleService.getPuzzleById(request.getPuzzleId())
            .map(puzzle -> {
                ChatResponse response = aiService.generateResponse(
                    request.getUserInput(),
                    request.getCode(),
                    puzzle.getType()
                );
                return ResponseEntity.ok(Map.of(
                    "text", response.getText(),
                    "code", response.getCode()
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    public static class SolveRequest {
        private Integer puzzleId;
        private String userInput;
        private String code;

        public Integer getPuzzleId() { return puzzleId; }
        public void setPuzzleId(Integer puzzleId) { this.puzzleId = puzzleId; }
        public String getUserInput() { return userInput; }
        public void setUserInput(String userInput) { this.userInput = userInput; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}

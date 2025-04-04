package info.sup.proj.backend.controllers;

import info.sup.proj.backend.model.Puzzle;
import info.sup.proj.backend.services.PuzzleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/puzzles")
public class PuzzleController {

    private final PuzzleService puzzleService;

    public PuzzleController(PuzzleService puzzleService) {
        this.puzzleService = puzzleService;
    }

    @GetMapping
    public List<Puzzle> getPuzzles() {
        return puzzleService.getAllPuzzles();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Puzzle> getPuzzleById(@PathVariable Integer id) {
        return puzzleService.getPuzzleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Puzzle createPuzzle(@RequestBody Puzzle puzzle) {
        return puzzleService.savePuzzle(puzzle);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePuzzle(@PathVariable Integer id) {
        puzzleService.deletePuzzle(id);
        return ResponseEntity.noContent().build();
    }
}
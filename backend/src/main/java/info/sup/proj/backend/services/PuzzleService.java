package info.sup.proj.backend.services;

import info.sup.proj.backend.model.Puzzle;
import info.sup.proj.backend.repositories.PuzzleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PuzzleService {
    private final PuzzleRepository puzzleRepository;

    public PuzzleService(PuzzleRepository puzzleRepository) {
        this.puzzleRepository = puzzleRepository;
    }

    public List<Puzzle> getAllPuzzles() {
        return puzzleRepository.findAll();
    }

    public Optional<Puzzle> getPuzzleById(Integer id) {
        return puzzleRepository.findById(id);
    }

    public Puzzle savePuzzle(Puzzle puzzle) {
        return puzzleRepository.save(puzzle);
    }

    public void deletePuzzle(Integer id) {
        puzzleRepository.deleteById(id);
    }
}

package info.sup.proj.backend.services;

import info.sup.proj.backend.model.Puzzle;
import info.sup.proj.backend.repositories.PuzzleRepository;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PuzzleService {
    private final PuzzleRepository puzzleRepository;

    public PuzzleService(PuzzleRepository puzzleRepository) {
        this.puzzleRepository = puzzleRepository;
    }

    @PostConstruct
    public void initializeSamplePuzzles() {
        // Add a sample multi-step puzzle if no puzzles exist
        if (puzzleRepository.count() == 0) {
            Puzzle samplePuzzle = new Puzzle(
                "File Word Counter",
                Puzzle.Difficulty.Medium,
                Puzzle.Type.Multi_Step,
                """
                Create a Java program that processes a text file and finds the most frequent words:
                1. Read a text file from disk
                2. Process the text to extract words
                3. Count word occurrences
                4. Sort words by frequency
                5. Display the top 10 most frequent words
                
                This is a multi-step challenge. Break it down and tackle one part at a time!
                """
            );
            puzzleRepository.save(samplePuzzle);
        }
    }

    public List<Puzzle> getAllPuzzles() {
        return puzzleRepository.findAll();
    }

    public Optional<Puzzle> getPuzzleById(Integer id) {
        return puzzleRepository.findById(id);
    }
    
    public List<Puzzle> getPuzzlesByType(Puzzle.Type type) {
        return puzzleRepository.findAll().stream()
                .filter(puzzle -> puzzle.getType() == type)
                .collect(Collectors.toList());
    }

    public Puzzle savePuzzle(Puzzle puzzle) {
        return puzzleRepository.save(puzzle);
    }

    public void deletePuzzle(Integer id) {
        puzzleRepository.deleteById(id);
    }
}

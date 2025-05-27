package info.sup.proj.backend.services;

import info.sup.proj.backend.model.Puzzle;
import info.sup.proj.backend.repositories.PuzzleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PuzzleServiceTest {

    @Mock
    private PuzzleRepository puzzleRepository;

    @InjectMocks
    private PuzzleService puzzleService;

    private Puzzle puzzle1;
    private Puzzle puzzle2;

    @BeforeEach
    void setUp() {
        // Setup test puzzles
        puzzle1 = new Puzzle();
        puzzle1.setId(1);
        puzzle1.setName("Test Puzzle 1"); // Changed from setTitle to setName
        puzzle1.setType(Puzzle.Type.MULTI_STEP);
        puzzle1.setDescription("A test puzzle for multi-step problem solving");
        puzzle1.setDifficulty(Puzzle.Difficulty.MEDIUM); // Changed from ordinal to enum

        puzzle2 = new Puzzle();
        puzzle2.setId(2);
        puzzle2.setName("Test Puzzle 2"); // Changed from setTitle to setName
        puzzle2.setType(Puzzle.Type.FAULTY);
        puzzle2.setDescription("A test puzzle for debugging");
        puzzle2.setDifficulty(Puzzle.Difficulty.HARD); // Changed from ordinal to enum
    }

    @Test
    void testGetAllPuzzles() {
        // Arrange
        List<Puzzle> expectedPuzzles = Arrays.asList(puzzle1, puzzle2);
        when(puzzleRepository.findAll()).thenReturn(expectedPuzzles);

        // Act
        List<Puzzle> actualPuzzles = puzzleService.getAllPuzzles();

        // Assert
        assertEquals(expectedPuzzles.size(), actualPuzzles.size());
        assertEquals(expectedPuzzles, actualPuzzles);
        verify(puzzleRepository).findAll();
    }

    @Test
    void testGetPuzzleById_Found() {
        // Arrange
        Integer puzzleId = 1;
        when(puzzleRepository.findById(puzzleId)).thenReturn(Optional.of(puzzle1));

        // Act
        Optional<Puzzle> result = puzzleService.getPuzzleById(puzzleId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(puzzle1, result.get());
        verify(puzzleRepository).findById(puzzleId);
    }

    @Test
    void testGetPuzzleById_NotFound() {
        // Arrange
        Integer puzzleId = 999;
        when(puzzleRepository.findById(puzzleId)).thenReturn(Optional.empty());

        // Act
        Optional<Puzzle> result = puzzleService.getPuzzleById(puzzleId);

        // Assert
        assertFalse(result.isPresent());
        verify(puzzleRepository).findById(puzzleId);
    }

    @Test
    void testSavePuzzle() {
        // Arrange
        Puzzle newPuzzle = new Puzzle();
        newPuzzle.setName("New Puzzle"); // Changed from setTitle to setName
        newPuzzle.setType(Puzzle.Type.BY_PASS);
        newPuzzle.setDescription("A new test puzzle");
        newPuzzle.setDifficulty(Puzzle.Difficulty.EASY); // Changed from ordinal to enum
        
        when(puzzleRepository.save(newPuzzle)).thenReturn(newPuzzle);

        // Act
        Puzzle savedPuzzle = puzzleService.savePuzzle(newPuzzle);

        // Assert
        assertNotNull(savedPuzzle);
        assertEquals(newPuzzle, savedPuzzle);
        verify(puzzleRepository).save(newPuzzle);
    }

    @Test
    void testDeletePuzzle() {
        // Arrange
        Integer puzzleId = 1;
        doNothing().when(puzzleRepository).deleteById(puzzleId);

        // Act
        puzzleService.deletePuzzle(puzzleId);

        // Assert
        verify(puzzleRepository).deleteById(puzzleId);
    }
}
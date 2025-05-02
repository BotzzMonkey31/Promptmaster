package info.sup.proj.backend.config;

import info.sup.proj.backend.model.Puzzle;
import info.sup.proj.backend.services.PuzzleService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    private final PuzzleService puzzleService;
    
    @Value("${app.initialization.force:false}")
    private boolean forceInitialization;

    public DataInitializer(PuzzleService puzzleService) {
        this.puzzleService = puzzleService;
    }

    @Override
    public void run(String... args) {
        try {
            logger.info("Checking if database initialization is needed. Force initialization: {}", forceInitialization);
            
            // Check if database is empty or force initialization is enabled
            if (forceInitialization || puzzleService.getAllPuzzles().isEmpty()) {
                logger.info("Seeding puzzle data into database");
                seedPuzzles().forEach(puzzleService::savePuzzle);
                logger.info("Database initialization completed successfully");
            } else {
                logger.info("Database already contains puzzles, skipping initialization");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize database: {}", e.getMessage(), e);
            // Don't rethrow to allow application to start even if initialization fails
        }
    }

    private List<Puzzle> seedPuzzles() {
        return List.of(
                new Puzzle("FizzBuzz", Puzzle.Difficulty.Easy, Puzzle.Type.BY_PASS, "Write a program that prints numbers from 1 to 100, replacing multiples of three and five."),
                new Puzzle("Palindrome Checker", Puzzle.Difficulty.Easy, Puzzle.Type.Faulty, "Create a function that checks if a given string is a palindrome."),
                new Puzzle("Fibonacci Sequence", Puzzle.Difficulty.Easy, Puzzle.Type.Multi_Step, "Write a function that returns the nth number in the Fibonacci sequence."),
                new Puzzle("Prime number lookup", Puzzle.Difficulty.Easy, Puzzle.Type.Multi_Step, "Write a program that looks for all prime numbers up until 1000."),
                new Puzzle("File reader", Puzzle.Difficulty.Medium, Puzzle.Type.BY_PASS, "Write a program that reads a file and displays certain content."),
                new Puzzle("Caesershift", Puzzle.Difficulty.Hard, Puzzle.Type.Faulty, "Write a function that solves a caesershift."),
                new Puzzle("FizzBuzz1", Puzzle.Difficulty.Easy, Puzzle.Type.BY_PASS, "Write a program that prints numbers from 1 to 100, replacing multiples of three and five."),
                new Puzzle("Palindrome Checker1", Puzzle.Difficulty.Easy, Puzzle.Type.Faulty, "Create a function that checks if a given string is a palindrome."),
                new Puzzle("Fibonacci Sequence1", Puzzle.Difficulty.Easy, Puzzle.Type.Multi_Step, "Write a function that returns the nth number in the Fibonacci sequence."),
                new Puzzle("Prime number lookup1", Puzzle.Difficulty.Easy, Puzzle.Type.Multi_Step, "Write a program that looks for all prime numbers up until 1000."),
                new Puzzle("File reader1", Puzzle.Difficulty.Medium, Puzzle.Type.BY_PASS, "Write a program that reads a file and displays certain content."),
                new Puzzle("Caesershift1", Puzzle.Difficulty.Hard, Puzzle.Type.Faulty, "Write a function that solves a caesershift."),
                new Puzzle("FizzBuzz2", Puzzle.Difficulty.Easy, Puzzle.Type.BY_PASS, "Write a program that prints numbers from 1 to 100, replacing multiples of three and five."),
                new Puzzle("Palindrome Checker2", Puzzle.Difficulty.Easy, Puzzle.Type.Faulty, "Create a function that checks if a given string is a palindrome."),
                new Puzzle("Fibonacci Sequence2", Puzzle.Difficulty.Easy, Puzzle.Type.Multi_Step, "Write a function that returns the nth number in the Fibonacci sequence."),
                new Puzzle("Prime number lookup2", Puzzle.Difficulty.Easy, Puzzle.Type.Multi_Step, "Write a program that looks for all prime numbers up until 1000."),
                new Puzzle("File reader2", Puzzle.Difficulty.Medium, Puzzle.Type.BY_PASS, "Write a program that reads a file and displays certain content."),
                new Puzzle("Caesershift2", Puzzle.Difficulty.Hard, Puzzle.Type.Faulty, "Write a function that solves a caesershift."),
                new Puzzle("FizzBuzz3", Puzzle.Difficulty.Easy, Puzzle.Type.BY_PASS, "Write a program that prints numbers from 1 to 100, replacing multiples of three and five."),
                new Puzzle("Palindrome Checker3", Puzzle.Difficulty.Easy, Puzzle.Type.Faulty, "Create a function that checks if a given string is a palindrome."),
                new Puzzle("Fibonacci Sequence3", Puzzle.Difficulty.Easy, Puzzle.Type.Multi_Step, "Write a function that returns the nth number in the Fibonacci sequence."),
                new Puzzle("Prime number lookup3", Puzzle.Difficulty.Easy, Puzzle.Type.Multi_Step, "Write a program that looks for all prime numbers up until 1000."),
                new Puzzle("File reader3", Puzzle.Difficulty.Medium, Puzzle.Type.BY_PASS, "Write a program that reads a file and displays certain content."),
                new Puzzle("Caesershift3", Puzzle.Difficulty.Hard, Puzzle.Type.Faulty, "Write a function that solves a caesershift.")
        );
    }
}
package info.sup.proj.backend.controllers;

import info.sup.proj.backend.model.Puzzle;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/puzzles")
public class PuzzleController {

    @GetMapping
    public List<Puzzle> getPuzzles() {
        return List.of(
                new Puzzle("FizzBuzz", "BY-PASS", "Easy", "Write a program that prints numbers from 1 to 100, replacing multiples of three and five."),
                new Puzzle("Palindrome Checker", "Faulty", "Easy", "Create a function that checks if a given string is a palindrome."),
                new Puzzle("Fibonacci Sequence", "Multi-Step", "Easy", "Write a function that returns the nth number in the Fibonacci sequence."),
                new Puzzle("Prime number lookup", "Multi-Step", "Easy", "Write a program that looks for all prime numbers up until 1000."),
                new Puzzle("File reader", "BY-PASS", "Medium", "Write a program that reads a file and displays certain content."),
                new Puzzle("Caesershift", "Faulty", "Hard", "Write a function that solves a caesershift.")
        );
    }
}

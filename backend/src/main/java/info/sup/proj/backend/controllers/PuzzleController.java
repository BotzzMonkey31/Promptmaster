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
                new Puzzle("Caesershift", "Faulty", "Hard", "Write a function that solves a caesershift."),
                new Puzzle("FizzBuzz1", "BY-PASS", "Easy", "Write a program that prints numbers from 1 to 100, replacing multiples of three and five."),
                new Puzzle("Palindrome Checker1", "Faulty", "Easy", "Create a function that checks if a given string is a palindrome."),
                new Puzzle("Fibonacci Sequence1", "Multi-Step", "Easy", "Write a function that returns the nth number in the Fibonacci sequence."),
                new Puzzle("Prime number lookup1", "Multi-Step", "Easy", "Write a program that looks for all prime numbers up until 1000."),
                new Puzzle("File reader1", "BY-PASS", "Medium", "Write a program that reads a file and displays certain content."),
                new Puzzle("Caesershift1", "Faulty", "Hard", "Write a function that solves a caesershift."),
                new Puzzle("FizzBuzz2", "BY-PASS", "Easy", "Write a program that prints numbers from 1 to 100, replacing multiples of three and five."),
                new Puzzle("Palindrome Checker2", "Faulty", "Easy", "Create a function that checks if a given string is a palindrome."),
                new Puzzle("Fibonacci Sequence2", "Multi-Step", "Easy", "Write a function that returns the nth number in the Fibonacci sequence."),
                new Puzzle("Prime number lookup2", "Multi-Step", "Easy", "Write a program that looks for all prime numbers up until 1000."),
                new Puzzle("File reader2", "BY-PASS", "Medium", "Write a program that reads a file and displays certain content."),
                new Puzzle("Caesershift2", "Faulty", "Hard", "Write a function that solves a caesershift."),
                new Puzzle("FizzBuzz3", "BY-PASS", "Easy", "Write a program that prints numbers from 1 to 100, replacing multiples of three and five."),
                new Puzzle("Palindrome Checker3", "Faulty", "Easy", "Create a function that checks if a given string is a palindrome."),
                new Puzzle("Fibonacci Sequence3", "Multi-Step", "Easy", "Write a function that returns the nth number in the Fibonacci sequence."),
                new Puzzle("Prime number lookup3", "Multi-Step", "Easy", "Write a program that looks for all prime numbers up until 1000."),
                new Puzzle("File reader3", "BY-PASS", "Medium", "Write a program that reads a file and displays certain content."),
                new Puzzle("Caesershift3", "Faulty", "Hard", "Write a function that solves a caesershift.")

        );
    }
}

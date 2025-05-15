package info.sup.proj.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import info.sup.proj.backend.model.Puzzle;
import java.util.List;

public interface PuzzleRepository extends JpaRepository<Puzzle, Integer> {
    List<Puzzle> findByType(Puzzle.Type type);
}
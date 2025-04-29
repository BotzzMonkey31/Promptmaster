package info.sup.proj.backend.repositories;

import info.sup.proj.backend.model.PuzzleSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PuzzleSessionRepository extends JpaRepository<PuzzleSession, Long> {
    Optional<PuzzleSession> findByPuzzleId(Integer puzzleId);
    Optional<PuzzleSession> findByPuzzleIdAndUserId(Integer puzzleId, Long userId);
}
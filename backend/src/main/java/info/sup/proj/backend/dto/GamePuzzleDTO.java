package info.sup.proj.backend.dto;

import info.sup.proj.backend.model.Puzzle;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GamePuzzleDTO {
    private String id;
    private String name;
    private String content;
    private String description;
    private boolean completed;

    public GamePuzzleDTO() {
    }

    public GamePuzzleDTO(String id, String name, String content, String description, boolean completed) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.description = description;
        this.completed = completed;
    }

    public static GamePuzzleDTO fromEntity(Puzzle puzzle) {
        return new GamePuzzleDTO(
            puzzle.getId().toString(),
            puzzle.getName(),
            puzzle.getDescription(),
            "Solve this puzzle by implementing the correct solution.",
            false
        );
    }
} 
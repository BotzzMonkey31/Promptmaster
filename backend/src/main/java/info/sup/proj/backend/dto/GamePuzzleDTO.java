package info.sup.proj.backend.dto;

import info.sup.proj.backend.model.Puzzle;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
} 
package info.sup.proj.backend.model;

public class Puzzle {
    private String name;
    private String description;
    private String type;
    private String difficulty;

    public Puzzle(String name, String description, String type, String difficulty) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.difficulty = difficulty;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDifficulty() {
        return difficulty;
    }
}
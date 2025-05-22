package info.sup.proj.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "puzzles")
@Getter
@Setter
public class Puzzle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private Type type;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    public Puzzle() {
    }

    public Puzzle(String name, Difficulty difficulty, Type type, String description) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.difficulty = difficulty;
    }

    public enum Type {
        BY_PASS, FAULTY, MULTI_STEP
    }

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

}
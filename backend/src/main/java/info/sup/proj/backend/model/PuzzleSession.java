package info.sup.proj.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "puzzle_sessions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"puzzle_id", "user_id"})
})
public class PuzzleSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "puzzle_id", nullable = false)
    private Puzzle puzzle;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ElementCollection
    @CollectionTable(name = "session_interactions", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "interaction_order")
    private List<Interaction> interactions = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String currentCode = "";
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;
    
    // Metrics
    private Integer attemptCount = 1;
    private Integer bestInteractionCount = null;
    private Long bestTimeSeconds = null;
    private Boolean isCompleted = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUpdatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }

    // For storing individual user-AI interaction
    @Embeddable
    public static class Interaction {
        private String userInput;
        
        @Column(columnDefinition = "TEXT")
        private String aiTextResponse;
        
        @Column(columnDefinition = "TEXT")
        private String aiCodeResponse;

        public Interaction() {
        }

        public Interaction(String userInput, String aiTextResponse, String aiCodeResponse) {
            this.userInput = userInput;
            this.aiTextResponse = aiTextResponse;
            this.aiCodeResponse = aiCodeResponse;
        }

        public String getUserInput() {
            return userInput;
        }

        public void setUserInput(String userInput) {
            this.userInput = userInput;
        }

        public String getAiTextResponse() {
            return aiTextResponse;
        }

        public void setAiTextResponse(String aiTextResponse) {
            this.aiTextResponse = aiTextResponse;
        }

        public String getAiCodeResponse() {
            return aiCodeResponse;
        }

        public void setAiCodeResponse(String aiCodeResponse) {
            this.aiCodeResponse = aiCodeResponse;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Puzzle getPuzzle() {
        return puzzle;
    }

    public void setPuzzle(Puzzle puzzle) {
        this.puzzle = puzzle;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Interaction> getInteractions() {
        return interactions;
    }

    public void setInteractions(List<Interaction> interactions) {
        this.interactions = interactions;
    }

    public void addInteraction(String userInput, String aiTextResponse, String aiCodeResponse) {
        this.interactions.add(new Interaction(userInput, aiTextResponse, aiCodeResponse));
    }

    public String getCurrentCode() {
        return currentCode;
    }

    public void setCurrentCode(String currentCode) {
        this.currentCode = currentCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public void incrementAttemptCount() {
        this.attemptCount = (this.attemptCount == null ? 1 : this.attemptCount + 1);
    }

    public Integer getBestInteractionCount() {
        return bestInteractionCount;
    }

    public void setBestInteractionCount(Integer bestInteractionCount) {
        this.bestInteractionCount = bestInteractionCount;
    }

    public Long getBestTimeSeconds() {
        return bestTimeSeconds;
    }

    public void setBestTimeSeconds(Long bestTimeSeconds) {
        this.bestTimeSeconds = bestTimeSeconds;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean completed) {
        isCompleted = completed;
    }

    public void updateBestMetrics() {
        if (isCompleted && interactions != null && !interactions.isEmpty()) {
            int currentInteractionCount = interactions.size();
            
            // Calculate time taken in seconds
            long currentTimeSeconds = java.time.Duration.between(createdAt, lastUpdatedAt).getSeconds();
            
            // Update best metrics if this is first completion or better than previous
            if (bestInteractionCount == null || currentInteractionCount < bestInteractionCount) {
                bestInteractionCount = currentInteractionCount;
            }
            
            if (bestTimeSeconds == null || currentTimeSeconds < bestTimeSeconds) {
                bestTimeSeconds = currentTimeSeconds;
            }
        }
    }
}
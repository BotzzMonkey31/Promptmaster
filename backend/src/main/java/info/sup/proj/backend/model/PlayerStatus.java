package info.sup.proj.backend.model;

public class PlayerStatus {
    private int score;
    private boolean hasCompleted;

    public PlayerStatus() {
    }

    public PlayerStatus(int score, boolean hasCompleted) {
        this.score = score;
        this.hasCompleted = hasCompleted;
    }

    public static PlayerStatus create(int score, boolean hasCompleted) {
        return new PlayerStatus(score, hasCompleted);
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isHasCompleted() {
        return hasCompleted;
    }

    public void setHasCompleted(boolean hasCompleted) {
        this.hasCompleted = hasCompleted;
    }
} 
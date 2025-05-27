package info.sup.proj.backend.model;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
public class Game {
    private final String id;
    private final List<Player> players;
    private final int totalRounds;
    private int currentRound;
    private String currentTurn;
    private Puzzle currentPuzzle;
    private final Map<String, PlayerStatus> playerStatus;
    private GameState state;
    private long roundStartTime;
    private Map<String, String> playerCode;

    public Game(String id, List<Player> players, Puzzle puzzle, int totalRounds) {
        this.id = id;
        this.players = new ArrayList<>(players);
        this.currentPuzzle = puzzle;
        this.totalRounds = totalRounds;
        this.currentRound = 1;
        this.currentTurn = players.getFirst().getId();
        this.playerStatus = new HashMap<>();
        this.state = GameState.IN_PROGRESS;
        this.roundStartTime = System.currentTimeMillis();
        this.playerCode = new HashMap<>();

        // Initialize player status and code
        for (Player player : players) {
            playerStatus.put(player.getId(), new PlayerStatus());
            playerCode.put(player.getId(), "");
        }
    }

    public String getId() {
        return id;
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public Puzzle getCurrentPuzzle() {
        return currentPuzzle;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public int getTotalRounds() {
        return totalRounds;
    }

    public String getPlayerCode(String playerId) {
        return playerCode.getOrDefault(playerId, "");
    }

    public Map<String, PlayerStatus> getPlayerStatus() {
        return Collections.unmodifiableMap(playerStatus);
    }

    public long getRoundStartTime() {
        return roundStartTime;
    }

    public boolean hasPlayer(String playerId) {
        return players.stream().anyMatch(p -> p.getId().equals(playerId));
    }

    public void updateCurrentCode(String playerId, String code) {
        PlayerStatus status = playerStatus.get(playerId);
        if (status != null) {
            status.setCode(code);
        }
    }

    public void updatePlayerScore(String playerId, int score) {
        PlayerStatus status = playerStatus.get(playerId);
        if (status != null) {
            status.setScore(status.getScore() + score);
        }
    }

    public void markPlayerCompleted(String playerId) {
        PlayerStatus status = playerStatus.get(playerId);
        if (status != null) {
            status.setHasCompleted(true);
        }
    }

    public boolean hasPlayerCompleted(String playerId) {
        PlayerStatus status = playerStatus.get(playerId);
        return status != null && status.isHasCompleted();
    }

    public boolean allPlayersCompleted() {
        return playerStatus.values().stream().allMatch(PlayerStatus::isHasCompleted);
    }

    public void startNextRound(Puzzle puzzle) {
        this.currentPuzzle = puzzle;
        this.currentRound++;
        this.roundStartTime = System.currentTimeMillis();
        
        // Reset player status for new round
        playerStatus.values().forEach(status -> {
            status.setHasCompleted(false);
            status.setCode("");
        });
    }

    /**
     * Start a new round with an explicitly specified round number.
     * This helps prevent any round skipping issues.
     * 
     * @param newPuzzle The puzzle for the new round
     * @param roundNumber The explicit round number to set
     */
    public synchronized void startNextRoundWithExplicitNumber(Puzzle newPuzzle, int roundNumber) {
        if (roundNumber < currentRound) {
            return;
        }
        
        if (roundNumber > totalRounds) {
            roundNumber = totalRounds;
        }

        currentRound = roundNumber;

        if (newPuzzle == null) {
            if (currentPuzzle == null) {
                return;
            }
        } else {
            currentPuzzle = newPuzzle;
        }

        roundStartTime = System.currentTimeMillis();

        resetPlayersForNewRound();
    }

    private void resetPlayersForNewRound() {
        playerStatus.values().forEach(status -> {
            status.setHasCompleted(false);
            status.setCode("");
        });

        players.forEach(player -> playerCode.put(player.getId(), ""));
    }

    public void forfeit(String playerId) {
        PlayerStatus status = playerStatus.get(playerId);
        if (status != null) {
            status.setHasCompleted(true);
            status.setHasForfeit(true);
        }
        endGame();
    }

    public void endGame() {
        this.state = GameState.ENDED;
    }

    public boolean isEnded() {
        return this.state == GameState.ENDED;
    }

    @Getter
    @Setter
    public static class PlayerStatus {
        private int score;
        private boolean hasCompleted;
        private boolean hasForfeit;
        private String code;

        public PlayerStatus() {
            this.score = 0;
            this.hasCompleted = false;
            this.hasForfeit = false;
            this.code = "";
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

        public boolean isHasForfeit() {
            return hasForfeit;
        }

        public void setHasForfeit(boolean hasForfeit) {
            this.hasForfeit = hasForfeit;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    public enum GameState {
        IN_PROGRESS,
        ENDED
    }
} 
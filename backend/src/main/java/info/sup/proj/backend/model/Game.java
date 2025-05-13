package info.sup.proj.backend.model;

import java.util.*;

public class Game {
    private final String id;
    private final List<Player> players;
    private final int totalRounds;
    private int currentRound;
    private String currentTurn;
    private Puzzle puzzle;
    private final Map<String, PlayerStatus> playerStatus;
    private GameState state;
    private long roundStartTime;
    private Map<String, String> playerCode;

    public Game(String id, List<Player> players, Puzzle puzzle, int totalRounds) {
        this.id = id;
        this.players = new ArrayList<>(players);
        this.puzzle = puzzle;
        this.totalRounds = totalRounds;
        this.currentRound = 1;
        this.currentTurn = players.get(0).getId();
        this.playerStatus = new HashMap<>();
        this.state = GameState.IN_PROGRESS;
        this.roundStartTime = System.currentTimeMillis();
        this.playerCode = new HashMap<>();

        // Initialize player status and code
        for (Player player : players) {
            playerStatus.put(player.getId(), new PlayerStatus(0, false));
            playerCode.put(player.getId(), "");
        }
    }

    public String getId() {
        return id;
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public int getTotalRounds() {
        return totalRounds;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }

    public Puzzle getPuzzle() {
        return puzzle;
    }

    public Puzzle getCurrentPuzzle() {
        return puzzle;
    }

    public long getRoundStartTime() {
        return roundStartTime;
    }

    public String getCurrentCode() {
        return playerCode.get(currentTurn);
    }

    public void updateCurrentCode(String playerId, String code) {
        if (playerCode.containsKey(playerId)) {
            playerCode.put(playerId, code);
        }
    }

    public Map<String, PlayerStatus> getPlayerStatus() {
        return Collections.unmodifiableMap(playerStatus);
    }

    public GameState getState() {
        return state;
    }

    public boolean hasPlayer(String playerId) {
        return players.stream().anyMatch(p -> p.getId().equals(playerId));
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

    public boolean allPlayersCompleted() {
        return playerStatus.values().stream().allMatch(PlayerStatus::isHasCompleted);
    }

    public void startNextRound(Puzzle newPuzzle) {
        currentRound++;
        puzzle = newPuzzle;
        roundStartTime = System.currentTimeMillis();
        
        // Reset completion status and code for new round
        playerStatus.values().forEach(status -> status.setHasCompleted(false));
        players.forEach(player -> playerCode.put(player.getId(), ""));
        
        // Switch starting player
        currentTurn = players.get((currentRound - 1) % players.size()).getId();
    }

    public void forfeit(String playerId) {
        state = GameState.ENDED;
        
        // Set forfeiting player's score to 0 for the round
        PlayerStatus status = playerStatus.get(playerId);
        if (status != null) {
            status.setScore(0);
            status.setHasCompleted(true);
        }
    }

    public void endGame() {
        state = GameState.ENDED;
    }

    public static class PlayerStatus {
        private int score;
        private boolean hasCompleted;

        public PlayerStatus(int score, boolean hasCompleted) {
            this.score = score;
            this.hasCompleted = hasCompleted;
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

    public enum GameState {
        IN_PROGRESS,
        ENDED
    }
} 
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
        this.currentTurn = players.getFirst().getId();
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




    public String getPlayerCode(String playerId) {
        return playerCode.getOrDefault(playerId, "");
    }

    public Puzzle getCurrentPuzzle() {
        return puzzle;
    }

    public void updateCurrentCode(String playerId, String code) {
        playerCode.computeIfPresent(playerId, (k, v) -> code);
    }


    public Map<String, PlayerStatus> getPlayerStatus() {
        return Collections.unmodifiableMap(playerStatus);
    }

    public boolean hasPlayer(String playerId) {
        return players.stream().anyMatch(p -> p.getId().equals(playerId));
    }

    public void updatePlayerScore(String playerId, int score) {
        PlayerStatus status = playerStatus.get(playerId);
        if (status != null) {
            // Get the current score and add the new score
            int currentScore = status.getScore();
            int newScore = currentScore + score;
            
            status.setScore(newScore);
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

    /**
     * Start a new round with an explicitly specified round number.
     * This helps prevent any round skipping issues.
     * 
     * @param newPuzzle The puzzle for the new round
     * @param roundNumber The explicit round number to set
     */
    public synchronized void startNextRoundWithExplicitNumber(Puzzle newPuzzle, int roundNumber) {
        // Ensure the round number is valid (not less than current, not more than total)
        if (roundNumber < currentRound) {
            return;
        }
        
        if (roundNumber > totalRounds) {
            roundNumber = totalRounds;
        }
        
        // Force the specific round number
        currentRound = roundNumber;
        
        // Ensure we have a valid puzzle
        if (newPuzzle == null) {
            if (puzzle == null) {
                return;
            }
        } else {
            puzzle = newPuzzle;
        }
        
        // Reset round timer
        roundStartTime = System.currentTimeMillis();
        
        // Reset player states for the new round
        resetPlayersForNewRound();
    }
    
    /**
     * Helper method to reset player states for a new round
     */
    private void resetPlayersForNewRound() {
        // Reset completion status
        playerStatus.values().forEach(status -> status.setHasCompleted(false));
        
        // Reset player code
        players.forEach(player -> playerCode.put(player.getId(), ""));
        
        // Switch starting player (round-robin)
        if (players.isEmpty()) {
            currentTurn = players.get((currentRound - 1) % players.size()).getId();
        }
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

    @Getter
    @Setter
    public static class PlayerStatus {
        private int score;
        private boolean hasCompleted;

        public PlayerStatus(int score, boolean hasCompleted) {
            this.score = score;
            this.hasCompleted = hasCompleted;
        }
    }

    public enum GameState {
        IN_PROGRESS,
        ENDED
    }
} 
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
        System.out.println("GAME MODEL: Starting next round - currentRound: " + currentRound);
        int previousRound = currentRound;
        currentRound++;
        System.out.println("GAME MODEL: Round incremented from " + previousRound + " to " + currentRound);
        
        puzzle = newPuzzle;
        roundStartTime = System.currentTimeMillis();
        
        // Reset completion status and code for new round
        playerStatus.values().forEach(status -> status.setHasCompleted(false));
        players.forEach(player -> playerCode.put(player.getId(), ""));
        
        // Switch starting player
        currentTurn = players.get((currentRound - 1) % players.size()).getId();
        System.out.println("GAME MODEL: Next round setup complete - current turn: " + currentTurn);
    }

    /**
     * Start a new round with an explicitly specified round number.
     * This helps prevent any round skipping issues.
     * 
     * @param newPuzzle The puzzle for the new round
     * @param roundNumber The explicit round number to set
     */
    public synchronized void startNextRoundWithExplicitNumber(Puzzle newPuzzle, int roundNumber) {
        System.out.println("GAME MODEL: Starting next round with explicit number - from currentRound: " + currentRound + " to: " + roundNumber);
        
        // Ensure the round number is valid (not less than current, not more than total)
        if (roundNumber < currentRound) {
            System.out.println("GAME MODEL ERROR: Attempted to set round number (" + roundNumber + 
                ") less than current round (" + currentRound + "). Ignoring.");
            return;
        }
        
        if (roundNumber > totalRounds) {
            System.out.println("GAME MODEL WARNING: Attempted to set round number (" + roundNumber + 
                ") greater than total rounds (" + totalRounds + "). Capping at " + totalRounds);
            roundNumber = totalRounds;
        }
        
        int previousRound = currentRound;
        
        // Force the specific round number
        currentRound = roundNumber;
        
        System.out.println("GAME MODEL: Round explicitly set from " + previousRound + " to " + currentRound);
        
        // Ensure we have a valid puzzle
        if (newPuzzle == null) {
            System.out.println("GAME MODEL ERROR: Null puzzle provided for round " + currentRound);
            // Try to keep the existing puzzle rather than setting it to null
            if (puzzle == null) {
                System.out.println("GAME MODEL ERROR: No existing puzzle to fallback to!");
            } else {
                System.out.println("GAME MODEL: Using existing puzzle as fallback");
            }
        } else {
            puzzle = newPuzzle;
        }
        
        // Reset round timer
        roundStartTime = System.currentTimeMillis();
        System.out.println("GAME MODEL: Round timer reset to " + roundStartTime);
        
        // Reset player states for the new round
        resetPlayersForNewRound();
        System.out.println("GAME MODEL: Next round setup complete - current turn: " + currentTurn);
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
        if (players.size() > 0) {
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
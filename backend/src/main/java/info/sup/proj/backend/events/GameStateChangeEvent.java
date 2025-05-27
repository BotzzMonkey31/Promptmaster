package info.sup.proj.backend.events;

import org.springframework.context.ApplicationEvent;
import info.sup.proj.backend.model.Game;

public class GameStateChangeEvent extends ApplicationEvent {
    private final Game game;

    public GameStateChangeEvent(Object source, Game game) {
        super(source);
        this.game = game;
    }

    public Game getGame() {
        return game;
    }
} 
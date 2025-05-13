package info.sup.proj.backend.model;

public class Player {
    private String id;
    private String username;
    private String picture;

    public Player() {
    }

    public Player(String id, String username, String picture) {
        this.id = id;
        this.username = username;
        this.picture = picture;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }
} 
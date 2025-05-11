package info.sup.proj.backend.model;

public class GameMessage {
    private String type;
    private String content;
    private Long userId;

    // Default constructor for JSON deserialization
    public GameMessage() {
    }

    public GameMessage(String type, String content, Long userId) {
        this.type = type;
        this.content = content;
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
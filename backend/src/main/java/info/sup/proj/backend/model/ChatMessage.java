package info.sup.proj.backend.model;

public class ChatMessage {
    private String type;
    private String content;
    private String username;
    private String userPicture;
    private Long timestamp;
    
    public ChatMessage() {
    }
    
    public ChatMessage(String type, String content, String username, String userPicture, Long timestamp) {
        this.type = type;
        this.content = content;
        this.username = username;
        this.userPicture = userPicture;
        this.timestamp = timestamp;
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
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getUserPicture() {
        return userPicture;
    }
    
    public void setUserPicture(String userPicture) {
        this.userPicture = userPicture;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
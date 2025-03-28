package info.sup.proj.backend.model;

import lombok.Data;

@Data
public class ChatMessage {
    private String type;
    private String content;
    private String username;
    private String userPicture;
    private Long timestamp;
}
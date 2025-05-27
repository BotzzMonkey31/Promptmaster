package info.sup.proj.backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String type;
    private String content;
    private String username;
    private String userPicture;
    private Long timestamp;
}
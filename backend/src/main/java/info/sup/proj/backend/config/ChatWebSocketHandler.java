package info.sup.proj.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.sup.proj.backend.model.ChatMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);
        String messageJson = objectMapper.writeValueAsString(chatMessage);

        for (WebSocketSession webSocketSession : sessions.values()) {
            if (webSocketSession.isOpen()) {
                webSocketSession.sendMessage(new TextMessage(messageJson));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }
}
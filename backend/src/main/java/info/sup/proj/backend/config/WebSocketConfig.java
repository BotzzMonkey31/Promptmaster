package info.sup.proj.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Value("${app.cors.allowed-origins:https://localhost:5173,https://promptmaster-frontend.braveforest-8e4d5d0c.westeurope.azurecontainerapps.io}")
    private String[] allowedOrigins;
    
    @Autowired
    private GameWebSocketHandler gameWebSocketHandler;
    
    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Chat WebSocket handler
        registry.addHandler(chatWebSocketHandler, "/chat")
                .setAllowedOrigins(allowedOrigins);
        
        // Game WebSocket handler
        registry.addHandler(gameWebSocketHandler, "/game")
                .setAllowedOrigins(allowedOrigins);
    }
}
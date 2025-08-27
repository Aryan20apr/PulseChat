package com.messaging.pulsechat.webSocketComponents;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        // Extract user info from query parameters
        String query = request.getURI().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    attributes.put(keyValue[0], keyValue[1]);
                }
            }
        }
        
        return true; // Allow connection
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                             WebSocketHandler wsHandler, Exception exception) {
        // Post-handshake logic
    }
}
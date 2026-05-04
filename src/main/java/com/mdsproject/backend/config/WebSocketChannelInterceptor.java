package com.mdsproject.backend.config;

import com.mdsproject.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;

@Configuration
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Try to get token from native header first, then from session attributes (query param)
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            } else {
                // token passed as query param is stored in session attributes
                Object tokenAttr = accessor.getSessionAttributes() != null
                        ? accessor.getSessionAttributes().get("token")
                        : null;
                if (tokenAttr != null) {
                    token = tokenAttr.toString();
                }
            }

            if (token != null) {
                try {
                    String email = jwtService.extractEmail(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    if (jwtService.isTokenValid(token, userDetails)) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList());
                        accessor.setUser(auth);
                    }
                } catch (Exception ignored) {
                    // Invalid token – connection will still proceed but user won't be set
                }
            }
        }

        return message;
    }
}

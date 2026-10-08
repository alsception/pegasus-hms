
package org.alsception.pegasus.core.websocket;

import org.alsception.pegasus.core.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.alsception.pegasus.core.security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * WebSocketAuthInterceptor: autentifikacija JWT-om

    WebSocket nema klasične HTTP headere nakon spajanja, pa se autentifikacija radi na STOMP CONNECT frame-u:

    Provjeri je li poruka CONNECT.
    Pročita header Authorization: Bearer <token>.
    Validira JWT (jwtUtils.validateJwtToken).
    Iz tokena izvuče username i učita korisnika (loadUserByUsername).
    Napravi UsernamePasswordAuthenticationToken i postavi ga kao accessor.setUser(...).

    Zahvaljujući tome Spring zna ko je korisnik na toj WebSocket sesiji, pa /user/queue/... slanje radi prema username-u (Principal.getName()).
 * @author nix 08/10/2026
 */

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) 
    {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) 
        {
            logger.info("STOMP CONNECTED");

            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) 
            {
                String token = authHeader.substring(7);

                if (jwtUtils.validateJwtToken(token)) 
                {
                    String username = jwtUtils.getUsernameFromJwtToken(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    accessor.setUser(authentication);
                }
            }
        }

        return message;
    }
}
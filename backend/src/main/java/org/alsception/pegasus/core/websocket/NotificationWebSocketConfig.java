package org.alsception.pegasus.core.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;


/**
 
    Ova klasa konfigurira WebSocket komunikaciju preko STOMP protokola (poruke preko WebSocketa).

    enableSimpleBroker("/queue"): 
    uključuje ugrađeni in-memory broker. 
    Klijenti se pretplaćuju na destinacije koje počinju s /queue, a server im šalje poruke tamo.

    setApplicationDestinationPrefixes("/app"): 
    poruke koje klijent šalje na /app/... idu u @MessageMapping metode na serveru.

    setUserDestinationPrefix("/user"): 
    omogućava slanje poruke određenom korisniku (npr. convertAndSendToUser(username, "/queue/notifications", ...)). 
    Klijent se pretplati na /user/queue/notifications i dobije samo svoje poruke.

    registerStompEndpoints: 
    klijent se spaja na /api/ws. withSockJS() dodaje fallback (long-polling i sl.) ako WebSocket nije dostupan, 
    a setAllowedOriginPatterns("*") dozvoljava CORS sa bilo kojeg origina.

    configureClientInboundChannel: na ulazni kanal se ubacuje webSocketAuthInterceptor, pa svaka dolazna STOMP poruka prolazi kroz njega.

*/

@Configuration
@EnableWebSocketMessageBroker
public class NotificationWebSocketConfig implements WebSocketMessageBrokerConfigurer
{
    @Autowired
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config)
    {
        config.enableSimpleBroker("/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry)
    {
        registry.addEndpoint("/api/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();  //THIS IS IMPORTANT
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) 
    {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
package com.example.htmx.config;

import org.apache.catalina.Session;
import org.apache.catalina.session.StandardManager;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** メモリ内セッションの総数を制限し、再起動時にデータを復元しない。 */
@Configuration
public class SessionLimitConfig {
    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> sessionLimit() {
        return factory -> factory.addContextCustomizers(context -> {
            var manager = new StandardManager() {
                @Override
                public synchronized Session createSession(String sessionId) {
                    if (getActiveSessions() >= getMaxActiveSessions()) {
                        throw new SessionCapacityExceededException();
                    }
                    return super.createSession(sessionId);
                }
            };
            manager.setMaxActiveSessions(100);
            manager.setPathname(null);
            context.setManager(manager);
        });
    }
}

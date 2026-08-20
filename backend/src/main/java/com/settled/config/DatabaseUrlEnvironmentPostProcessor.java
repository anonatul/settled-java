package com.settled.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Converts a platform DATABASE_URL (postgres:// or postgresql://) into the
 * JDBC properties the app uses (DB_URL / DB_USERNAME / DB_PASSWORD), unless
 * those are already set explicitly. The PostgreSQL JDBC driver does not
 * understand the user:password@host form, so credentials are extracted here.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String JDBC_URL_PROPERTY = "DB_URL";
    private static final String USERNAME_PROPERTY = "DB_USERNAME";
    private static final String PASSWORD_PROPERTY = "DB_PASSWORD";
    private static final String RENDER_VAR = "DATABASE_URL";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.containsProperty(JDBC_URL_PROPERTY)) {
            return;
        }
        String databaseUrl = environment.getProperty(RENDER_VAR);
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        String url = databaseUrl.trim()
                .replaceFirst("^postgres://", "jdbc:postgresql://")
                .replaceFirst("^postgresql://", "jdbc:postgresql://");

        Properties properties = new Properties();
        properties.setProperty(JDBC_URL_PROPERTY, url);

        int at = url.indexOf('@');
        if (at > 0 && url.startsWith("jdbc:postgresql://")) {
            String userinfo = url.substring("jdbc:postgresql://".length(), at);
            String hostPart = url.substring(at + 1);
            properties.setProperty(JDBC_URL_PROPERTY, "jdbc:postgresql://" + hostPart);
            int colon = userinfo.indexOf(':');
            if (colon > 0) {
                if (!environment.containsProperty(USERNAME_PROPERTY)) {
                    properties.setProperty(USERNAME_PROPERTY, decode(userinfo.substring(0, colon)));
                }
                if (!environment.containsProperty(PASSWORD_PROPERTY)) {
                    properties.setProperty(PASSWORD_PROPERTY, decode(userinfo.substring(colon + 1)));
                }
            } else if (!environment.containsProperty(USERNAME_PROPERTY)) {
                properties.setProperty(USERNAME_PROPERTY, decode(userinfo));
            }
        }

        PropertySource<?> source = new PropertiesPropertySource("settledDatabaseUrl", properties);
        environment.getPropertySources().addFirst(source);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
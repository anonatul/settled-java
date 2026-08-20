package com.settled.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Properties;

/**
 * Converts Render's standard DATABASE_URL (postgres://user:pass@host:port/db)
 * into a JDBC URL the app can use, unless DB_URL is already set explicitly.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY = "DB_URL";
    private static final String RENDER_VAR = "DATABASE_URL";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.containsProperty(PROPERTY)) {
            return;
        }
        String databaseUrl = environment.getProperty(RENDER_VAR);
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        String jdbcUrl = databaseUrl
                .trim()
                .replaceFirst("^postgres://", "jdbc:postgresql://")
                .replaceFirst("^postgresql://", "jdbc:postgresql://");
        Properties properties = new Properties();
        properties.setProperty(PROPERTY, jdbcUrl);
        PropertySource<?> source = new PropertiesPropertySource("settledDatabaseUrl", properties);
        environment.getPropertySources().addFirst(source);
    }
}
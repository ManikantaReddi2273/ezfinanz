package com.ezfinanz;

import com.ezfinanz.config.PostgresDatabaseCreator;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Spring Boot entry point for the EZFINANZ personal-loan backend.
 * Loads local env vars, ensures the Postgres database exists, then starts the app.
 */
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class EzfinanzApplication {

    /** Bootstraps configuration and launches the Spring application context. */
    public static void main(String[] args) {
        loadLocalDotEnv();
        PostgresDatabaseCreator.ensureDatabaseExists();
        SpringApplication.run(EzfinanzApplication.class, args);
    }

    /**
     * Loads {@code backend/.env} into system properties before Spring starts
     * so early bootstrap (DB create) and springboot3-dotenv share the same values.
     * Real OS environment variables always win.
     */
    private static void loadLocalDotEnv() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        for (DotenvEntry entry : dotenv.entries()) {
            String key = entry.getKey();
            if (isBlank(System.getenv(key)) && isBlank(System.getProperty(key))) {
                System.setProperty(key, entry.getValue());
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

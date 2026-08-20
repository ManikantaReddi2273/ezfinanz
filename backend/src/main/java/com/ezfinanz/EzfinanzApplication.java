package com.ezfinanz;

import com.ezfinanz.config.PostgresDatabaseCreator;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class EzfinanzApplication {

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

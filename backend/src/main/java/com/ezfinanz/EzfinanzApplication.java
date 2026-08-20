package com.ezfinanz;

import com.ezfinanz.config.PostgresDatabaseCreator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class EzfinanzApplication {

    public static void main(String[] args) {
        PostgresDatabaseCreator.ensureDatabaseExists();
        SpringApplication.run(EzfinanzApplication.class, args);
    }
}

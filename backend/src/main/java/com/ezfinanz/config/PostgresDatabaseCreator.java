package com.ezfinanz.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates the {@code ezfinanz} PostgreSQL database before Spring Boot starts.
 * JDBC cannot create a database through the application URL.
 */
public final class PostgresDatabaseCreator {

    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/ezfinanz";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASSWORD = "reddi2273";
    private static final String TARGET_DATABASE = "ezfinanz";
    private static final Pattern JDBC_URL = Pattern.compile(
            "^jdbc:postgresql://([^/:]+)(?::(\\d+))?/([^?]+)"
    );

    private PostgresDatabaseCreator() {
    }

    public static void ensureDatabaseExists() {
        String appUrl = firstNonBlank(
                System.getenv("SPRING_DATASOURCE_URL"),
                DEFAULT_URL
        );
        String username = firstNonBlank(
                System.getenv("SPRING_DATASOURCE_USERNAME"),
                System.getenv("POSTGRES_USER"),
                DEFAULT_USER
        );
        String password = firstNonBlank(
                System.getenv("SPRING_DATASOURCE_PASSWORD"),
                System.getenv("POSTGRES_PASSWORD"),
                DEFAULT_PASSWORD
        );

        Matcher matcher = JDBC_URL.matcher(appUrl);
        if (!matcher.find()) {
            throw new IllegalStateException("Cannot parse PostgreSQL JDBC URL: " + appUrl);
        }

        String host = matcher.group(1);
        String port = matcher.group(2) != null ? matcher.group(2) : "5432";
        String database = matcher.group(3);
        if (!TARGET_DATABASE.equals(database)) {
            System.out.println("Skipping auto-create; datasource database is '" + database + "', not '" + TARGET_DATABASE + "'.");
            return;
        }

        String maintenanceUrl = "jdbc:postgresql://" + host + ":" + port + "/postgres";
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("PostgreSQL JDBC driver not found", e);
        }

        try (Connection connection = DriverManager.getConnection(maintenanceUrl, username, password);
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(true);
            boolean exists;
            try (ResultSet rs = statement.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + TARGET_DATABASE + "'"
            )) {
                exists = rs.next();
            }
            if (!exists) {
                statement.executeUpdate("CREATE DATABASE " + TARGET_DATABASE);
                System.out.println("Created PostgreSQL database '" + TARGET_DATABASE + "'.");
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not ensure PostgreSQL database '" + TARGET_DATABASE + "' exists. "
                            + "Is PostgreSQL running and are credentials correct?",
                    e
            );
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}

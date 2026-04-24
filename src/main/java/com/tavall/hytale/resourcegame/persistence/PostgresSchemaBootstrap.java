package com.tavall.hytale.resourcegame.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Applies the packaged Postgres schema scripts when a live database is available.
 */
public class PostgresSchemaBootstrap {
    private static final List<String> SCHEMA_RESOURCES = List.of(
            "schema/postgres/001_player_profile.sql",
            "schema/postgres/002_player_game_state.sql",
            "schema/postgres/003_player_game_state_evolution.sql"
    );

    private final Logger logger;

    public PostgresSchemaBootstrap(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public boolean ensureSchema(PostgresConnectionProvider connectionProvider) {
        Objects.requireNonNull(connectionProvider, "connectionProvider");
        try (Connection connection = connectionProvider.open()) {
            connection.setAutoCommit(false);
            for (String resourcePath : SCHEMA_RESOURCES) {
                runScript(connection, resourcePath);
            }
            connection.commit();
            logger.info("Postgres schema verified for resource game tables.");
            return true;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to verify Postgres schema for resource game tables: " + ex.getMessage(), ex);
            return false;
        }
    }

    private void runScript(Connection connection, String resourcePath) throws IOException, SQLException {
        String script = loadResource(resourcePath);
        for (String statementText : splitStatements(script)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(statementText);
            }
        }
    }

    private String loadResource(String resourcePath) throws IOException {
        ClassLoader classLoader = PostgresSchemaBootstrap.class.getClassLoader();
        InputStream stream = classLoader == null ? null : classLoader.getResourceAsStream(resourcePath);
        if (stream == null) {
            stream = PostgresSchemaBootstrap.class.getResourceAsStream("/" + resourcePath);
        }
        try (InputStream inputStream = stream) {
            if (inputStream == null) {
                throw new IOException("Schema resource not found: " + resourcePath);
            }
            String script = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            if (!script.isEmpty() && script.charAt(0) == '\uFEFF') {
                return script.substring(1);
            }
            return script;
        }
    }

    private List<String> splitStatements(String script) {
        return script.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> !line.startsWith("--"))
                .reduce(new java.util.ArrayList<String>(), (statements, line) -> {
                    if (statements.isEmpty()) {
                        statements.add(line);
                    } else {
                        int lastIndex = statements.size() - 1;
                        statements.set(lastIndex, statements.get(lastIndex) + System.lineSeparator() + line);
                    }
                    if (line.endsWith(";")) {
                        int lastIndex = statements.size() - 1;
                        statements.set(lastIndex, statements.get(lastIndex).substring(0, statements.get(lastIndex).length() - 1).trim());
                        statements.add("");
                    }
                    return statements;
                }, (left, right) -> {
                    left.addAll(right);
                    return left;
                }).stream()
                .map(String::trim)
                .filter(statement -> !statement.isBlank())
                .toList();
    }
}

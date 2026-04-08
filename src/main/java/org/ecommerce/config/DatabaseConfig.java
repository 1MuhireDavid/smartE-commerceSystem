package org.ecommerce.config;

import org.ecommerce.exception.PropertiesNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Provides a single shared database connection loaded from db.properties.
 * For a production system a proper connection pool would replace this.
 */
public class DatabaseConfig {

    private static DatabaseConfig instance;
    private Connection connection;

    private final String url;
    private final String username;
    private final String password;

    private DatabaseConfig() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new PropertiesNotFoundException(
                    "db.properties not found on classpath. " +
                    "Copy src/main/resources/db.properties and fill in your credentials.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new PropertiesNotFoundException("Failed to load db.properties", e);
        }
        url      = props.getProperty("db.url");
        username = props.getProperty("db.username");
        password = props.getProperty("db.password");
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    /** Returns the shared connection, re-opening it if it was closed. */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, username, password);
        }
        return connection;
    }


    public void beginTransaction() throws SQLException {
        getConnection().setAutoCommit(false);
    }

    public void commit() throws SQLException {
        Connection c = getConnection();
        c.commit();
        c.setAutoCommit(true);
    }

    public void rollback() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        } catch (SQLException ignored) {}
    }

    public void closeConnection() {
        if (connection != null) {
            try { connection.close(); }
            catch (SQLException ignored) {}
            finally { connection = null; }
        }
    }
}

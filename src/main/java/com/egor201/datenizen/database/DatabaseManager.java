package com.egor201.datenizen.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {

    private final Map<String, HikariDataSource> connectionPools;

    public DatabaseManager() {
        this.connectionPools = new ConcurrentHashMap<>();
    }

    public boolean connect(String id, String driver, String url, String user, String password) {
        if (connectionPools.containsKey(id)) {
            return false;
        }

        try {
            HikariConfig config = new HikariConfig();
            config.setDriverClassName(driver);
            config.setJdbcUrl(url);

            if (user != null && !user.isEmpty()) config.setUsername(user);
            if (password != null && !password.isEmpty()) config.setPassword(password);

            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(10000);
            config.setIdleTimeout(600000);
            config.setPoolName("Datenizen-" + id);

            HikariDataSource dataSource = new HikariDataSource(config);
            connectionPools.put(id, dataSource);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Connection getConnection(String id) throws SQLException {
        HikariDataSource dataSource = connectionPools.get(id);
        if (dataSource != null) {
            return dataSource.getConnection();
        }
        throw new SQLException("Database ID " + id + " not found");
    }

    public void closeAllConnections() {
        for (HikariDataSource ds : connectionPools.values()) {
            if (ds != null && !ds.isClosed()) {
                ds.close();
            }
        }
        connectionPools.clear();
    }
}
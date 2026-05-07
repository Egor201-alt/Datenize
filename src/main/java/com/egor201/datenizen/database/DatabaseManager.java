package com.egor201.datenizen.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {

    private final Map<String, HikariDataSource> connectionPools;
    private final Map<String, Connection> activeTransactions;

    public DatabaseManager() {
        this.connectionPools = new ConcurrentHashMap<>();
        this.activeTransactions = new ConcurrentHashMap<>();
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
    
    public HikariDataSource getDataSource(String id) {
        return connectionPools.get(id);
    }

    public boolean startTransaction(String txId, String dbId) throws SQLException {
        if (activeTransactions.containsKey(txId)) return false;
        Connection conn = getConnection(dbId);
        conn.setAutoCommit(false);
        activeTransactions.put(txId, conn);
        return true;
    }

    public boolean commitTransaction(String txId) throws SQLException {
        Connection conn = activeTransactions.remove(txId);
        if (conn != null) {
            conn.commit();
            conn.setAutoCommit(true);
            conn.close();
            return true;
        }
        return false;
    }

    public boolean rollbackTransaction(String txId) throws SQLException {
        Connection conn = activeTransactions.remove(txId);
        if (conn != null) {
            conn.rollback();
            conn.setAutoCommit(true);
            conn.close();
            return true;
        }
        return false;
    }

    public Connection getTransactionConnection(String txId) {
        return activeTransactions.get(txId);
    }

    public void closeAllConnections() {
        for (Connection conn : activeTransactions.values()) {
            try {
                conn.rollback();
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {}
        }
        activeTransactions.clear();

        for (HikariDataSource ds : connectionPools.values()) {
            if (ds != null && !ds.isClosed()) {
                ds.close();
            }
        }
        connectionPools.clear();
    }
}
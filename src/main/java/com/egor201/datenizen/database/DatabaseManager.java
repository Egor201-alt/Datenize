package com.egor201.datenizen.database;

import com.egor201.datenizen.Datenizen;
import com.egor201.datenizen.events.DbConnectionLeakedEvent;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {

    private final Map<String, HikariDataSource> connectionPools;
    private final Map<String, Connection> activeTransactions;
    private final Map<String, Long> transactionStartTimes;
    private final Map<String, String> transactionDbIds;

    public DatabaseManager() {
        this.connectionPools = new ConcurrentHashMap<>();
        this.activeTransactions = new ConcurrentHashMap<>();
        this.transactionStartTimes = new ConcurrentHashMap<>();
        this.transactionDbIds = new ConcurrentHashMap<>();

        Bukkit.getScheduler().runTaskTimerAsynchronously(Datenizen.getInstance(), () -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, Long> entry : transactionStartTimes.entrySet()) {
                if (now - entry.getValue() > 300000) { 
                    String txId = entry.getKey();
                    long duration = (now - entry.getValue()) / 1000;
                    
                    Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> 
                        DbConnectionLeakedEvent.instance.fireFor(txId, duration)
                    );
                    
                    try {
                        rollbackTransaction(txId);
                    } catch (SQLException ignored) {}
                }
            }
        }, 1200L, 1200L); 
    }

    public boolean connect(String id, String driver, String url, String user, String password) {
        if (connectionPools.containsKey(id)) return false;

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
        if (dataSource != null) return dataSource.getConnection();
        throw new SQLException("Database ID " + id + " not found");
    }

    public HikariDataSource getDataSource(String id) {
        return connectionPools.get(id);
    }

    public String getDatabaseType(String id) {
        HikariDataSource ds = connectionPools.get(id);
        if (ds == null) return "unknown";
        String url = ds.getJdbcUrl();
        if (url.contains("sqlite")) return "sqlite";
        if (url.contains("postgresql")) return "postgresql";
        return "mysql";
    }

    public boolean startTransaction(String txId, String dbId) throws SQLException {
        if (activeTransactions.containsKey(txId)) return false;
        Connection conn = getConnection(dbId);
        conn.setAutoCommit(false);
        activeTransactions.put(txId, conn);
        transactionStartTimes.put(txId, System.currentTimeMillis());
        transactionDbIds.put(txId, dbId);
        return true;
    }

    public boolean commitTransaction(String txId) throws SQLException {
        Connection conn = activeTransactions.remove(txId);
        transactionStartTimes.remove(txId);
        transactionDbIds.remove(txId);
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
        transactionStartTimes.remove(txId);
        transactionDbIds.remove(txId);
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

    public String getTxDbId(String txId) {
        return transactionDbIds.get(txId);
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
        transactionStartTimes.clear();
        transactionDbIds.clear();

        for (HikariDataSource ds : connectionPools.values()) {
            if (ds != null && !ds.isClosed()) ds.close();
        }
        connectionPools.clear();
    }
}
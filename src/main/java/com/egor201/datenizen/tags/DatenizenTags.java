package com.egor201.datenizen.tags;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.tags.TagManager;
import com.egor201.datenizen.Datenizen;
import com.egor201.datenizen.events.DbErrorEvent;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.regex.Pattern;

public class DatenizenTags {

    private static final Pattern SAFE_NAME = Pattern.compile("^[a-zA-Z0-9_]+$");

    private static ListTag getResultSetAsList(String id, String sql, ListTag args) {
        try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (args != null) {
                for (int i = 0; i < args.size(); i++) ps.setObject(i + 1, args.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                ListTag resultList = new ListTag();

                while (rs.next()) {
                    MapTag row = new MapTag();
                    for (int i = 1; i <= columnCount; i++) {
                        Object val = rs.getObject(i);
                        row.putObject(meta.getColumnName(i), new ElementTag(val == null ? "null" : val.toString()));
                    }
                    resultList.addObject(row);
                }
                return resultList;
            }
        } catch (Exception e) {
            Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> DbErrorEvent.instance.fireFor(id, e.getMessage(), sql));
            return null;
        }
    }

    public static void register() {

        // <--[tag]
        // @Attribute <db_query[<id>].sql[<query>].args[<list>]>
        // @Returns ListTag
        // @Group Datenizen
        // @Description Returns query results as a ListTag of MapTags.
        // -->
        TagManager.registerTagHandler(ListTag.class, "db_query", attribute -> {
            if (!attribute.hasParam()) return null;
            String id = attribute.getParam();
            attribute.fulfill(1);
            if (attribute.startsWith("sql") && attribute.hasParam()) {
                String sql = attribute.getParam();
                attribute.fulfill(1);
                ListTag args = attribute.startsWith("args") && attribute.hasParam() ? attribute.contextAsType(1, ListTag.class) : null;
                if (args != null) attribute.fulfill(1);
                return getResultSetAsList(id, sql, args);
            }
            return null;
        });

        // <--[tag]
        // @Attribute <db_convert_map[<id>].sql[<query>].args[<list>]>
        // @Returns MapTag
        // @Group Datenizen
        // @Description Returns a MapTag where the first column is the key and the second is the value.
        // -->
        TagManager.registerTagHandler(MapTag.class, "db_convert_map", attribute -> {
            if (!attribute.hasParam()) return null;
            String id = attribute.getParam();
            attribute.fulfill(1);

            if (attribute.startsWith("sql") && attribute.hasParam()) {
                String sql = attribute.getParam();
                attribute.fulfill(1);

                ListTag args = attribute.startsWith("args") && attribute.hasParam() ? attribute.contextAsType(1, ListTag.class) : null;
                if (args != null) attribute.fulfill(1);

                try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id);
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    if (args != null) {
                        for (int i = 0; i < args.size(); i++) ps.setObject(i + 1, args.get(i));
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        MapTag map = new MapTag();
                        while (rs.next()) {
                            Object key = rs.getObject(1);
                            Object val = rs.getObject(2);
                            if (key != null) {
                                map.putObject(key.toString(), new ElementTag(val == null ? "null" : val.toString()));
                            }
                        }
                        return map;
                    }
                } catch (Exception e) {
                    Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> DbErrorEvent.instance.fireFor(id, e.getMessage(), sql));
                }
            }
            return null;
        });

        // <--[tag]
        // @Attribute <db_query_as_json[<id>].sql[<query>].args[<list>]>
        // @Returns ElementTag
        // @Group Datenizen
        // @Description Returns the query result as a JSON array string.
        // -->
        TagManager.registerTagHandler(ElementTag.class, "db_query_as_json", attribute -> {
            if (!attribute.hasParam()) return null;
            String id = attribute.getParam();
            attribute.fulfill(1);

            if (attribute.startsWith("sql") && attribute.hasParam()) {
                String sql = attribute.getParam();
                attribute.fulfill(1);

                ListTag args = attribute.startsWith("args") && attribute.hasParam() ? attribute.contextAsType(1, ListTag.class) : null;
                if (args != null) attribute.fulfill(1);

                try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id);
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    if (args != null) {
                        for (int i = 0; i < args.size(); i++) ps.setObject(i + 1, args.get(i));
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData meta = rs.getMetaData();
                        int columnCount = meta.getColumnCount();
                        JsonArray jsonArray = new JsonArray();

                        while (rs.next()) {
                            JsonObject obj = new JsonObject();
                            for (int i = 1; i <= columnCount; i++) {
                                String val = rs.getString(i);
                                obj.addProperty(meta.getColumnName(i), val == null ? "null" : val);
                            }
                            jsonArray.add(obj);
                        }
                        return new ElementTag(jsonArray.toString());
                    }
                } catch (Exception e) {
                    Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> DbErrorEvent.instance.fireFor(id, e.getMessage(), sql));
                }
            }
            return null;
        });

        // <--[tag]
        // @Attribute <db_last_id[<tx_id>]>
        // @Returns ElementTag
        // @Group Datenizen
        // @Description Returns the last inserted ID. MUST be used providing a Transaction ID to guarantee the same connection.
        // -->
        TagManager.registerTagHandler(ElementTag.class, "db_last_id", attribute -> {
            if (!attribute.hasParam()) return null;
            String txId = attribute.getParam();
            attribute.fulfill(1);

            Connection conn = Datenizen.getInstance().getDatabaseManager().getTransactionConnection(txId);
            if (conn == null) return null;

            String dbId = Datenizen.getInstance().getDatabaseManager().getTxDbId(txId);
            String type = Datenizen.getInstance().getDatabaseManager().getDatabaseType(dbId);
            String sql = type.equals("sqlite") ? "SELECT last_insert_rowid()" : (type.equals("postgresql") ? "SELECT LASTVAL()" : "SELECT LAST_INSERT_ID()");

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ElementTag(rs.getString(1));
                }
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> DbErrorEvent.instance.fireFor(dbId, e.getMessage(), sql));
            }
            return null;
        });

        // <--[tag]
        // @Attribute <db_exists_table[<id>].table[<name>]>
        // @Returns ElementTag(Boolean)
        // @Group Datenizen
        // @Description Returns true if the specified table exists in the database.
        // -->
        TagManager.registerTagHandler(ElementTag.class, "db_exists_table", attribute -> {
            if (!attribute.hasParam()) return null;
            String id = attribute.getParam();
            attribute.fulfill(1);

            if (attribute.startsWith("table") && attribute.hasParam()) {
                String table = attribute.getParam();
                attribute.fulfill(1);

                try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id)) {
                    DatabaseMetaData meta = conn.getMetaData();
                    try (ResultSet rs = meta.getTables(null, null, table, null)) {
                        return new ElementTag(rs.next());
                    }
                } catch (Exception e) {
                    Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> DbErrorEvent.instance.fireFor(id, e.getMessage(), "CHECK TABLE EXISTS"));
                }
            }
            return new ElementTag(false);
        });

        // <--[tag]
        // @Attribute <db_value[<id>].sql[<query>].args[<list>]>
        // @Returns ElementTag
        // @Group Datenizen
        // @Description Returns the first column of the first row of the query result.
        // -->
        TagManager.registerTagHandler(ElementTag.class, "db_value", attribute -> {
            if (!attribute.hasParam()) return null;
            String id = attribute.getParam();
            attribute.fulfill(1);
            if (attribute.startsWith("sql") && attribute.hasParam()) {
                String sql = attribute.getParam();
                attribute.fulfill(1);
                ListTag args = attribute.startsWith("args") && attribute.hasParam() ? attribute.contextAsType(1, ListTag.class) : null;
                if (args != null) attribute.fulfill(1);
                try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id);
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    if (args != null) {
                        for (int i = 0; i < args.size(); i++) ps.setObject(i + 1, args.get(i));
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            Object val = rs.getObject(1);
                            return new ElementTag(val == null ? "null" : val.toString());
                        }
                    }
                } catch (Exception e) {
                    Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> DbErrorEvent.instance.fireFor(id, e.getMessage(), sql));
                }
            }
            return null;
        });

        // <--[tag]
        // @Attribute <db_exists[<id>].sql[<query>].args[<list>]>
        // @Returns ElementTag(Boolean)
        // @Group Datenizen
        // @Description Returns true if the query returns at least one row.
        // -->
        TagManager.registerTagHandler(ElementTag.class, "db_exists", attribute -> {
            if (!attribute.hasParam()) return null;
            String id = attribute.getParam();
            attribute.fulfill(1);
            if (attribute.startsWith("sql") && attribute.hasParam()) {
                String sql = attribute.getParam();
                attribute.fulfill(1);
                ListTag args = attribute.startsWith("args") && attribute.hasParam() ? attribute.contextAsType(1, ListTag.class) : null;
                if (args != null) attribute.fulfill(1);
                try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id);
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    if (args != null) {
                        for (int i = 0; i < args.size(); i++) ps.setObject(i + 1, args.get(i));
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        return new ElementTag(rs.next());
                    }
                } catch (Exception e) {
                    Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> DbErrorEvent.instance.fireFor(id, e.getMessage(), sql));
                }
            }
            return new ElementTag(false);
        });

        // <--[tag]
        // @Attribute <db_columns[<id>].table[<name>]>
        // @Returns ListTag
        // @Group Datenizen
        // @Description Returns column names for a given table.
        // -->
        TagManager.registerTagHandler(ListTag.class, "db_columns", attribute -> {
            if (!attribute.hasParam()) return null;
            String id = attribute.getParam();
            attribute.fulfill(1);
            if (attribute.startsWith("table") && attribute.hasParam()) {
                String table = attribute.getParam();
                attribute.fulfill(1);
                try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id)) {
                    DatabaseMetaData meta = conn.getMetaData();
                    try (ResultSet rs = meta.getColumns(null, null, table, null)) {
                        ListTag list = new ListTag();
                        while (rs.next()) {
                            list.addObject(new ElementTag(rs.getString("COLUMN_NAME")));
                        }
                        return list;
                    }
                } catch (Exception e) {
                    Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> DbErrorEvent.instance.fireFor(id, e.getMessage(), "GET COLUMNS"));
                }
            }
            return null;
        });

        // <--[tag]
        // @Attribute <db_count[<id>].table[<name>].where[<condition>]>
        // @Returns ElementTag
        // @Group Datenizen
        // @Description Returns the row count for a table. Table name must be alphanumeric/underscores only.
        // Use db_value with a parameterized query for dynamic conditions.
        // -->
        TagManager.registerTagHandler(ElementTag.class, "db_count", attribute -> {
            if (!attribute.hasParam()) return null;
            String id = attribute.getParam();
            attribute.fulfill(1);
            if (attribute.startsWith("table") && attribute.hasParam()) {
                String table = attribute.getParam();
                attribute.fulfill(1);

                if (!SAFE_NAME.matcher(table).matches()) {
                    Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                        DbErrorEvent.instance.fireFor(id, "Invalid table name: " + table, "db_count")
                    );
                    return null;
                }

                String sql = "SELECT COUNT(*) FROM " + table;
                try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id);
                     PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new ElementTag(rs.getInt(1));
                    }
                } catch (Exception e) {
                    Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> DbErrorEvent.instance.fireFor(id, e.getMessage(), sql));
                }
            }
            return null;
        });

        // <--[tag]
        // @Attribute <db_info[<id>]>
        // @Returns MapTag
        // @Group Datenizen
        // @Description Returns pool statistics for the specified database connection.
        // -->
        TagManager.registerTagHandler(MapTag.class, "db_info", attribute -> {
            if (!attribute.hasParam()) return null;
            String id = attribute.getParam();
            attribute.fulfill(1);
            HikariDataSource ds = Datenizen.getInstance().getDatabaseManager().getDataSource(id);
            if (ds != null) {
                HikariPoolMXBean mxBean = ds.getHikariPoolMXBean();
                if (mxBean != null) {
                    MapTag map = new MapTag();
                    map.putObject("active_connections", new ElementTag(mxBean.getActiveConnections()));
                    map.putObject("idle_connections", new ElementTag(mxBean.getIdleConnections()));
                    map.putObject("total_connections", new ElementTag(mxBean.getTotalConnections()));
                    map.putObject("threads_awaiting", new ElementTag(mxBean.getThreadsAwaitingConnection()));
                    return map;
                }
            }
            return null;
        });

        // <--[tag]
        // @Attribute <db_list>
        // @Returns ListTag
        // @Group Datenizen
        // @Description Returns a list of all active database connection IDs.
        // -->
        TagManager.registerTagHandler(ListTag.class, "db_list", attribute -> {
            ListTag list = new ListTag();
            list.addAll(Datenizen.getInstance().getDatabaseManager().getActiveIds());
            return list;
        });

        // <--[tag]
        // @Attribute <db_table_exists[<id>|<table_name>]>
        // @Returns ElementTag(Boolean)
        // @Group Datenizen
        // @Description Cleaner way to check if a table exists without using nested tags.
        // -->
        TagManager.registerTagHandler(ElementTag.class, "db_table_exists", attribute -> {
            if (!attribute.hasParam()) return null;
            String param = attribute.getParam();
            
            if (param.contains("|")) {
                String[] parts = param.split("\\|", 2);
                String id = parts[0];
                String table = parts[1];
                attribute.fulfill(1);
                
                try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id)) {
                    DatabaseMetaData meta = conn.getMetaData();
                    try (ResultSet rs = meta.getTables(null, null, table, null)) {
                        return new ElementTag(rs.next());
                    }
                } catch (Exception e) {
                    Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> DbErrorEvent.instance.fireFor(id, e.getMessage(), "CHECK TABLE EXISTS"));
                }
            }
            return new ElementTag(false);
        });
    }
}
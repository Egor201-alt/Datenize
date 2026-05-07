package com.egor201.datenizen.tags;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.tags.TagManager;
import com.egor201.datenizen.Datenizen;
import com.egor201.datenizen.events.DbErrorEvent;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class DatenizenTags {

    public static void register() {

        // <--[tag]
        // @Attribute <db_query[<id>].sql[<query>].args[<list>]>
        // @Returns ListTag
        // @Group Datenizen
        // @Description Queries the database and returns a list of maps.
        // -->
        TagManager.registerTagHandler(ListTag.class, "db_query", attribute -> {
            if (!attribute.hasParam()) return null;
            String id = attribute.getParam();
            attribute.fulfill(1);

            if (attribute.startsWith("sql") && attribute.hasParam()) {
                String sql = attribute.getParam();
                attribute.fulfill(1);

                ListTag args = null;
                if (attribute.startsWith("args") && attribute.hasParam()) {
                    args = attribute.contextAsType(1, ListTag.class);
                    attribute.fulfill(1);
                }

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
            return null;
        });

        // <--[tag]
        // @Attribute <db_rows[<id>].sql[<query>].args[<list>]>
        // @Returns ListTag
        // @Group Datenizen
        // @Description Alias for db_query.
        // -->
        TagManager.registerTagHandler(ListTag.class, "db_rows", attribute -> {
            // Re-uses db_query behavior
            return TagManager.tagObject("db_query" + attribute.getRawContext(), attribute.context).asType(ListTag.class, attribute.context);
        });

        // <--[tag]
        // @Attribute <db_value[<id>].sql[<query>].args[<list>]>
        // @Returns ElementTag
        // @Group Datenizen
        // @Description Returns the first column of the first row of a query.
        // -->
        TagManager.registerTagHandler(ElementTag.class, "db_value", attribute -> {
            if (!attribute.hasParam()) return null;
            String id = attribute.getParam();
            attribute.fulfill(1);

            if (attribute.startsWith("sql") && attribute.hasParam()) {
                String sql = attribute.getParam();
                attribute.fulfill(1);

                ListTag args = null;
                if (attribute.startsWith("args") && attribute.hasParam()) {
                    args = attribute.contextAsType(1, ListTag.class);
                    attribute.fulfill(1);
                }

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

                ListTag args = null;
                if (attribute.startsWith("args") && attribute.hasParam()) {
                    args = attribute.contextAsType(1, ListTag.class);
                    attribute.fulfill(1);
                }

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
        // @Description Returns a list of column names in a table.
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
        // @Returns ElementTag(Number)
        // @Group Datenizen
        // @Description Generates and executes a SELECT COUNT(*) query.
        // -->
        TagManager.registerTagHandler(ElementTag.class, "db_count", attribute -> {
            if (!attribute.hasParam()) return null;
            String id = attribute.getParam();
            attribute.fulfill(1);

            if (attribute.startsWith("table") && attribute.hasParam()) {
                String table = attribute.getParam();
                attribute.fulfill(1);

                String condition = "1=1";
                if (attribute.startsWith("where") && attribute.hasParam()) {
                    condition = attribute.getParam();
                    attribute.fulfill(1);
                }

                String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + condition;
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
        // @Description Returns HikariCP pool information.
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
    }
}
package com.egor201.datenizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.datenizen.Datenizen;
import com.egor201.datenizen.events.DbErrorEvent;
import com.egor201.datenizen.events.DbExecutedEvent;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class DbTableDeleteCommand extends AbstractCommand {

    // <--[command]
    // @Name db_table_delete
    // @Syntax db_table_delete [id:<id>] [table:<table>] [where:<list>] (label:<label>)
    // @Required 3
    // @Maximum 4
    // @Short Deletes rows from a table without writing SQL manually.
    // @Group Datenizen
    //
    // @Description
    // Builds and executes a DELETE statement asynchronously.
    // 'where' is a ListTag of "column=value" pairs joined with AND.
    // Column names must be alphanumeric/underscores only. Values are safely parameterized.
    // Fires 'db executed' on success with <context.affected_rows>.
    //
    // @Usage
    // Use to delete a player record.
    // - db_table_delete id:main table:players where:<list[uuid=abc-123]> label:remove_player
    // -->

    private static final Pattern SAFE_NAME = Pattern.compile("^[a-zA-Z0-9_]+$");

    public DbTableDeleteCommand() {
        setName("db_table_delete");
        setSyntax("db_table_delete [id:<id>] [table:<table>] [where:<list>] (label:<label>)");
        setRequiredArguments(3, 4);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("id") && arg.matchesPrefix("id")) {
                scriptEntry.addObject("id", arg.asElement());
            } else if (!scriptEntry.hasObject("table") && arg.matchesPrefix("table")) {
                scriptEntry.addObject("table", arg.asElement());
            } else if (!scriptEntry.hasObject("where") && arg.matchesPrefix("where")) {
                scriptEntry.addObject("where", arg.asType(ListTag.class));
            } else if (!scriptEntry.hasObject("label") && arg.matchesPrefix("label")) {
                scriptEntry.addObject("label", arg.asElement());
            } else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("id") || !scriptEntry.hasObject("table") || !scriptEntry.hasObject("where")) {
            throw new InvalidArgumentsException("Must specify id, table, and where!");
        }
    }

    private record Pair(String col, String val) {}

    @Override
    public void execute(ScriptEntry scriptEntry) {
        String id = scriptEntry.getElement("id").asString();
        String table = scriptEntry.getElement("table").asString();
        ListTag whereList = scriptEntry.getObjectTag("where");
        String label = scriptEntry.hasObject("label") ? scriptEntry.getElement("label").asString() : null;

        if (!SAFE_NAME.matcher(table).matches()) {
            Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                DbErrorEvent.instance.fireFor(id, "Invalid table name: " + table, "db_table_delete")
            );
            return;
        }

        List<Pair> wherePairs = new ArrayList<>();
        for (String entry : whereList) {
            int eq = entry.indexOf('=');
            if (eq < 1) {
                Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                    DbErrorEvent.instance.fireFor(id, "Invalid where entry: " + entry, "db_table_delete")
                );
                return;
            }
            String col = entry.substring(0, eq).trim();
            String val = entry.substring(eq + 1);
            if (!SAFE_NAME.matcher(col).matches()) {
                Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                    DbErrorEvent.instance.fireFor(id, "Invalid column name: " + col, "db_table_delete")
                );
                return;
            }
            wherePairs.add(new Pair(col, val));
        }

        if (wherePairs.isEmpty()) {
            Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                DbErrorEvent.instance.fireFor(id, "where list must not be empty", "db_table_delete")
            );
            return;
        }

        StringBuilder wherePart = new StringBuilder();
        for (int i = 0; i < wherePairs.size(); i++) {
            if (i > 0) wherePart.append(" AND ");
            wherePart.append(wherePairs.get(i).col()).append("=?");
        }

        String sql = "DELETE FROM " + table + " WHERE " + wherePart;

        Bukkit.getScheduler().runTaskAsynchronously(Datenizen.getInstance(), () -> {
            try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                for (int i = 0; i < wherePairs.size(); i++) {
                    ps.setObject(i + 1, wherePairs.get(i).val());
                }

                int affected = ps.executeUpdate();
                Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                    DbExecutedEvent.instance.fireFor(id, label, affected)
                );
            } catch (java.sql.SQLException e) {
                Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                    DbErrorEvent.instance.fireFor(id, e.getMessage(), e.getSQLState(), sql)
                );
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                    DbErrorEvent.instance.fireFor(id, e.getMessage(), null, sql)
                );
            }
        });
    }
}
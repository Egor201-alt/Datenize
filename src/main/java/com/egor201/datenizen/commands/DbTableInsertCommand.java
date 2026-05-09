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
import java.util.List;
import java.util.regex.Pattern;

public class DbTableInsertCommand extends AbstractCommand {

    // <--[command]
    // @Name db_table_insert
    // @Syntax db_table_insert [id:<id>] [table:<table>] [columns:<list>] [values:<list>] (label:<label>)
    // @Required 4
    // @Maximum 5
    // @Short Inserts a row into a table without writing SQL manually.
    // @Group Datenizen
    //
    // @Description
    // Builds and executes an INSERT statement from column and value lists asynchronously.
    // Column names must be alphanumeric/underscores only.
    // Values are safely passed as PreparedStatement parameters.
    // Fires 'db executed' on success with <context.affected_rows>.
    //
    // @Usage
    // Use to insert a player into a table.
    // - db_table_insert id:main table:players columns:<list[name|uuid|score]> values:<list[Steve|abc-123|0]> label:new_player
    // -->

    private static final Pattern SAFE_NAME = Pattern.compile("^[a-zA-Z0-9_]+$");

    public DbTableInsertCommand() {
        setName("db_table_insert");
        setSyntax("db_table_insert [id:<id>] [table:<table>] [columns:<list>] [values:<list>] (label:<label>)");
        setRequiredArguments(4, 5);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("id") && arg.matchesPrefix("id")) {
                scriptEntry.addObject("id", arg.asElement());
            } else if (!scriptEntry.hasObject("table") && arg.matchesPrefix("table")) {
                scriptEntry.addObject("table", arg.asElement());
            } else if (!scriptEntry.hasObject("columns") && arg.matchesPrefix("columns")) {
                scriptEntry.addObject("columns", arg.asType(ListTag.class));
            } else if (!scriptEntry.hasObject("values") && arg.matchesPrefix("values")) {
                scriptEntry.addObject("values", arg.asType(ListTag.class));
            } else if (!scriptEntry.hasObject("label") && arg.matchesPrefix("label")) {
                scriptEntry.addObject("label", arg.asElement());
            } else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("id") || !scriptEntry.hasObject("table")
                || !scriptEntry.hasObject("columns") || !scriptEntry.hasObject("values")) {
            throw new InvalidArgumentsException("Must specify id, table, columns, and values!");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        String id = scriptEntry.getElement("id").asString();
        String table = scriptEntry.getElement("table").asString();
        ListTag columns = scriptEntry.getObjectTag("columns");
        ListTag values = scriptEntry.getObjectTag("values");
        String label = scriptEntry.hasObject("label") ? scriptEntry.getElement("label").asString() : null;

        if (!SAFE_NAME.matcher(table).matches()) {
            Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                DbErrorEvent.instance.fireFor(id, "Invalid table name: " + table, "db_table_insert")
            );
            return;
        }

        for (String col : columns) {
            if (!SAFE_NAME.matcher(col).matches()) {
                Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                    DbErrorEvent.instance.fireFor(id, "Invalid column name: " + col, "db_table_insert")
                );
                return;
            }
        }

        if (columns.size() != values.size()) {
            Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                DbErrorEvent.instance.fireFor(id, "columns and values lists must be the same size", "db_table_insert")
            );
            return;
        }

        StringBuilder colPart = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<String> colList = columns;
        for (int i = 0; i < colList.size(); i++) {
            colPart.append(colList.get(i));
            placeholders.append("?");
            if (i < colList.size() - 1) {
                colPart.append(",");
                placeholders.append(",");
            }
        }

        String sql = "INSERT INTO " + table + " (" + colPart + ") VALUES (" + placeholders + ")";

        Bukkit.getScheduler().runTaskAsynchronously(Datenizen.getInstance(), () -> {
            try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                List<String> valList = values;
                for (int i = 0; i < valList.size(); i++) {
                    ps.setObject(i + 1, valList.get(i));
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
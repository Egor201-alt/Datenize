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

public class DbTableUpdateCommand extends AbstractCommand {

    // <--[command]
    // @Name db_table_update
    // @Syntax db_table_update [id:<id>] [table:<table>] [set:<list>] [where:<list>] (label:<label>)
    // @Required 4
    // @Maximum 5
    // @Short Updates rows in a table without writing SQL manually.
    // @Group Datenizen
    //
    // @Description
    // Builds and executes an UPDATE statement asynchronously.
    // 'set' and 'where' are ListTags of "column=value" pairs.
    // Column names must be alphanumeric/underscores only. Values are safely parameterized.
    // Fires 'db executed' on success with <context.affected_rows>.
    //
    // @Usage
    // Use to update a player's score.
    // - db_table_update id:main table:players set:<list[score=100|rank=gold]> where:<list[uuid=abc-123]> label:update_score
    // -->

    private static final Pattern SAFE_NAME = Pattern.compile("^[a-zA-Z0-9_]+$");

    public DbTableUpdateCommand() {
        setName("db_table_update");
        setSyntax("db_table_update [id:<id>] [table:<table>] [set:<list>] [where:<list>] (label:<label>)");
        setRequiredArguments(4, 5);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("id") && arg.matchesPrefix("id")) {
                scriptEntry.addObject("id", arg.asElement());
            } else if (!scriptEntry.hasObject("table") && arg.matchesPrefix("table")) {
                scriptEntry.addObject("table", arg.asElement());
            } else if (!scriptEntry.hasObject("set") && arg.matchesPrefix("set")) {
                scriptEntry.addObject("set", arg.asType(ListTag.class));
            } else if (!scriptEntry.hasObject("where") && arg.matchesPrefix("where")) {
                scriptEntry.addObject("where", arg.asType(ListTag.class));
            } else if (!scriptEntry.hasObject("label") && arg.matchesPrefix("label")) {
                scriptEntry.addObject("label", arg.asElement());
            } else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("id") || !scriptEntry.hasObject("table")
                || !scriptEntry.hasObject("set") || !scriptEntry.hasObject("where")) {
            throw new InvalidArgumentsException("Must specify id, table, set, and where!");
        }
    }

    private record Pair(String col, String val) {}

    private List<Pair> parsePairs(ListTag list) {
        List<Pair> pairs = new ArrayList<>();
        for (String entry : list) {
            int eq = entry.indexOf('=');
            if (eq < 1) return null;
            String col = entry.substring(0, eq).trim();
            String val = entry.substring(eq + 1);
            if (!SAFE_NAME.matcher(col).matches()) return null;
            pairs.add(new Pair(col, val));
        }
        return pairs;
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        String id = scriptEntry.getElement("id").asString();
        String table = scriptEntry.getElement("table").asString();
        ListTag setList = scriptEntry.getObjectTag("set");
        ListTag whereList = scriptEntry.getObjectTag("where");
        String label = scriptEntry.hasObject("label") ? scriptEntry.getElement("label").asString() : null;

        if (!SAFE_NAME.matcher(table).matches()) {
            Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                DbErrorEvent.instance.fireFor(id, "Invalid table name: " + table, "db_table_update")
            );
            return;
        }

        List<Pair> setPairs = parsePairs(setList);
        List<Pair> wherePairs = parsePairs(whereList);

        if (setPairs == null || setPairs.isEmpty()) {
            Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                DbErrorEvent.instance.fireFor(id, "Invalid or empty 'set' list", "db_table_update")
            );
            return;
        }
        if (wherePairs == null || wherePairs.isEmpty()) {
            Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                DbErrorEvent.instance.fireFor(id, "Invalid or empty 'where' list", "db_table_update")
            );
            return;
        }

        StringBuilder setPart = new StringBuilder();
        for (int i = 0; i < setPairs.size(); i++) {
            if (i > 0) setPart.append(",");
            setPart.append(setPairs.get(i).col()).append("=?");
        }

        StringBuilder wherePart = new StringBuilder();
        for (int i = 0; i < wherePairs.size(); i++) {
            if (i > 0) wherePart.append(" AND ");
            wherePart.append(wherePairs.get(i).col()).append("=?");
        }

        String sql = "UPDATE " + table + " SET " + setPart + " WHERE " + wherePart;

        Bukkit.getScheduler().runTaskAsynchronously(Datenizen.getInstance(), () -> {
            try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                int idx = 1;
                for (Pair p : setPairs) ps.setObject(idx++, p.val());
                for (Pair p : wherePairs) ps.setObject(idx++, p.val());

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
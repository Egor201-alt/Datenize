package com.egor201.datenizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.datenizen.Datenizen;
import com.egor201.datenizen.events.DbErrorEvent;
import com.egor201.datenizen.events.DbExecutedEvent;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class DbExecuteBatchCommand extends AbstractCommand {

    // <--[command]
    // @Name db_execute_batch
    // @Syntax db_execute_batch[id:<id>] [sql:<query>] [args:<list_of_lists>] (label:<label>)
    // @Required 3
    // @Maximum 4
    // @Short Executes a parameterized SQL query multiple times efficiently.
    // @Group Datenizen
    //
    // @Description
    // Uses JDBC batching to insert or update multiple rows in a single operation.
    // Provide a ListTag where each element is a ListTag of arguments for one row.
    // Statements are executed inside a transaction and rolled back on failure.
    // Fires 'db executed' on success with <context.affected_rows>.
    // -->

    public DbExecuteBatchCommand() {
        setName("db_execute_batch");
        setSyntax("db_execute_batch [id:<id>] [sql:<query>][args:<list_of_lists>] (label:<label>)");
        setRequiredArguments(3, 4);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        StringBuilder sqlBuilder = new StringBuilder();
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("id") && arg.matchesPrefix("id")) {
                scriptEntry.addObject("id", arg.asElement());
            } else if (!scriptEntry.hasObject("args") && arg.matchesPrefix("args")) {
                scriptEntry.addObject("args", arg.asType(ListTag.class));
            } else if (!scriptEntry.hasObject("label") && arg.matchesPrefix("label")) {
                scriptEntry.addObject("label", arg.asElement());
            } else if (arg.matchesPrefix("sql")) {
                sqlBuilder.append(arg.getValue());
            } else if (!arg.hasPrefix() && sqlBuilder.length() > 0) {
                sqlBuilder.append(" ").append(arg.getRawValue());
            } else if (!scriptEntry.hasObject("sql") && arg.getRawValue().startsWith("sql:")) {
                sqlBuilder.append(arg.getRawValue().substring(4));
            } else {
                arg.reportUnhandled();
            }
        }
        if (sqlBuilder.length() > 0) {
            scriptEntry.addObject("sql", new ElementTag(sqlBuilder.toString().trim()));
        }
        if (!scriptEntry.hasObject("id") || !scriptEntry.hasObject("sql") || !scriptEntry.hasObject("args")) {
            throw new InvalidArgumentsException("Must specify id, sql, and args!");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        String id = scriptEntry.getElement("id").asString();
        String sql = scriptEntry.getElement("sql").asString();
        ListTag argsList = scriptEntry.getObjectTag("args");
        String label = scriptEntry.hasObject("label") ? scriptEntry.getElement("label").asString() : null;

        Bukkit.getScheduler().runTaskAsynchronously(Datenizen.getInstance(), () -> {
            Connection conn = null;
            try {
                conn = Datenizen.getInstance().getDatabaseManager().getConnection(id);
                conn.setAutoCommit(false);
                
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (String row : argsList) {
                        ListTag rowArgs = ListTag.valueOf(row, scriptEntry.getContext());
                        for (int i = 0; i < rowArgs.size(); i++) {
                            ps.setObject(i + 1, rowArgs.get(i));
                        }
                        ps.addBatch();
                    }
                    int[] results = ps.executeBatch();
                    int totalAffected = 0;
                    for (int res : results) {
                        if (res > 0) totalAffected += res;
                    }
                    
                    conn.commit();
                    
                    final int affected = totalAffected;
                    Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> 
                        DbExecutedEvent.instance.fireFor(id, label, affected)
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (conn != null) {
                    try { conn.rollback(); } catch (Exception ignored) {}
                }
                Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> 
                    DbErrorEvent.instance.fireFor(id, e.getMessage(), sql)
                );
            } finally {
                if (conn != null) {
                    try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
                }
            }
        });
    }
}
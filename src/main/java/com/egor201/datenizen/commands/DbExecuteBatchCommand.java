package com.egor201.datenizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.datenizen.Datenizen;
import com.egor201.datenizen.events.DbErrorEvent;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class DbExecuteBatchCommand extends AbstractCommand {

    // <--[command]
    // @Name db_execute_batch
    // @Syntax db_execute_batch [id:<id>][sql:<query>] [args:<list_of_lists>]
    // @Required 3
    // @Maximum 3
    // @Short Executes a parameterized SQL query multiple times efficiently.
    // @Group Datenizen
    //
    // @Description
    // Uses JDBC batching to insert or update multiple rows in a single operation.
    // Provide a ListTag where each element is a ListTag of arguments for one row.
    // -->

    public DbExecuteBatchCommand() {
        setName("db_execute_batch");
        setSyntax("db_execute_batch[id:<id>] [sql:<query>] [args:<list_of_lists>]");
        setRequiredArguments(3, 3);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("id") && arg.matchesPrefix("id")) {
                scriptEntry.addObject("id", arg.asElement());
            } else if (!scriptEntry.hasObject("sql") && arg.matchesPrefix("sql")) {
                scriptEntry.addObject("sql", arg.asElement());
            } else if (!scriptEntry.hasObject("args") && arg.matchesPrefix("args")) {
                scriptEntry.addObject("args", arg.asType(ListTag.class));
            } else {
                arg.reportUnhandled();
            }
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        String id = scriptEntry.getElement("id").asString();
        String sql = scriptEntry.getElement("sql").asString();
        ListTag argsList = scriptEntry.getObjectTag("args");

        Bukkit.getScheduler().runTaskAsynchronously(Datenizen.getInstance(), () -> {
            try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                for (String row : argsList) {
                    ListTag rowArgs = ListTag.valueOf(row, scriptEntry.getContext());
                    for (int i = 0; i < rowArgs.size(); i++) {
                        ps.setObject(i + 1, rowArgs.get(i));
                    }
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> 
                    DbErrorEvent.instance.fireFor(id, e.getMessage(), sql)
                );
            }
        });
    }
}
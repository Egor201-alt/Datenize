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
import java.sql.Statement;

public class DbExecuteAsyncListCommand extends AbstractCommand {

    // <--[command]
    // @Name db_execute_async_list
    // @Syntax db_execute_async_list [id:<id>] [sql:<list>]
    // @Required 2
    // @Maximum 2
    // @Short Executes a list of SQL queries asynchronously.
    // @Group Datenizen
    //
    // @Description
    // Runs multiple raw SQL statements sequentially in a single async task using batch execution.
    // -->

    public DbExecuteAsyncListCommand() {
        setName("db_execute_async_list");
        setSyntax("db_execute_async_list[id:<id>] [sql:<list>]");
        setRequiredArguments(2, 2);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("id") && arg.matchesPrefix("id")) {
                scriptEntry.addObject("id", arg.asElement());
            } else if (!scriptEntry.hasObject("sql") && arg.matchesPrefix("sql")) {
                scriptEntry.addObject("sql", arg.asType(ListTag.class));
            } else {
                arg.reportUnhandled();
            }
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        String id = scriptEntry.getElement("id").asString();
        ListTag sqlList = scriptEntry.getObjectTag("sql");

        Bukkit.getScheduler().runTaskAsynchronously(Datenizen.getInstance(), () -> {
            try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id);
                 Statement st = conn.createStatement()) {
                
                for (String sql : sqlList) {
                    st.addBatch(sql);
                }
                st.executeBatch();
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> 
                    DbErrorEvent.instance.fireFor(id, e.getMessage(), "ASYNC LIST BATCH")
                );
            }
        });
    }
}
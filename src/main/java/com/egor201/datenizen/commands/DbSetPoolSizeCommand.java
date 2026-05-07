package com.egor201.datenizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.datenizen.Datenizen;
import com.zaxxer.hikari.HikariDataSource;

public class DbSetPoolSizeCommand extends AbstractCommand {

    // <--[command]
    // @Name db_set_pool_size
    // @Syntax db_set_pool_size [id:<id>][size:<int>]
    // @Required 2
    // @Maximum 2
    // @Short Modifies the maximum pool size dynamically.
    // @Group Datenizen
    //
    // @Description
    // Changes the HikariCP maximum pool size without restarting the plugin.
    // -->

    public DbSetPoolSizeCommand() {
        setName("db_set_pool_size");
        setSyntax("db_set_pool_size[id:<id>] [size:<int>]");
        setRequiredArguments(2, 2);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("id") && arg.matchesPrefix("id")) {
                scriptEntry.addObject("id", arg.asElement());
            } else if (!scriptEntry.hasObject("size") && arg.matchesPrefix("size")) {
                scriptEntry.addObject("size", arg.asElement());
            } else {
                arg.reportUnhandled();
            }
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        String id = scriptEntry.getElement("id").asString();
        int size = scriptEntry.getElement("size").asInt();

        HikariDataSource ds = Datenizen.getInstance().getDatabaseManager().getDataSource(id);
        if (ds != null) {
            ds.setMaximumPoolSize(size);
        }
    }
}
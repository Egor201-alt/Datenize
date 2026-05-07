package com.egor201.datenizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.datenizen.Datenizen;
import com.zaxxer.hikari.HikariDataSource;

public class DbTimeoutCommand extends AbstractCommand {
    // <--[command]
    // @Name db_timeout
    // @Syntax db_timeout [id:<id>] [ms:<int>]
    // @Required 2
    // @Maximum 2
    // @Short Dynamically sets the connection timeout in milliseconds.
    // @Group Datenizen
    // -->
    public DbTimeoutCommand() { setName("db_timeout"); setSyntax("db_timeout[id:<id>] [ms:<int>]"); setRequiredArguments(2, 2); }
    @Override public void parseArgs(ScriptEntry se) throws InvalidArgumentsException {
        for (Argument arg : se) {
            if (!se.hasObject("id") && arg.matchesPrefix("id")) se.addObject("id", arg.asElement());
            else if (!se.hasObject("ms") && arg.matchesPrefix("ms")) se.addObject("ms", arg.asElement());
        }
    }
    @Override public void execute(ScriptEntry se) {
        HikariDataSource ds = Datenizen.getInstance().getDatabaseManager().getDataSource(se.getElement("id").asString());
        if (ds != null) ds.getHikariConfigMXBean().setConnectionTimeout(se.getElement("ms").asInt());
    }
}
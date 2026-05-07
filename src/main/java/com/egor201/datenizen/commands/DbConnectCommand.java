package com.egor201.datenizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.datenizen.Datenizen;
import com.egor201.datenizen.events.DbConnectedEvent;

public class DbConnectCommand extends AbstractCommand {

    // <--[command]
    // @Name db_connect
    // @Syntax db_connect [id:<id>] [driver:<driver>][url:<url>] (user:<user>) (pass:<pass>)
    // @Required 3
    // @Maximum 5
    // @Short Connects to a database using HikariCP connection pooling.
    // @Group Datenizen
    //
    // @Description
    // Connects to a database and stores the connection pool under the specified ID.
    // Supported drivers include org.sqlite.JDBC, com.mysql.cj.jdbc.Driver, org.postgresql.Driver.
    //
    // @Usage
    // Use to connect to an SQLite database.
    // - db_connect id:local driver:org.sqlite.JDBC url:jdbc:sqlite:plugins/Datenizen/local.db
    //
    // @Usage
    // Use to connect to a MySQL database.
    // - db_connect id:main driver:com.mysql.cj.jdbc.Driver url:jdbc:mysql://localhost:3306/db user:root pass:123
    // -->

    public DbConnectCommand() {
        setName("db_connect");
        setSyntax("db_connect [id:<id>] [driver:<driver>][url:<url>] (user:<user>) (pass:<pass>)");
        setRequiredArguments(3, 5);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("id") && arg.matchesPrefix("id")) {
                scriptEntry.addObject("id", arg.asElement());
            } else if (!scriptEntry.hasObject("driver") && arg.matchesPrefix("driver")) {
                scriptEntry.addObject("driver", arg.asElement());
            } else if (!scriptEntry.hasObject("url") && arg.matchesPrefix("url")) {
                scriptEntry.addObject("url", arg.asElement());
            } else if (!scriptEntry.hasObject("user") && arg.matchesPrefix("user")) {
                scriptEntry.addObject("user", arg.asElement());
            } else if (!scriptEntry.hasObject("pass") && arg.matchesPrefix("pass")) {
                scriptEntry.addObject("pass", arg.asElement());
            } else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("id") || !scriptEntry.hasObject("driver") || !scriptEntry.hasObject("url")) {
            throw new InvalidArgumentsException("Must specify id, driver, and url!");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        ElementTag id = scriptEntry.getElement("id");
        ElementTag driver = scriptEntry.getElement("driver");
        ElementTag url = scriptEntry.getElement("url");
        ElementTag user = scriptEntry.getElement("user");
        ElementTag pass = scriptEntry.getElement("pass");

        String u = user != null ? user.asString() : "";
        String p = pass != null ? pass.asString() : "";

        boolean success = Datenizen.getInstance().getDatabaseManager().connect(id.asString(), driver.asString(), url.asString(), u, p);

        if (success) {
            DbConnectedEvent.instance.fireFor(id.asString());
        }
    }
}
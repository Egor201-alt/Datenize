package com.egor201.datenizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.datenizen.Datenizen;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class DbExecuteCommand extends AbstractCommand {

    // <--[command]
    // @Name db_execute
    // @Syntax db_execute [id:<id>] [sql:<query>] (args:<list>)
    // @Required 2
    // @Maximum 3
    // @Short Executes an async SQL update, insert, or delete query.
    // @Group Datenizen
    //
    // @Description
    // Executes a query asynchronously via HikariCP. Use the 'args' argument to pass a list of values for prepared statements.
    // This is the safest way to execute queries without risking SQL injection.
    //
    // @Usage
    // Use to update a player's money securely.
    // - db_execute id:main sql:"UPDATE players SET money = ? WHERE uuid = ?" args:<list[100|<player.uuid>]>
    // -->

    public DbExecuteCommand() {
        setName("db_execute");
        setSyntax("db_execute [id:<id>] [sql:<query>] (args:<list>)");
        setRequiredArguments(2, 3);
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
        if (!scriptEntry.hasObject("id") || !scriptEntry.hasObject("sql")) {
            throw new InvalidArgumentsException("Must specify id and sql!");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        ElementTag id = scriptEntry.getElement("id");
        ElementTag sql = scriptEntry.getElement("sql");
        ListTag args = scriptEntry.getObjectTag("args");

        Bukkit.getScheduler().runTaskAsynchronously(Datenizen.getInstance(), () -> {
            try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id.asString());
                 PreparedStatement ps = conn.prepareStatement(sql.asString())) {

                if (args != null) {
                    for (int i = 0; i < args.size(); i++) {
                        ps.setObject(i + 1, args.get(i));
                    }
                }
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
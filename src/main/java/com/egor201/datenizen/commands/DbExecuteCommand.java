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

public class DbExecuteCommand extends AbstractCommand {

    // <--[command]
    // @Name db_execute
    // @Syntax db_execute [id:<id>] [sql:<query>] (args:<list>) (tx:<tx_id>) (label:<label>)
    // @Required 2
    // @Maximum 5
    // @Short Executes an async SQL update, insert, or delete query.
    // @Group Datenizen
    //
    // @Description
    // Executes a query asynchronously.
    // Use 'tx' to execute within a specific transaction started by db_transaction.
    // Use 'label' to identify the operation in the 'db executed' event via <context.label>.
    //
    // @Usage
    // Use to insert a player record and react to completion.
    // - db_execute id:main sql:"INSERT INTO players (name) VALUES (?)" args:<[player_name]> label:player_insert
    // - on db executed label:player_insert:
    //   - narrate "Inserted <context.affected_rows> row(s)."
    // -->

    public DbExecuteCommand() {
        setName("db_execute");
        setSyntax("db_execute [id:<id>] [sql:<query>] (args:<list>) (tx:<tx_id>) (label:<label>)");
        setRequiredArguments(2, 5);
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
            } else if (!scriptEntry.hasObject("tx") && arg.matchesPrefix("tx")) {
                scriptEntry.addObject("tx", arg.asElement());
            } else if (!scriptEntry.hasObject("label") && arg.matchesPrefix("label")) {
                scriptEntry.addObject("label", arg.asElement());
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
        String id = scriptEntry.getElement("id").asString();
        String sql = scriptEntry.getElement("sql").asString();
        ListTag args = scriptEntry.getObjectTag("args");
        ElementTag txTag = scriptEntry.getElement("tx");
        ElementTag labelTag = scriptEntry.getElement("label");
        String txId = txTag != null ? txTag.asString() : null;
        String label = labelTag != null ? labelTag.asString() : null;

        Bukkit.getScheduler().runTaskAsynchronously(Datenizen.getInstance(), () -> {
            Connection conn = null;
            PreparedStatement ps = null;
            try {
                conn = txId != null
                        ? Datenizen.getInstance().getDatabaseManager().getTransactionConnection(txId)
                        : Datenizen.getInstance().getDatabaseManager().getConnection(id);

                if (conn == null) throw new Exception("Connection not found!");

                ps = conn.prepareStatement(sql);
                if (args != null) {
                    for (int i = 0; i < args.size(); i++) {
                        ps.setObject(i + 1, args.get(i));
                    }
                }
                int affected = ps.executeUpdate();
                final int finalAffected = affected;
                Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                    DbExecutedEvent.instance.fireFor(id, label, finalAffected)
                );
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                    DbErrorEvent.instance.fireFor(id, e.getMessage(), sql)
                );
            } finally {
                try { if (ps != null) ps.close(); } catch (Exception ignored) {}
                try { if (conn != null && txId == null) conn.close(); } catch (Exception ignored) {}
            }
        });
    }
}
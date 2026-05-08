package com.egor201.datenizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.datenizen.Datenizen;
import com.egor201.datenizen.events.DbErrorEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class DbExecuteSyncCommand extends AbstractCommand {

    // <--[command]
    // @Name db_execute_sync
    // @Syntax db_execute_sync [id:<id>] [sql:<query>] (args:<list>) (tx:<tx_id>)
    // @Required 2
    // @Maximum 4
    // @Short Executes a synchronous SQL query.
    // @Group Datenizen
    //
    // @Description
    // Executes a query on the main thread. Useful for server shutdown or player quit events.
    // -->

    public DbExecuteSyncCommand() {
        setName("db_execute_sync");
        setSyntax("db_execute_sync [id:<id>] [sql:<query>] (args:<list>) (tx:<tx_id>)");
        setRequiredArguments(2, 4);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        StringBuilder sqlBuilder = new StringBuilder();
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("id") && arg.matchesPrefix("id")) {
                scriptEntry.addObject("id", arg.asElement());
            } else if (!scriptEntry.hasObject("args") && arg.matchesPrefix("args")) {
                scriptEntry.addObject("args", arg.asType(ListTag.class));
            } else if (!scriptEntry.hasObject("tx") && arg.matchesPrefix("tx")) {
                scriptEntry.addObject("tx", arg.asElement());
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
        String txId = txTag != null ? txTag.asString() : null;

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = txId != null ? Datenizen.getInstance().getDatabaseManager().getTransactionConnection(txId) 
                                : Datenizen.getInstance().getDatabaseManager().getConnection(id);
            
            if (conn == null) throw new Exception("Connection not found!");

            ps = conn.prepareStatement(sql);
            if (args != null) {
                for (int i = 0; i < args.size(); i++) {
                    ps.setObject(i + 1, args.get(i));
                }
            }
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            DbErrorEvent.instance.fireFor(id, e.getMessage(), sql);
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
            try { if (conn != null && txId == null) conn.close(); } catch (Exception ignored) {}
        }
    }
}
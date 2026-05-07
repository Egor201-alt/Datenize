package com.egor201.datenizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.datenizen.Datenizen;

public class DbTransactionCommand extends AbstractCommand {

    // <--[command]
    // @Name db_transaction
    // @Syntax db_transaction[id:<id>] [action:start/commit/rollback] [tx:<tx_id>]
    // @Required 3
    // @Maximum 3
    // @Short Manages SQL transactions.
    // @Group Datenizen
    //
    // @Description
    // Starts, commits, or rolls back a transaction.
    // -->

    public DbTransactionCommand() {
        setName("db_transaction");
        setSyntax("db_transaction [id:<id>][action:start/commit/rollback] [tx:<tx_id>]");
        setRequiredArguments(3, 3);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("id") && arg.matchesPrefix("id")) {
                scriptEntry.addObject("id", arg.asElement());
            } else if (!scriptEntry.hasObject("action") && arg.matchesPrefix("action")) {
                scriptEntry.addObject("action", arg.asElement());
            } else if (!scriptEntry.hasObject("tx") && arg.matchesPrefix("tx")) {
                scriptEntry.addObject("tx", arg.asElement());
            } else {
                arg.reportUnhandled();
            }
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        String id = scriptEntry.getElement("id").asString();
        String action = scriptEntry.getElement("action").asString().toLowerCase();
        String txId = scriptEntry.getElement("tx").asString();

        try {
            switch (action) {
                case "start" -> Datenizen.getInstance().getDatabaseManager().startTransaction(txId, id);
                case "commit" -> Datenizen.getInstance().getDatabaseManager().commitTransaction(txId);
                case "rollback" -> Datenizen.getInstance().getDatabaseManager().rollbackTransaction(txId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
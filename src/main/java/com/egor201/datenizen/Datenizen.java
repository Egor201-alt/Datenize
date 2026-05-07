package com.egor201.datenizen;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.egor201.datenizen.commands.*;
import com.egor201.datenizen.database.DatabaseManager;
import com.egor201.datenizen.events.DbConnectedEvent;
import com.egor201.datenizen.events.DbErrorEvent;
import com.egor201.datenizen.events.DbConnectionLeakedEvent;
import com.egor201.datenizen.tags.DatenizenTags;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Datenizen extends JavaPlugin {

    private static Datenizen instance;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;

        if (Bukkit.getPluginManager().getPlugin("Denizen") == null) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        databaseManager = new DatabaseManager();

        DenizenCore.commandRegistry.registerCommand(DbConnectCommand.class);
        DenizenCore.commandRegistry.registerCommand(DbExecuteCommand.class);
        DenizenCore.commandRegistry.registerCommand(DbExecuteSyncCommand.class);
        DenizenCore.commandRegistry.registerCommand(DbTransactionCommand.class);
        DenizenCore.commandRegistry.registerCommand(DbBackupCommand.class);
        DenizenCore.commandRegistry.registerCommand(DbImportCsvCommand.class);
        DenizenCore.commandRegistry.registerCommand(DbExecuteAsyncListCommand.class);
        DenizenCore.commandRegistry.registerCommand(DbExecuteBatchCommand.class);
        DenizenCore.commandRegistry.registerCommand(DbSetPoolSizeCommand.class);
        DenizenCore.commandRegistry.registerCommand(DbDropTableCommand.class);

        ScriptEvent.registerScriptEvent(new DbConnectedEvent());
        ScriptEvent.registerScriptEvent(new DbErrorEvent());
        ScriptEvent.registerScriptEvent(new DbConnectionLeakedEvent());

        DatenizenTags.register();
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeAllConnections();
        }
    }

    public static Datenizen getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
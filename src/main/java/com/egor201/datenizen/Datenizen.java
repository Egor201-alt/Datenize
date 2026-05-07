package com.egor201.datenizen;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.egor201.datenizen.commands.DbConnectCommand;
import com.egor201.datenizen.commands.DbExecuteCommand;
import com.egor201.datenizen.database.DatabaseManager;
import com.egor201.datenizen.events.DbConnectedEvent;
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

        saveDefaultConfig();
        databaseManager = new DatabaseManager();

        DenizenCore.commandRegistry.registerCommand(DbConnectCommand.class);
        DenizenCore.commandRegistry.registerCommand(DbExecuteCommand.class);

        ScriptEvent.registerScriptEvent(new DbConnectedEvent());

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
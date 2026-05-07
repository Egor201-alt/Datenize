package com.egor201.datenizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.datenizen.Datenizen;
import com.egor201.datenizen.events.DbErrorEvent;
import org.bukkit.Bukkit;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Statement;

public class DbExecuteScriptCommand extends AbstractCommand {
    // <--[command]
    // @Name db_execute_script
    // @Syntax db_execute_script [id:<id>] [path:<path>]
    // @Required 2
    // @Maximum 2
    // @Short Executes a full SQL script from a file.
    // @Group Datenizen
    // -->
    public DbExecuteScriptCommand() { setName("db_execute_script"); setSyntax("db_execute_script [id:<id>] [path:<path>]"); setRequiredArguments(2, 2); }
    @Override public void parseArgs(ScriptEntry se) throws InvalidArgumentsException {
        for (Argument arg : se) {
            if (!se.hasObject("id") && arg.matchesPrefix("id")) se.addObject("id", arg.asElement());
            else if (!se.hasObject("path") && arg.matchesPrefix("path")) se.addObject("path", arg.asElement());
        }
    }
    @Override public void execute(ScriptEntry se) {
        String id = se.getElement("id").asString();
        String path = se.getElement("path").asString();
        File f = new File(path);
        if (!f.exists()) return;
        Bukkit.getScheduler().runTaskAsynchronously(Datenizen.getInstance(), () -> {
            try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id);
                 Statement st = conn.createStatement()) {
                String content = new String(Files.readAllBytes(f.toPath()));
                String[] queries = content.split(";");
                for (String q : queries) if (!q.trim().isEmpty()) st.addBatch(q);
                st.executeBatch();
            } catch (Exception e) { Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> DbErrorEvent.instance.fireFor(id, e.getMessage(), "SCRIPT EXECUTION")); }
        });
    }
}
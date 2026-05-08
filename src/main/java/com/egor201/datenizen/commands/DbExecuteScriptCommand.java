package com.egor201.datenizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.datenizen.Datenizen;
import com.egor201.datenizen.events.DbErrorEvent;
import org.bukkit.Bukkit;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class DbExecuteScriptCommand extends AbstractCommand {

    // <--[command]
    // @Name db_execute_script
    // @Syntax db_execute_script [id:<id>] [path:<path>]
    // @Required 2
    // @Maximum 2
    // @Short Executes a full SQL script from a file.
    // @Group Datenizen
    //
    // @Description
    // Reads a .sql file and executes each statement separated by semicolons asynchronously.
    // All statements run inside a transaction and are rolled back on failure.
    // -->

    public DbExecuteScriptCommand() {
        setName("db_execute_script");
        setSyntax("db_execute_script [id:<id>] [path:<path>]");
        setRequiredArguments(2, 2);
    }

    @Override
    public void parseArgs(ScriptEntry se) throws InvalidArgumentsException {
        for (Argument arg : se) {
            if (!se.hasObject("id") && arg.matchesPrefix("id")) {
                se.addObject("id", arg.asElement());
            } else if (!se.hasObject("path") && arg.matchesPrefix("path")) {
                se.addObject("path", arg.asElement());
            } else {
                arg.reportUnhandled();
            }
        }
        if (!se.hasObject("id") || !se.hasObject("path")) {
            throw new InvalidArgumentsException("Must specify id and path!");
        }
    }

    @Override
    public void execute(ScriptEntry se) {
        String id = se.getElement("id").asString();
        String path = se.getElement("path").asString();

        File f = new File(path);
        if (!f.exists()) return;

        Bukkit.getScheduler().runTaskAsynchronously(Datenizen.getInstance(), () -> {
            Connection conn = null;
            try {
                String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                String[] queries = content.split(";");

                conn = Datenizen.getInstance().getDatabaseManager().getConnection(id);
                conn.setAutoCommit(false);

                for (String q : queries) {
                    String trimmed = q.trim();
                    if (trimmed.isEmpty()) continue;
                    try (PreparedStatement ps = conn.prepareStatement(trimmed)) {
                        ps.executeUpdate();
                    }
                }

                conn.commit();
            } catch (Exception e) {
                e.printStackTrace();
                if (conn != null) {
                    try { conn.rollback(); } catch (Exception ignored) {}
                }
                Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                    DbErrorEvent.instance.fireFor(id, e.getMessage(), "SCRIPT EXECUTION")
                );
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (Exception ignored) {}
                }
            }
        });
    }
}
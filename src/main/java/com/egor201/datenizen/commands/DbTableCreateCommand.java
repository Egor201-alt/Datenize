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
import java.sql.Statement;
import java.util.regex.Pattern;

public class DbTableCreateCommand extends AbstractCommand {

    // <--[command]
    // @Name db_table_create
    // @Syntax db_table_create [id:<id>] [table:<table>] [columns:<list>] (if_not_exists:true/false)
    // @Required 3
    // @Maximum 4
    // @Short Creates a table without writing SQL manually.
    // @Group Datenizen
    //
    // @Description
    // Builds and executes a CREATE TABLE statement asynchronously.
    // Each entry in 'columns' is a full column definition, e.g. "id INTEGER PRIMARY KEY".
    // Table name must be alphanumeric/underscores only.
    // By default uses IF NOT EXISTS (safe to call on startup).
    // Fires 'db executed' on success with label 'db_table_create'.
    //
    // @Usage
    // - db_table_create id:main table:players columns:<list[id INTEGER PRIMARY KEY AUTOINCREMENT|name TEXT NOT NULL|coins INTEGER DEFAULT 0]>
    //
    // @Usage
    // - db_table_create id:main table:logs columns:<list[id INTEGER PRIMARY KEY|message TEXT|timestamp INTEGER]> if_not_exists:false
    // -->

    private static final Pattern SAFE_NAME = Pattern.compile("^[a-zA-Z0-9_]+$");

    public DbTableCreateCommand() {
        setName("db_table_create");
        setSyntax("db_table_create [id:<id>] [table:<table>] [columns:<list>] (if_not_exists:true/false)");
        setRequiredArguments(3, 4);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("id") && arg.matchesPrefix("id")) {
                scriptEntry.addObject("id", arg.asElement());
            } else if (!scriptEntry.hasObject("table") && arg.matchesPrefix("table")) {
                scriptEntry.addObject("table", arg.asElement());
            } else if (!scriptEntry.hasObject("columns") && arg.matchesPrefix("columns")) {
                scriptEntry.addObject("columns", arg.asType(ListTag.class));
            } else if (!scriptEntry.hasObject("if_not_exists") && arg.matchesPrefix("if_not_exists")) {
                scriptEntry.addObject("if_not_exists", arg.asElement());
            } else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("id") || !scriptEntry.hasObject("table") || !scriptEntry.hasObject("columns")) {
            throw new InvalidArgumentsException("Must specify id, table, and columns!");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        String id = scriptEntry.getElement("id").asString();
        String table = scriptEntry.getElement("table").asString();
        ListTag columns = scriptEntry.getObjectTag("columns");
        ElementTag ifNotExistsTag = scriptEntry.getElement("if_not_exists");
        boolean ifNotExists = ifNotExistsTag == null || ifNotExistsTag.asBoolean();

        if (!SAFE_NAME.matcher(table).matches()) {
            Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                DbErrorEvent.instance.fireFor(id, "Invalid table name: " + table, "db_table_create")
            );
            return;
        }

        StringBuilder colDefs = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) colDefs.append(", ");
            colDefs.append(columns.get(i));
        }

        String keyword = ifNotExists ? "CREATE TABLE IF NOT EXISTS " : "CREATE TABLE ";
        String sql = keyword + table + " (" + colDefs + ")";

        Bukkit.getScheduler().runTaskAsynchronously(Datenizen.getInstance(), () -> {
            try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id);
                 Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
                Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                    DbExecutedEvent.instance.fireFor(id, "db_table_create", 0)
                );
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                    DbErrorEvent.instance.fireFor(id, e.getMessage(), sql)
                );
            }
        });
    }
}
package com.egor201.datenizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.datenizen.Datenizen;
import com.egor201.datenizen.events.DbCsvImportedEvent;
import com.egor201.datenizen.events.DbErrorEvent;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class DbImportCsvCommand extends AbstractCommand {

    // <--[command]
    // @Name db_import_csv
    // @Syntax db_import_csv [id:<id>] [table:<table>] [path:<path>]
    // @Required 3
    // @Maximum 3
    // @Short Imports a CSV file into a database table.
    // @Group Datenizen
    //
    // @Description
    // Reads a basic comma-separated CSV file asynchronously and inserts rows into the specified table.
    // -->

    public DbImportCsvCommand() {
        setName("db_import_csv");
        setSyntax("db_import_csv[id:<id>] [table:<table>] [path:<path>]");
        setRequiredArguments(3, 3);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("id") && arg.matchesPrefix("id")) {
                scriptEntry.addObject("id", arg.asElement());
            } else if (!scriptEntry.hasObject("table") && arg.matchesPrefix("table")) {
                scriptEntry.addObject("table", arg.asElement());
            } else if (!scriptEntry.hasObject("path") && arg.matchesPrefix("path")) {
                scriptEntry.addObject("path", arg.asElement());
            } else {
                arg.reportUnhandled();
            }
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        String id = scriptEntry.getElement("id").asString();
        String table = scriptEntry.getElement("table").asString();
        String path = scriptEntry.getElement("path").asString();

        File file = new File(path);
        if (!file.exists()) return;

        Bukkit.getScheduler().runTaskAsynchronously(Datenizen.getInstance(), () -> {
            try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id);
                 BufferedReader reader = new BufferedReader(new FileReader(file))) {

                String headerLine = reader.readLine();
                if (headerLine == null) return;
                
                String[] headers = headerLine.split(",");
                StringBuilder placeholders = new StringBuilder();
                for (int i = 0; i < headers.length; i++) {
                    placeholders.append("?");
                    if (i < headers.length - 1) placeholders.append(",");
                }

                String sql = "INSERT INTO " + table + " VALUES (" + placeholders.toString() + ")";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    String line;
                    int rowsCount = 0;
                    
                    while ((line = reader.readLine()) != null) {
                        String[] values = line.split(",");
                        for (int i = 0; i < values.length; i++) {
                            ps.setObject(i + 1, values[i].trim());
                        }
                        ps.addBatch();
                        rowsCount++;
                    }
                    
                    ps.executeBatch();
                    
                    final int finalRowsCount = rowsCount;
                    Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> 
                        DbCsvImportedEvent.instance.fireFor(id, table, finalRowsCount)
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> 
                    DbErrorEvent.instance.fireFor(id, e.getMessage(), "CSV IMPORT " + table)
                );
            }
        });
    }
}
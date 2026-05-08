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
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

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
    // Reads a CSV file asynchronously and inserts rows into the specified table.
    // Supports quoted fields containing commas and escaped quotes.
    // -->

    public DbImportCsvCommand() {
        setName("db_import_csv");
        setSyntax("db_import_csv [id:<id>] [table:<table>] [path:<path>]");
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
        if (!scriptEntry.hasObject("id") || !scriptEntry.hasObject("table") || !scriptEntry.hasObject("path")) {
            throw new InvalidArgumentsException("Must specify id, table, and path!");
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString().trim());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString().trim());
        return fields;
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
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

                String headerLine = reader.readLine();
                if (headerLine == null) return;

                List<String> headers = parseCsvLine(headerLine);
                int columnCount = headers.size();

                StringBuilder placeholders = new StringBuilder();
                for (int i = 0; i < columnCount; i++) {
                    placeholders.append("?");
                    if (i < columnCount - 1) placeholders.append(",");
                }

                String sql = "INSERT INTO " + table + " VALUES (" + placeholders + ")";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    String line;
                    int rowsCount = 0;

                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) continue;
                        List<String> values = parseCsvLine(line);
                        for (int i = 0; i < Math.min(values.size(), columnCount); i++) {
                            ps.setObject(i + 1, values.get(i));
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
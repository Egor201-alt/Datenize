package com.egor201.datenizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.datenizen.Datenizen;
import com.egor201.datenizen.events.DbCsvExportedEvent;
import com.egor201.datenizen.events.DbErrorEvent;
import org.bukkit.Bukkit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class DbExportCsvCommand extends AbstractCommand {

    // <--[command]
    // @Name db_export_csv
    // @Syntax db_export_csv [id:<id>] [sql:<query>][path:<path>] (args:<list>)
    // @Required 3
    // @Maximum 4
    // @Short Exports a query result to a CSV file.
    // @Group Datenizen
    //
    // @Description
    // Runs a SELECT query asynchronously and writes the result to a CSV file.
    // Values containing commas, quotes, or newlines are properly escaped per RFC 4180.
    // -->

    public DbExportCsvCommand() {
        setName("db_export_csv");
        setSyntax("db_export_csv [id:<id>] [sql:<query>] [path:<path>] (args:<list>)");
        setRequiredArguments(3, 4);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        StringBuilder sqlBuilder = new StringBuilder();
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("id") && arg.matchesPrefix("id")) {
                scriptEntry.addObject("id", arg.asElement());
            } else if (!scriptEntry.hasObject("path") && arg.matchesPrefix("path")) {
                scriptEntry.addObject("path", arg.asElement());
            } else if (!scriptEntry.hasObject("args") && arg.matchesPrefix("args")) {
                scriptEntry.addObject("args", arg.asType(ListTag.class));
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
        if (!scriptEntry.hasObject("id") || !scriptEntry.hasObject("sql") || !scriptEntry.hasObject("path")) {
            throw new InvalidArgumentsException("Must specify id, sql, and path!");
        }
    }

    private String escapeCsvField(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Override
    public void execute(ScriptEntry se) {
        String id = se.getElement("id").asString();
        String sql = se.getElement("sql").asString();
        String path = se.getElement("path").asString();
        ListTag args = se.getObjectTag("args");

        Bukkit.getScheduler().runTaskAsynchronously(Datenizen.getInstance(), () -> {
            File outputFile = new File(path);
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }

            try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                if (args != null) {
                    for (int i = 0; i < args.size(); i++) ps.setObject(i + 1, args.get(i));
                }

                try (ResultSet rs = ps.executeQuery();
                     BufferedWriter writer = new BufferedWriter(
                             new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {

                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();

                    for (int i = 1; i <= cols; i++) {
                        writer.write(escapeCsvField(meta.getColumnName(i)));
                        if (i < cols) writer.write(",");
                    }
                    writer.newLine();

                    while (rs.next()) {
                        for (int i = 1; i <= cols; i++) {
                            writer.write(escapeCsvField(rs.getString(i)));
                            if (i < cols) writer.write(",");
                        }
                        writer.newLine();
                    }

                    Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                        DbCsvExportedEvent.instance.fireFor(id, path)
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                    DbErrorEvent.instance.fireFor(id, e.getMessage(), sql)
                );
            }
        });
    }
}
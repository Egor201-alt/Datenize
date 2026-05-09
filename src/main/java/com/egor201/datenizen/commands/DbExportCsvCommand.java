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
    // @Syntax db_export_csv [id:<id>] [sql:<query>] [path:<path>] (args:<list>)
    // @Required 3
    // @Maximum 4
    // @Short Exports a query result to a CSV file.
    // @Group Datenizen
    //
    // @Description
    // Runs a SELECT query asynchronously and writes the result to a CSV file.
    // Values containing commas, quotes, or newlines are properly escaped per RFC 4180.
    // The sql argument supports both prefixed and quoted forms:
    //   sql:SELECT * FROM players
    //   "sql:SELECT * FROM players WHERE active = ?"
    // -->

    public DbExportCsvCommand() {
        setName("db_export_csv");
        setSyntax("db_export_csv [id:<id>] [sql:<query>] [path:<path>] (args:<list>)");
        setRequiredArguments(3, 4);
    }

    @Override
    public void parseArgs(ScriptEntry se) throws InvalidArgumentsException {
        for (Argument arg : se) {
            if (!se.hasObject("id") && arg.matchesPrefix("id")) {
                se.addObject("id", arg.asElement());
            } else if (!se.hasObject("sql") && arg.matchesPrefix("sql")) {
                se.addObject("sql", arg.asElement());
            } else if (!se.hasObject("sql") && !arg.hasPrefix()
                    && arg.getValue().startsWith("sql:")) {
                se.addObject("sql", new ElementTag(arg.getValue().substring(4)));
            } else if (!se.hasObject("path") && arg.matchesPrefix("path")) {
                se.addObject("path", arg.asElement());
            } else if (!se.hasObject("args") && arg.matchesPrefix("args")) {
                se.addObject("args", arg.asType(ListTag.class));
            } else {
                arg.reportUnhandled();
            }
        }
        if (!se.hasObject("id") || !se.hasObject("sql") || !se.hasObject("path")) {
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
            } catch (java.sql.SQLException e) {
                Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                    DbErrorEvent.instance.fireFor(id, e.getMessage(), e.getSQLState(), sql)
                );
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(Datenizen.getInstance(), () ->
                    DbErrorEvent.instance.fireFor(id, e.getMessage(), null, sql)
                );
            }
        });
    }
}
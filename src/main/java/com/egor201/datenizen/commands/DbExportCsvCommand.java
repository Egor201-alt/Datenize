package com.egor201.datenizen.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.egor201.datenizen.Datenizen;
import com.egor201.datenizen.events.DbCsvExportedEvent;
import com.egor201.datenizen.events.DbErrorEvent;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class DbExportCsvCommand extends AbstractCommand {
    // <--[command]
    // @Name db_export_csv
    // @Syntax db_export_csv [id:<id>][sql:<query>] [path:<path>] (args:<list>)
    // @Required 3
    // @Maximum 4
    // @Short Exports a query result to a CSV file.
    // @Group Datenizen
    // -->
    public DbExportCsvCommand() { setName("db_export_csv"); setSyntax("db_export_csv [id:<id>] [sql:<query>] [path:<path>] (args:<list>)"); setRequiredArguments(3, 4); }
    @Override public void parseArgs(ScriptEntry se) throws InvalidArgumentsException {
        for (Argument arg : se) {
            if (!se.hasObject("id") && arg.matchesPrefix("id")) se.addObject("id", arg.asElement());
            else if (!se.hasObject("sql") && arg.matchesPrefix("sql")) se.addObject("sql", arg.asElement());
            else if (!se.hasObject("path") && arg.matchesPrefix("path")) se.addObject("path", arg.asElement());
            else if (!se.hasObject("args") && arg.matchesPrefix("args")) se.addObject("args", arg.asType(ListTag.class));
        }
    }
    @Override public void execute(ScriptEntry se) {
        String id = se.getElement("id").asString();
        String sql = se.getElement("sql").asString();
        String path = se.getElement("path").asString();
        ListTag args = se.getObjectTag("args");

        Bukkit.getScheduler().runTaskAsynchronously(Datenizen.getInstance(), () -> {
            try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id);
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                if (args != null) for (int i = 0; i < args.size(); i++) ps.setObject(i + 1, args.get(i));
                try (ResultSet rs = ps.executeQuery(); FileWriter writer = new FileWriter(new File(path))) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    for (int i = 1; i <= cols; i++) writer.append(meta.getColumnName(i)).append(i == cols ? "" : ",");
                    writer.append("\n");
                    while (rs.next()) {
                        for (int i = 1; i <= cols; i++) {
                            String val = rs.getString(i);
                            if (val != null) {
                                if (val.contains(",")) val = "\"" + val + "\"";
                                writer.append(val);
                            }
                            writer.append(i == cols ? "" : ",");
                        }
                        writer.append("\n");
                    }
                    Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> DbCsvExportedEvent.instance.fireFor(id, path));
                }
            } catch (Exception e) { Bukkit.getScheduler().runTask(Datenizen.getInstance(), () -> DbErrorEvent.instance.fireFor(id, e.getMessage(), sql)); }
        });
    }
}
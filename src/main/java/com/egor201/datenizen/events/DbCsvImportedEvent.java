package com.egor201.datenizen.events;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;

public class DbCsvImportedEvent extends ScriptEvent {
    // <--[event]
    // @Events
    // db csv imported
    // @Group Datenizen
    // @Context
    // <context.id> returns the database ID.
    // <context.table> returns the table name.
    // <context.rows> returns the number of rows imported.
    // -->
    public static DbCsvImportedEvent instance;
    private ElementTag id, table, rows;

    public DbCsvImportedEvent() { instance = this; registerCouldMatcher("db csv imported"); }
    @Override public ObjectTag getContext(String name) { return name.equals("id") ? id : (name.equals("table") ? table : (name.equals("rows") ? rows : super.getContext(name))); }
    public void fireFor(String id, String table, int rows) { this.id = new ElementTag(id); this.table = new ElementTag(table); this.rows = new ElementTag(rows); fire(); }
}
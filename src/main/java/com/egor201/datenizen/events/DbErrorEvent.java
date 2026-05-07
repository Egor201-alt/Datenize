package com.egor201.datenizen.events;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;

public class DbErrorEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // db error
    //
    // @Group Datenizen
    //
    // @Cancellable false
    //
    // @Triggers when a database SQL exception occurs.
    //
    // @Context
    // <context.id> returns the database ID.
    // <context.error> returns the error message.
    // <context.query> returns the SQL query that caused the error.
    //
    // -->

    public static DbErrorEvent instance;
    private ElementTag id;
    private ElementTag error;
    private ElementTag query;

    public DbErrorEvent() {
        instance = this;
        registerCouldMatcher("db error");
    }

    @Override
    public boolean matches(ScriptPath path) {
        return super.matches(path);
    }

    @Override
    public ObjectTag getContext(String name) {
        return switch (name) {
            case "id" -> id;
            case "error" -> error;
            case "query" -> query;
            default -> super.getContext(name);
        };
    }

    public void fireFor(String id, String error, String query) {
        this.id = new ElementTag(id);
        this.error = new ElementTag(error);
        this.query = new ElementTag(query);
        fire();
    }
}
package com.egor201.datenizen.events;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;

public class DbConnectedEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // db connected
    //
    // @Group Datenizen
    //
    // @Cancellable false
    //
    // @Switch id:<id> to only fire for a specific database id.
    //
    // @Triggers when a database connection pool is successfully initialized.
    //
    // @Context
    // <context.id> returns the ElementTag of the connected database ID.
    //
    // -->

    public static DbConnectedEvent instance;
    private ElementTag id;

    public DbConnectedEvent() {
        instance = this;
        registerCouldMatcher("db connected");
        registerSwitches("id");
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!runGenericSwitchCheck(path, "id", id.asString())) return false;
        return super.matches(path);
    }

    @Override
    public ObjectTag getContext(String name) {
        return switch (name) {
            case "id" -> id;
            default -> super.getContext(name);
        };
    }

    public void fireFor(String id) {
        this.id = new ElementTag(id);
        fire();
    }
}
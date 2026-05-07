package com.egor201.datenizen.events;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;

public class DbConnectionLeakedEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // db connection leaked
    //
    // @Group Datenizen
    //
    // @Cancellable false
    //
    // @Triggers when a transaction remains uncommitted for more than 5 minutes.
    //
    // @Context
    // <context.tx> returns the leaked transaction ID.
    // <context.duration> returns the time in seconds it was kept open.
    //
    // -->

    public static DbConnectionLeakedEvent instance;
    private ElementTag tx;
    private ElementTag duration;

    public DbConnectionLeakedEvent() {
        instance = this;
        registerCouldMatcher("db connection leaked");
    }

    @Override
    public boolean matches(ScriptPath path) {
        return super.matches(path);
    }

    @Override
    public ObjectTag getContext(String name) {
        return switch (name) {
            case "tx" -> tx;
            case "duration" -> duration;
            default -> super.getContext(name);
        };
    }

    public void fireFor(String txId, long timeSeconds) {
        this.tx = new ElementTag(txId);
        this.duration = new ElementTag(timeSeconds);
        fire();
    }
}
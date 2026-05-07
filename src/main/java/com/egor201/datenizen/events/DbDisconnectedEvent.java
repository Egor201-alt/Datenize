package com.egor201.datenizen.events;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;

public class DbDisconnectedEvent extends ScriptEvent {
    // <--[event]
    // @Events
    // db disconnected
    // @Group Datenizen
    // @Context
    // <context.id> returns the disconnected database ID.
    // -->
    public static DbDisconnectedEvent instance;
    private ElementTag id;

    public DbDisconnectedEvent() { instance = this; registerCouldMatcher("db disconnected"); }
    @Override public ObjectTag getContext(String name) { return name.equals("id") ? id : super.getContext(name); }
    public void fireFor(String id) { this.id = new ElementTag(id); fire(); }
}
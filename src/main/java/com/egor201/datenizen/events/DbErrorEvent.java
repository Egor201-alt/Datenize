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
    // @Switch id:<id> to only fire for a specific database id.
    // @Switch category:<category> to only fire for a specific error category.
    //   Valid categories: constraint, syntax, connection, data, permission, timeout, unknown
    //
    // @Triggers when a database SQL exception occurs.
    //
    // @Context
    // <context.id> returns the database ID.
    // <context.error> returns the full error message from the driver.
    // <context.query> returns the SQL query that caused the error, or empty if not applicable.
    // <context.sql_state> returns the 5-character SQL state code (e.g. 23000 for constraint violation).
    //   Returns empty if not a SQL exception (e.g. connection error).
    // <context.category> returns a simple category string based on the SQL state:
    //   constraint  - duplicate key, unique violation, foreign key (23xxx)
    //   syntax      - SQL syntax error (42xxx)
    //   connection  - network or pool error (08xxx, or no sql_state)
    //   data        - data type or truncation error (22xxx)
    //   permission  - access denied (28xxx, 42501)
    //   timeout     - lock wait or query timeout (HYT00, HYT01, 40001)
    //   unknown     - anything else
    //
    // @Usage
    // Catch any error for main database.
    // - on db error id:main:
    //   - narrate "DB error: <context.error>"
    //
    // @Usage
    // Catch only duplicate key / unique constraint violations.
    // - on db error id:main category:constraint:
    //   - narrate "That record already exists!"
    //
    // @Usage
    // Log with full detail.
    // - on db error:
    //   - narrate "[<context.id>] <context.category> (<context.sql_state>): <context.error>"
    //   - narrate "Query: <context.query>"
    // -->

    public static DbErrorEvent instance;
    private ElementTag id;
    private ElementTag error;
    private ElementTag query;
    private ElementTag sqlState;
    private ElementTag category;

    public DbErrorEvent() {
        instance = this;
        registerCouldMatcher("db error");
        registerSwitches("id", "category");
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!runGenericSwitchCheck(path, "id", id.asString())) return false;
        if (!runGenericSwitchCheck(path, "category", category.asString())) return false;
        return super.matches(path);
    }

    @Override
    public ObjectTag getContext(String name) {
        return switch (name) {
            case "id"        -> id;
            case "error"     -> error;
            case "query"     -> query;
            case "sql_state" -> sqlState;
            case "category"  -> category;
            default          -> super.getContext(name);
        };
    }

    private static String resolveCategory(String state) {
        if (state == null || state.isEmpty()) return "connection";
        return switch (state.substring(0, Math.min(2, state.length()))) {
            case "23" -> "constraint";
            case "42" -> state.equals("42501") ? "permission" : "syntax";
            case "08" -> "connection";
            case "22" -> "data";
            case "28" -> "permission";
            case "40" -> "timeout";
            default   -> {
                if (state.startsWith("HYT")) yield "timeout";
                yield "unknown";
            }
        };
    }

    public void fireFor(String id, String error, String sqlStateCode, String query) {
        this.id       = new ElementTag(id != null ? id : "");
        this.error    = new ElementTag(error != null ? error : "");
        this.query    = new ElementTag(query != null ? query : "");
        this.sqlState = new ElementTag(sqlStateCode != null ? sqlStateCode : "");
        this.category = new ElementTag(resolveCategory(sqlStateCode));
        fire();
    }

    public void fireFor(String id, String error, String query) {
        fireFor(id, error, null, query);
    }
}
package com.egor201.datenizen.tags;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.tags.TagManager;
import com.egor201.datenizen.Datenizen;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class DatenizenTags {

    // <--[tag]
    // @Attribute <db_query[<id>].sql[<query>].args[<list>]>
    // @Returns ListTag
    // @Group Datenizen
    //
    // @Description
    // Queries the database synchronously and returns a ListTag of MapTags representing the rows.
    // Always use arguments via .args[...] to prevent SQL injection.
    //
    // @Usage
    // Use to get a player's data.
    // - define user_data <db_query[main].sql[SELECT * FROM users WHERE uuid = ?].args[<list[<player.uuid>]>]>
    // - narrate "Your money is <[user_data].first.get[money]>"
    // -->

    public static void register() {
        TagManager.registerTagHandler(ListTag.class, "db_query", attribute -> {
            if (!attribute.hasParam()) return null;
            String id = attribute.getParam();

            attribute.fulfill(1);

            if (attribute.startsWith("sql") && attribute.hasParam()) {
                String sql = attribute.getParam();
                attribute.fulfill(1);

                ListTag args = null;
                if (attribute.startsWith("args") && attribute.hasParam()) {
                    args = attribute.contextAsType(1, ListTag.class);
                    attribute.fulfill(1);
                }

                try (Connection conn = Datenizen.getInstance().getDatabaseManager().getConnection(id);
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    if (args != null) {
                        for (int i = 0; i < args.size(); i++) {
                            ps.setObject(i + 1, args.get(i));
                        }
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        ResultSetMetaData meta = rs.getMetaData();
                        int columnCount = meta.getColumnCount();
                        ListTag resultList = new ListTag();

                        while (rs.next()) {
                            MapTag row = new MapTag();
                            for (int i = 1; i <= columnCount; i++) {
                                Object val = rs.getObject(i);
                                row.putObject(meta.getColumnName(i), new ElementTag(val == null ? "null" : val.toString()));
                            }
                            resultList.addObject(row);
                        }
                        return resultList;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            return null;
        });
    }
}
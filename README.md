# Datenizen

A database addon for [Denizen](https://github.com/DenizenScript/Denizen) that provides full SQL database support directly in your scripts. Connect to SQLite, MySQL, MariaDB, or PostgreSQL - execute queries, manage transactions, and react to results through Denizen events.

**Minecraft:** 1.21+  
**Java:** 21+  
**Requires:** [Denizen](https://github.com/DenizenScript/Denizen)

---

## Features

- Async-first - all database operations run off the main thread
- HikariCP connection pooling built-in
- PreparedStatement everywhere - no SQL injection
- CRUD commands that don't require writing SQL
- Full transaction support with automatic expiry
- CSV import/export, batch execution, backup
- Rich error events with SQL state codes and categories

---

## Quick Start

### Connect to a database

```yaml
# SQLite
- db_connect id:local driver:sqlite url:plugins/Datenizen/data.db

# MySQL
- db_connect id:main driver:mysql url:localhost:3306/mydb user:root pass:secret

# PostgreSQL
- db_connect id:pg driver:postgres url:localhost:5432/mydb user:admin pass:secret
```

### Create a table

```yaml
- db_table_create id:local table:players
  columns:<list[id INTEGER PRIMARY KEY AUTOINCREMENT|uuid TEXT UNIQUE|name TEXT|coins INTEGER DEFAULT 0]>
```

### Save player data (insert or update)

```yaml
- db_upsert id:local table:players key_column:uuid key_value:<player.uuid>
  set:<list[name=<player.name>|coins=0]> label:save_player

on db executed label:save_player:
  - narrate "Saved! Rows affected: <context.affected_rows>"
```

### Read data

```yaml
# Single value
- define coins <db_value[local].sql[SELECT coins FROM players WHERE uuid=?].args[<player.uuid>]>

# Full row as a map
- define row <db_query_first[local].sql[SELECT * FROM players WHERE uuid=?].args[<player.uuid>]>
- narrate "Name: <[row].get[name]> | Coins: <[row].get[coins]>"

# All rows
- define all <db_query[local].sql[SELECT * FROM players]>
- foreach <[all]>:
  - narrate "<[value].get[name]>: <[value].get[coins]>"
```

### Update and delete

```yaml
- db_table_update id:local table:players set:<list[coins=100]> where:<list[uuid=<player.uuid>]>

- db_table_delete id:local table:players where:<list[uuid=<player.uuid>]>
```

### Handle errors

```yaml
on db error id:local:
  - narrate "Error: <context.error> (<context.category>)"

# React to specific error types
on db error category:constraint:
  - narrate "That record already exists."

on db error category:connection:
  - db_reconnect id:local
```

### Transactions

```yaml
- db_transaction id:local action:start tx:my_tx
- db_execute id:local sql:"UPDATE players SET coins=coins-50 WHERE uuid=?" args:<list[<player.uuid>]> tx:my_tx
- db_execute id:local sql:"INSERT INTO logs (msg) VALUES (?)" args:<list[purchase]> tx:my_tx
- db_transaction id:local action:commit tx:my_tx
```

---

## Building

Requires **JDK 21** and **Gradle 8.8**.

```bash
git clone https://github.com/Egor201-alt/Datenizen.git
cd Datenizen
./gradlew build
```

The compiled jar will be at `build/libs/Datenizen-<version>.jar`.

> On Windows use `gradlew.bat build` instead.

---

## Installation

1. Build the jar or download it from [Releases](https://github.com/Egor201-alt/Datenizen/releases)
2. Drop it into your server's `plugins/` folder alongside Denizen
3. Restart the server

No configuration needed - connections are managed entirely through scripts.

---

## Documentation

Full command, tag, and event reference: [Datenizen Meta](https://egor201-alt.github.io/Datenizen-Meta)

## License

[MIT](LICENSE)

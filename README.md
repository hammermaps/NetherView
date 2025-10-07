# NetherView
Lets you peek through portals at the nether.

More infos and demonstrations at: [SpigotMc](https://www.spigotmc.org/resources/nether-view.78885/)

## Requirements
- **Server**: PaperMC 1.21.8+ (or compatible version)
- **Java**: Java 17 or higher
- **Dependencies**: ProtocolLib 5.3.0+

## Building
This plugin now uses PaperMC API instead of Spigot. To build:

```bash
mvn clean package
```

The compiled JAR will be in the `target/` directory.

## Migration from Spigot
See [MIGRATION_TO_PAPERMC.md](MIGRATION_TO_PAPERMC.md) for details about the migration from Spigot API to PaperMC API.

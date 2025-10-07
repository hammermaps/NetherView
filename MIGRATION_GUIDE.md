# Migration Guide: Upgrading to NetherView 2.0 (Java 21 / MC 1.21+)

## Overview

NetherView 2.0 removes support for legacy Minecraft versions (1.8-1.12) and requires Java 21 and Minecraft 1.21 or higher. This document explains the changes and how to migrate.

## Breaking Changes

### 1. Minimum Requirements
- **Java**: Now requires Java 21 (previously Java 8)
- **Minecraft**: Now requires 1.21+ (previously supported back to 1.8)
- **API Version**: Updated to 1.21 (previously 1.13)
- **ProtocolLib**: Now requires 5.3.0+ (previously 4.5.0)

### 2. Removed Code
- **LegacyBlockType**: Completely removed. The plugin now only uses modern BlockData API.
- **Legacy version detection**: `isLegacyServer` flag and `loadServerVersion()` method removed
- **Legacy Material handling**: `Material.PORTAL` compatibility removed, now uses `Material.NETHER_PORTAL` only
- **Legacy Metrics reflection**: Removed reflection-based player count that handled MC 1.7-1.8 compatibility

### 3. Configuration Changes
The plugin no longer supports legacy material names. If your configuration uses old material names like:
- `stained_clay` → Use `white_terracotta`, `red_terracotta`, etc.
- `stained_clay:14` (data values) → Use proper material names
- `wool:15` → Use `black_wool`

The plugin will automatically use modern defaults:
- `overworld-border`: `white_terracotta`
- `nether-border`: `red_concrete`
- `end-border`: `black_concrete`

## Migration Steps

### For Server Administrators

1. **Check Java Version**
   ```bash
   java -version
   ```
   Ensure you're running Java 21 or higher. Download from [Adoptium](https://adoptium.net/) if needed.

2. **Check Minecraft Version**
   Your server must be running Minecraft 1.21 or higher (Spigot, Paper, Purpur, etc.)

3. **Update ProtocolLib**
   Download ProtocolLib 5.3.0 or higher from [SpigotMC](https://www.spigotmc.org/resources/protocollib.1997/)

4. **Backup Your Data**
   ```bash
   # Backup your NetherView data
   cp -r plugins/NetherView/ backups/NetherView-old/
   ```

5. **Update Configuration**
   If you customized world border blocks, ensure they use modern material names:
   ```yaml
   # Old (no longer works)
   overworld-border: stained_clay
   nether-border: stained_clay:14
   end-border: wool:15
   
   # New (required)
   overworld-border: white_terracotta
   nether-border: red_concrete
   end-border: black_concrete
   ```

6. **Install New Version**
   Replace the old NetherView.jar with the new version and restart your server.

7. **Test Portal Viewing**
   - Create a test portal
   - Verify you can see through it
   - Check console for any errors

### For Developers

1. **Update Build Configuration**
   ```xml
   <!-- pom.xml -->
   <properties>
       <java.version>21</java.version>
   </properties>
   
   <dependencies>
       <dependency>
           <groupId>io.papermc.paper</groupId>
           <artifactId>paper-api</artifactId>
           <version>1.21.8-R0.1-SNAPSHOT</version>
       </dependency>
       <dependency>
           <groupId>com.comphenix.protocol</groupId>
           <artifactId>ProtocolLib</artifactId>
           <version>5.3.0</version>
       </dependency>
   </dependencies>
   ```

2. **Remove Legacy References**
   - No need to check `isLegacyServer`
   - Always use `Material.NETHER_PORTAL`
   - Use modern `BlockData` API only
   - Remove any Material data value handling

3. **Use Modern Java Features**
   You can now use Java 21 features:
   - Records
   - Pattern matching for switch
   - Text blocks
   - Virtual threads
   - Sealed classes

   See [OPTIMIZATIONS.md](OPTIMIZATIONS.md) for suggestions.

## Compatibility Matrix

| NetherView Version | Minecraft Version | Java Version | ProtocolLib |
|-------------------|-------------------|--------------|-------------|
| 1.2.1 and earlier | 1.8 - 1.20       | 8+           | 4.5.0+      |
| 2.0+              | 1.21+            | 21+          | 5.3.0+      |

## Rollback Instructions

If you need to revert to the old version:

1. Stop your server
2. Replace the new NetherView.jar with your backed-up old version
3. Restore your backed-up configuration if you made changes
4. Downgrade ProtocolLib if you upgraded it
5. Restart your server

## FAQ

### Q: Can I use this on Minecraft 1.20?
**A:** No, this version requires 1.21+. Use NetherView 1.2.1 for older versions.

### Q: Will my existing portals and configuration be preserved?
**A:** Yes! Portal data in `portals.yml` is fully compatible. Only material names in `config.yml` may need updating.

### Q: Do I need to regenerate my worlds?
**A:** No, world data is not affected by this update.

### Q: What about Paper/Purpur/Folia?
**A:** The plugin works on any Spigot-compatible server running 1.21+. Folia support may require additional changes (see OPTIMIZATIONS.md).

### Q: Why remove legacy support?
**A:** 
- Simplifies codebase and maintenance
- Enables use of modern Java 21 features
- Improves performance with newer APIs
- Legacy versions (1.8-1.12) are 7-10+ years old
- Modern versions have significant performance improvements

### Q: Is there a cross-server/proxy version?
**A:** Not yet, but see [OPTIMIZATIONS.md](OPTIMIZATIONS.md) for detailed discussion of cross-server architecture possibilities.

## Getting Help

If you encounter issues during migration:

1. Check the console for error messages
2. Verify all requirements are met (Java 21, MC 1.21.8+, ProtocolLib 5.3.0+)
3. Review this guide and [OPTIMIZATIONS.md](OPTIMIZATIONS.md)
4. Report issues on [GitHub](https://github.com/hammermaps/NetherView/issues) or [SpigotMC](https://www.spigotmc.org/resources/nether-view.78885/)

Include in your report:
- Server version (Paper/Spigot/etc and MC version)
- Java version
- ProtocolLib version
- Any error messages from console
- Steps to reproduce the issue

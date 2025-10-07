# Update to PaperMC 1.21.8

## Summary

This document describes the update from PaperMC 1.21.1 (via spigot-api) to PaperMC 1.21.8 (via paper-api).

## Changes Made

### 1. POM.xml Updates

#### API Dependency
- **Before**: `org.spigotmc:spigot-api:1.21.1-R0.1-SNAPSHOT`
- **After**: `io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT`
- **Reason**: Proper PaperMC API usage instead of Spigot API

#### ProtocolLib Version
- **Before**: `5.1.0`
- **After**: `5.3.0`
- **Reason**: Better compatibility with PaperMC 1.21.8

#### Java Version
- **Current**: Java 21 (already configured correctly)
- **Note**: PaperMC 1.21.8 **requires** Java 21 (not just Java 17)

### 2. Documentation Updates

All documentation files have been updated to reflect:
- PaperMC version 1.21.8 (from 1.21.4 or 1.21.1)
- ProtocolLib version 5.3.0 (from 5.1.0)
- Java 21 requirement (corrected from Java 17 in some docs)

Updated files:
- `README.md`
- `MIGRATION_TO_PAPERMC.md`
- `BUILD_NOTES.md`
- `ZUSAMMENFASSUNG.md`
- `CHANGES_OVERVIEW.md`

### 3. Code Compatibility

No code changes were required because:
- The plugin uses standard Bukkit API classes
- No Spigot-specific or Paper-specific APIs are used
- All imports are compatible with PaperMC API

## Requirements

### Runtime
- **Server**: PaperMC 1.21.8 or higher
- **Java**: Java 21 or higher
- **Dependencies**: ProtocolLib 5.3.0 or higher

### Build
- **Java**: Java 21 or higher
- **Maven**: 3.6 or higher
- **Network Access**: Required to Maven repositories:
  - `repo.papermc.io` (PaperMC)
  - `repo.dmulloy2.net` (ProtocolLib)
  - `repo.codemc.org` (Additional dependencies)
  - `repo.maven.apache.org` (Maven Central)

## Build Instructions

```bash
mvn clean package
```

The compiled plugin will be available at `target/netherview-1.2.1.jar`

## Testing Recommendations

1. **Build Test**: Verify the plugin builds successfully with `mvn clean package`
2. **Runtime Test**: Deploy on a PaperMC 1.21.8 test server
3. **Feature Test**: Verify all portal viewing and linking features work correctly
4. **Compatibility Test**: Test with ProtocolLib 5.3.0+

## What Was Not Changed

- Plugin version remains `1.2.1`
- Java source code (no changes required)
- `plugin.yml` (already set to `api-version: 1.21`)
- Java version in pom.xml (already set to 21)
- Maven plugin versions (already up to date)

## Compatibility

The plugin should be compatible with:
- ✅ PaperMC 1.21.8 and newer
- ✅ PaperMC 1.21.x versions
- ⚠️ PaperMC 1.20.x (may work but not guaranteed)
- ⚠️ PaperMC 1.19.x and older (potential limitations)
- ⚠️ Spigot servers (backward compatible, but PaperMC recommended)

## Known Limitations

### Network Restrictions
The build could not be completed in the sandboxed environment due to blocked Maven repository domains. The changes are complete and correct, but require an environment with full network access to:
- Download `paper-api:1.21.8-R0.1-SNAPSHOT`
- Download `ProtocolLib:5.3.0`

## Next Steps

1. ✅ **Complete**: Code and documentation updates
2. ⏳ **Pending**: Build in environment with network access
3. ⏳ **Pending**: Runtime testing on PaperMC 1.21.8 server
4. ⏳ **Pending**: CodeQL security check
5. ⏳ **Pending**: Merge to main branch after successful testing

## Migration Notes

If you're running the plugin on an older PaperMC version:
- Upgrade to PaperMC 1.21.8
- Ensure Java 21 is installed
- Update ProtocolLib to version 5.3.0 or higher
- Replace the old plugin JAR with the new build
- Restart the server

No configuration changes are required - all settings and portal data will be preserved.

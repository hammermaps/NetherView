# Migration to PaperMC

This document describes the migration from Spigot API to PaperMC API for the NetherView plugin.

## Changes Made

### 1. Java Version Update
- **Old**: Java 1.8
- **New**: Java 21
- **Reason**: PaperMC 1.21.8 requires Java 21 as minimum version

### 2. API Dependency
- **Old**: `org.spigotmc:spigot-api:1.15.2-R0.1-SNAPSHOT`
- **New**: `io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT`
- **Reason**: PaperMC is a high-performance fork of Spigot with better API and features

### 3. API Version in plugin.yml
- **Old**: `api-version: 1.13`
- **New**: `api-version: 1.21`
- **Reason**: Updated to match the PaperMC version being targeted

### 4. Maven Repository
- **Added**: `https://repo.papermc.io/repository/maven-public/`
- **Reason**: PaperMC artifacts are hosted on their own repository

### 5. ProtocolLib Version
- **Old**: `4.5.0`
- **New**: `5.3.0`
- **Reason**: Updated to a version compatible with modern Minecraft/PaperMC versions

### 6. JUnit Version
- **Old**: `5.6.1`
- **New**: `5.10.1`
- **Reason**: Updated to current stable version for better Java 17 compatibility

### 7. Maven Compiler Plugin
- **Old**: `3.7.0`
- **New**: `3.11.0`
- **Reason**: Better Java 17 support and modern features

### 8. Maven Shade Plugin
- **Old**: `3.1.0`
- **New**: `3.5.1`
- **Reason**: Better compatibility with Java 17 and modern dependencies

## Compatibility

### Backward Compatibility
The plugin continues to support the same features as before. PaperMC is backward compatible with Spigot, so all Bukkit/Spigot API calls will continue to work.

### Code Changes Required
The existing code does not require changes because:
1. PaperMC maintains full backward compatibility with Bukkit/Spigot APIs
2. All imports using `org.bukkit.*` continue to work
3. The plugin uses standard Bukkit API methods that are unchanged

### Legacy Server Support
The plugin's version detection code in `NetherView.java` still works:
- It detects legacy servers (1.8-1.12) and modern servers
- The logic remains unchanged and compatible

## Testing Recommendations

1. **Build Test**: Run `mvn clean package` to ensure the plugin builds successfully
2. **Runtime Test**: Deploy on a PaperMC 1.21.4 server to verify functionality
3. **Legacy Test**: Optionally test on older PaperMC versions (1.20.x, 1.19.x) for backward compatibility
4. **Feature Test**: Test all portal viewing and linking features

## Benefits of PaperMC

1. **Performance**: PaperMC includes numerous optimizations over Spigot
2. **Bug Fixes**: Many Vanilla and Spigot bugs are fixed
3. **Better API**: More events and methods available for plugin developers
4. **Active Development**: Regular updates and improvements
5. **Configuration**: More server configuration options

## Network Restrictions Note

If building in an environment with limited internet access:
- Ensure `repo.papermc.io` is accessible
- Ensure `repo.dmulloy2.net` is accessible for ProtocolLib
- Maven Central (`repo.maven.apache.org`) is required for standard dependencies

## Deployment

The plugin can be deployed on:
- PaperMC 1.21.8 and newer
- PaperMC 1.21.x (fully compatible)
- PaperMC 1.20.x (should work with api-version: 1.21)
- PaperMC 1.19.x and older (with potential feature limitations)
- Spigot servers (backward compatible, though PaperMC is recommended)

## Future Considerations

1. Consider using Paper-specific APIs for enhanced features
2. Update to newer API versions as they become available
3. Consider dropping support for very old Minecraft versions (pre-1.13)

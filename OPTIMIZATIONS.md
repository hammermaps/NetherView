# NetherView Optimizations for Minecraft 1.24.x+ and Java 21

This document outlines possible optimizations and improvements for NetherView after removing legacy server support and requiring Java 21+.

## Java 21 Specific Optimizations

### 1. Virtual Threads (Project Loom)
- **Use Case**: Portal block cache generation and projection calculations
- **Implementation**: Replace synchronous block loading with virtual threads for better concurrency
- **Benefits**: Dramatically improved throughput when multiple players view portals simultaneously
- **Example**:
```java
// In PortalHandler or BlockCacheFactory
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
executor.submit(() -> {
    // Load block caches asynchronously
    loadBlockCachesOf(portal);
});
```

### 2. Pattern Matching for Switch Expressions
- **Use Case**: Block type handling and rotation logic
- **Benefits**: More readable and maintainable code with less boilerplate
- **Example**:
```java
// Modern switch with pattern matching
BlockType transformedBlock = switch (blockData) {
    case Directional d -> rotateDirectional(d, quarterTurns);
    case Rotatable r -> rotateRotatable(r, quarterTurns);
    case MultipleFacing mf -> rotateMultipleFacing(mf, quarterTurns);
    default -> blockData;
};
```

### 3. Record Classes
- **Use Case**: Immutable data transfer objects like BlockVec, Transform
- **Benefits**: Reduced boilerplate, built-in equals/hashCode/toString
- **Example**:
```java
public record BlockPosition(int x, int y, int z) {
    public BlockPosition add(BlockPosition other) {
        return new BlockPosition(x + other.x, y + other.y, z + other.z);
    }
}
```

### 4. Sealed Classes
- **Use Case**: BlockType hierarchy (restrict to AquaticBlockType only)
- **Benefits**: Better type safety and exhaustive pattern matching
- **Example**:
```java
public sealed abstract class BlockType permits AquaticBlockType {
    // Implementation
}
```

### 5. Text Blocks
- **Use Case**: Configuration messages, help text, error messages
- **Benefits**: More readable multi-line strings
- **Example**:
```java
String helpMessage = """
    NetherView Commands:
    /netherview reload - Reload configuration
    /netherview debug - Toggle debug messages
    /netherview list - List all portals
    """;
```

## Performance Optimizations

### 1. Async Block Cache Generation
- **Current**: Block caches are generated synchronously on the main thread
- **Optimization**: Use CompletableFuture or virtual threads to generate caches asynchronously
- **Impact**: Reduces server lag when players approach portals

### 2. Cache Expiration Timer Optimization
- **Current**: Timer checks all recently viewed portals every interval
- **Optimization**: Use a priority queue sorted by expiration time
- **Impact**: O(1) access to next-expiring cache instead of O(n) iteration

### 3. Spatial Indexing for Portal Lookups
- **Current**: Linear search through all portals in a world
- **Optimization**: Implement a spatial hash grid or R-tree for O(1) portal lookups by location
- **Impact**: Faster portal detection and updates, especially with many portals

### 4. Block Update Batching
- **Current**: Individual block updates trigger cache recalculations
- **Optimization**: Batch block updates within a tick and process once
- **Impact**: Reduced CPU usage during explosive events or large builds

### 5. Lazy Projection Cache Loading
- **Current**: Projection caches may be generated even when not visible
- **Optimization**: Only generate projection caches when a player is actually looking at a portal
- **Impact**: Reduced memory usage and CPU for background portals

### 6. Modern Collection APIs
- **Optimization**: Use Java 21's SequencedCollection, SequencedSet, and SequencedMap
- **Benefits**: More efficient operations on ordered collections
- **Example**:
```java
// Get first and last portals efficiently
Portal firstPortal = portals.getFirst();
Portal lastPortal = portals.getLast();
```

### 7. Stream API Improvements
- **Optimization**: Use Stream.toList() instead of collect(Collectors.toList())
- **Benefits**: More efficient and concise code
- **Example**:
```java
List<Portal> activePortals = allPortals.stream()
    .filter(Portal::hasActivePlayers)
    .toList(); // More efficient in Java 21
```

## Cross-Server/Proxy Compatibility

### Proxy Architecture Overview
NetherView can be adapted for cross-server functionality using a proxy like Velocity or BungeeCord:

### 1. Proxy-Based Portal Linking
- **Architecture**: Central database stores portal coordinates across all servers
- **Implementation**:
  - Each backend server tracks local portals
  - Proxy plugin aggregates portal data from all servers
  - Portal views show blocks from linked server
  - Teleportation triggers server transfer

### 2. Redis/Database Integration
- **Use Case**: Synchronize portal data across multiple servers
- **Implementation**:
```java
// Store portal in Redis
redisClient.hset("portals", portal.getId(), portal.serialize());

// Listen for portal updates from other servers
redisClient.subscribe("portal-updates", message -> {
    Portal updatedPortal = Portal.deserialize(message);
    localPortalHandler.updatePortal(updatedPortal);
});
```

### 3. Plugin Messaging Channels
- **Use Case**: Lightweight portal synchronization without external database
- **Implementation**:
```java
// Send portal data to proxy
ByteArrayDataOutput out = ByteStreams.newDataOutput();
out.writeUTF("PortalCreate");
out.writeUTF(portal.serialize());
player.sendPluginMessage(plugin, "netherview:portal", out.toByteArray());
```

### 4. Cross-Server Block Streaming
- **Challenge**: Showing blocks from another server requires packet interception
- **Solution**:
  - Proxy caches block data from target server
  - When player views portal, proxy streams cached blocks
  - Use ProtocolLib on both sides for packet manipulation

### 5. Limitations and Considerations
- **Latency**: Cross-server block updates have inherent network delay
- **Chunk Loading**: Target server chunks must be loaded (use chunk tickets)
- **Performance**: Proxy becomes bottleneck if many players view cross-server portals
- **Complexity**: Significantly more complex than single-server implementation

### 6. Recommended Approach
For initial cross-server support:
1. Start with portal coordinate synchronization only
2. Allow teleportation between servers via portals
3. Display simplified "connecting server" view instead of real blocks
4. Add full block streaming in later version if needed

### 7. Alternative: Shared World Storage
- **Concept**: Multiple server instances access same world files
- **Benefits**: Simpler than proxy-based streaming
- **Drawbacks**: Requires careful world locking and coordination
- **Not Recommended**: High risk of world corruption

## Memory Optimizations

### 1. Weak References for Cached Data
- **Use Case**: Block caches that can be regenerated
- **Benefits**: Automatic memory management during low-memory situations
- **Implementation**:
```java
Map<Portal, WeakReference<BlockCache>> caches = new WeakHashMap<>();
```

### 2. Object Pooling for BlockVec
- **Current**: New BlockVec objects created frequently
- **Optimization**: Pool and reuse BlockVec objects
- **Impact**: Reduced garbage collection pressure

### 3. Primitive Collections
- **Library**: Eclipse Collections or fastutil
- **Use Case**: Store block coordinates as primitives
- **Benefits**: Lower memory footprint, better cache locality

## API Modernization

### 1. Paper API Features
- **Folia Support**: Make plugin thread-safe for Folia's regionized threading
- **Adventure API**: Use Component instead of legacy ChatColor strings
- **Modern Events**: Use Paper's enhanced event system with priorities

### 2. ProtocolLib Updates
- **Version**: Update to 5.x for 1.21+ support
- **Features**: Use newer packet types and structures
- **Performance**: Leverage ProtocolLib's async packet handling

## Monitoring and Diagnostics

### 1. JFR (Java Flight Recorder) Integration
- **Use Case**: Production performance monitoring
- **Implementation**: Add custom JFR events for portal operations
- **Benefits**: Low-overhead profiling in production

### 2. Metrics Improvements
- **Add**: Portal view distance distribution
- **Add**: Average cache generation time
- **Add**: Cross-server portal usage (if implemented)
- **Add**: Memory usage trends

## Migration Path

### Phase 1: Foundation (Current)
- ✅ Remove legacy server support
- ✅ Update to Java 21
- ✅ Update API versions

### Phase 2: Quick Wins
- [ ] Implement virtual threads for cache generation
- [ ] Use modern switch expressions
- [ ] Convert DTOs to records
- [ ] Update collection APIs

### Phase 3: Performance
- [ ] Async block cache generation
- [ ] Spatial indexing for portals
- [ ] Block update batching
- [ ] Cache optimization

### Phase 4: Cross-Server (Optional)
- [ ] Redis integration for portal sync
- [ ] Proxy plugin development
- [ ] Cross-server teleportation
- [ ] (Optional) Cross-server block streaming

## Testing Strategy

### 1. Performance Benchmarks
- Portal view generation time
- Memory usage under load
- Concurrent player handling
- Cache expiration efficiency

### 2. Compatibility Testing
- Test on Paper, Spigot, Purpur
- Test with various world sizes
- Test with many concurrent portals
- Test with various ProtocolLib versions

### 3. Regression Testing
- Ensure portal linking still works
- Verify block hiding functionality
- Check teleportation cancellation
- Validate configuration loading

## Conclusion

These optimizations provide a roadmap for modernizing NetherView. Priority should be:
1. **Java 21 features** - Low effort, high readability improvement
2. **Async operations** - High impact on user experience
3. **Spatial indexing** - Scalability for large servers
4. **Cross-server** - Only if there's significant user demand

Each optimization should be measured for actual impact before and after implementation.

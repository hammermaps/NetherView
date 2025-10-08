package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.NetherView;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.BlockCacheFactory;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.ProjectionCachePair;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.blocktype.Axis;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.portal.PortalLocator;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PortalHandler {
	
	private NetherView main;
	
	private Map<UUID, Set<Portal>> worldsWithPortals;
	private Map<Portal, Long> recentlyViewedPortals;
	
	private BukkitRunnable expirationTimer;
	private long cacheExpirationDuration;
	
	// Virtual thread executor for async cache generation (Java 21)
	private final ExecutorService cacheExecutor;
	
	// Phase 3: Spatial indexing for O(1) portal lookups
	private final SpatialPortalIndex spatialIndex;
	
	// Phase 3: Priority queue for O(1) next-expiring cache access
	private final PriorityQueue<CacheExpirationEntry> expirationQueue;
	
	public PortalHandler(NetherView main) {
		
		this.main = main;
		
		worldsWithPortals = new HashMap<>();
		recentlyViewedPortals = new HashMap<>();
		cacheExpirationDuration = Duration.ofMinutes(10).toMillis();
		
		// Initialize virtual thread executor for cache generation
		cacheExecutor = Executors.newVirtualThreadPerTaskExecutor();
		
		// Phase 3 optimizations
		spatialIndex = new SpatialPortalIndex();
		expirationQueue = new PriorityQueue<>();
	}
	
	public void reset() {
		
		worldsWithPortals.clear();
		recentlyViewedPortals.clear();
		spatialIndex.clear();
		expirationQueue.clear();
	}
	
	/**
	 * Shuts down the virtual thread executor for cache generation.
	 * Should be called when the plugin is disabled.
	 */
	public void shutdown() {
		cacheExecutor.shutdown();
	}
	
	public Set<Portal> getPortals(World world) {
		// Phase 3: Use spatial index for portal retrieval
		return spatialIndex.getPortals(world);
	}
	
	public boolean hasPortals(World world) {
		// Phase 3: Use spatial index for existence check
		return spatialIndex.hasPortals(world);
	}
	
	/**
	 * Returns the count of currently registered portals of the server
	 */
	public Integer getTotalPortalCount() {
		
		int portalCount = 0;
		
		for (Map.Entry<UUID, Set<Portal>> entry : worldsWithPortals.entrySet()) {
			portalCount += entry.getValue().size();
		}
		
		return portalCount;
	}
	
	/**
	 * Returns the count of portals that have been viewed in the last 10 minutes.
	 */
	public Integer getRecentlyViewedPortalsCount() {
		return recentlyViewedPortals.size();
	}
	
	/**
	 * Returns the first portal that contains the passed block as part of the portal surface.
	 * If none was found it will  be tried to add the portal related to this block.
	 */
	public Portal getPortalByBlock(Block portalBlock) {
		// Phase 3: Use spatial index for O(1) portal lookup by block
		Portal portal = spatialIndex.getPortalByBlock(portalBlock);
		
		if (portal != null) {
			return portal;
		}
		
		return addPortalStructure(portalBlock);
	}
	
	/**
	 * Returns the first portal matching the passed hashcode. Returns null if none was found.
	 * (Portal hash codes are based on the location of the portal block with the lowest coordinates)
	 */
	public Portal getPortalByHashCode(int portalHashCode) {
		
		for (UUID worldID : worldsWithPortals.keySet()) {
			for (Portal portal : worldsWithPortals.get(worldID)) {
				
				if (portal.hashCode() == portalHashCode) {
					return portal;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the the nearest portal in a world to the passed Location. Returns null if none was found.
	 *
	 * @param mustBeLinked specify if the returned portal should be linked already
	 */
	public Portal getNearestPortal(Location playerLoc, boolean mustBeLinked) {
		// Phase 3: Use spatial index for optimized nearest portal search
		return spatialIndex.getNearestPortal(
			playerLoc.getWorld(),
			playerLoc.getBlockX(),
			playerLoc.getBlockZ(),
			mustBeLinked
		);
	}
	
	/**
	 * Returns a Set of all portals connected with their projections to the passed portal. Returns an empty set if none was found.
	 */
	public Set<Portal> getPortalsLinkedTo(Portal portal) {
		
		Set<Portal> linkedToPortals = new HashSet<>();
		
		for (UUID worldID : worldsWithPortals.keySet()) {
			for (Portal secondPortal : worldsWithPortals.get(worldID)) {
				
				if (secondPortal == portal) {
					continue;
				}
				
				if (secondPortal.isLinked() && secondPortal.getCounterPortal() == portal) {
					linkedToPortals.add(secondPortal);
				}
			}
		}
		
		return linkedToPortals;
	}
	
	/**
	 * Returns all block caches (2 for each portal) of all portals in specified world.
	 */
	public Set<BlockCache> getBlockCaches(World world) {
		
		Set<BlockCache> caches = new HashSet<>();
		
		for (Portal portal : getPortals(world)) {
			
			if (portal.blockCachesAreLoaded()) {
				caches.add(portal.getFrontCache());
				caches.add(portal.getBackCache());
			}
		}
		
		return caches;
	}
	
	/**
	 * Returns a Set of projection caches that are not connected to a portal but to a specific block cache (one of two for a portal).
	 * Returns an empty Set if none were found.
	 */
	public Set<ProjectionCache> getProjectionsLinkedTo(BlockCache cache) {
		
		Set<ProjectionCache> linkedToProjections = new HashSet<>();
		Portal portal = cache.getPortal();
		
		boolean isFrontCache = portal.getFrontCache() == cache;
		
		for (Portal linkedPortal : getPortalsLinkedTo(portal)) {
			
			if (linkedPortal.projectionsAreLoaded()) {
				linkedToProjections.add(isFrontCache ? linkedPortal.getBackProjection() : linkedPortal.getFrontProjection());
			}
		}
		
		return linkedToProjections;
	}
	
	/**
	 * Locates and registers a new portal.
	 *
	 * @param portalBlock one block of the structure required to detect the rest of it
	 */
	public Portal addPortalStructure(Block portalBlock) {
		
		Portal portal = PortalLocator.locatePortalStructure(portalBlock);
		UUID worldID = portal.getWorld().getUID();
		
		worldsWithPortals.putIfAbsent(worldID, new HashSet<>());
		worldsWithPortals.get(worldID).add(portal);
		
		// Phase 3: Add portal to spatial index
		spatialIndex.addPortal(portal);
		
		if (main.debugMessagesEnabled()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] Located portal at " + portal.toString());
		}
		
		return portal;
	}
	
	private void loadBlockCachesOf(Portal portal) {
		
		// Use virtual threads for async cache generation (Java 21 optimization)
		cacheExecutor.submit(() -> {
			portal.setBlockCaches(BlockCacheFactory.createBlockCaches(
					portal,
					main.getPortalProjectionDist(),
					main.getWorldBorderBlockType(portal.getWorld().getEnvironment())));
			
			addPortalToExpirationTimer(portal);
			
			if (main.debugMessagesEnabled()) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] Loaded block data for portal " + portal.toString());
			}
		});
	}
	
	public void loadProjectionCachesOf(Portal portal) {
		
		if (!portal.isLinked()) {
			return;
		}
		
		Portal counterPortal = portal.getCounterPortal();
		Transform linkTransform = calculateLinkTransform(portal, counterPortal);
		
		if (!counterPortal.blockCachesAreLoaded()) {
			loadBlockCachesOf(counterPortal);
		}
		
		BlockCache frontCache = counterPortal.getFrontCache();
		BlockCache backCache = counterPortal.getBackCache();
		
		//the projections caches are switching positions because of the transform
		ProjectionCache frontProjection = new ProjectionCache(portal, backCache, linkTransform);
		ProjectionCache backProjection = new ProjectionCache(portal, frontCache, linkTransform);
		
		portal.setProjectionCaches(new ProjectionCachePair(frontProjection, backProjection));
		addPortalToExpirationTimer(portal);
	}
	
	private void addPortalToExpirationTimer(Portal portal) {
		long now = System.currentTimeMillis();
		recentlyViewedPortals.put(portal, now);
		
		// Phase 3: Add to priority queue for O(1) next-expiring access
		long expirationTime = now + cacheExpirationDuration;
		expirationQueue.offer(new CacheExpirationEntry(portal, expirationTime));
		
		if (expirationTimer == null) {
			startCacheExpirationTimer();
		}
	}
	
	public void updateExpirationTime(Portal portal) {
		long now = System.currentTimeMillis();
		recentlyViewedPortals.put(portal, now);
		
		// Phase 3: Add updated expiration to priority queue
		// Note: Old entries will be filtered out when processed
		long expirationTime = now + cacheExpirationDuration;
		expirationQueue.offer(new CacheExpirationEntry(portal, expirationTime));
	}
	
	/**
	 * Removes all references to a registered portal
	 */
	public void removePortal(Portal portal) {
		
		Set<Portal> linkedToPortals = getPortalsLinkedTo(portal);
		
		if (main.debugMessagesEnabled()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] Removing portal at " + portal.toString());
			Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] Un-linking " + linkedToPortals.size() + " portal projections.");
		}
		
		for (Portal linkedPortal : getPortalsLinkedTo(portal)) {
			linkedPortal.removeLink();
		}
		
		portal.removeLink();
		
		recentlyViewedPortals.remove(portal);
		worldsWithPortals.get(portal.getWorld().getUID()).remove(portal);
		
		// Phase 3: Remove from spatial index
		spatialIndex.removePortal(portal);
		// Note: Expired entries in priority queue will be filtered when processed
	}
	
	/**
	 * Links a portal to it's counter portal it teleports to.
	 */
	public void linkPortalTo(Portal portal, Portal counterPortal) {
		
		if (!counterPortal.equalsInSize(portal)) {
			
			if (main.debugMessagesEnabled()) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] Cannot connect portal with size "
				                                      + (int) portal.getPortalRect().width() + "x" + (int) portal.getPortalRect().height() + " to portal with size "
				                                      + (int) counterPortal.getPortalRect().width() + "x" + (int) counterPortal.getPortalRect().height());
			}
			
			throw new IllegalStateException(ChatColor.GRAY + "" + ChatColor.ITALIC + "These portals are not the same size.");
		}
		
		portal.setLinkedTo(counterPortal);
		
		if (main.debugMessagesEnabled()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] Linked portal "
			                                      + portal.toString() + " to portal "
			                                      + counterPortal.toString());
		}
	}
	
	public void savePortals(FileConfiguration portalConfig) {
		
		portalConfig.set("portal-locations", null);
		portalConfig.set("linked-portals", null);
		
		ConfigurationSection portalLocations = portalConfig.createSection("portal-locations");
		ConfigurationSection portalLinks = portalConfig.createSection("linked-portals");
		
		for (UUID worldID : worldsWithPortals.keySet()) {
			
			List<String> portalsInWorld = new ArrayList<>();
			
			for (Portal portal : worldsWithPortals.get(worldID)) {
				
				portalsInWorld.add(new BlockVec(portal.getLocation()).toString());
				
				if (portal.isLinked()) {
					portalLinks.set(String.valueOf(portal.hashCode()), portal.getCounterPortal().hashCode());
				}
			}
			
			portalLocations.set(worldID.toString(), portalsInWorld);
		}
	}
	
	public void loadPortals(FileConfiguration portalConfig) {
		
		if (!portalConfig.contains("portal-locations")) {
			return;
		}
		
		ConfigurationSection portalLocations = portalConfig.getConfigurationSection("portal-locations");
		
		for (String worldID : portalLocations.getKeys(false)) {
			
			World worldWithPortals = Bukkit.getWorld(UUID.fromString(worldID));
			
			if (worldWithPortals == null) {
				main.getLogger().warning("Could not find world with ID: '" + worldID + "'. Portals saved for this world will not be loaded.");
				continue;
			}
			
			if (!main.canCreatePortalViews(worldWithPortals)) {
				continue;
			}
			
			List<String> portalBlocksLocs = portalLocations.getStringList(worldID);
			
			for (String serializedBlockVec : portalBlocksLocs) {
				
				try {
					BlockVec portalLoc = BlockVec.fromString(serializedBlockVec);
					addPortalStructure(worldWithPortals.getBlockAt(portalLoc.getX(), portalLoc.getY(), portalLoc.getZ()));
					
				} catch (IllegalArgumentException | IllegalStateException e) {
					main.getLogger().warning("Unable to load portal at [" + worldWithPortals.getName() + ", " + serializedBlockVec + "]: " + e.getMessage());
				}
			}
		}
	}
	
	public void loadPortalLinks(FileConfiguration portalConfig) {
		
		if (!portalConfig.contains("linked-portals")) {
			return;
		}
		
		ConfigurationSection portalLinks = portalConfig.getConfigurationSection("linked-portals");
		
		for (String portalHashString : portalLinks.getKeys(false)) {
			
			Portal portal = getPortalByHashCode(Integer.parseInt(portalHashString));
			Portal counterPortal = getPortalByHashCode(portalLinks.getInt(portalHashString));
			
			if (portal != null && counterPortal != null) {
				linkPortalTo(portal, counterPortal);
			}
		}
	}
	
	/**
	 * Calculates a Transform that is needed to translate and rotate block types at the positions of the block cache
	 * of the counter portal to the related position in the projection cache of the portal.
	 */
	private Transform calculateLinkTransform(Portal portal, Portal counterPortal) {
		
		Transform linkTransform;
		Vector distance = portal.getLocation().toVector().subtract(counterPortal.getLocation().toVector());
		
		linkTransform = new Transform();
		linkTransform.setTranslation(new BlockVec(distance));
		linkTransform.setRotCenter(new BlockVec(counterPortal.getPortalRect().getMin()));
		
		//during the rotation some weird shifts happen
		//I did not figure out where they come from, for now some extra translations are a good workaround
		if (portal.getAxis() == counterPortal.getAxis()) {
			
			linkTransform.setRotY180Deg();
			int portalBlockWidth = (int) portal.getPortalRect().width() - 1;
			
			if (counterPortal.getAxis() == Axis.X) {
				linkTransform.translate(new BlockVec(portalBlockWidth, 0, 0));
			} else {
				linkTransform.translate(new BlockVec(0, 0, portalBlockWidth));
			}
			
		} else if (counterPortal.getAxis() == Axis.X) {
			linkTransform.setRotY90DegRight();
			linkTransform.translate(new BlockVec(0, 0, 1));
			
		} else {
			linkTransform.setRotY90DegLeft();
			linkTransform.translate(new BlockVec(1, 0, 0));
		}
		
		return linkTransform;
	}
	
	/**
	 * Starts a scheduler that handles the removal of block caches (and projection caches) that weren't used for a certain expiration time.
	 */
	private void startCacheExpirationTimer() {
		
		if (main.debugMessagesEnabled()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] Starting cache expiration timer");
		}
		
		expirationTimer = new BukkitRunnable() {
			@Override
			public void run() {
				// Phase 3: Use priority queue for O(1) next-expiring cache access
				long now = System.currentTimeMillis();
				
				// Process all expired entries from the priority queue
				while (!expirationQueue.isEmpty()) {
					CacheExpirationEntry entry = expirationQueue.peek();
					
					// Stop if next entry hasn't expired yet
					if (entry.expirationTime() > now) {
						break;
					}
					
					// Remove from queue
					expirationQueue.poll();
					Portal portal = entry.portal();
					
					// Check if this entry is still valid (not updated or removed)
					Long lastViewTime = recentlyViewedPortals.get(portal);
					if (lastViewTime == null) {
						// Portal was removed, skip
						continue;
					}
					
					long actualExpirationTime = lastViewTime + cacheExpirationDuration;
					if (actualExpirationTime > now) {
						// Portal was updated, skip this old entry
						continue;
					}
					
					// Remove expired cache
					portal.removeProjectionCaches();
					portal.removeBlockCaches();
					recentlyViewedPortals.remove(portal);
					
					if (main.debugMessagesEnabled()) {
						Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GRAY + "[Debug] Removed cached blocks of portal " + portal.toString());
					}
				}
				
				// Stop timer if no more portals to expire
				if (recentlyViewedPortals.isEmpty()) {
					this.cancel();
					expirationTimer = null;
					expirationQueue.clear();
				}
			}
		};
		
		expirationTimer.runTaskTimerAsynchronously(main, ticksTillNextMinute(), 10 * 20);
	}
	
	private long ticksTillNextMinute() {
		
		LocalTime now = LocalTime.now();
		LocalTime nextMinute = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
		return now.until(nextMinute, ChronoUnit.MILLIS) / 50;
	}
}
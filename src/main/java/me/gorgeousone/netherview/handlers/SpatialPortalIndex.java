package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.threedstuff.BlockVec;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.*;

/**
 * Spatial hash grid for O(1) portal lookups by location.
 * Phase 3 Performance Optimization: Replaces O(n) linear searches with spatial indexing.
 */
public class SpatialPortalIndex {
	
	// Cell size for spatial hash grid (in blocks)
	private static final int CELL_SIZE = 16;
	
	// Map: World UUID -> Spatial Grid
	private final Map<UUID, Map<Long, Set<Portal>>> worldGrids;
	
	public SpatialPortalIndex() {
		this.worldGrids = new HashMap<>();
	}
	
	/**
	 * Adds a portal to the spatial index.
	 */
	public void addPortal(Portal portal) {
		UUID worldId = portal.getWorld().getUID();
		Map<Long, Set<Portal>> grid = worldGrids.computeIfAbsent(worldId, k -> new HashMap<>());
		
		// Get all cells that the portal bounds intersect
		Set<Long> cells = getCellsForPortal(portal);
		for (Long cellKey : cells) {
			grid.computeIfAbsent(cellKey, k -> new HashSet<>()).add(portal);
		}
	}
	
	/**
	 * Removes a portal from the spatial index.
	 */
	public void removePortal(Portal portal) {
		UUID worldId = portal.getWorld().getUID();
		Map<Long, Set<Portal>> grid = worldGrids.get(worldId);
		if (grid == null) {
			return;
		}
		
		// Remove from all cells that the portal intersects
		Set<Long> cells = getCellsForPortal(portal);
		for (Long cellKey : cells) {
			Set<Portal> portalsInCell = grid.get(cellKey);
			if (portalsInCell != null) {
				portalsInCell.remove(portal);
				if (portalsInCell.isEmpty()) {
					grid.remove(cellKey);
				}
			}
		}
		
		// Clean up empty world grids
		if (grid.isEmpty()) {
			worldGrids.remove(worldId);
		}
	}
	
	/**
	 * Finds a portal that contains the given block.
	 * O(1) average case instead of O(n) linear search.
	 */
	public Portal getPortalByBlock(Block block) {
		UUID worldId = block.getWorld().getUID();
		Map<Long, Set<Portal>> grid = worldGrids.get(worldId);
		if (grid == null) {
			return null;
		}
		
		long cellKey = getCellKey(block.getX(), block.getZ());
		Set<Portal> candidates = grid.get(cellKey);
		if (candidates == null) {
			return null;
		}
		
		// Check candidates in this cell
		BlockVec blockVec = new BlockVec(block);
		for (Portal portal : candidates) {
			if (portal.getPortalBlocks().contains(block)) {
				return portal;
			}
		}
		
		return null;
	}
	
	/**
	 * Gets all portals in the same world.
	 * Returns empty set if no portals exist in the world.
	 */
	public Set<Portal> getPortals(World world) {
		Map<Long, Set<Portal>> grid = worldGrids.get(world.getUID());
		if (grid == null) {
			return new HashSet<>();
		}
		
		// Collect all unique portals from all cells in this world
		Set<Portal> allPortals = new HashSet<>();
		for (Set<Portal> cellPortals : grid.values()) {
			allPortals.addAll(cellPortals);
		}
		return allPortals;
	}
	
	/**
	 * Finds the nearest portal to a given location.
	 * Uses spatial indexing to check nearby cells first for better performance.
	 */
	public Portal getNearestPortal(World world, int x, int z, boolean mustBeLinked) {
		Map<Long, Set<Portal>> grid = worldGrids.get(world.getUID());
		if (grid == null) {
			return null;
		}
		
		Portal nearest = null;
		double minDistSquared = Double.MAX_VALUE;
		
		// Check expanding rings of cells for better locality
		int searchRadius = 0;
		final int MAX_SEARCH_RADIUS = 10; // Search up to 10 cells away (~160 blocks)
		
		while (searchRadius < MAX_SEARCH_RADIUS) {
			Set<Portal> candidates = getPortalsInRadius(grid, x, z, searchRadius);
			
			if (!candidates.isEmpty()) {
				// Check all candidates in this radius
				for (Portal portal : candidates) {
					if (mustBeLinked && !portal.isLinked()) {
						continue;
					}
					
					double distSquared = distanceSquared(x, z, portal);
					if (distSquared < minDistSquared) {
						nearest = portal;
						minDistSquared = distSquared;
					}
				}
				
				// If we found a portal, check if any portal in next radius could be closer
				if (nearest != null) {
					double nextRadiusMinDist = (searchRadius + 1) * CELL_SIZE;
					if (minDistSquared < nextRadiusMinDist * nextRadiusMinDist) {
						// Current nearest is definitely the closest
						break;
					}
				}
			}
			searchRadius++;
		}
		
		return nearest;
	}
	
	/**
	 * Checks if any portals exist in the given world.
	 */
	public boolean hasPortals(World world) {
		Map<Long, Set<Portal>> grid = worldGrids.get(world.getUID());
		return grid != null && !grid.isEmpty();
	}
	
	/**
	 * Clears all portals from the index.
	 */
	public void clear() {
		worldGrids.clear();
	}
	
	// ==================== Private Helper Methods ====================
	
	/**
	 * Computes a unique cell key for a given x, z coordinate.
	 */
	private long getCellKey(int x, int z) {
		int cellX = Math.floorDiv(x, CELL_SIZE);
		int cellZ = Math.floorDiv(z, CELL_SIZE);
		return ((long) cellX << 32) | (cellZ & 0xFFFFFFFFL);
	}
	
	/**
	 * Gets all cell keys that a portal's bounds intersect.
	 */
	private Set<Long> getCellsForPortal(Portal portal) {
		Set<Long> cells = new HashSet<>();
		BlockVec min = portal.getMin();
		BlockVec max = portal.getMax();
		
		int minCellX = Math.floorDiv(min.getX(), CELL_SIZE);
		int maxCellX = Math.floorDiv(max.getX(), CELL_SIZE);
		int minCellZ = Math.floorDiv(min.getZ(), CELL_SIZE);
		int maxCellZ = Math.floorDiv(max.getZ(), CELL_SIZE);
		
		for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
			for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
				long key = ((long) cellX << 32) | (cellZ & 0xFFFFFFFFL);
				cells.add(key);
			}
		}
		
		return cells;
	}
	
	/**
	 * Gets all portals within a given radius (in cells) from a center point.
	 */
	private Set<Portal> getPortalsInRadius(Map<Long, Set<Portal>> grid, int x, int z, int radius) {
		Set<Portal> portals = new HashSet<>();
		int centerCellX = Math.floorDiv(x, CELL_SIZE);
		int centerCellZ = Math.floorDiv(z, CELL_SIZE);
		
		// Check cells in a square around the center
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				long cellKey = ((long) (centerCellX + dx) << 32) | ((centerCellZ + dz) & 0xFFFFFFFFL);
				Set<Portal> cellPortals = grid.get(cellKey);
				if (cellPortals != null) {
					portals.addAll(cellPortals);
				}
			}
		}
		
		return portals;
	}
	
	/**
	 * Computes squared distance from x,z coordinates to portal location.
	 */
	private double distanceSquared(int x, int z, Portal portal) {
		BlockVec portalLoc = new BlockVec(portal.getLocation());
		int dx = x - portalLoc.getX();
		int dz = z - portalLoc.getZ();
		return dx * dx + dz * dz;
	}
}

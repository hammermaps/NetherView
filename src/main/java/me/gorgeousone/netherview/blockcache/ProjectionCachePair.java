package me.gorgeousone.netherview.blockcache;

/**
 * Immutable pair of front and back projection caches for a portal.
 * This record replaces AbstractMap.SimpleEntry for better type safety and readability.
 * 
 * @param front The front projection cache
 * @param back The back projection cache
 */
public record ProjectionCachePair(ProjectionCache front, ProjectionCache back) {
}

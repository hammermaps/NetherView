package me.gorgeousone.netherview.blockcache;

/**
 * Immutable pair of front and back block caches for a portal.
 * This record replaces AbstractMap.SimpleEntry for better type safety and readability.
 * 
 * @param front The front block cache
 * @param back The back block cache
 */
public record BlockCachePair(BlockCache front, BlockCache back) {
}

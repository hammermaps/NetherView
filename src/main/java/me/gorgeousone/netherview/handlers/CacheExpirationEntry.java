package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.portal.Portal;

/**
 * Entry for cache expiration priority queue.
 * Phase 3 Performance Optimization: Enables O(1) access to next expiring cache.
 */
record CacheExpirationEntry(Portal portal, long expirationTime) implements Comparable<CacheExpirationEntry> {
	
	@Override
	public int compareTo(CacheExpirationEntry other) {
		return Long.compare(this.expirationTime, other.expirationTime);
	}
}

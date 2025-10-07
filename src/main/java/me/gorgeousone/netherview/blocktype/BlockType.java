package me.gorgeousone.netherview.blocktype;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

public abstract class BlockType {
	
	public static BlockType of(Block block) {
		return new AquaticBlockType(block);
	}
	
	public static BlockType of(Material material) {
		return new AquaticBlockType(material);
	}
	
	public static BlockType of(BlockState state) {
		return new AquaticBlockType(state);
	}
	
	public static BlockType of(String serialized) {
		return new AquaticBlockType(serialized);
	}
	
	/**
	 * Rotates the BlockType if it is rotatable in the xz plane in any way
	 *
	 * @param quarterTurns count of 90° turns performed (between 0 and 3)
	 */
	public abstract BlockType rotate(int quarterTurns);
	
	public abstract WrappedBlockData getWrapped();
	
	public abstract boolean isOccluding();
	
	public abstract BlockType clone();
}

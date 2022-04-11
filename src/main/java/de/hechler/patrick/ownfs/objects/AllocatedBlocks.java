package de.hechler.patrick.ownfs.objects;


public class AllocatedBlocks {
	
	public final long startBlock;
	public final long count;
	
	public AllocatedBlocks(long startBlock, long count) {
		this.startBlock = startBlock;
		this.count = count;
	}
	
}

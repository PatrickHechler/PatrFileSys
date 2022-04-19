package de.hechler.patrick.pfs.objects;

import java.util.ArrayList;
import java.util.List;

public class AllocatedBlocks implements Comparable <AllocatedBlocks> {
	
	public final long startBlock;
	public final long count;
	
	public AllocatedBlocks(long startBlock, long count) {
		this.startBlock = startBlock;
		this.count = count;
	}
	
	public boolean contains(long block) {
		if (block < startBlock) {
			return false;
		} else if (block > startBlock + count) {
			return false;
		} else {
			return true;
		}
	}
	
	public AllocatedBlocks[] remove(AllocatedBlocks rem) {
		return remove(rem.startBlock, rem.count);
	}
	
	public AllocatedBlocks[] remove(long startBlock, long count) {
		List <AllocatedBlocks> result = new ArrayList <>();
		if (this.startBlock < startBlock) {
			result.add(new AllocatedBlocks(this.startBlock, startBlock - this.startBlock));
		}
		if (this.startBlock + this.count > startBlock + count) {
			result.add(new AllocatedBlocks(startBlock + count, this.startBlock + this.count - startBlock - count));
		}
		return result.toArray(new AllocatedBlocks[result.size()]);
	}
	
	public boolean hasOverlapp(AllocatedBlocks other) {
		return hasOverlapp(other.startBlock, other.count);
	}
	
	public boolean hasOverlapp(long startBlock, long count) {
		long cnt = Math.min(this.startBlock + this.count, startBlock + count) - Math.max(this.startBlock, startBlock);
		if (cnt > 0L) {
			return true;
		} else {
			return false;
		}
	}
	
	public AllocatedBlocks overlapp(long startBlock, long count) {
		long start = Math.max(this.startBlock, startBlock),
			cnt = Math.min(this.startBlock + this.count, startBlock + count) - start;
		if (cnt <= 0L) {
			return null;
		} else if (start != this.startBlock || cnt != this.count) {
			return new AllocatedBlocks(start, cnt);
		} else {
			return this;
		}
	}
	
	@Override
	public int compareTo(AllocatedBlocks o) {
		return compareTo(o.startBlock, o.count);
	}
	
	public int compareTo(long startBlock, long count) {
		int cmp = Long.compare(this.startBlock, startBlock);
		if (cmp != 0) {
			return cmp;
		}
		cmp = Long.compare(this.count, count);
		return cmp;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (count ^ (count >>> 32));
		result = prime * result + (int) (startBlock ^ (startBlock >>> 32));
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		AllocatedBlocks other = (AllocatedBlocks) obj;
		if (count != other.count) return false;
		if (startBlock != other.startBlock) return false;
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AllocatedBlocks [startBlock=");
		builder.append(startBlock);
		builder.append(", count=");
		builder.append(count);
		builder.append("]");
		return builder.toString();
	}
	
}

package de.hechler.patrick.pfs.other;


public class Place implements Cloneable {
	
	public long block;
	public int  pos;
	
	public Place(long block, int pos) {
		this.block = block;
		this.pos = pos;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (block ^ (block >>> 32));
		result = prime * result + (int) (pos ^ (pos >>> 32));
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Place other = (Place) obj;
		if (block != other.block) return false;
		if (pos != other.pos) return false;
		return true;
	}
	
	@Override
	public Place clone() {
		try {
			return (Place) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return new Place(block, pos);
		}
	}
	
}

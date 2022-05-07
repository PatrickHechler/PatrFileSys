package de.hechler.patrick.pfs.objects;


public class LongInt {
	
	public final long l;
	public final int i;
	
	public LongInt(long l, int i) {
		this.l = l;
		this.i = i;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + i;
		result = prime * result + (int) (l ^ (l >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if ( ! (obj instanceof LongInt)) return false;
		LongInt other = (LongInt) obj;
		if (i != other.i) return false;
		if (l != other.l) return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LongInt [l=");
		builder.append(l);
		builder.append(", i=");
		builder.append(i);
		builder.append("]");
		return builder.toString();
	}
	
}

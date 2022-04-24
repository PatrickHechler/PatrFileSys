package de.hechler.patrick.pfs.objects;

import de.hechler.patrick.pfs.interfaces.BlockManager;

public class PatrID {
	
	public final PatrFileSysImpl fs;
	public final BlockManager    bm;
	public final long            id;
	public final long            startTime;
	
	public PatrID(PatrFileSysImpl fs, BlockManager bm, long id, long startTime) {
		this.fs = fs;
		this.bm = bm;
		this.id = id;
		this.startTime = startTime;
	}
	
	@Override
	public int hashCode() {
		return (int) (startTime ^ (startTime >>> 32));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if ( ! (obj instanceof PatrID)) return false;
		PatrID other = (PatrID) obj;
		if (bm == null) {
			if (other.bm != null) return false;
		} else if ( !bm.equals(other.bm)) return false;
		if (fs == null) {
			if (other.fs != null) return false;
		} else if ( !fs.equals(other.fs)) return false;
		if (id != other.id) return false;
		if (startTime != other.startTime) return false;
		return true;
	}
	
}

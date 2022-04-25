package de.hechler.patrick.pfs.objects;

import de.hechler.patrick.pfs.utils.PatrFileSysConstants;

public class PatrID {
	
	public final PatrFileSysImpl fs;
	public final long            id;
	public final long            startTime;
	
	public PatrID(PatrFileSysImpl fs, long id, long startTime) {
		this.fs = fs;
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
		if ( !fs.equals(other.fs)) return false;
		if (id != other.id) return false;
		if (startTime != other.startTime) return false;
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PatrID [id=");
		builder.append(id);
		if (id == PatrFileSysConstants.ROOT_FOLDER_ID) {
			builder.append(": ROOT");
		} else if (id == PatrFileSysConstants.ELEMENT_TABLE_FILE_ID) {
			builder.append(": ELEMENT_TABLE_FILE");
		} else if (id == PatrFileSysConstants.NO_ID) {
			builder.append(": NONE");
		}
		builder.append("]");
		return builder.toString();
	}
	
}

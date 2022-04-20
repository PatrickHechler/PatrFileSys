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
	
}

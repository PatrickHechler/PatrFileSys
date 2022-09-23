package de.hechler.patrick.pfs.fs;

import de.hechler.patrick.pfs.folder.PatrFileSysFolder;

public interface PatrFileSys {
	
	void format(long block_count);
	
	PatrFileSysFolder root();
	
	long blockCount();
	
	int blockSize();
	
	
	
}

package de.hechler.patrick.pfs.fs;

import de.hechler.patrick.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.folder.PFSFolder;

public interface PFS {
	
	void format(long blockCount) throws PatrFileSysException;
	
	PFSFolder root() throws PatrFileSysException;
	
	long blockCount() throws PatrFileSysException;
	
	int blockSize() throws PatrFileSysException;
	
	void close() throws PatrFileSysException;
	
}

package de.hechler.patrick.pfs.bm;

import de.hechler.patrick.exceptions.PatrFileSysException;

public interface BlockManager {
	
	byte[] get(long block) throws PatrFileSysException;
	
	void unget(long block) throws PatrFileSysException;
	
	void set(long block) throws PatrFileSysException;
	
	int blockSize();
	
	void sync()throws PatrFileSysException;
	
	void close() throws PatrFileSysException;
	
	default int flagsPerBlock() {
		return 0;
	}
	
	default long flags(long block) throws PatrFileSysException {
		return 0L;
	}
	
	default void flag(long block, long flags) throws PatrFileSysException {
		
	}
	
}

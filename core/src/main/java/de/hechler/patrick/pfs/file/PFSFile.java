package de.hechler.patrick.pfs.file;

import de.hechler.patrick.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.element.PFSElement;

public interface PFSFile extends PFSElement {
	
	void read(long position, byte[] data, int length) throws PatrFileSysException;
	
	void overwrite(long position, byte[] data, int length) throws PatrFileSysException;
	
	void append(byte[] data, int length) throws PatrFileSysException;
	
	void truncate(long newLength) throws PatrFileSysException;
	
	long length() throws PatrFileSysException;
	
}

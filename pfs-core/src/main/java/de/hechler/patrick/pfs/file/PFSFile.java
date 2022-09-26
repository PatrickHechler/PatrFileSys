package de.hechler.patrick.pfs.file;

import de.hechler.patrick.pfs.element.PFSElement;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;

public interface PFSFile extends PFSElement {
	
	byte[] read(long position, int length) throws PatrFileSysException;
	
	void read(long position, int byteOff, byte[] data, int length) throws PatrFileSysException;
	
	void overwrite(long position, int byteOff, byte[] data, int length) throws PatrFileSysException;
	
	void append(int byteOff, byte[] data, int length) throws PatrFileSysException;
	
	void truncate(long newLength) throws PatrFileSysException;
	
	long length() throws PatrFileSysException;
	
}

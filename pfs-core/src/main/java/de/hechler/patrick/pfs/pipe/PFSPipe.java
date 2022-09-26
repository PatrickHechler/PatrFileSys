package de.hechler.patrick.pfs.pipe;

import de.hechler.patrick.pfs.element.PFSElement;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;

public interface PFSPipe extends PFSElement {
	
	byte[] read(int length) throws PatrFileSysException;
	
	void read(int byteOff, byte[] data, int length) throws PatrFileSysException;
	
	void append(int byteOff, byte[] data, int length) throws PatrFileSysException;
	
	long length() throws PatrFileSysException;
	
}

package de.hechler.patrick.pfs.pipe;

import de.hechler.patrick.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.element.PFSElement;

public interface PFSPipe extends PFSElement {
	
	void read(byte[] data, int length) throws PatrFileSysException;
	
	void append(byte[] data, int length) throws PatrFileSysException;
	
	long length() throws PatrFileSysException;
	
}

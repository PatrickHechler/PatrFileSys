package de.hechler.patrick.pfs.pipe;

import java.nio.ByteBuffer;

import de.hechler.patrick.pfs.element.PFSElement;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;

public interface PFSPipe extends PFSElement {
	
	ByteBuffer read(int length) throws PatrFileSysException;
	
	void read(ByteBuffer data, int length) throws PatrFileSysException;
	
	void append(ByteBuffer data, int length) throws PatrFileSysException;
	
	long length() throws PatrFileSysException;
	
}

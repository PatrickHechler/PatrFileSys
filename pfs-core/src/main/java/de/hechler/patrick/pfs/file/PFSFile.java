package de.hechler.patrick.pfs.file;

import java.nio.ByteBuffer;

import de.hechler.patrick.pfs.element.PFSElement;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;

public interface PFSFile extends PFSElement {
	
	ByteBuffer read(long position, int length) throws PatrFileSysException;
	
	void read(long position, ByteBuffer data, int length) throws PatrFileSysException;
	
	void overwrite(long position, ByteBuffer data, int length) throws PatrFileSysException;
	
	void append(ByteBuffer data, int length) throws PatrFileSysException;
	
	void truncate(long newLength) throws PatrFileSysException;
	
	long length() throws PatrFileSysException;
	
}

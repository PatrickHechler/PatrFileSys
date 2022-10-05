package de.hechler.patrick.pfs.file;

import java.io.IOException;
import java.nio.ByteBuffer;

import de.hechler.patrick.pfs.element.PFSElement;

public interface PFSFile extends PFSElement {
	
	ByteBuffer read(long position, int length) throws IOException;
	
	void read(long position, ByteBuffer data, int length) throws IOException;
	
	void overwrite(long position, ByteBuffer data, int length) throws IOException;
	
	void append(ByteBuffer data, int length) throws IOException;
	
	void truncate(long newLength) throws IOException;
	
	long length() throws IOException;
	
}

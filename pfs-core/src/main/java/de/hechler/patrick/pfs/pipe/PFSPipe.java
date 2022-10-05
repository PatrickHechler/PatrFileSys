package de.hechler.patrick.pfs.pipe;

import java.io.IOException;
import java.nio.ByteBuffer;

import de.hechler.patrick.pfs.element.PFSElement;

public interface PFSPipe extends PFSElement {
	
	ByteBuffer read(int length) throws IOException;
	
	void read(ByteBuffer data, int length) throws IOException;
	
	void append(ByteBuffer data, int length) throws IOException;
	
	long length() throws IOException;
	
}

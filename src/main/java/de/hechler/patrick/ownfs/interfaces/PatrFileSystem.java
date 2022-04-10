package de.hechler.patrick.ownfs.interfaces;

import java.io.IOException;

public interface PatrFileSystem {
	
	/**
	 * returns the root folder of the File System.
	 * 
	 * @return the root folder of the File System.
	 */
	PatrFolder getRoot();
	
	void close() throws IOException;
	
}

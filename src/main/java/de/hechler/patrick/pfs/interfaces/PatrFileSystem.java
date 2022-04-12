package de.hechler.patrick.pfs.interfaces;

import java.io.Closeable;
import java.io.IOException;

public interface PatrFileSystem extends Closeable {
	
	/**
	 * returns the root folder of the File System.
	 * 
	 * @return the root folder of the File System.
	 * @throws IOException
	 *             if an IO error occurs
	 */
	PatrFolder getRoot() throws IOException;
	
	/**
	 * formats this file system.<br>
	 * after this operation this file system will be empty.
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void format() throws IOException;
	
}

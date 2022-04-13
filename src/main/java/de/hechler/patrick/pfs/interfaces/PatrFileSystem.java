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
	
	/**
	 * returns the total space in this file system measured in bytes <br>
	 * the returned value may not be accurate
	 * 
	 * @return the total space in this file system measured in bytes
	 * @throws IOException
	 *             if an IO error occurs
	 */
	long totalSpace() throws IOException;
	
	/**
	 * returns the free space in this file system measured in bytes <br>
	 * the returned value may not be accurate
	 * 
	 * @return the free space in this file system measured in bytes
	 * @throws IOException
	 *             if an IO error occurs
	 */
	long freeSpace() throws IOException;
	
	/**
	 * returns the used space in this file system measured in bytes <br>
	 * the returned value may not be accurate
	 * 
	 * @return the used space in this file system measured in bytes
	 * @throws IOException
	 *             if an IO error occurs
	 */
	long usedSpace() throws IOException;
	
	/**
	 * returns the block size of this file system
	 * 
	 * @return the block size of this file system
	 * @throws IOException
	 *             if an IO error occurs
	 */
	int blockSize() throws IOException;
	
	/**
	 * returns number of the block in this file system
	 * 
	 * @return number of the block in this file system
	 * @throws IOException
	 *             if an IO error occurs
	 */
	long blockCount() throws IOException;
	
}

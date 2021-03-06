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
	 * returns the element with the given {@code id}
	 * 
	 * @param id
	 *            the id from the element
	 * @return the element
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws IllegalArgumentException
	 *             if the given id is invalid
	 * @throws NullPointerException
	 *             if the given id is <code>null</code>
	 */
	PatrFileSysElement fromID(Object id) throws IOException, IllegalArgumentException, NullPointerException;
	
	/**
	 * formats this file system.<br>
	 * after this operation this file system will be empty.
	 * 
	 * @param blockCount the number of blocks usable by this file system
	 * @param blockSize the size of each block used by this file system
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void format(long blockCount, int blockSize) throws IOException;
	
	/**
	 * returns the free space in this file system measured in bytes <br>
	 * the returned value may not be accurate
	 * 
	 * @return the free space in this file system measured in bytes
	 * @throws IOException
	 *             if an IO error occurs
	 */
	long freeBlocks() throws IOException;
	
	/**
	 * returns the used space in this file system measured in bytes <br>
	 * the returned value may not be accurate
	 * 
	 * @return the used space in this file system measured in bytes
	 * @throws IOException
	 *             if an IO error occurs
	 */
	long usedBlocks() throws IOException;
	
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

package de.hechler.patrick.pfs.interfaces;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

public interface BlockManager {
	
	/**
	 * returns the length of the arrays returned by {@link #loadBlock(long)}
	 * 
	 * @return the length of the arrays returned by {@link #loadBlock(long)}
	 */
	int blockSize();
	
	/**
	 * the equivalent to {@link #loadBlock(long)}, but it builds a stack, so the manage keeps the track on how often wich block is loaded.<br>
	 * all loaded block will work with the same array, so changes on one loaded block will be made on all instances of the loaded block.<br>
	 * 
	 * note that for each {@link #getBlock(long)} only one time {@link #ungetBlock(long)} OR {@link #setBlock(long)} should be called.
	 * 
	 * @param block
	 *            the block number
	 * @return the given block
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ClosedChannelException
	 *             if the manager has been closed
	 */
	byte[] getBlock(long block) throws ClosedChannelException, IOException;
	
	/**
	 * tells the manager that it is not needed to save this block.<br>
	 * note that the block will still be saved if at least one tells the manager to save the block<br>
	 * 
	 * note that for each {@link #getBlock(long)} only one time {@link #ungetBlock(long)} OR {@link #setBlock(long)} should be called.
	 * 
	 * @param block
	 *            the given block
	 * @throws ClosedChannelException
	 *             if the manager has been closed
	 * @throws IOException
	 *             if the block needed to be saved and an error occurs
	 * @throws IllegalStateException
	 *             if the block is not currently loaded
	 */
	void ungetBlock(long block) throws ClosedChannelException, IOException, IllegalStateException;
	
	/**
	 * tells the manager that changes has been made and the block needs to be saved.<br>
	 * 
	 * note that for each {@link #getBlock(long)} only one time {@link #ungetBlock(long)} OR {@link #setBlock(long)} should be called.
	 * 
	 * @param block
	 *            the block which has been changed
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ClosedChannelException
	 *             if the manager has been closed
	 * @throws IllegalStateException
	 *             if the block is not currently loaded
	 */
	void setBlock(long block) throws ClosedChannelException, IOException, IllegalStateException;
	
	/**
	 * closes the manager and discards all unsaved blocks.
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void close() throws IOException;
	
	/**
	 * save all loaded blocks
	 * 
	 * @throws ClosedChannelException
	 *             if the manager has been closed
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void saveAll() throws ClosedChannelException, IOException;
	
	/**
	 * discard all loaded blocks without saving them
	 * 
	 * @throws ClosedChannelException
	 *             if the manager has been closed
	 */
	void discardAll() throws ClosedChannelException;
	
}

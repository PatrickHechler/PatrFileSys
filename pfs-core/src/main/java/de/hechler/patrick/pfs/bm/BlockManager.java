package de.hechler.patrick.pfs.bm;

import java.nio.ByteBuffer;

import de.hechler.patrick.pfs.exceptions.PFSErr;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.fs.PFS;

public interface BlockManager {
	
	/**
	 * get the given block
	 * <p>
	 * if the block is already loaded the same {@link ByteBuffer} instance will be returned
	 * <p>
	 * the {@link BlockManager} will internally count how many times each block is loaded.<br>
	 * when a block is {@link #unget(long)} and {@link #set(long)} as often as it was
	 * {@link #get(long)} it will be unloaded/saved
	 * 
	 * @param block
	 *            the block number
	 * @return the data of the block
	 * @throws PatrFileSysException
	 *             if an error occurs
	 */
	ByteBuffer get(long block) throws PatrFileSysException;
	
	/**
	 * tell the {@link BlockManager} that the block is no longer needed and no changes has been made
	 * (or no changes needs to be saved)
	 * <p>
	 * it is up to the {@link BlockManager} to decide if the block will be saved or if all changes
	 * will be discarded or if the block will be saved when the times for unloading/saving comes for
	 * the given block<br>
	 * normally the {@link BlockManager} will just decide to save all blocks which has been
	 * {@link #set(long)} at least once and discard all others
	 * 
	 * @param block
	 *            the block number
	 * @throws PatrFileSysException
	 *             if an error occurs
	 */
	void unget(long block) throws PatrFileSysException;
	
	/**
	 * tell the {@link BlockManager} that the block is no longer needed and that there has been made
	 * changes which need to be saved.
	 * <p>
	 * when a block is {@link #set(long)} at least once the {@link BlockManager} is not allowed to
	 * just discard the changes and has to save all changes of the block in the underlying storage
	 * 
	 * @throws PatrFileSysException
	 *             if an error occurs
	 */
	void set(long block) throws PatrFileSysException;
	
	/**
	 * returns the size of each block
	 * <p>
	 * all blocks will have the given size, so {@link ByteBuffer#capacity()} of all blocks will be
	 * equal to {@link #blockSize()}
	 * 
	 * @return the size of each block
	 */
	int blockSize();
	
	/**
	 * synchronises all blocks, which are currently not in use
	 * 
	 * @throws PatrFileSysException
	 *             if an error occurs
	 */
	void sync() throws PatrFileSysException;
	
	/**
	 * closes the {@link BlockManager}
	 * <p>
	 * if there are currently any blocks used an exception should be thrown
	 * 
	 * @throws PatrFileSysException
	 *             if an error occurred
	 */
	void close() throws PatrFileSysException;
	
	/**
	 * returns the number of flags each block can have
	 * <p>
	 * the return value of this method is never allowed to change
	 * <p>
	 * the minimum value is {@code 0} and the maximum value is {@code 63}
	 * 
	 * @return the number of flags each block can have
	 */
	default int flagsPerBlock() {
		return 0;
	}
	
	/**
	 * get the flags of the given block
	 * <p>
	 * the unsupported bits will be filled with zeros.<br>
	 * the (supported) flags will be represented in the lowest possible bits<br>
	 * this means that all returned values will be positive, since {@link #flagsPerBlock()} is only
	 * allowed to return values from {@code 0} to {@code 63} (both inclusive)
	 * 
	 * @param block
	 *            the block number
	 * @return the flags of the block
	 * @throws PatrFileSysException
	 *             if an error occurred
	 */
	default long flags(long block) throws PatrFileSysException {
		return 0L;
	}
	
	/**
	 * set the flags of the given block
	 * <p>
	 * unsupported flags will be ignored<br>
	 * the (supported) flags are be represented in the lowest possible bits
	 * 
	 * @param block
	 *            the block number
	 * @param flags
	 *            the new flags of the block
	 * @throws PatrFileSysException
	 *             if an error occurred
	 */
	default void flag(long block, long flags) throws PatrFileSysException {}
	
	/**
	 * returns the number of the first block which has no flags
	 * <p>
	 * if no flags are supported ({@link #flagsPerBlock()} returns {@code 0}) the operation will
	 * fail
	 * <p>
	 * if the {@link BlockManager} has a maximum block number (like {@link PFS#blockCount()}) and
	 * the first block without any flags would be out of the boundaries the operation will fail
	 * 
	 * @return the number of the first block which has no flags
	 * @throws PatrFileSysException
	 *             if an error occurred
	 */
	default long firstZeroFlaggedBlock() throws PatrFileSysException {
		throw PFSErr.createAndThrow(PFSErr.PFS_ERR_OUT_OF_SPACE, "operation not supported");
	}
	
	/**
	 * removes all flags of all blocks.
	 * <p>
	 * if no flags are supported ({@link #flagsPerBlock()} returns {@code 0}) this method will do
	 * nothing
	 * 
	 * @throws PatrFileSysException
	 *             if an error occurs
	 */
	default void deleteAllFlags() throws PatrFileSysException {}
	
}

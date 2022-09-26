package de.hechler.patrick.pfs.bm;

import java.io.Closeable;
import java.nio.ByteBuffer;

import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.fs.PFS;

public interface BlockAccessor extends Closeable {
	
	/**
	 * returns the length of the arrays returned by {@link #loadBlock(long)}
	 * 
	 * @return the length of the arrays returned by {@link #loadBlock(long)}
	 */
	int blockSize();
	
	/**
	 * returns the value of the block at the position {@code block}<br>
	 * all changes on the returned array possibly have a direct impact on the {@link BlockAccessor},
	 * but the {@link #saveBlock(byte[], long)} or {@link #discardBlock(long)} method has to be
	 * called after calling this method with the returned block and the position.<br>
	 * the behaviour is undefined if this method is called twice without calling
	 * {@link #saveBlock(byte[], long)} or {@link #discardBlock(long)} between the calls.
	 * 
	 * @param block
	 *            the position of the block returned
	 * @return the value of the block
	 * @throws PatrFileSysException
	 *             if an IO error occurs during the operation
	 */
	ByteBuffer loadBlock(long block) throws PatrFileSysException;
	
	/**
	 * saves the block after changing it.<br>
	 * the behaviour is undefined if the {@code value} array is not the same array as the array
	 * previously returned by {@link #loadBlock(long)} with the same {@code block} or if the block
	 * has already been unloaded/saved.
	 * 
	 * @param data
	 *            the data of the block
	 * @param block
	 *            the position of the block
	 * @throws PatrFileSysException
	 *             if an IO error occurs during the operation
	 */
	void saveBlock(ByteBuffer data, long block) throws PatrFileSysException;
	
	/**
	 * unloads the given block without saving it.<br>
	 * the behaviour is undefined if the {@code block} has not been {@link #loadBlock(long) loaded}
	 * before or the block has been unloaded already using the {@link #saveBlock(byte[], long)} or
	 * {@link #discardBlock(long)} method.
	 * 
	 * @throws PatrFileSysException
	 *             if an IO error occurs during the operation
	 */
	void discardBlock(long block) throws PatrFileSysException;
	
	/**
	 * discards all loaded blocks without saving them. If they should be saved use
	 * {@link #saveAll()}. further calls to {@link #loadBlock(long)},
	 * {@link #saveBlock(byte[], long)}, {@link #discardBlock(long)} and {@link #saveAll()} should
	 * throw an {@link ClosedChannelException}.
	 * <p>
	 * If the {@link BlockAccessor} is already closed then invoking this method has no effect.
	 * 
	 * @throws PatrFileSysException
	 *             if an IO error occurs during the operation
	 */
	@Override
	void close() throws PatrFileSysException;
	
	/**
	 * synchronises all blocks, if any changes has been made with
	 * {@link #saveBlock(ByteBuffer, long)} they are ensured to be saved on the underlying storage
	 * and not in any buffer
	 * 
	 * @throws PatrFileSysException
	 *             if an error occurred
	 */
	void sync() throws PatrFileSysException;
	
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
	 * if no flags are supported ({@link #flagsPerBlock()} returns {@code 0}) {@code -1} will be
	 * <p>
	 * if the {@link BlockManager} has a maximum block number (like {@link PFS#blockCount()}) and
	 * the first block without any flags would be out of the boundaries {@code -1} will be returned
	 * returned
	 * 
	 * @return the number of the first block which has no flags
	 * @throws PatrFileSysException
	 *             if an error occurred
	 */
	default long firstZeroFlaggedBlock() throws PatrFileSysException {
		return -1L;
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

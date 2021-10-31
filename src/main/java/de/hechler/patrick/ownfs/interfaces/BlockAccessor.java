package de.hechler.patrick.ownfs.interfaces;

import java.io.Closeable;
import java.io.IOException;

public interface BlockAccessor extends Closeable {
	
	/**
	 * returns the length of the arrays returned by {@link #loadBlock(long)}
	 * 
	 * @return the length of the arrays returned by {@link #loadBlock(long)}
	 */
	int blockSize();
	
	/**
	 * returns the value of the block at the position {@code block}<br>
	 * all changes on the returned array possibly have a direct impact on the {@link BlockAccessor}, but the {@link #saveBlock(byte[], long)} or {@link #unloadBlock(long)} method has to be called
	 * after calling this method with the returned block and the position.<br>
	 * 
	 * the behavior is undefined if this method is called twice without calling {@link #saveBlock(byte[], long)} or {@link #unloadBlock(long)} between the calls.
	 * 
	 * @param block
	 *            the position of the block returned
	 * @return the value of the block
	 * @throws IOException
	 *             if an IO error occurs during the operation
	 */
	byte[] loadBlock(long block) throws IOException;
	
	/**
	 * saves the block after changing it.<br>
	 * 
	 * if {@link #usesDirectBlocks()} returns <code>true</code> it is effectively the same as {@link #unloadBlock(long)}.<br>
	 * 
	 * the behavior is undefined if the {@code value} array is not the same array as the array previously returned by {@link #loadBlock(long)} with the same {@code block}.
	 * 
	 * @param value
	 *            the value of the block
	 * @param block
	 *            the position of the block
	 */
	void saveBlock(byte[] value, long block) throws IOException;
	
	/**
	 * unloads the given block.<br>
	 * 
	 * if {@link #usesDirectBlocks()} returns <code>true</code> it is effectively the same as {@link #saveBlock(byte[], long)}.<br>
	 * 
	 * the behavior is undefined if the {@code block} has not been {@link #loadBlock(long) loaded} before or the block has been unloaded already using the {@link #saveBlock(byte[], long)} or
	 * {@link #unloadBlock(long)} method.
	 * 
	 * @param block
	 *            the block to unload
	 */
	void unloadBlock(long block);
	
	/**
	 * returns <code>true</code> when this {@link BlockAccessor} returns arrays where modifications has direct impacts on this {@link BlockAccessor}
	 * 
	 * @return
	 */
	boolean usesDirectBlocks();
	
	@Override
	void close();
	
}

package de.hechler.patrick.pfs.fs;

import java.io.IOException;

import de.hechler.patrick.pfs.folder.PFSFolder;

public interface PFS {
	
	/**
	 * formats the {@link PFS}
	 * <p>
	 * after this operation all data previously saved on the PFS will be lost
	 * 
	 * @param blockCount
	 *            the block count available for the {@link PFS}
	 * @throws IOException
	 *             if an error occurs
	 */
	void format(long blockCount) throws IOException;
	
	/**
	 * get the root folder of the {@link PFS}
	 * 
	 * @return the root folder of the {@link PFS}
	 * @throws IOException
	 *             if an error occurs
	 */
	PFSFolder root() throws IOException;
	
	/**
	 * returns the block count available for the {@link PFS}
	 * 
	 * @return the block count available for the {@link PFS}
	 * @throws IOException
	 */
	long blockCount() throws IOException;
	
	/**
	 * returns the block size available for the {@link PFS}
	 * 
	 * @return the block size available for the {@link PFS}
	 * @throws IOException
	 *             if an error occurs
	 */
	int blockSize() throws IOException;
	
	/**
	 * closes the {@link PFS}
	 * 
	 * @throws IOException
	 *             if an error occurs
	 */
	void close() throws IOException;
	
}

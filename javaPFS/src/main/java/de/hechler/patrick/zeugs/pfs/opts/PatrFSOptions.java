package de.hechler.patrick.zeugs.pfs.opts;

import de.hechler.patrick.zeugs.pfs.interfaces.FSOptions;

/**
 * the {@link PatrFSOptions} are for a patr file system which is stored in some
 * file.
 * <p>
 * to load an already existing file system use the
 * {@link #PatrFSOptions(String)} constructor.<br>
 * to load and format/create an new file system use the
 * {@link #PatrFSOptions(String, long, int)} constructor.
 * 
 * @author pat
 */
public record PatrFSOptions(String path, boolean format, long blockCount, int blockSize) implements FSOptions {
	/**
	 * creates new {@link PatrFSOptions} with the given path and {@link #format} set
	 * to <code>false</code>
	 * 
	 * @param path the path of the patr file system
	 */
	public PatrFSOptions(String path) {
		this(path, false, -1L, -1);
	}

	/**
	 * creates new {@link PatrFSOptions} with the given path, {@link #format} set to
	 * <code>true</code> and the given {@link #blockCount} and {@link #blockSize}
	 * 
	 * @param path       the path of the patr file system
	 * @param blockCount the number of blocks available for the patr file system
	 * @param blockSize  the size of each block available for the patr file system
	 */
	public PatrFSOptions(String path, long blockCount, int blockSize) {
		this(path, true, blockCount, blockSize);
	}
}

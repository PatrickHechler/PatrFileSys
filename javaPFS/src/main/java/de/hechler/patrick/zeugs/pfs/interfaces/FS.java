package de.hechler.patrick.zeugs.pfs.interfaces;

import java.io.Closeable;
import java.io.IOException;

import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

/**
 * the {@link FS} interface provides methods:
 * <ul>
 * <li>to get a file system element from a path ({@link #element(String)},
 * {@link #folder(String)}, {@link #file(String)} and
 * {@link #pipe(String)})</li>
 * <li>to get the current working directory ({@link #cwd()})</li>
 * <li>to set the current working directory ({@link #cwd(Folder)})</li>
 * </ul>
 * 
 * @author pat
 */
public interface FS extends Closeable {

	/**
	 * returns the number of available blocks for the file system
	 * 
	 * @return the number of available blocks for the file system
	 * @throws IOException
	 */
	long blockCount() throws IOException;

	/**
	 * returns the size of the blocks in the file system
	 * 
	 * @return the size of the blocks in the file system
	 * @throws IOException
	 */
	int blockSize() throws IOException;

	/**
	 * get the element which can be referred with the given path
	 * 
	 * @param path the path of the element
	 * @return an handle for the given element refereed by {@code path}
	 * @throws IOException
	 */
	FSElement element(String path) throws IOException;

	/**
	 * get the element which can be referred with the given path <br>
	 * this operation will fail if the element is no folder
	 * 
	 * @param path the path of the element
	 * @return an handle for the given element refereed by {@code path}
	 * @throws IOException
	 */
	Folder folder(String path) throws IOException;

	/**
	 * get the element which can be referred with the given path <br>
	 * this operation will fail if the element is no file
	 * 
	 * @param path the path of the element
	 * @return an handle for the given element refereed by {@code path}
	 * @throws IOException
	 */
	File file(String path) throws IOException;

	/**
	 * get the element which can be referred with the given path <br>
	 * this operation will fail if the element is no pipe
	 * 
	 * @param path the path of the element
	 * @return an handle for the given element refereed by {@code path}
	 * @throws IOException
	 */
	Pipe pipe(String path) throws IOException;

	/**
	 * opens a {@link Stream} for the given path.
	 * 
	 * @param path the path of the element
	 * @param opts 
	 * @return the opened stream
	 * @throws IOException
	 */
	Stream stream(String path, StreamOpenOptions opts) throws IOException;

	/**
	 * returns the current working directory
	 * 
	 * @return the current working directory
	 * @throws IOException
	 */
	Folder cwd() throws IOException;

	/**
	 * changes the current working directory to {@code f}
	 * 
	 * @param f the new current working directory
	 * @throws IOException
	 */
	void cwd(Folder f) throws IOException;

	/**
	 * Closes this file system and releases any system resources associated with it.
	 * If the file system is already closed then invoking this method has no effect.
	 * <p>
	 * As noted in {@link AutoCloseable#close()}, cases where the close may fail
	 * require careful attention. It is strongly advised to relinquish the
	 * underlying resources and to internally <em>mark</em> the {@code Closeable} as
	 * closed, prior to throwing the {@code IOException}.
	 * <p>
	 * all file system methods (except of {@link #close()}) will throw an exception
	 * when they are called after the file system has been closed
	 */
	@Override
	void close() throws IOException;

}

package de.hechler.patrick.zeugs.pfs.interfaces;

import java.io.IOException;

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
public interface FS {

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

}

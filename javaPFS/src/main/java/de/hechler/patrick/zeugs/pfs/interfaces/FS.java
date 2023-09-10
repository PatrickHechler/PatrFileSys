//This file is part of the Patr File System Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
	 * @throws IOException if an IO error occurs
	 */
	long blockCount() throws IOException;

	/**
	 * returns the size of the blocks in the file system
	 * 
	 * @return the size of the blocks in the file system
	 * @throws IOException if an IO error occurs
	 */
	int blockSize() throws IOException;

	/**
	 * get the element which can be referred with the given path
	 * 
	 * @param path the path of the element
	 * @return an handle for the given element refereed by {@code path}
	 * @throws IOException if an IO error occurs
	 */
	FSElement element(String path) throws IOException;

	/**
	 * get the element which can be referred with the given path <br>
	 * this operation will fail if the element is no folder or mount point
	 * 
	 * @param path the path of the element
	 * @return an handle for the given element refereed by {@code path}
	 * @throws IOException if an IO error occurs
	 */
	Folder folder(String path) throws IOException;

	/**
	 * get the element which can be referred with the given path <br>
	 * this operation will fail if the element is no mount point
	 * 
	 * @param path the path of the element
	 * @return an handle for the given element refereed by {@code path}
	 * @throws IOException if an IO error occurs
	 */
	Mount mount(String path) throws IOException;

	/**
	 * get the element which can be referred with the given path <br>
	 * this operation will fail if the element is no file
	 * 
	 * @param path the path of the element
	 * @return an handle for the given element refereed by {@code path}
	 * @throws IOException if an IO error occurs
	 */
	File file(String path) throws IOException;

	/**
	 * get the element which can be referred with the given path <br>
	 * this operation will fail if the element is no pipe
	 * 
	 * @param path the path of the element
	 * @return an handle for the given element refereed by {@code path}
	 * @throws IOException if an IO error occurs
	 */
	Pipe pipe(String path) throws IOException;

	/**
	 * opens a {@link Stream} for the given path.
	 * 
	 * @param path the path of the element
	 * @param opts 
	 * @return the opened stream
	 * @throws IOException if an IO error occurs
	 */
	Stream stream(String path, StreamOpenOptions opts) throws IOException;

	/**
	 * returns the current working directory
	 * 
	 * @return the current working directory
	 * @throws IOException if an IO error occurs
	 */
	Folder cwd() throws IOException;
	
	/**
	 * returns the root directory
	 * 
	 * @return the root directory
	 * @throws IOException if an IO error occurs
	 */
	Mount root() throws IOException;

	/**
	 * changes the current working directory to {@code f}
	 * 
	 * @param f the new current working directory
	 * @throws IOException if an IO error occurs
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

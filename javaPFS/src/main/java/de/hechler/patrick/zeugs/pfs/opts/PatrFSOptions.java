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
 * @param path       the path of the file, which should be used as underlying
 *                   block storage
 * @param format     <code>true</code> if the file system should be created and
 *                   <code>false</code> if it should be opened
 * @param blockCount the number of blocks, which can be used by the file system
 * @param blockSize  the size of the blocks, which can be used by the file
 *                   system
 * 
 * @author pat
 */
public record PatrFSOptions(String path, boolean format, long blockCount, int blockSize) implements FSOptions {
	
	/**
	 * creates new {@link PatrFSOptions} with the given parameters
	 * 
	 * @param path       the path of the file, which should be used as underlying
	 *                   block storage
	 * @param format     <code>true</code> if the file system should be created and
	 *                   <code>false</code> if it should be opened
	 * @param blockCount the number of blocks, which can be used by the file system
	 * @param blockSize  the size of the blocks, which can be used by the file
	 *                   system
	 * 
	 * @return the newly created options
	 */
	public static PatrFSOptions create(String path, boolean format, long blockCount, int blockSize) {
		return new PatrFSOptions(path, format, blockCount, blockSize);
	}
	
	/**
	 * creates new {@link PatrFSOptions} with the given path and {@link #format()} set
	 * to <code>false</code>
	 * 
	 * @param path the path of the patr file system
	 */
	public PatrFSOptions(String path) {
		this(path, false, -1L, -1);
	}
	
	/**
	 * creates new {@link PatrFSOptions} with the given path, {@link #format()} set to
	 * <code>true</code> and the given {@link #blockCount()} and {@link #blockSize()}
	 * 
	 * @param path       the path of the patr file system
	 * @param blockCount the number of blocks available for the patr file system
	 * @param blockSize  the size of each block available for the patr file system
	 */
	public PatrFSOptions(String path, long blockCount, int blockSize) {
		this(path, true, blockCount, blockSize);
	}
	
}

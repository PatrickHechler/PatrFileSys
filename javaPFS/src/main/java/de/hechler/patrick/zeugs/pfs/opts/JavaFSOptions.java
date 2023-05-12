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

import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;

import de.hechler.patrick.zeugs.pfs.FSProvider;
import de.hechler.patrick.zeugs.pfs.interfaces.FSOptions;

/**
 * the {@link FSOptions} which need to be used, when creating an File System for
 * the java wrapping provider
 * <p>
 * it is save to use a Path, which is not from the default
 * {@link FileSystemProvider}
 * 
 * @author pat
 * 
 * @param root                    the root path of the file system
 * @param allowReuseAlreadyLoaded if an already loaded FileSystem should be
 *                                used if the path is already open or if the
 *                                load operation should fail
 * 								
 * @see FSProvider#JAVA_FS_PROVIDER_NAME
 */
public record JavaFSOptions(Path root, boolean allowReuseAlreadyLoaded) implements FSOptions {
	
	/**
	 * creates new {@link JavaFSOptions} with the given options
	 * 
	 * @param root                    the root path of the file system
	 * @param allowReuseAlreadyLoaded if an already loaded FileSystem should be
	 *                                used if the path is already open or if the
	 *                                load operation should fail
	 */
	public JavaFSOptions(Path root, boolean allowReuseAlreadyLoaded) {
		this.root                    = root.normalize().toAbsolutePath();
		this.allowReuseAlreadyLoaded = allowReuseAlreadyLoaded;
	}
	
	/**
	 * creates new {@link JavaFSOptions} with the given path and
	 * {@link #allowReuseAlreadyLoaded()} set to <code>true</code>
	 * 
	 * @param root the root {@link Path} of the file system
	 */
	public JavaFSOptions(Path root) {
		this(root, true);
	}
	
}

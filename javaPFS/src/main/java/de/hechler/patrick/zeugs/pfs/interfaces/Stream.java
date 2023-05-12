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
 * {@link Stream} can be used to transfer data
 * 
 * @author pat
 */
public interface Stream extends Closeable {

	/**
	 * returns the {@link StreamOpenOptions} of this {@link Stream}
	 * 
	 * @return the {@link StreamOpenOptions} of this {@link Stream}
	 */
	StreamOpenOptions options();

	/**
	 * changes the position of the stream to {@code pos}<br>
	 * if the stream is not {@link StreamOpenOptions#seekable()} this operation will
	 * fail
	 * <p>
	 * {@code pos} does not need to be inside of the file
	 * 
	 * @param pos the new position of the stream
	 * @throws IOException if an IO error occurs
	 */
	void seek(long pos) throws IOException;

	/**
	 * changes the position of the stream to the sum of the current position and
	 * {@code pos}<br>
	 * if the stream is not {@link StreamOpenOptions#seekable()} this operation will
	 * fail
	 * <p>
	 * the new position does not need to be inside of the file
	 * 
	 * @param add the new position of the stream
	 * @throws IOException if an IO error occurs
	 */
	long seekAdd(long add) throws IOException;

	/**
	 * returns the current position of the stream <br>
	 * if the stream is not {@link StreamOpenOptions#seekable()} this operation will
	 * fail
	 * 
	 * @return the current position of the stream
	 * @throws IOException if an IO error occurs
	 */
	default long position() throws IOException {
		return seekAdd(0L);
	}

	/**
	 * changes the current position of the stream to the end of the file <br>
	 * if the stream is not {@link StreamOpenOptions#seekable()} this operation will
	 * fail
	 * 
	 * @return the new position of the stream
	 * @throws IOException if an IO error occurs
	 */
	long seekEOF() throws IOException;

}

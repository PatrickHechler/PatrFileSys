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

import java.io.IOException;

import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

/**
 * the {@link File} interface provides a bunch of methods:
 * <ul>
 * <li>to get ({@link #length()} the files length</li>
 * <li>to set ({@link #truncate(long)} the files length</li>
 * <li>the content of the file can be read with {@link #openRead()}</li>
 * <li>to write to the file use {@link #openWrite()} and
 * {@link #openAppend()}</li>
 * <li>to read and write to the file use {@link #openReadWrite()}</li>
 * <li>also {@link #open(StreamOpenOptions)} can be used to open a stream with the
 * given options</li>
 * </ul>
 * 
 * @author pat
 */
public interface File extends FSElement {

	/**
	 * returns the current length of the file
	 * 
	 * @return the current length of the file
	 * @throws IOException if an IO error occurs
	 */
	long length() throws IOException;

	/**
	 * changes the length of the file. <br>
	 * all content after length will be removed from the file. <br>
	 * there will be zeros added to the file until the files length is
	 * {@code length}
	 * 
	 * @param length the new length of the file
	 * @throws IOException if an IO error occurs
	 */
	void truncate(long length) throws IOException;

	/**
	 * opens a {@link ReadStream} for this file
	 * <p>
	 * this call is like {@link #open(StreamOpenOptions)} with a {@link StreamOpenOptions}
	 * set to {@link StreamOpenOptions#read()}, but not {@link StreamOpenOptions#write()}
	 * <p>
	 * the stream will be {@link StreamOpenOptions#seekable()}
	 * 
	 * @return the opened {@link ReadStream}
	 * @throws IOException if an IO error occurs
	 */
	default ReadStream openRead() throws IOException {
		return (ReadStream) open(new StreamOpenOptions(true, false));
	}

	/**
	 * opens a {@link WriteStream} for this file
	 * <p>
	 * this call is like {@link #open(StreamOpenOptions)} with a {@link StreamOpenOptions}
	 * set to {@link StreamOpenOptions#write()}, but not {@link StreamOpenOptions#read()}
	 * <p>
	 * the stream will be {@link StreamOpenOptions#seekable()}
	 * 
	 * @return the opened {@link WriteStream}
	 * @throws IOException if an IO error occurs
	 */
	default WriteStream openWrite() throws IOException {
		return (WriteStream) open(new StreamOpenOptions(false, true));
	}

	/**
	 * opens a {@link ReadStream} for this file
	 * <p>
	 * this call is like {@link #open(StreamOpenOptions)} with a {@link StreamOpenOptions}
	 * set to {@link StreamOpenOptions#write()}, but not {@link StreamOpenOptions#read()}
	 * <p>
	 * the stream will not be {@link StreamOpenOptions#seekable()}
	 * 
	 * @return the opened {@link WriteStream}
	 * @throws IOException if an IO error occurs
	 */
	default WriteStream openAppend() throws IOException {
		return (WriteStream) open(new StreamOpenOptions(false, true, true));
	}

	/**
	 * opens a {@link Stream} for this file
	 * <p>
	 * this call is like {@link #open(StreamOpenOptions)} with a {@link StreamOpenOptions}
	 * set to {@link StreamOpenOptions#read()} and {@link StreamOpenOptions#write()}
	 * <p>
	 * the stream will be {@link StreamOpenOptions#seekable()}
	 * 
	 * @return the opened {@link Stream}
	 * @throws IOException if an IO error occurs
	 */
	default ReadWriteStream openReadWrite() throws IOException {
		return (ReadWriteStream) open(new StreamOpenOptions(true, true));
	}

	/**
	 * opens a new {@link Stream} for this file
	 * <p>
	 * the {@link StreamOpenOptions#seekable()} option will be ignored.
	 * 
	 * @param options the options for the stream
	 * @return the opened {@link Stream}
	 * @throws IOException if an IO error occurs
	 */
	Stream open(StreamOpenOptions options) throws IOException;

}

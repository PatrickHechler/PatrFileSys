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
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;

import de.hechler.patrick.zeugs.pfs.misc.ElementType;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

/**
 * a {@link ReadStream} is a stream with the {@link StreamOpenOptions#read()} set to <code>true</code>
 * <p>
 * a {@link ReadStream} provides methods to read data from the {@link Stream} with different types (byte[],
 * {@link ByteBuffer} and {@link MemorySegment})
 * 
 * @author pat
 */
public interface ReadStream extends Stream {
	
	/**
	 * read the next data.length bytes of the {@link Stream} and copy them to the array
	 * 
	 * @param data the buffer which should be filled with the Stream data
	 * 
	 * @return the number of bytes actually read may be less because of an error or end of file
	 * 
	 * @throws IOException if an IO error occurs
	 */
	default int read(byte[] data) throws IOException {
		// if read(byte[], int, int) is overwritten
		return read(data, 0, data.length);
	}
	
	/**
	 * read the next len bytes of the {@link Stream} and copy them to the array with the offset off
	 * 
	 * @param data the buffer which should be filled with the Stream data
	 * @param off  the offset of the data inside the array
	 * @param len  the number of bytes which should be read
	 * 
	 * @return the number of bytes actually read may be less because of an error or end of file
	 * 
	 * @throws IOException if an IO error occurs
	 */
	default int read(byte[] data, int off, int len) throws IOException {
		if (len < 0 || off < 0) {
			throw new IllegalArgumentException("off: " + off + " len: " + len);
		}
		if (data.length - off > len) {
			throw new IllegalArgumentException("off: " + off + " len: " + len + " arr.len: " + data.length);
		}
		try (Arena ses = Arena.openConfined()) {
			MemorySegment mem  = ses.allocate(len);
			long          reat = read(mem);
			assert reat <= len;
			int res = (int) reat;
			if (res == -1) return -1;
			MemorySegment.copy(mem, ValueLayout.JAVA_BYTE, 0, data, off, res);
			return res;
		}
	}
	
	/**
	 * read the next {@link ByteBuffer#remaining()} bytes to the {@link ByteBuffer} data
	 * 
	 * @param data the {@link ByteBuffer} which should be filled with the read data
	 * 
	 * @return the number of bytes actually read may be less because of an error or end of file
	 * 
	 * @throws IOException if an IO error occurs
	 */
	default int read(ByteBuffer data) throws IOException {
		MemorySegment seg = MemorySegment.ofBuffer(data);
		return (int) read(seg);
	}
	
	/**
	 * read the next {@link MemorySegment#byteSize()} bytes to the {@link MemorySegment} {@code seg}
	 * 
	 * @param seg the {@link MemorySegment} which should be filled with the read data
	 * 
	 * @return the number of bytes actually read may be less because of an error or end of file
	 * 
	 * @throws IOException if an IO error occurs
	 */
	long read(MemorySegment seg) throws IOException;
	
	/**
	 * return this read stream as an input stream
	 * 
	 * @return this read stream as an input stream
	 */
	default InputStream asInputStream() {
		return new InputStream() {
			
			@Override
			public int read() throws IOException {
				byte[] b = new byte[1];
				if (ReadStream.this.read(b, 0, b.length) <= 0) { return -1; }
				return b[0] & 0xFF;
			}
			
			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				int r = ReadStream.this.read(b, off, len);
				if (r <= 0) {
					return -1;
				}
				return r;
			}
			
			@Override
			public void close() throws IOException {
				ReadStream.this.close();
			}
			
			@Override
			public long skip(long n) throws IOException {
				if (options().type() == ElementType.FILE) {
					seekAdd(n); // may skip beyond EOF
					return n;
				} else {
					return super.skip(n);
				}
			}
			
		};
	}
	
}

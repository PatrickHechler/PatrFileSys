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
package de.hechler.patrick.zeugs.pfs.impl.pfs;

import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.LOCKUP;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.INT;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.LINKER;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.LONG;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.PNTR;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider.thrw;

import java.io.IOException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.nio.channels.ClosedChannelException;

import de.hechler.patrick.zeugs.pfs.interfaces.ReadStream;
import de.hechler.patrick.zeugs.pfs.interfaces.ReadWriteStream;
import de.hechler.patrick.zeugs.pfs.interfaces.Stream;
import de.hechler.patrick.zeugs.pfs.interfaces.WriteStream;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

/**
 * this class implements only the {@link Stream} interface, but also provides method which effectively implement the
 * {@link WriteStream} and {@link ReadStream} interface.
 * <p>
 * the three subclasses of this class implement the {@link ReadStream} or {@link WriteStream} (or {@link ReadWriteStream}) interfaces.
 * 
 * @author pat
 */
public abstract sealed class PatrStream implements Stream permits PatrWriteStream, PatrReadStream, PatrReadWriteStream {
	
	private boolean closed = false;
	
	private final StreamOpenOptions opts;
	
	private static final MethodHandle PFS_STREAM_CLOSE;
	private static final MethodHandle PFS_STREAM_WRITE;
	private static final MethodHandle PFS_STREAM_READ;
	private static final MethodHandle PFS_STREAM_GET_POS;
	private static final MethodHandle PFS_STREAM_SET_POS;
	private static final MethodHandle PFS_STREAM_ADD_POS;
	private static final MethodHandle PFS_STREAM_SEEK_EOF;
	
	static {
		PFS_STREAM_CLOSE = LINKER.downcallHandle(LOCKUP.find("pfs_stream_close").orElseThrow(),
				FunctionDescriptor.of(INT, INT));
		PFS_STREAM_WRITE = LINKER.downcallHandle(LOCKUP.find("pfs_stream_write").orElseThrow(),
				FunctionDescriptor.of(LONG, INT, PNTR, LONG));
		PFS_STREAM_READ = LINKER.downcallHandle(LOCKUP.find("pfs_stream_read").orElseThrow(),
				FunctionDescriptor.of(LONG, INT, PNTR, LONG));
		PFS_STREAM_GET_POS = LINKER.downcallHandle(LOCKUP.find("pfs_stream_get_pos").orElseThrow(),
				FunctionDescriptor.of(INT, INT));
		PFS_STREAM_SET_POS = LINKER.downcallHandle(LOCKUP.find("pfs_stream_set_pos").orElseThrow(),
				FunctionDescriptor.of(INT, INT, LONG));
		PFS_STREAM_ADD_POS = LINKER.downcallHandle(LOCKUP.find("pfs_stream_add_pos").orElseThrow(),
				FunctionDescriptor.of(LONG, INT, LONG));
		PFS_STREAM_SEEK_EOF = LINKER.downcallHandle(LOCKUP.find("pfs_stream_seek_eof").orElseThrow(),
				FunctionDescriptor.of(LONG, INT));
	}
	
	private final int handle;
	
	protected PatrStream(int handle, StreamOpenOptions opts) {
		this.opts = opts;
		this.handle = handle;
	}
	
	/**
	 * implementation of the write method for subclasses which implement the {@link WriteStream} interface
	 * <p>
	 * note that the native implementation will fail if this stream is not opened for writing
	 * 
	 * @param seg the segment, which holds the data, which should be written to this stream
	 * 
	 * @return the number of bytes written by the stream
	 * 
	 * @throws IOException if an error occurs
	 * 
	 * @see WriteStream#write(MemorySegment)
	 */
	public long write(MemorySegment seg) throws IOException {
		if (closed) { throw new ClosedChannelException(); }
		try {
			long res = (long) PFS_STREAM_WRITE.invoke(this.handle, seg, seg.byteSize());
			if (res == -1L) { throw thrw(PFSErrorCause.WRITE, seg.byteSize()); }
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	/**
	 * implementation of the read method for subclasses which implement the {@link ReadStream} interface
	 * <p>
	 * note that the native implementation will fail if this stream is not opened for reading
	 * 
	 * @param seg the memory segment, where the data should be saved from this stream
	 * 
	 * @return the number of bytes read by the stream
	 * 
	 * @throws IOException if an error occurs
	 * 
	 * @see ReadStream#read(MemorySegment)
	 */
	public long read(MemorySegment seg) throws IOException {
		if (closed) { throw new ClosedChannelException(); }
		try {
			long res = (long) PFS_STREAM_READ.invoke(this.handle, seg, seg.byteSize());
			if (res == -1L) throw thrw(PFSErrorCause.READ, seg.byteSize());
			else if (res == 0) return -1;
			else return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public StreamOpenOptions options() {
		return opts;
	}
	
	@Override
	public void seek(long pos) throws IOException {
		if (closed) { throw new ClosedChannelException(); }
		try {
			if (0 == (int) PFS_STREAM_SET_POS.invoke(this.handle, pos)) { throw thrw(PFSErrorCause.SET_POS, null); }
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public long seekAdd(long add) throws IOException {
		if (closed) { throw new ClosedChannelException(); }
		try {
			long res = (long) PFS_STREAM_ADD_POS.invoke(this.handle, add);
			if (res == -1L) { throw thrw(PFSErrorCause.ADD_POS, null); }
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public long position() throws IOException {
		if (closed) { throw new ClosedChannelException(); }
		try {
			long res = (long) PFS_STREAM_GET_POS.invoke(this.handle);
			if (res == -1L) { throw thrw(PFSErrorCause.GET_POS, null); }
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public long seekEOF() throws IOException {
		if (closed) { throw new ClosedChannelException(); }
		try {
			long res = (long) PFS_STREAM_SEEK_EOF.invoke(this.handle);
			if (res == -1L) { throw thrw(PFSErrorCause.SEEK_EOF, null); }
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void close() throws IOException {
		if (closed) { return; }
		closed = true;
		try {
			if (0 == (int) PFS_STREAM_CLOSE.invoke(this.handle)) { throw thrw(PFSErrorCause.CLOSE_STREAM, null); }
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
}

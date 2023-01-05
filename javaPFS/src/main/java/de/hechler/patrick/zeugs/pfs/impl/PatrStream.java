package de.hechler.patrick.zeugs.pfs.impl;

import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.INT;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.LINKER;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.LONG;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.PNTR;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFSProvider.loaded;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFSProvider.thrw;

import java.io.IOException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.nio.channels.ClosedChannelException;

import de.hechler.patrick.zeugs.pfs.interfaces.ReadStream;
import de.hechler.patrick.zeugs.pfs.interfaces.Stream;
import de.hechler.patrick.zeugs.pfs.interfaces.WriteStream;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

/**
 * this class implements only the {@link Stream} interface, but also provides
 * method which effectively implement the {@link WriteStream} and
 * {@link ReadStream} interface.
 * <p>
 * the three subclasses of this class implement the {@link ReadStream} and/or
 * {@link WriteStream} interfaces.
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
		PFS_STREAM_CLOSE    = LINKER.downcallHandle(loaded.lockup.lookup("pfs_stream_close").orElseThrow(), FunctionDescriptor.of(INT, INT));
		PFS_STREAM_WRITE    = LINKER.downcallHandle(loaded.lockup.lookup("pfs_stream_write").orElseThrow(),
				FunctionDescriptor.of(LONG, INT, PNTR, LONG));
		PFS_STREAM_READ     = LINKER.downcallHandle(loaded.lockup.lookup("pfs_stream_read").orElseThrow(),
				FunctionDescriptor.of(LONG, INT, PNTR, LONG));
		PFS_STREAM_GET_POS  = LINKER.downcallHandle(loaded.lockup.lookup("pfs_stream_get_pos").orElseThrow(), FunctionDescriptor.of(INT, INT));
		PFS_STREAM_SET_POS  = LINKER.downcallHandle(loaded.lockup.lookup("pfs_stream_set_pos").orElseThrow(), FunctionDescriptor.of(INT, INT, LONG));
		PFS_STREAM_ADD_POS  = LINKER.downcallHandle(loaded.lockup.lookup("pfs_stream_add_pos").orElseThrow(), FunctionDescriptor.of(LONG, INT, LONG));
		PFS_STREAM_SEEK_EOF = LINKER.downcallHandle(loaded.lockup.lookup("pfs_stream_seek_eof").orElseThrow(), FunctionDescriptor.of(LONG, INT));
	}
	
	private final int handle;
	
	protected PatrStream(int handle, StreamOpenOptions opts) {
		this.opts   = opts;
		this.handle = handle;
	}
	
	/**
	 * implementation of the write method for subclasses which implement the
	 * {@link WriteStream} interface
	 * <p>
	 * note that the native implementation will fail if this stream is not opened
	 * for writing
	 * 
	 * @param seg the segment, which holds the data, which should be written to this
	 *            stream
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
			if (res == -1L) { throw thrw(loaded.lockup, "write " + seg.byteSize() + " bytes"); }
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	
	/**
	 * implementation of the read method for subclasses which implement the
	 * {@link ReadStream} interface
	 * <p>
	 * note that the native implementation will fail if this stream is not opened
	 * for reading
	 * 
	 * @param seg the memory segment, where the data should be saved from this
	 *            stream
	 * 
	 * @return the number of bytes read by the stream
	 * 
	 * @throws IOException if an error occurs
	 * 
	 * @see ReadStream#write(MemorySegment)
	 */
	public long read(MemorySegment seg) throws IOException {
		if (closed) { throw new ClosedChannelException(); }
		try {
			long res = (long) PFS_STREAM_READ.invoke(this.handle, seg, seg.byteSize());
			if (res == -1L) { throw thrw(loaded.lockup, "read " + seg.byteSize() + " bytes"); }
			return res;
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
			if (0 == (int) PFS_STREAM_SET_POS.invoke(this.handle, pos)) { throw thrw(loaded.lockup, "set pos"); }
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public long seekAdd(long add) throws IOException {
		if (closed) { throw new ClosedChannelException(); }
		try {
			long res = (long) PFS_STREAM_ADD_POS.invoke(this.handle, add);
			if (res == -1L) { throw thrw(loaded.lockup, "add pos"); }
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
			if (res == -1L) { throw thrw(loaded.lockup, "get pos"); }
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
			if (res == -1L) { throw thrw(loaded.lockup, "seek eof"); }
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
			if (0 == (int) PFS_STREAM_CLOSE.invoke(this.handle)) { throw thrw(loaded.lockup, "close"); }
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
}

package de.hechler.patrick.zeugs.pfs.impl;

import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.INT;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.LONG;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.PNTR;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.LINKER;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFSProvider.loaded;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFSProvider.thrw;

import java.io.IOException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.nio.channels.ClosedChannelException;

import de.hechler.patrick.zeugs.pfs.interfaces.Stream;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

public abstract class PatrStream implements Stream {

	private boolean closed = false;

	private final StreamOpenOptions opts;

	private final MethodHandle pfs_stream_close;
	private final MethodHandle pfs_stream_write;
	private final MethodHandle pfs_stream_read;
	private final MethodHandle pfs_stream_get_pos;
	private final MethodHandle pfs_stream_set_pos;
	private final MethodHandle pfs_stream_add_pos;
	private final MethodHandle pfs_stream_seek_eof;

	public PatrStream(int handle, StreamOpenOptions opts) {
		this.opts = opts;

		this.pfs_stream_close = LINKER
				.downcallHandle(loaded.lockup.lookup("pfs_stream_close").orElseThrow(), FunctionDescriptor.of(INT, INT))
				.bindTo(handle);
		this.pfs_stream_write = LINKER.downcallHandle(loaded.lockup.lookup("pfs_stream_write").orElseThrow(),
				FunctionDescriptor.of(LONG, INT, PNTR, LONG)).bindTo(handle);
		this.pfs_stream_read = LINKER.downcallHandle(loaded.lockup.lookup("pfs_stream_read").orElseThrow(),
				FunctionDescriptor.of(LONG, INT, PNTR, LONG)).bindTo(handle);
		this.pfs_stream_get_pos = LINKER.downcallHandle(loaded.lockup.lookup("pfs_stream_get_pos").orElseThrow(),
				FunctionDescriptor.of(INT, INT)).bindTo(handle);
		this.pfs_stream_set_pos = LINKER.downcallHandle(loaded.lockup.lookup("pfs_stream_set_pos").orElseThrow(),
				FunctionDescriptor.of(INT, INT, LONG)).bindTo(handle);
		this.pfs_stream_add_pos = LINKER.downcallHandle(loaded.lockup.lookup("pfs_stream_add_pos").orElseThrow(),
				FunctionDescriptor.of(LONG, INT, LONG)).bindTo(handle);
		this.pfs_stream_seek_eof = LINKER.downcallHandle(loaded.lockup.lookup("pfs_stream_seek_eof").orElseThrow(),
				FunctionDescriptor.of(LONG, INT)).bindTo(handle);
	}

	public long write(MemorySegment seg) throws IOException {
		if (closed) {
			throw new ClosedChannelException();
		}
		try {
			long res = (long) pfs_stream_write.invoke(seg, seg.byteSize());
			if (res == -1L) {
				throw thrw(loaded.lockup, "write " + seg.byteSize() + " bytes");
			}
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	public long read(MemorySegment seg) throws IOException {
		if (closed) {
			throw new ClosedChannelException();
		}
		try {
			long res = (long) pfs_stream_read.invoke(seg, seg.byteSize());
			if (res == -1L) {
				throw thrw(loaded.lockup, "read " + seg.byteSize() + " bytes");
			}
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
		if (closed) {
			throw new ClosedChannelException();
		}
		try {
			if (0 == (int) pfs_stream_set_pos.invoke(pos)) {
				throw thrw(loaded.lockup, "set pos");
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public long seekAdd(long add) throws IOException {
		if (closed) {
			throw new ClosedChannelException();
		}
		try {
			long res = (long) pfs_stream_add_pos.invoke(add);
			if (res == -1L) {
				throw thrw(loaded.lockup, "add pos");
			}
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public long position() throws IOException {
		if (closed) {
			throw new ClosedChannelException();
		}
		try {
			long res = (long) pfs_stream_get_pos.invoke();
			if (res == -1L) {
				throw thrw(loaded.lockup, "get pos");
			}
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public long seekEOF() throws IOException {
		if (closed) {
			throw new ClosedChannelException();
		}
		try {
			long res = (long) pfs_stream_seek_eof.invoke();
			if (res == -1L) {
				throw thrw(loaded.lockup, "seek eof");
			}
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public void close() throws IOException {
		if (closed) {
			return;
		}
		closed = true;
		try {
			if (0 == (int) pfs_stream_close.invoke()) {
				throw thrw(loaded.lockup, "close");
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

}

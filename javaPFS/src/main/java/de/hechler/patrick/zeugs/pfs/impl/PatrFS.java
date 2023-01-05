package de.hechler.patrick.zeugs.pfs.impl;

import static de.hechler.patrick.zeugs.pfs.impl.PatrFSProvider.thrw;

import java.io.IOException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.ValueLayout.OfAddress;
import java.lang.foreign.ValueLayout.OfInt;
import java.lang.foreign.ValueLayout.OfLong;
import java.lang.invoke.MethodHandle;
import java.nio.channels.ClosedChannelException;

import de.hechler.patrick.zeugs.pfs.interfaces.FS;
import de.hechler.patrick.zeugs.pfs.interfaces.FSElement;
import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Folder;
import de.hechler.patrick.zeugs.pfs.interfaces.Pipe;
import de.hechler.patrick.zeugs.pfs.interfaces.Stream;
import de.hechler.patrick.zeugs.pfs.misc.ElementType;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

public class PatrFS implements FS {

	static final OfLong LONG = ValueLayout.JAVA_LONG;
	static final OfInt INT = ValueLayout.JAVA_INT;
	static final OfAddress PNTR = ValueLayout.ADDRESS;

	static final Linker LINKER = Linker.nativeLinker();

	final MemorySession session;
	final SymbolLookup lockup;

	private boolean closed;

	private final MethodHandle pfs_block_count;
	private final MethodHandle pfs_block_size;
	private final MethodHandle pfs_handle;
	private final MethodHandle pfs_handle_folder;
	private final MethodHandle pfs_handle_file;
	private final MethodHandle pfs_handle_pipe;
	private final MethodHandle pfs_change_dir;
	private final MethodHandle pfs_stream;
	private final MethodHandle pfs_element_close;
	private final MethodHandle pfs_stream_close;
	private final MethodHandle pfs_element_get_flags;
	private final MethodHandle pfs_close;

	public PatrFS(MemorySession session, SymbolLookup lockup) {
		this.session = session;
		this.lockup = lockup;

		this.pfs_block_count = LINKER.downcallHandle(lockup.lookup("pfs_block_count").orElseThrow(),
				FunctionDescriptor.of(LONG));
		this.pfs_block_size = LINKER.downcallHandle(lockup.lookup("pfs_block_size").orElseThrow(),
				FunctionDescriptor.of(INT));
		this.pfs_handle = LINKER.downcallHandle(lockup.lookup("pfs_handle").orElseThrow(),
				FunctionDescriptor.of(INT, PNTR));
		this.pfs_handle_folder = LINKER.downcallHandle(lockup.lookup("pfs_handle_folder").orElseThrow(),
				FunctionDescriptor.of(INT, PNTR));
		this.pfs_handle_file = LINKER.downcallHandle(lockup.lookup("pfs_handle_file").orElseThrow(),
				FunctionDescriptor.of(INT, PNTR));
		this.pfs_handle_pipe = LINKER.downcallHandle(lockup.lookup("pfs_handle_pipe").orElseThrow(),
				FunctionDescriptor.of(INT, PNTR));
		this.pfs_change_dir = LINKER.downcallHandle(lockup.lookup("pfs_change_dir").orElseThrow(),
				FunctionDescriptor.of(INT, INT));
//		this.pfs_change_working_directoy = LINKER.downcallHandle(lockup.lookup("pfs_change_working_directoy").orElseThrow(),
//				FunctionDescriptor.of(INT, PNTR));
//		this.pfs_delete = LINKER.downcallHandle(lockup.lookup("pfs_delete").orElseThrow(),
//				FunctionDescriptor.of(INT, PNTR));
		this.pfs_stream = LINKER.downcallHandle(lockup.lookup("pfs_stream").orElseThrow(),
				FunctionDescriptor.of(INT, PNTR));
//		this.pfs_iter = LINKER.downcallHandle(lockup.lookup("pfs_iter").orElseThrow(),
//				FunctionDescriptor.of(INT, PNTR));
		this.pfs_stream_close = LINKER.downcallHandle(lockup.lookup("pfs_stream_close").orElseThrow(),
				FunctionDescriptor.of(INT, INT));
		this.pfs_element_close = LINKER.downcallHandle(lockup.lookup("pfs_element_close").orElseThrow(),
				FunctionDescriptor.of(INT, INT));
		this.pfs_element_get_flags = LINKER.downcallHandle(lockup.lookup("pfs_element_get_flags").orElseThrow(),
				FunctionDescriptor.of(INT, INT));
		this.pfs_close = LINKER.downcallHandle(lockup.lookup("pfs_close").orElseThrow(), FunctionDescriptor.of(INT));
	}

	@Override
	public long blockCount() throws IOException {
		if (closed) {
			throw new ClosedChannelException();
		}
		try {
			long res = (long) pfs_block_count.invoke();
			if (res == -1L) {
				throw thrw(lockup, "get the block count");
			}
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public int blockSize() throws IOException {
		if (closed) {
			throw new ClosedChannelException();
		}
		try {
			int res = (int) pfs_block_size.invoke();
			if (res == -1) {
				throw thrw(lockup, "get the block size");
			}
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public FSElement element(String path) throws IOException {
		if (closed) {
			throw new ClosedChannelException();
		}
		try {
			try (MemorySession ses = MemorySession.openConfined()) {
				int res = (int) pfs_handle.invoke(ses.allocateUtf8String(path));
				if (res == -1) {
					throw thrw(lockup, "get the element '" + path + "'");
				}
				return new PatrFSElement(res);
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public Folder folder(String path) throws IOException {
		if (closed) {
			throw new ClosedChannelException();
		}
		try {
			try (MemorySession ses = MemorySession.openConfined()) {
				int res = (int) pfs_handle_folder.invoke(ses.allocateUtf8String(path));
				if (res == -1) {
					throw thrw(lockup, "get the element '" + path + "'");
				}
				return new PatrFolder(res);
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public File file(String path) throws IOException {
		if (closed) {
			throw new ClosedChannelException();
		}
		try {
			try (MemorySession ses = MemorySession.openConfined()) {
				int res = (int) pfs_handle_file.invoke(ses.allocateUtf8String(path));
				if (res == -1) {
					throw thrw(lockup, "get the element '" + path + "'");
				}
				return new PatrFile(res);
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public Pipe pipe(String path) throws IOException {
		if (closed) {
			throw new ClosedChannelException();
		}
		try {
			try (MemorySession ses = MemorySession.openConfined()) {
				int res = (int) pfs_handle_pipe.invoke(ses.allocateUtf8String(path));
				if (res == -1) {
					throw thrw(lockup, "get the element '" + path + "'");
				}
				return new PatrPipe(res);
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	static final int SO_CREATE_ONLY = 0x00000001;
	static final int SO_ALSO_CREATE = 0x00000002;
	static final int SO_FILE = 0x00000004;
	static final int SO_PIPE = 0x00000008;
	static final int SO_READ = 0x00000100;
	static final int SO_WRITE = 0x00000200;
	static final int SO_APPEND = 0x00000400;
	static final int SO_FILE_TRUNC = 0x00010000;
	static final int SO_FILE_EOF = 0x00020000;

	public Stream stream(String path, StreamOpenOptions opts) throws IOException {
		if (closed) {
			throw new ClosedChannelException();
		}
		try {
			try (MemorySession ses = MemorySession.openConfined()) {
				int o = 0;
				if (opts.createOnly()) {
					o |= SO_CREATE_ONLY;
				} else if (opts.createAlso()) {
					o |= SO_ALSO_CREATE;
				}
				if (opts.read()) {
					o |= SO_READ;
				}
				if (opts.append()) {
					o |= SO_APPEND;
				} else if (opts.write()) {
					o |= SO_WRITE;
				}
				MemorySegment pathSeg = ses.allocateUtf8String(path);
				int res = (int) pfs_stream.invoke(pathSeg, o);
				if (res == -1) {
					throw thrw(lockup, "open stream for the element '" + path + "'");
				}
				try {
					if (opts.type() == null) {
						int handle = (int) pfs_handle.invoke(pathSeg);
						if (handle == -1) {
							throw thrw(lockup, "find out type of stream (get element handle)");
						}
						try {
							int flags = (int) pfs_element_get_flags.invoke(handle);
							if (flags == -1) {
								throw thrw(lockup, "find out type of stream (get flags)");
							}
							if ((flags & FSElement.FLAG_FILE) != 0) {
								opts = opts.setType(ElementType.file);
							} else if ((flags & FSElement.FLAG_PIPE) != 0) {
								opts = opts.setType(ElementType.pipe);
							} else {
								throw new InternalError("unknown element type: flags=0x" + Integer.toHexString(flags));
							}
						} finally {
							@SuppressWarnings("unused")
							int ignore = (int) pfs_element_close.invoke(handle);
						}
					}
					if (opts.read()) {
						if (opts.write()) {
							return new PatrReadWriteStream(res, opts);
						} else {
							return new PatrReadStream(res, opts);
						}
					} else {
						return new PatrWriteStream(res, opts);
					}
				} catch (Throwable t) {
					@SuppressWarnings("unused")
					int ignore = (int) pfs_stream_close.invoke(res);
					throw t;
				}
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public Folder cwd() throws IOException {
		return folder("./");
	}

	@Override
	public void cwd(Folder f) throws IOException {
		if (closed) {
			throw new ClosedChannelException();
		}
		try {
			if (f instanceof PatrFolder pf) {
				if (0 == (int) pfs_change_dir.invoke(pf.handle)) {
					throw thrw(lockup, "change working directory");
				}
			} else {
				throw new ClassCastException("the folder is not of type PatrFolder");
			}
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
			if (0 == (int) pfs_close.invoke()) {
				throw thrw(lockup, "close patr file system");
			}
		} catch (Throwable e) {
			session.close();
			throw thrw(e);
		}
		session.close();
	}

}

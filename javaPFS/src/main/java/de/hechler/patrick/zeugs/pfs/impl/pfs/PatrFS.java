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

import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider.thrw;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.ValueLayout.OfAddress;
import java.lang.foreign.ValueLayout.OfInt;
import java.lang.foreign.ValueLayout.OfLong;
import java.lang.invoke.MethodHandle;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Paths;

import de.hechler.patrick.zeugs.pfs.interfaces.FS;
import de.hechler.patrick.zeugs.pfs.interfaces.FSElement;
import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Folder;
import de.hechler.patrick.zeugs.pfs.interfaces.Pipe;
import de.hechler.patrick.zeugs.pfs.interfaces.Stream;
import de.hechler.patrick.zeugs.pfs.misc.ElementType;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

public class PatrFS implements FS {
	
	static final OfLong    LONG = ValueLayout.JAVA_LONG;
	static final OfInt     INT  = ValueLayout.JAVA_INT;
	static final OfAddress PNTR = ValueLayout.ADDRESS;
	
	static final Linker LINKER = Linker.nativeLinker();
	
	static final SymbolLookup LOCKUP;
	
	private static final MethodHandle PFS_BLOCK_COUNT;
	private static final MethodHandle PFS_BLOCK_SIZE;
	private static final MethodHandle PFS_HANDLE;
	private static final MethodHandle PFS_HANDLE_FOLDER;
	private static final MethodHandle PFS_HANDLE_FILE;
	private static final MethodHandle PFS_HANDLE_PIPE;
	private static final MethodHandle PFS_CHANGE_DIR;
	private static final MethodHandle PFS_STREAM;
	private static final MethodHandle PFS_ELEMENT_CLOSE;
	private static final MethodHandle PFS_STREAM_CLOSE;
	private static final MethodHandle PFS_ELEMENT_GET_FLAGS;
	private static final MethodHandle PFS_CLOSE;
	
	static {
		SymbolLookup lockup;
		try {
			lockup = SymbolLookup.libraryLookup("pfs-core", SegmentScope.global());
		} catch (@SuppressWarnings("unused") IllegalArgumentException e) {
			try {
				lockup = SymbolLookup.libraryLookup(Paths.get("lib/libpfs-core.so").toAbsolutePath().toString(),
					SegmentScope.global());
			} catch (@SuppressWarnings("unused") IllegalArgumentException e2) {
				lockup = SymbolLookup.libraryLookup("/lib/libpfs-core.so", SegmentScope.global());
			}
		}
		LOCKUP = lockup;
		PFS_BLOCK_COUNT = LINKER.downcallHandle(lockup.find("pfs_block_count").orElseThrow(),
				FunctionDescriptor.of(LONG));
		PFS_BLOCK_SIZE = LINKER.downcallHandle(lockup.find("pfs_block_size").orElseThrow(),
				FunctionDescriptor.of(INT));
		PFS_HANDLE = LINKER.downcallHandle(lockup.find("pfs_handle").orElseThrow(), FunctionDescriptor.of(INT, PNTR));
		PFS_HANDLE_FOLDER = LINKER.downcallHandle(lockup.find("pfs_handle_folder").orElseThrow(),
				FunctionDescriptor.of(INT, PNTR));
		PFS_HANDLE_FILE = LINKER.downcallHandle(lockup.find("pfs_handle_file").orElseThrow(),
				FunctionDescriptor.of(INT, PNTR));
		PFS_HANDLE_PIPE = LINKER.downcallHandle(lockup.find("pfs_handle_pipe").orElseThrow(),
				FunctionDescriptor.of(INT, PNTR));
		PFS_CHANGE_DIR = LINKER.downcallHandle(lockup.find("pfs_change_dir").orElseThrow(),
				FunctionDescriptor.of(INT, INT));
		PFS_STREAM = LINKER.downcallHandle(lockup.find("pfs_stream").orElseThrow(),
				FunctionDescriptor.of(INT, PNTR, INT));
		PFS_STREAM_CLOSE = LINKER.downcallHandle(lockup.find("pfs_stream_close").orElseThrow(),
				FunctionDescriptor.of(INT, INT));
		PFS_ELEMENT_CLOSE = LINKER.downcallHandle(lockup.find("pfs_element_close").orElseThrow(),
				FunctionDescriptor.of(INT, INT));
		PFS_ELEMENT_GET_FLAGS = LINKER.downcallHandle(lockup.find("pfs_element_get_flags").orElseThrow(),
				FunctionDescriptor.of(INT, INT));
		PFS_CLOSE = LINKER.downcallHandle(lockup.find("pfs_close").orElseThrow(), FunctionDescriptor.of(INT));
	}
	
	final Arena session;
	
	private boolean closed;
	
	public PatrFS(Arena session) { this.session = session; }
	
	@Override
	public long blockCount() throws IOException {
		if (this.closed) { throw new ClosedChannelException(); }
		try {
			long res = (long) PFS_BLOCK_COUNT.invoke();
			if (res == -1L) { throw thrw(PFSErrorCause.GET_BLOCK_COUNT, null); }
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public int blockSize() throws IOException {
		if (this.closed) { throw new ClosedChannelException(); }
		try {
			int res = (int) PFS_BLOCK_SIZE.invoke();
			if (res == -1) { throw thrw(PFSErrorCause.GET_BLOCK_SIZE, null); }
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public FSElement element(String path) throws IOException {
		if (this.closed) { throw new ClosedChannelException(); }
		try {
			try (Arena ses = Arena.openConfined()) {
				int res = (int) PFS_HANDLE.invoke(ses.allocateUtf8String(path));
				if (res == -1) { throw thrw(PFSErrorCause.GET_ELEMENT, path); }
				return new PatrFSElement(res);
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Folder folder(String path) throws IOException {
		if (this.closed) { throw new ClosedChannelException(); }
		try {
			try (Arena ses = Arena.openConfined()) {
				int res = (int) PFS_HANDLE_FOLDER.invoke(ses.allocateUtf8String(path));
				if (res == -1) { throw thrw(PFSErrorCause.GET_ELEMENT, path); }
				return new PatrFolder(res);
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public File file(String path) throws IOException {
		if (this.closed) { throw new ClosedChannelException(); }
		try {
			try (Arena ses = Arena.openConfined()) {
				int res = (int) PFS_HANDLE_FILE.invoke(ses.allocateUtf8String(path));
				if (res == -1) { throw thrw(PFSErrorCause.GET_ELEMENT, path); }
				return new PatrFile(res);
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Pipe pipe(String path) throws IOException {
		if (this.closed) { throw new ClosedChannelException(); }
		try {
			try (Arena ses = Arena.openConfined()) {
				int res = (int) PFS_HANDLE_PIPE.invoke(ses.allocateUtf8String(path));
				if (res == -1) { throw thrw(PFSErrorCause.GET_ELEMENT, path); }
				return new PatrPipe(res);
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	static final int SO_CREATE_ONLY = 0x00000001;
	static final int SO_ALSO_CREATE = 0x00000002;
	static final int SO_FILE        = 0x00000004;
	static final int SO_PIPE        = 0x00000008;
	static final int SO_READ        = 0x00000100;
	static final int SO_WRITE       = 0x00000200;
	static final int SO_APPEND      = 0x00000400;
	static final int SO_FILE_TRUNC  = 0x00010000;
	static final int SO_FILE_EOF    = 0x00020000;
	
	public Stream stream(String path, StreamOpenOptions opts) throws IOException {
		if (this.closed) { throw new ClosedChannelException(); }
		try {
			try (Arena ses = Arena.openConfined()) {
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
				if (opts.truncate()) {
					o |= SO_FILE_TRUNC;
				}
				if (opts.type() != null) {
					switch (opts.type()) {
					case FILE -> o |= SO_FILE;
					case PIPE -> o |= SO_PIPE;
					case FOLDER -> throw new AssertionError("stream options with type folder");
					default -> throw new InternalError("unknown type: " + opts.type().name());
					}
				}
				MemorySegment pathSeg = ses.allocateUtf8String(path);
				int           res     = (int) PFS_STREAM.invoke(pathSeg, o);
				if (res == -1) { throw thrw(PFSErrorCause.OPEN_STREAM, path); }
				try {
					if (opts.type() == null) {
						int handle = (int) PFS_HANDLE.invoke(pathSeg);
						if (handle == -1) { throw thrw(PFSErrorCause.GET_ELEMENT, path); }
						try {
							int flags = (int) PFS_ELEMENT_GET_FLAGS.invoke(handle);
							if (flags == -1) { throw thrw(PFSErrorCause.GET_FLAGS, path); }
							if ((flags & FSElement.FLAG_FILE) != 0) {
								opts = opts.ensureType(ElementType.FILE);
							} else if ((flags & FSElement.FLAG_PIPE) != 0) {
								opts = opts.ensureType(ElementType.PIPE);
							} else {
								throw new InternalError("unknown element type: flags=0x" + Integer.toHexString(flags));
							}
						} finally {
							@SuppressWarnings("unused")
							int ignore = (int) PFS_ELEMENT_CLOSE.invoke(handle);
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
					int ignore = (int) PFS_STREAM_CLOSE.invoke(res);
					throw t;
				}
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Folder cwd() throws IOException { return folder("./"); }
	
	@Override
	public void cwd(Folder f) throws IOException {
		if (this.closed) { throw new ClosedChannelException(); }
		try {
			if (f instanceof PatrFolder pf) {
				if (0 == (int) PFS_CHANGE_DIR.invoke(pf.handle)) { throw thrw(PFSErrorCause.SET_CWD, null); }
			} else {
				throw new ClassCastException("the folder is not of type PatrFolder");
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void close() throws IOException {
		if (this.closed) { return; }
		this.closed = true;
		PatrFSProvider.unload(this);
		try {
			if (0 == (int) PFS_CLOSE.invoke()) {
				throw thrw(PFSErrorCause.CLOSE_PFS, null);
			}
		} catch (Throwable e) {
			this.session.close();
			throw thrw(e);
		}
		this.session.close();
	}
	
}

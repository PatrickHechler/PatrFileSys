// This file is part of the Patr File System Project
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
// Copyright (C) 2023 Patrick Hechler
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
package de.hechler.patrick.zeugs.pfs.impl.pfs;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.ValueLayout.OfAddress;
import java.lang.foreign.ValueLayout.OfInt;
import java.lang.foreign.ValueLayout.OfLong;
import java.lang.invoke.MethodHandle;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import de.hechler.patrick.zeugs.pfs.FSProvider;
import de.hechler.patrick.zeugs.pfs.interfaces.FS;
import de.hechler.patrick.zeugs.pfs.interfaces.FSOptions;
import de.hechler.patrick.zeugs.pfs.opts.PatrFSOptions;
import de.hechler.patrick.zeugs.pfs.opts.PatrRamFSOpts;

/**
 * this class implements the File System interfaces by delegating to the linux native Patr-FileSystem API <br>
 * the {@link #name()} of this provider is {@value FSProvider#PATR_FS_PROVIDER_NAME}
 * 
 * @author pat
 * 
 * @see FSProvider#PATR_FS_PROVIDER_NAME
 */
@SuppressWarnings("javadoc")
public class PatrFSProvider extends FSProvider {
	
	static final OfLong    LONG = ValueLayout.JAVA_LONG;
	static final OfInt     INT  = ValueLayout.JAVA_INT;
	static final OfAddress PNTR = ValueLayout.ADDRESS;
	
	static PatrFS loaded;
	
	private volatile boolean exists = false;
	
	public PatrFSProvider() {
		super(FSProvider.PATR_FS_PROVIDER_NAME, 1);
		synchronized (PatrFSProvider.class) {
			if (this.exists) { throw new IllegalStateException("this class is only allowed to be created once"); }
			this.exists = true;
		}
	}
	
	@Override
	public FS loadFS(FSOptions fso) throws IOException {
		if (fso == null) { throw new NullPointerException("options are null!"); }
		synchronized (this) {
			if (loaded != null) {
				throw new IllegalStateException("maximum amount of file systems has been loaded! (max amount: 1)");
			}
			Arena session = Arena.openConfined();
			try {
				Linker linker = Linker.nativeLinker();
				try (Arena local = Arena.openConfined()) {
					if (fso instanceof PatrFSOptions opts) {
						loadPatrOpts(linker, local, opts);
					} else if (fso instanceof PatrRamFSOpts opts) {
						loadPatrRamOpts(linker, opts);
					} else {
						throw new IllegalArgumentException("FSOptions of an unknown " + fso.getClass());
					}
				}
				loaded = new PatrFS(session);
			} catch (Throwable e) {
				session.close();
				throw thrw(e);
			}
			return loaded;
		}
	}
	
	private static void loadPatrRamOpts(Linker linker, PatrRamFSOpts opts) throws Throwable {
		MethodHandle  newBm            = linker.downcallHandle(PatrFS.LOCKUP.find("bm_new_ram_block_manager").orElseThrow(), FunctionDescriptor.of(PNTR, LONG, INT));
		MethodHandle  pfsLoadAndFormat = linker.downcallHandle(PatrFS.LOCKUP.find("pfs_load_and_format").orElseThrow(), FunctionDescriptor.of(INT, PNTR, LONG));
		MemorySegment bm               = (MemorySegment) newBm.invoke(opts.blockCount(), opts.blockSize());
		if (0 == (int) pfsLoadAndFormat.invoke(bm, opts.blockCount())) {
			throw thrw(PFSErrorCause.LOAD_PFS_AND_FORMAT, opts);
		}
	}
	
	private static void loadPatrOpts(Linker linker, Arena local, PatrFSOptions opts) throws Throwable {
		MemorySegment path = local.allocateUtf8String(opts.path());
		if (opts.format()) {
			loadWithFormat(linker, opts, path);
		} else {
			loadWithoutFormat(linker, opts, path);
		}
	}
	
	private static void loadWithFormat(Linker linker, PatrFSOptions opts, MemorySegment path) throws Throwable {
		MethodHandle  newBm            = linker.downcallHandle(PatrFS.LOCKUP.find("bm_new_file_block_manager_path_bs").orElseThrow(),
				FunctionDescriptor.of(PNTR, PNTR, INT, INT));
		MethodHandle  pfsLoadAndFormat = linker.downcallHandle(PatrFS.LOCKUP.find("pfs_load_and_format").orElseThrow(),
				FunctionDescriptor.of(INT, PNTR, LONG, PNTR, PNTR));
		MemorySegment bm               = (MemorySegment) newBm.invoke(path, opts.blockSize(), 0);
		if (bm.address() == 0) {
			throw thrw(PFSErrorCause.LOAD_PFS_AND_FORMAT, opts);
		}
		try (Arena arena = Arena.openConfined()) {
			MemorySegment uuid = convUUID(arena, opts.uuid());
			MemorySegment name = opts.name() == null ? MemorySegment.NULL : arena.allocateUtf8String(opts.name());
			if (0 == (int) pfsLoadAndFormat.invoke(bm, opts.blockCount(), uuid, name)) {
				throw thrw(PFSErrorCause.LOAD_PFS_AND_FORMAT, opts);
			}
		}
	}
	
	private static void loadWithoutFormat(Linker linker, PatrFSOptions opts, MemorySegment path) throws Throwable {
		MemorySegment bm;
		if (opts.blockSize() == -1) {
			MethodHandle newBm = linker.downcallHandle(PatrFS.LOCKUP.find("bm_new_file_block_manager_path").orElseThrow(), FunctionDescriptor.of(PNTR, PNTR, INT));
			bm = (MemorySegment) newBm.invoke(path, 0);
		} else {
			MethodHandle newBm = linker.downcallHandle(PatrFS.LOCKUP.find("bm_new_file_block_manager_path_bs").orElseThrow(),
					FunctionDescriptor.of(PNTR, PNTR, INT, INT));
			bm = (MemorySegment) newBm.invoke(path, opts.blockSize(), 0);
		}
		MethodHandle pfsLoad = linker.downcallHandle(PatrFS.LOCKUP.find("pfs_load").orElseThrow(), FunctionDescriptor.of(INT, PNTR, PNTR));
		if (0 == (int) pfsLoad.invoke(bm, MemorySegment.NULL)) {
			throw thrw(PFSErrorCause.LOAD_PFS, opts.path());
		}
	}
	
	public static MemorySegment convUUID(Arena arena, UUID uuid) {
		if (uuid == null) {
			return MemorySegment.NULL;
		}
		MemorySegment mem = arena.allocate(16L, 8L);
		if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
			mem.set(ValueLayout.JAVA_LONG, 0L, uuid.getLeastSignificantBits());
			mem.set(ValueLayout.JAVA_LONG, 8L, uuid.getMostSignificantBits());
		} else {
			mem.set(ValueLayout.JAVA_LONG, 0L, uuid.getMostSignificantBits());
			mem.set(ValueLayout.JAVA_LONG, 8L, uuid.getLeastSignificantBits());
		}
		return mem;
	}
	
	public static UUID getUUID(MemorySegment seg) {
		long low;
		long high;
		if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
			low  = seg.get(ValueLayout.JAVA_LONG, 0L);
			high = seg.get(ValueLayout.JAVA_LONG, 8L);
		} else {
			high = seg.get(ValueLayout.JAVA_LONG, 0L);
			low  = seg.get(ValueLayout.JAVA_LONG, 8L);
		}
		return new UUID(high, low);
	}
	
	public static IOException thrw(Throwable t) throws IOException {
		if (t instanceof IOException ioe) {
			throw ioe;
		} else if (t instanceof Error err) {
			throw err;
		} else if (t instanceof RuntimeException re) {
			throw re;
		} else {
			throw new AssertionError(t);
		}
	}
	
	public static ClosedChannelException thrwR(Throwable t) throws ClosedChannelException {
		if (t instanceof ClosedChannelException ioe) {
			throw ioe;
		} else if (t instanceof Error err) {
			throw err;
		} else if (t instanceof RuntimeException re) {
			throw re;
		} else {
			throw new AssertionError(t);
		}
	}
	
	public static IOException thrw(PFSErrorCause cause, Object info) throws IOException {
		int pfsErrno = pfsErrno();
		pfsErrno(0);
		String msg = cause.str.apply(info);
		throw cause.func.apply(msg, pfsErrno);
	}
	
	private static MemorySegment cach;
	private static MemorySegment cach2;
	
	public static int pfsErrno() {
		MemorySegment c1 = cach;
		if (c1 == null) {
			c1   = MemorySegment.ofAddress(PatrFS.LOCKUP.find("pfs_err_loc").orElseThrow().address(), 8L);
			cach = c1;
		}
		long          addr = c1.get(LONG, 0L);
		MemorySegment c2   = cach2;
		if (c2 == null || addr != c2.address()) {
			c2    = MemorySegment.ofAddress(addr, 4);
			cach2 = c2;
		}
		return c2.get(INT, 0);
	}
	
	public static void pfsErrno(int newErrno) {
		MemorySegment c1 = cach;
		if (c1 == null) {
			c1   = MemorySegment.ofAddress(PatrFS.LOCKUP.find("pfs_err_loc").orElseThrow().address(), 8L);
			cach = c1;
		}
		long          addr = c1.get(LONG, 0L);
		MemorySegment c2   = cach2;
		if (c2 == null || addr != c2.address()) {
			c2    = MemorySegment.ofAddress(addr, 4);
			cach2 = c2;
		}
		c2.set(INT, 0, newErrno);
	}
	
	public static void unload(FS loaded) {
		if (loaded != PatrFSProvider.loaded) { throw new AssertionError(); }
		PatrFSProvider.loaded = null;
	}
	
	@Override
	public Collection<? extends FS> loadedFS() {
		if (loaded != null) {
			return Arrays.asList(loaded);
		}
		return Collections.emptyList();
	}
	
}

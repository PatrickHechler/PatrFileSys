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

import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.LINKER;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.LOCKUP;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.PFS_FREE;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider.INT;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider.LONG;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider.PNTR;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider.convUUID;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider.getUUID;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider.thrw;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.UUID;

import de.hechler.patrick.zeugs.pfs.interfaces.Mount;

public class PatrMount extends PatrFolder implements Mount {
	// they are ordered correctly for the native enum values
	private static final MountType[] MOUNT_TYPES = MountType.values();
	
	private static final MethodHandle PFS_MOUNT_FS_BLOCK_COUNT;
	private static final MethodHandle PFS_MOUNT_FS_BLOCK_SIZE;
	private static final MethodHandle PFS_MOUNT_FS_UUID;
	private static final MethodHandle PFS_MOUNT_FS_SET_UUID;
	private static final MethodHandle PFS_MOUNT_FS_NAME;
	private static final MethodHandle PFS_MOUNT_FS_IS_READ_ONLY;
	private static final MethodHandle PFS_MOUNT_FS_TYPE;
	
	static {
		PFS_MOUNT_FS_BLOCK_COUNT  = LINKER.downcallHandle(LOCKUP.find("pfs_mount_fs_block_count").orElseThrow(), FunctionDescriptor.of(LONG, INT));
		PFS_MOUNT_FS_BLOCK_SIZE   = LINKER.downcallHandle(LOCKUP.find("pfs_mount_fs_block_size").orElseThrow(), FunctionDescriptor.of(INT, INT));
		PFS_MOUNT_FS_UUID         = LINKER.downcallHandle(LOCKUP.find("pfs_mount_fs_uuid").orElseThrow(), FunctionDescriptor.of(PNTR, INT, PNTR));
		PFS_MOUNT_FS_SET_UUID     = LINKER.downcallHandle(LOCKUP.find("pfs_mount_fs_set_uuid").orElseThrow(), FunctionDescriptor.of(INT, INT, PNTR));
		PFS_MOUNT_FS_NAME         = LINKER.downcallHandle(LOCKUP.find("pfs_mount_fs_name").orElseThrow(), FunctionDescriptor.of(PNTR, INT));
		PFS_MOUNT_FS_IS_READ_ONLY = LINKER.downcallHandle(LOCKUP.find("pfs_mount_fs_is_read_only").orElseThrow(), FunctionDescriptor.of(INT, INT));
		PFS_MOUNT_FS_TYPE         = LINKER.downcallHandle(LOCKUP.find("pfs_mount_fs_type").orElseThrow(), FunctionDescriptor.of(INT, INT));
	}
	
	public PatrMount(int handle) {
		super(handle);
	}
	
	@Override
	public long blockCount() throws IOException {
		ensureOpen();
		try {
			long res = (long) PFS_MOUNT_FS_BLOCK_COUNT.invoke(this.handle);
			if (res == -1L) thrw(PFSErrorCause.GET_BLOCK_COUNT, this);
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public int blockSize() throws IOException {
		ensureOpen();
		try {
			int res = (int) PFS_MOUNT_FS_BLOCK_SIZE.invoke(this.handle);
			if (res == -1) thrw(PFSErrorCause.GET_BLOCK_SIZE, this);
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public UUID uuid() throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			MemorySegment val = ses.allocate(16L, 8L);
			MemorySegment res = (MemorySegment) PFS_MOUNT_FS_UUID.invoke(this.handle, val);
			if (res.address() == 0L) throw thrw(PFSErrorCause.GET_UUID, this);
			if (res.address() != val.address()) throw new AssertionError("the pfs_mount_fs_uuid function behaves incorectly");
			return getUUID(val); // use val here, since res has length set to zero
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void uuid(UUID uuid) throws IOException { 
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			MemorySegment mem = convUUID(ses, uuid);
			int res = (int) PFS_MOUNT_FS_SET_UUID.invoke(this.handle, mem);
			if (res == 0) thrw(PFSErrorCause.GET_BLOCK_SIZE, this);
		} catch (Throwable e) {
			throw thrw(e);
		}
	 }
	
	@Override
	public String fsName() throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			MemorySegment res = (MemorySegment) PFS_MOUNT_FS_NAME.invoke(this.handle);
			if (res == null || res.address() == 0L) throw thrw(PFSErrorCause.GET_UUID, this);
			String str = res.getUtf8String(0L);
			PFS_FREE.invoke(res);
			return str;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public boolean readOnly() throws IOException {
		ensureOpen();
		try {
			int res = (int) PFS_MOUNT_FS_IS_READ_ONLY.invoke(this.handle);
			if (res == -1) thrw(PFSErrorCause.GET_READ_ONLY, this);
			return res != 0;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public MountType mountType() throws IOException {
		ensureOpen();
		try {
			int res = (int) PFS_MOUNT_FS_TYPE.invoke(this.handle);
			if (res == -1) thrw(PFSErrorCause.GET_MOUNT_TYPPE, this);
			if (res < 0 || res >= MOUNT_TYPES.length) {
				throw new AssertionError("invalid return value: " + res);
			}
			return MOUNT_TYPES[res];
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
}

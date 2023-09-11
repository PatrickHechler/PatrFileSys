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
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider.thrw;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider.thrwR;

import java.io.IOError;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.nio.channels.ClosedChannelException;

import de.hechler.patrick.zeugs.pfs.interfaces.FS;
import de.hechler.patrick.zeugs.pfs.interfaces.FSElement;
import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Folder;
import de.hechler.patrick.zeugs.pfs.interfaces.Mount;
import de.hechler.patrick.zeugs.pfs.interfaces.Pipe;

@SuppressWarnings("javadoc")
public class PatrFSElement implements FSElement {
	
	private static final MethodHandle PFS_ELEMENT_CLOSE;
	private static final MethodHandle PFS_ELEMENT_PARENT;
	private static final MethodHandle PFS_ELEMENT_PATH;
	private static final MethodHandle PFS_ELEMENT_FS_PATH;
	private static final MethodHandle PFS_ELEMENT_DELETE;
	private static final MethodHandle PFS_ELEMENT_GET_FLAGS;
	private static final MethodHandle PFS_ELEMENT_MODIFY_FLAGS;
	private static final MethodHandle PFS_ELEMENT_GET_CREATE_TIME;
	private static final MethodHandle PFS_ELEMENT_GET_LAST_MODIFY_TIME;
	private static final MethodHandle PFS_ELEMENT_SET_CREATE_TIME;
	private static final MethodHandle PFS_ELEMENT_SET_LAST_MODIFY_TIME;
	private static final MethodHandle PFS_ELEMENT_GET_NAME;
	private static final MethodHandle PFS_ELEMENT_SET_NAME;
	private static final MethodHandle PFS_ELEMENT_SET_PARENT;
	private static final MethodHandle PFS_ELEMENT_MOVE;
	private static final MethodHandle PFS_ELEMENT_SAME;
	private static final MethodHandle PFS_MOUNT_GET_MOUNT_POINT;
	
	static {
		PFS_ELEMENT_CLOSE                = LINKER.downcallHandle(LOCKUP.find("pfs_element_close").orElseThrow(), FunctionDescriptor.of(INT, INT));
		PFS_ELEMENT_PARENT               = LINKER.downcallHandle(LOCKUP.find("pfs_element_parent").orElseThrow(), FunctionDescriptor.of(INT, INT));
		PFS_ELEMENT_PATH                 = LINKER.downcallHandle(LOCKUP.find("pfs_element_path").orElseThrow(), FunctionDescriptor.of(PNTR, INT));
		PFS_ELEMENT_FS_PATH              = LINKER.downcallHandle(LOCKUP.find("pfs_element_fs_path").orElseThrow(), FunctionDescriptor.of(PNTR, INT));
		PFS_ELEMENT_DELETE               = LINKER.downcallHandle(LOCKUP.find("pfs_element_delete").orElseThrow(), FunctionDescriptor.of(INT, INT, INT));
		PFS_ELEMENT_GET_FLAGS            = LINKER.downcallHandle(LOCKUP.find("pfs_element_get_flags").orElseThrow(), FunctionDescriptor.of(INT, INT));
		PFS_ELEMENT_MODIFY_FLAGS         = LINKER.downcallHandle(LOCKUP.find("pfs_element_modify_flags").orElseThrow(), FunctionDescriptor.of(INT, INT, INT, INT));
		PFS_ELEMENT_GET_CREATE_TIME      = LINKER.downcallHandle(LOCKUP.find("pfs_element_get_create_time").orElseThrow(), FunctionDescriptor.of(LONG, INT));
		PFS_ELEMENT_GET_LAST_MODIFY_TIME = LINKER.downcallHandle(LOCKUP.find("pfs_element_get_last_modify_time").orElseThrow(), FunctionDescriptor.of(LONG, INT));
		PFS_ELEMENT_SET_CREATE_TIME      = LINKER.downcallHandle(LOCKUP.find("pfs_element_set_create_time").orElseThrow(), FunctionDescriptor.of(INT, INT, LONG));
		PFS_ELEMENT_SET_LAST_MODIFY_TIME = LINKER.downcallHandle(LOCKUP.find("pfs_element_set_last_modify_time").orElseThrow(),
				FunctionDescriptor.of(INT, INT, LONG));
		PFS_ELEMENT_GET_NAME             = LINKER.downcallHandle(LOCKUP.find("pfs_element_get_name").orElseThrow(), FunctionDescriptor.of(INT, INT, PNTR, PNTR));
		PFS_ELEMENT_SET_NAME             = LINKER.downcallHandle(LOCKUP.find("pfs_element_set_name").orElseThrow(), FunctionDescriptor.of(INT, INT, PNTR));
		PFS_ELEMENT_SET_PARENT           = LINKER.downcallHandle(LOCKUP.find("pfs_element_set_parent").orElseThrow(), FunctionDescriptor.of(INT, INT, INT));
		PFS_ELEMENT_MOVE                 = LINKER.downcallHandle(LOCKUP.find("pfs_element_move").orElseThrow(), FunctionDescriptor.of(INT, INT, INT, PNTR));
		PFS_ELEMENT_SAME                 = LINKER.downcallHandle(LOCKUP.find("pfs_element_same").orElseThrow(), FunctionDescriptor.of(INT, INT, INT));
		
		PFS_MOUNT_GET_MOUNT_POINT = LINKER.downcallHandle(LOCKUP.find("pfs_mount_get_mount_point").orElseThrow(), FunctionDescriptor.of(INT, INT));
	}
	
	public final int  handle;
	protected boolean closed;
	
	public PatrFSElement(int handle) {
		this.handle = handle;
	}
	
	protected void ensureOpen() throws ClosedChannelException {
		if (this.closed) { throw new ClosedChannelException(); }
	}
	
	@Override
	public Folder parent() throws IOException {
		ensureOpen();
		try {
			int res = (int) PFS_ELEMENT_PARENT.invoke(this.handle);
			if (res == -1) { throw thrw(PFSErrorCause.GET_PARENT, null); }
			return new PatrFolder(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public String path() throws IOException {
		return pathImpl(PFS_ELEMENT_PATH);
	}
	
	@Override
	public String pathFromMount() throws IOException {
		return pathImpl(PFS_ELEMENT_FS_PATH);
	}
	
	private String pathImpl(MethodHandle funcHandle) throws IOException {
		ensureOpen();
		try {
			MemorySegment path = (MemorySegment) funcHandle.invoke(this.handle);
			path = MemorySegment.ofAddress(path.address(), Integer.MAX_VALUE + 1L); // \0 offset can at max be int.max_value
			String res = path.getUtf8String(0L);
			PFS_FREE.invoke(path);
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public FS fs() throws ClosedChannelException {
		ensureOpen();
		return PatrFSProvider.loaded;
	}
	
	@Override
	public Mount mountPoint() throws ClosedChannelException {
		ensureOpen();
		try {
			int res = (int) PFS_MOUNT_GET_MOUNT_POINT.invoke(this.handle);
			if (res == -1) { throw thrw(PFSErrorCause.GET_PARENT, null); }
			return new PatrMount(res);
		} catch (Throwable e) {
			throw thrwR(e);
		}
	}
	
	@Override
	public int flags() throws IOException {
		ensureOpen();
		try {
			int res = (int) PFS_ELEMENT_GET_FLAGS.invoke(this.handle);
			if (res == -1) { throw thrw(PFSErrorCause.GET_FLAGS, null); }
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void flag(int add, int rem) throws IOException {
		ensureOpen();
		try {
			if (0 == (int) PFS_ELEMENT_MODIFY_FLAGS.invoke(this.handle, add, rem)) {
				throw thrw(PFSErrorCause.MODIFY_FLAGS, null);
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public long lastModTime() throws IOException {
		ensureOpen();
		try {
			long res = (long) PFS_ELEMENT_GET_LAST_MODIFY_TIME.invoke(this.handle);
			if (res == -1) { throw thrw(PFSErrorCause.GET_LAST_MODIFY_TIME, null); }
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void lastModTime(long time) throws IOException {
		ensureOpen();
		try {
			if (0 == (int) PFS_ELEMENT_SET_LAST_MODIFY_TIME.invoke(this.handle, time)) {
				throw thrw(PFSErrorCause.SET_LAST_MODIFY_TIME, null);
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public long createTime() throws IOException {
		ensureOpen();
		try {
			long res = (long) PFS_ELEMENT_GET_CREATE_TIME.invoke(this.handle);
			if (res == -1) { throw thrw(PFSErrorCause.GET_CREATE_TIME, null); }
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void createTime(long time) throws IOException {
		ensureOpen();
		try {
			if (0 == (int) PFS_ELEMENT_SET_CREATE_TIME.invoke(this.handle, time)) {
				throw thrw(PFSErrorCause.SET_CREATE_TIME, null);
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public String name() throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			MemorySegment data = ses.allocate(16);
			data.set(PNTR, 0L, MemorySegment.NULL);
			data.set(LONG, 8L, 0L);
			if (0 == (int) PFS_ELEMENT_GET_NAME.invoke(this.handle, data, data.asSlice(8L, 8L))) {
				throw thrw(PFSErrorCause.GET_NAME, null);
			}
			MemorySegment seg = data.get(PNTR, 0L);
			seg = MemorySegment.ofAddress(seg.address(), 1L << 31);
			return seg.getUtf8String(0L);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void name(String name) throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			if (0 == (int) PFS_ELEMENT_SET_NAME.invoke(this.handle, ses.allocateUtf8String(name))) {
				throw thrw(PFSErrorCause.SET_NAME, null);
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void parent(Folder parent) throws IOException {
		ensureOpen();
		try {
			if (parent instanceof PatrFolder p) {
				if (0 == (int) PFS_ELEMENT_SET_PARENT.invoke(this.handle, p.handle)) {
					throw thrw(PFSErrorCause.SET_PARENT, null);
				}
			} else {
				throw new ClassCastException("parent is not of class PatrFolder, but of " + parent.getClass());
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void move(Folder parent, String name) throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			if (parent instanceof PatrFolder p) {
				if (0 == (int) PFS_ELEMENT_MOVE.invoke(this.handle, p.handle, ses.allocateUtf8String(name))) {
					throw thrw(PFSErrorCause.MOVE_ELEMENT, null);
				}
			} else {
				throw new ClassCastException("parent is not of class PatrFolder, but of " + parent.getClass());
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void delete() throws IOException {
		ensureOpen();
		try {
			int val = (int) PFS_ELEMENT_DELETE.invoke(this.handle, 1);
			this.closed = true;
			if (0 == val) { throw thrw(PFSErrorCause.DELETE_ELEMENT, null); }
		} catch (Throwable e) {
			this.closed = true;
			throw thrw(e);
		}
	}
	
	@Override
	public Folder getFolder() throws IOException {
		if (this instanceof Folder res) {
			return res;
		}
		int flags = flags();
		if ((flags & FLAG_MOUNT) != 0) {
			return new PatrMount(this.handle);
		}
		if ((flags & FLAG_FOLDER) != 0) {
			return new PatrFolder(this.handle);
		}
		throw new IllegalStateException("this is no folder!");
	}
	
	@Override
	public Mount getMount() throws IOException {
		if (this instanceof Mount res) {
			return res;
		} else if (isFile()) {
			return new PatrMount(this.handle);
		} else {
			throw new IllegalStateException("this is no file!");
		}
	}
	
	@Override
	public File getFile() throws IOException {
		if (this instanceof File res) {
			return res;
		} else if (isFile()) {
			return new PatrFile(this.handle);
		} else {
			throw new IllegalStateException("this is no file!");
		}
	}
	
	@Override
	public Pipe getPipe() throws IOException {
		if (this instanceof Pipe res) {
			return res;
		} else if (isPipe()) {
			return new PatrPipe(this.handle);
		} else {
			throw new IllegalStateException("this is no pipe!");
		}
	}
	
	@Override
	public void close() throws IOException {
		if (this.closed) { return; }
		this.closed = true;
		try {
			if (0 == (int) PFS_ELEMENT_CLOSE.invoke(this.handle)) { throw thrw(PFSErrorCause.CLOSE_ELEMENT, null); }
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public boolean equals(FSElement other) throws IOException {
		try {
			if (!(other instanceof PatrFSElement pe)) return false;
			int res = (int) PFS_ELEMENT_SAME.invoke(this.handle, pe.handle);
			return switch (res) {
			case 0 -> false;
			case 1 -> true;
			case -1 -> throw thrw(PFSErrorCause.SAME, null);
			default -> throw thrw(PFSErrorCause.SAME, Integer.valueOf(res));
			};
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PatrFSElement element) {
			try {
				return equals(element);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		try {
			return name().hashCode();
		} catch (IOException e) {
			throw new IOError(e);
		}
	}
	
	@Override
	public String toString() {
		if (this.closed) {
			return "closed element handle";
		}
		try {
			return path();
		} catch (Throwable t) {
			return "<error: could not get path: " + t + ">";
		}
	}
	
}

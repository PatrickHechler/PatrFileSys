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
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider.INT;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider.LONG;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider.PNTR;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider.thrw;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;
import java.nio.channels.ClosedChannelException;
import java.nio.file.NoSuchFileException;

import de.hechler.patrick.zeugs.pfs.interfaces.FSElement;
import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Folder;
import de.hechler.patrick.zeugs.pfs.interfaces.Mount;
import de.hechler.patrick.zeugs.pfs.interfaces.Pipe;

public class PatrFolder extends PatrFSElement implements Folder {
	
	private static final MethodHandle PFS_FOLDER_OPEN_ITER;
	private static final MethodHandle PFS_FOLDER_CHILD_COUNT;
	private static final MethodHandle PFS_FOLDER_CHILD;
	private static final MethodHandle PFS_FOLDER_CHILD_FOLDER;
	private static final MethodHandle PFS_FOLDER_CHILD_MOUNT;
	private static final MethodHandle PFS_FOLDER_CHILD_FILE;
	private static final MethodHandle PFS_FOLDER_CHILD_PIPE;
	private static final MethodHandle PFS_FOLDER_DESCENDANT;
	private static final MethodHandle PFS_FOLDER_DESCENDANT_FOLDER;
	private static final MethodHandle PFS_FOLDER_DESCENDANT_MOUNT;
	private static final MethodHandle PFS_FOLDER_DESCENDANT_FILE;
	private static final MethodHandle PFS_FOLDER_DESCENDANT_PIPE;
	private static final MethodHandle PFS_FOLDER_CREATE_FOLDER;
	private static final MethodHandle PFS_FOLDER_CREATE_FILE;
	private static final MethodHandle PFS_FOLDER_CREATE_PIPE;
	private static final MethodHandle PFS_FOLDER_CREATE_MOUNT_INTERN;
	private static final MethodHandle PFS_FOLDER_CREATE_MOUNT_TEMP;
	private static final MethodHandle PFS_FOLDER_CREATE_MOUNT_RFS_FILE;
	
	static {
		PFS_FOLDER_OPEN_ITER             = LINKER.downcallHandle(LOCKUP.find("pfs_folder_open_iter").orElseThrow(), FunctionDescriptor.of(INT, INT, INT));
		PFS_FOLDER_CHILD_COUNT           = LINKER.downcallHandle(LOCKUP.find("pfs_folder_child_count").orElseThrow(), FunctionDescriptor.of(LONG, INT));
		PFS_FOLDER_CHILD                 = LINKER.downcallHandle(LOCKUP.find("pfs_folder_child").orElseThrow(), FunctionDescriptor.of(INT, INT, PNTR));
		PFS_FOLDER_CHILD_FOLDER          = LINKER.downcallHandle(LOCKUP.find("pfs_folder_child_folder").orElseThrow(), FunctionDescriptor.of(INT, INT, PNTR));
		PFS_FOLDER_CHILD_MOUNT           = LINKER.downcallHandle(LOCKUP.find("pfs_folder_child_mount").orElseThrow(), FunctionDescriptor.of(INT, INT, PNTR));
		PFS_FOLDER_CHILD_FILE            = LINKER.downcallHandle(LOCKUP.find("pfs_folder_child_file").orElseThrow(), FunctionDescriptor.of(INT, INT, PNTR));
		PFS_FOLDER_CHILD_PIPE            = LINKER.downcallHandle(LOCKUP.find("pfs_folder_child_pipe").orElseThrow(), FunctionDescriptor.of(INT, INT, PNTR));
		PFS_FOLDER_DESCENDANT            = LINKER.downcallHandle(LOCKUP.find("pfs_folder_descendant").orElseThrow(), FunctionDescriptor.of(INT, INT, PNTR));
		PFS_FOLDER_DESCENDANT_FOLDER     = LINKER.downcallHandle(LOCKUP.find("pfs_folder_descendant_folder").orElseThrow(), FunctionDescriptor.of(INT, INT, PNTR));
		PFS_FOLDER_DESCENDANT_MOUNT      = LINKER.downcallHandle(LOCKUP.find("pfs_folder_descendant_mount").orElseThrow(), FunctionDescriptor.of(INT, INT, PNTR));
		PFS_FOLDER_DESCENDANT_FILE       = LINKER.downcallHandle(LOCKUP.find("pfs_folder_descendant_file").orElseThrow(), FunctionDescriptor.of(INT, INT, PNTR));
		PFS_FOLDER_DESCENDANT_PIPE       = LINKER.downcallHandle(LOCKUP.find("pfs_folder_descendant_pipe").orElseThrow(), FunctionDescriptor.of(INT, INT, PNTR));
		PFS_FOLDER_CREATE_FOLDER         = LINKER.downcallHandle(LOCKUP.find("pfs_folder_create_folder").orElseThrow(), FunctionDescriptor.of(INT, INT, PNTR));
		PFS_FOLDER_CREATE_FILE           = LINKER.downcallHandle(LOCKUP.find("pfs_folder_create_file").orElseThrow(), FunctionDescriptor.of(INT, INT, PNTR));
		PFS_FOLDER_CREATE_PIPE           = LINKER.downcallHandle(LOCKUP.find("pfs_folder_create_pipe").orElseThrow(), FunctionDescriptor.of(INT, INT, PNTR));
		PFS_FOLDER_CREATE_MOUNT_INTERN   = LINKER.downcallHandle(LOCKUP.find("pfs_folder_create_mount_intern").orElseThrow(),
				FunctionDescriptor.of(INT, INT, PNTR, LONG, INT));
		PFS_FOLDER_CREATE_MOUNT_TEMP     = LINKER.downcallHandle(LOCKUP.find("pfs_folder_create_mount_temp").orElseThrow(),
				FunctionDescriptor.of(INT, INT, PNTR, LONG, INT));
		PFS_FOLDER_CREATE_MOUNT_RFS_FILE = LINKER.downcallHandle(LOCKUP.find("pfs_folder_create_mount_rfs_file").orElseThrow(),
				FunctionDescriptor.of(INT, INT, PNTR, PNTR));
	}
	
	public PatrFolder(int handle) {
		super(handle);
	}
	
	@Override
	public FolderIter iter(boolean showHidden) throws IOException {
		ensureOpen();
		try {
			int res = (int) PFS_FOLDER_OPEN_ITER.invoke(this.handle, showHidden ? 1 : 0);
			return new PatrFolderIter(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	public class PatrFolderIter implements FolderIter {
		
		private static final MethodHandle PFS_ITER_CLOSE;
		private static final MethodHandle PFS_ITER_NEXT;
		
		static {
			PFS_ITER_CLOSE = LINKER.downcallHandle(LOCKUP.find("pfs_iter_close").orElseThrow(), FunctionDescriptor.of(INT, INT));
			PFS_ITER_NEXT  = LINKER.downcallHandle(LOCKUP.find("pfs_iter_next").orElseThrow(), FunctionDescriptor.of(INT, INT));
		}
		
		private final int handle;
		
		private boolean   iterClosed;
		private FSElement next;
		private FSElement last;
		
		public PatrFolderIter(int handle) {
			this.handle = handle;
		}
		
		private void ensureIterOpen() throws ClosedChannelException {
			if (this.iterClosed) { throw new ClosedChannelException(); }
		}
		
		@Override
		public FSElement nextElement() throws IOException {
			ensureIterOpen();
			FSElement n = this.next;
			if (n != null) {
				this.last = n;
				this.next = null;
				return n;
			}
			try {
				int res = (int) PFS_ITER_NEXT.invoke(this.handle);
				if (res == -1) { throw thrw(PFSErrorCause.ITER_NEXT, null); }
				n         = new PatrFSElement(res);
				this.last = n;
				return n;
			} catch (Throwable e) {
				throw thrw(e);
			}
		}
		
		@Override
		public boolean hasNextElement() throws IOException {
			ensureIterOpen();
			if (this.next != null) {
				return true;
			}
			try {
				int old = PatrFSProvider.pfsErrno();
				int res = (int) PFS_ITER_NEXT.invoke(this.handle);
				if (res == -1) {
					if (PatrFSProvider.pfsErrno() == ErrConsts.NO_MORE_ELEMENTS) {
						PatrFSProvider.pfsErrno(old);
						return false;
					}
					throw thrw(PFSErrorCause.ITER_NEXT, null);
				}
				this.next = new PatrFSElement(res);
				return true;
			} catch (Throwable e) {
				throw thrw(e);
			}
		}
		
		@Override
		public void delete() throws IOException, IllegalStateException {
			ensureIterOpen();
			if (this.last == null) {
				throw new IllegalStateException("there is no last element");
			}
			this.last.delete();
			this.last = null;
		}
		
		@Override
		public void close() throws IOException {
			if (this.iterClosed) { return; }
			try {
				this.iterClosed = true;
				if (0 == (int) PFS_ITER_CLOSE.invoke(this.handle)) { throw thrw(PFSErrorCause.CLOSE_ITER, null); }
			} catch (Throwable e) {
				throw thrw(e);
			}
		}
		
	}
	
	@Override
	public long childCount() throws IOException {
		ensureOpen();
		try {
			long res = (long) PFS_FOLDER_CHILD_COUNT.invoke(this.handle);
			if (res == -1) { throw thrw(PFSErrorCause.GET_CHILD_COUNT, null); }
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public FSElement childElement(String name) throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			int res = (int) PFS_FOLDER_CHILD.invoke(this.handle, ses.allocateUtf8String(name));
			if (res == -1) { throw thrw(PFSErrorCause.GET_CHILD, name); }
			return new PatrFSElement(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Folder childFolder(String name) throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			int res = (int) PFS_FOLDER_CHILD_FOLDER.invoke(this.handle, ses.allocateUtf8String(name));
			if (res == -1) { throw thrw(PFSErrorCause.GET_CHILD, name); }
			return new PatrFolder(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Mount childMount(String name) throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			int res = (int) PFS_FOLDER_CHILD_MOUNT.invoke(this.handle, ses.allocateUtf8String(name));
			if (res == -1) { throw thrw(PFSErrorCause.GET_CHILD, name); }
			return new PatrMount(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public File childFile(String name) throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			int res = (int) PFS_FOLDER_CHILD_FILE.invoke(this.handle, ses.allocateUtf8String(name));
			if (res == -1) { throw thrw(PFSErrorCause.GET_CHILD, name); }
			return new PatrFile(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Pipe childPipe(String name) throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			int res = (int) PFS_FOLDER_CHILD_PIPE.invoke(this.handle, ses.allocateUtf8String(name));
			if (res == -1) { throw thrw(PFSErrorCause.GET_CHILD, name); }
			return new PatrPipe(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public FSElement descElement(String path) throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			int res = (int) PFS_FOLDER_DESCENDANT.invoke(this.handle, ses.allocateUtf8String(path));
			if (res == -1) { throw thrw(PFSErrorCause.GET_CHILD, path); }
			return new PatrFSElement(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Folder descFolder(String path) throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			int res = (int) PFS_FOLDER_DESCENDANT_FOLDER.invoke(this.handle, ses.allocateUtf8String(path));
			if (res == -1) { throw thrw(PFSErrorCause.GET_CHILD, path); }
			return new PatrFolder(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Mount descMount(String path) throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			int res = (int) PFS_FOLDER_DESCENDANT_MOUNT.invoke(this.handle, ses.allocateUtf8String(path));
			if (res == -1) { throw thrw(PFSErrorCause.GET_CHILD, path); }
			return new PatrMount(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public File descFile(String path) throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			int res = (int) PFS_FOLDER_DESCENDANT_FILE.invoke(this.handle, ses.allocateUtf8String(path));
			if (res == -1) { throw thrw(PFSErrorCause.GET_CHILD, path); }
			return new PatrFile(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Pipe descPipe(String path) throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			int res = (int) PFS_FOLDER_DESCENDANT_PIPE.invoke(this.handle, ses.allocateUtf8String(path));
			if (res == -1) { throw thrw(PFSErrorCause.GET_CHILD, path); }
			return new PatrPipe(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Folder createFolder(String name) throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			int res = (int) PFS_FOLDER_CREATE_FOLDER.invoke(this.handle, ses.allocateUtf8String(name));
			if (res == -1) { throw thrw(PFSErrorCause.CREATE_CHILD, name); }
			return new PatrFolder(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public File createFile(String name) throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			int res = (int) PFS_FOLDER_CREATE_FILE.invoke(this.handle, ses.allocateUtf8String(name));
			if (res == -1) { throw thrw(PFSErrorCause.CREATE_CHILD, name); }
			return new PatrFile(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Pipe createPipe(String name) throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			int res = (int) PFS_FOLDER_CREATE_PIPE.invoke(this.handle, ses.allocateUtf8String(name));
			if (res == -1) { throw thrw(PFSErrorCause.CREATE_CHILD, name); }
			return new PatrPipe(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Mount createMountIntern(String name, long blockCount, int blockSize) throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			int res = (int) PFS_FOLDER_CREATE_MOUNT_INTERN.invoke(this.handle, ses.allocateUtf8String(name), blockCount, blockSize);
			if (res == -1) { throw thrw(PFSErrorCause.CREATE_CHILD, name); }
			return new PatrMount(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Mount createMountTemp(String name, long blockCount, int blockSize) throws IOException {
		ensureOpen();
		try (Arena ses = Arena.openConfined()) {
			int res = (int) PFS_FOLDER_CREATE_MOUNT_TEMP.invoke(this.handle, ses.allocateUtf8String(name), blockCount, blockSize);
			if (res == -1) { throw thrw(PFSErrorCause.CREATE_CHILD, name); }
			return new PatrMount(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Mount createMountRFSFile(String name, java.io.File file) throws IOException {
		ensureOpen();
		if (!file.exists()) {
			throw new NoSuchFileException(file.toString(), null, "the file does not exist");
		}
		if (!file.isFile()) {
			throw new NoSuchFileException(file.toString(), null, "the file is no file");
		}
		try (Arena ses = Arena.openConfined()) {
			int res = (int) PFS_FOLDER_CREATE_MOUNT_RFS_FILE.invoke(this.handle, ses.allocateUtf8String(name), ses.allocateUtf8String(file.toString()));
			if (res == -1) { throw thrw(PFSErrorCause.CREATE_CHILD, name); }
			return new PatrMount(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
}

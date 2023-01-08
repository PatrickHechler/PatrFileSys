package de.hechler.patrick.zeugs.pfs.impl.pfs;

import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.INT;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.LINKER;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.LOCKUP;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.LONG;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.PNTR;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider.thrw;

import java.io.IOException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySession;
import java.lang.invoke.MethodHandle;
import java.nio.channels.ClosedChannelException;
import java.util.NoSuchElementException;

import de.hechler.patrick.zeugs.pfs.interfaces.FSElement;
import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Folder;
import de.hechler.patrick.zeugs.pfs.interfaces.Pipe;

public class PatrFolder extends PatrFSElement implements Folder {
	
	private static final MethodHandle PFS_FOLDER_OPEN_ITER;
	private static final MethodHandle PFS_FOLDER_CHILD_COUNT;
	private static final MethodHandle PFS_FOLDER_CHILD;
	private static final MethodHandle PFS_FOLDER_CHILD_FOLDER;
	private static final MethodHandle PFS_FOLDER_CHILD_FILE;
	private static final MethodHandle PFS_FOLDER_CHILD_PIPE;
	private static final MethodHandle PFS_FOLDER_CREATE_FOLDER;
	private static final MethodHandle PFS_FOLDER_CREATE_FILE;
	private static final MethodHandle PFS_FOLDER_CREATE_PIPE;
	
	static {
		PFS_FOLDER_OPEN_ITER     = LINKER.downcallHandle(LOCKUP.lookup("pfs_folder_open_iter").orElseThrow(), FunctionDescriptor.of(INT, INT, INT));
		PFS_FOLDER_CHILD_COUNT   = LINKER.downcallHandle(LOCKUP.lookup("pfs_folder_child_count").orElseThrow(), FunctionDescriptor.of(LONG, INT));
		PFS_FOLDER_CHILD         = LINKER.downcallHandle(LOCKUP.lookup("pfs_folder_child").orElseThrow(), FunctionDescriptor.of(INT, INT, PNTR));
		PFS_FOLDER_CHILD_FOLDER  = LINKER.downcallHandle(LOCKUP.lookup("pfs_folder_child_folder").orElseThrow(),
				FunctionDescriptor.of(INT, INT, PNTR));
		PFS_FOLDER_CHILD_FILE    = LINKER.downcallHandle(LOCKUP.lookup("pfs_folder_child_file").orElseThrow(), FunctionDescriptor.of(INT, INT, PNTR));
		PFS_FOLDER_CHILD_PIPE    = LINKER.downcallHandle(LOCKUP.lookup("pfs_folder_child_pipe").orElseThrow(), FunctionDescriptor.of(INT, INT, PNTR));
		PFS_FOLDER_CREATE_FOLDER = LINKER.downcallHandle(LOCKUP.lookup("pfs_folder_create_folder").orElseThrow(),
				FunctionDescriptor.of(INT, INT, PNTR));
		PFS_FOLDER_CREATE_FILE   = LINKER.downcallHandle(LOCKUP.lookup("pfs_folder_create_file").orElseThrow(),
				FunctionDescriptor.of(INT, INT, PNTR));
		PFS_FOLDER_CREATE_PIPE   = LINKER.downcallHandle(LOCKUP.lookup("pfs_folder_create_pipe").orElseThrow(),
				FunctionDescriptor.of(INT, INT, PNTR));
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
			PFS_ITER_CLOSE = LINKER.downcallHandle(LOCKUP.lookup("pfs_iter_close").orElseThrow(), FunctionDescriptor.of(INT, INT));
			PFS_ITER_NEXT  = LINKER.downcallHandle(LOCKUP.lookup("pfs_iter_next").orElseThrow(), FunctionDescriptor.of(INT, INT));
		}
		
		private final int handle;
		
		private boolean   iterClosed;
		private FSElement next;
		
		public PatrFolderIter(int handle) {
			this.handle = handle;
		}
		
		private void ensureIterOpen() throws ClosedChannelException {
			if (iterClosed) { throw new ClosedChannelException(); }
		}
		
		@Override
		public FSElement nextElement() throws IOException {
			ensureIterOpen();
			FSElement n = next;
			if (n != null) {
				next = null;
				return n;
			}
			try {
				int res = (int) PFS_ITER_NEXT.invoke(this.handle);
				if (res == -1) { throw thrw(LOCKUP, PFSErrorCause.ITER_NEXT, null); }
				return new PatrFSElement(res);
			} catch (Throwable e) {
				throw thrw(e);
			}
		}
		
		@Override
		public boolean hasNextElement() throws IOException {
			ensureIterOpen();
			try {
				next = nextElement();
				return true;
			} catch (NoSuchElementException e) {
				return false;
			}
		}
		
		@Override
		public void delete() throws IOException {
			/*
			 * FIXME: implement delete()
			 * caching the last element could confuse the native iterator
			 * (native implementation/changes needed)
			 */
			throw new UnsupportedOperationException("delete");
		}
		
		@Override
		public void close() throws IOException {
			if (iterClosed) { return; }
			try {
				iterClosed = true;
				if (0 == (int) PFS_ITER_CLOSE.invoke(this.handle)) { throw thrw(LOCKUP, PFSErrorCause.CLOSE_ITER, null); }
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
			if (res == -1) { throw thrw(LOCKUP, PFSErrorCause.GET_CHILD_COUNT, null); }
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public FSElement childElement(String name) throws IOException {
		ensureOpen();
		try (MemorySession ses = MemorySession.openConfined()) {
			int res = (int) PFS_FOLDER_CHILD.invoke(this.handle, ses.allocateUtf8String(name));
			if (res == -1) { throw thrw(LOCKUP, PFSErrorCause.GET_CHILD, name); }
			return new PatrFSElement(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Folder childFolder(String name) throws IOException {
		ensureOpen();
		try (MemorySession ses = MemorySession.openConfined()) {
			int res = (int) PFS_FOLDER_CHILD_FOLDER.invoke(this.handle, ses.allocateUtf8String(name));
			if (res == -1) { throw thrw(LOCKUP, PFSErrorCause.GET_CHILD, name); }
			return new PatrFolder(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public File childFile(String name) throws IOException {
		ensureOpen();
		try (MemorySession ses = MemorySession.openConfined()) {
			int res = (int) PFS_FOLDER_CHILD_FILE.invoke(this.handle, ses.allocateUtf8String(name));
			if (res == -1) { throw thrw(LOCKUP, PFSErrorCause.GET_CHILD, name); }
			return new PatrFile(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Pipe childPipe(String name) throws IOException {
		ensureOpen();
		try (MemorySession ses = MemorySession.openConfined()) {
			int res = (int) PFS_FOLDER_CHILD_PIPE.invoke(this.handle, ses.allocateUtf8String(name));
			if (res == -1) { throw thrw(LOCKUP, PFSErrorCause.GET_CHILD, name); }
			return new PatrPipe(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Folder createFolder(String name) throws IOException {
		ensureOpen();
		try (MemorySession ses = MemorySession.openConfined()) {
			int res = (int) PFS_FOLDER_CREATE_FOLDER.invoke(this.handle, ses.allocateUtf8String(name));
			if (res == -1) { throw thrw(LOCKUP, PFSErrorCause.CREATE_CHILD, name); }
			return new PatrFolder(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public File createFile(String name) throws IOException {
		ensureOpen();
		try (MemorySession ses = MemorySession.openConfined()) {
			int res = (int) PFS_FOLDER_CREATE_FILE.invoke(this.handle, ses.allocateUtf8String(name));
			if (res == -1) { throw thrw(LOCKUP, PFSErrorCause.CREATE_CHILD, name); }
			return new PatrFile(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Pipe createPipe(String name) throws IOException {
		ensureOpen();
		try (MemorySession ses = MemorySession.openConfined()) {
			int res = (int) PFS_FOLDER_CREATE_PIPE.invoke(this.handle, ses.allocateUtf8String(name));
			if (res == -1) { throw thrw(LOCKUP, PFSErrorCause.CREATE_CHILD, name); }
			return new PatrPipe(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
}

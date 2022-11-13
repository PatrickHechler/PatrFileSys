package de.hechler.patrick.zeugs.pfs.impl;

import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.INT;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.LINKER;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.LONG;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.PNTR;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFSProvider.loaded;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFSProvider.thrw;

import java.io.IOException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySession;
import java.lang.invoke.MethodHandle;
import java.util.NoSuchElementException;

import de.hechler.patrick.zeugs.pfs.interfaces.FSElement;
import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Folder;
import de.hechler.patrick.zeugs.pfs.interfaces.Pipe;

public class PatrFolder extends PatrFSElement implements Folder {

	private final MethodHandle pfs_folder_open_iter;
	private final MethodHandle pfs_folder_child_count;
	private final MethodHandle pfs_folder_child;
	private final MethodHandle pfs_folder_child_folder;
	private final MethodHandle pfs_folder_child_file;
	private final MethodHandle pfs_folder_child_pipe;
	private final MethodHandle pfs_folder_create_folder;
	private final MethodHandle pfs_folder_create_file;
	private final MethodHandle pfs_folder_create_pipe;

	public PatrFolder(int handle) {
		super(handle);
		this.pfs_folder_open_iter = LINKER.downcallHandle(loaded.lockup.lookup("pfs_folder_open_iter").orElseThrow(),
				FunctionDescriptor.of(INT, INT, INT)).bindTo(handle);
		this.pfs_folder_child_count = LINKER
				.downcallHandle(loaded.lockup.lookup("pfs_folder_child_count").orElseThrow(),
						FunctionDescriptor.of(LONG, INT))
				.bindTo(handle);
		this.pfs_folder_child = LINKER.downcallHandle(loaded.lockup.lookup("pfs_folder_child").orElseThrow(),
				FunctionDescriptor.of(INT, INT, PNTR)).bindTo(handle);
		this.pfs_folder_child_folder = LINKER
				.downcallHandle(loaded.lockup.lookup("pfs_folder_child_folder").orElseThrow(),
						FunctionDescriptor.of(INT, INT, PNTR))
				.bindTo(handle);
		this.pfs_folder_child_file = LINKER.downcallHandle(loaded.lockup.lookup("pfs_folder_child_file").orElseThrow(),
				FunctionDescriptor.of(INT, INT, PNTR)).bindTo(handle);
		this.pfs_folder_child_pipe = LINKER.downcallHandle(loaded.lockup.lookup("pfs_folder_child_pipe").orElseThrow(),
				FunctionDescriptor.of(INT, INT, PNTR)).bindTo(handle);
		this.pfs_folder_create_folder = LINKER
				.downcallHandle(loaded.lockup.lookup("pfs_folder_create_folder").orElseThrow(),
						FunctionDescriptor.of(INT, INT, PNTR))
				.bindTo(handle);
		this.pfs_folder_create_file = LINKER
				.downcallHandle(loaded.lockup.lookup("pfs_folder_create_file").orElseThrow(),
						FunctionDescriptor.of(INT, INT, PNTR))
				.bindTo(handle);
		this.pfs_folder_create_pipe = LINKER
				.downcallHandle(loaded.lockup.lookup("pfs_folder_create_pipe").orElseThrow(),
						FunctionDescriptor.of(INT, INT, PNTR))
				.bindTo(handle);
	}

	@Override
	public FolderIter iter(boolean showHidden) throws IOException {
		try {
			int res = (int) pfs_folder_open_iter.invoke(showHidden ? 1 : 0);
			return new PatrFolderIter(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	public class PatrFolderIter implements FolderIter {

		private final MethodHandle pfs_iter_close;
		private final MethodHandle pfs_iter_next;

		private FSElement next;

		public PatrFolderIter(int handle) {
			this.pfs_iter_close = LINKER.downcallHandle(loaded.lockup.lookup("pfs_iter_close").orElseThrow(),
					FunctionDescriptor.of(INT, INT)).bindTo(handle);
			this.pfs_iter_next = LINKER.downcallHandle(loaded.lockup.lookup("pfs_iter_next").orElseThrow(),
					FunctionDescriptor.of(INT, INT)).bindTo(handle);
		}

		@Override
		public FSElement nextElement() throws IOException {
			FSElement n = next;
			if (n != null) {
				next = null;
				return n;
			}
			try {
				int res = (int) pfs_iter_next.invoke();
				if (res == -1) {
					throw thrw(loaded.lockup, "next");
				}
				return new PatrFSElement(res);
			} catch (Throwable e) {
				throw thrw(e);
			}
		}

		@Override
		public boolean hasNextElement() throws IOException {
			try {
				next = nextElement();
				return true;
			} catch (NoSuchElementException e) {
				return false;
			}
		}

		@Override
		public void delete() throws IOException {
			throw new UnsupportedOperationException("delete");
		}

		@Override
		public void close() throws IOException {
			try {
				if (0 == (int) pfs_iter_close.invoke()) {
					throw thrw(loaded.lockup, "close folder iter");
				}
			} catch (Throwable e) {
				throw thrw(e);
			}
		}

	}

	@Override
	public long childCount() throws IOException {
		try {
			long res = (long) pfs_folder_child_count.invoke();
			if (res == -1) {
				throw thrw(loaded.lockup, "childCount");
			}
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public FSElement childElement(String name) throws IOException {
		try (MemorySession ses = MemorySession.openConfined()) {
			int res = (int) pfs_folder_child.invoke(ses.allocateUtf8String(name));
			if (res == -1) {
				throw thrw(loaded.lockup, "child");
			}
			return new PatrFSElement(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public Folder childFolder(String name) throws IOException {
		try (MemorySession ses = MemorySession.openConfined()) {
			int res = (int) pfs_folder_child_folder.invoke(ses.allocateUtf8String(name));
			if (res == -1) {
				throw thrw(loaded.lockup, "child folder");
			}
			return new PatrFolder(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public File childFile(String name) throws IOException {
		try (MemorySession ses = MemorySession.openConfined()) {
			int res = (int) pfs_folder_child_file.invoke(ses.allocateUtf8String(name));
			if (res == -1) {
				throw thrw(loaded.lockup, "child file");
			}
			return new PatrFile(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public Pipe childPipe(String name) throws IOException {
		try (MemorySession ses = MemorySession.openConfined()) {
			int res = (int) pfs_folder_child_pipe.invoke(ses.allocateUtf8String(name));
			if (res == -1) {
				throw thrw(loaded.lockup, "child pipe");
			}
			return new PatrPipe(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public Folder createFolder(String name) throws IOException {
		try (MemorySession ses = MemorySession.openConfined()) {
			int res = (int) pfs_folder_create_folder.invoke(ses.allocateUtf8String(name));
			if (res == -1) {
				throw thrw(loaded.lockup, "create folder");
			}
			return new PatrFolder(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public File createFile(String name) throws IOException {
		try (MemorySession ses = MemorySession.openConfined()) {
			int res = (int) pfs_folder_create_file.invoke(ses.allocateUtf8String(name));
			if (res == -1) {
				throw thrw(loaded.lockup, "create file");
			}
			return new PatrFile(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public Pipe createPipe(String name) throws IOException {
		try (MemorySession ses = MemorySession.openConfined()) {
			int res = (int) pfs_folder_create_pipe.invoke(ses.allocateUtf8String(name));
			if (res == -1) {
				throw thrw(loaded.lockup, "create pipe");
			}
			return new PatrPipe(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
}

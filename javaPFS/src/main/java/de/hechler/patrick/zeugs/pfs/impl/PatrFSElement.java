package de.hechler.patrick.zeugs.pfs.impl;

import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.INT;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.LINKER;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.LONG;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.PNTR;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFSProvider.loaded;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFSProvider.thrw;

import java.io.IOError;
import java.io.IOException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.invoke.MethodHandle;

import de.hechler.patrick.zeugs.pfs.interfaces.FSElement;
import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Folder;
import de.hechler.patrick.zeugs.pfs.interfaces.Pipe;

public class PatrFSElement implements FSElement {
	
	private static final MethodHandle PFS_ELEMENT_CLOSE;
	private static final MethodHandle PFS_ELEMENT_PARENT;
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
	
	static {
		PFS_ELEMENT_CLOSE                = LINKER.downcallHandle(loaded.lockup.lookup("pfs_element_close").orElseThrow(),
				FunctionDescriptor.of(INT, INT));
		PFS_ELEMENT_PARENT               = LINKER.downcallHandle(loaded.lockup.lookup("pfs_element_parent").orElseThrow(),
				FunctionDescriptor.of(INT, INT));
		PFS_ELEMENT_DELETE               = LINKER.downcallHandle(loaded.lockup.lookup("pfs_element_delete").orElseThrow(),
				FunctionDescriptor.of(INT, INT));
		PFS_ELEMENT_GET_FLAGS            = LINKER.downcallHandle(loaded.lockup.lookup("pfs_element_get_flags").orElseThrow(),
				FunctionDescriptor.of(INT, INT));
		PFS_ELEMENT_MODIFY_FLAGS         = LINKER.downcallHandle(loaded.lockup.lookup("pfs_element_modify_flags").orElseThrow(),
				FunctionDescriptor.of(INT, INT, INT, INT));
		PFS_ELEMENT_GET_CREATE_TIME      = LINKER.downcallHandle(loaded.lockup.lookup("pfs_element_get_create_time").orElseThrow(),
				FunctionDescriptor.of(LONG, INT));
		PFS_ELEMENT_GET_LAST_MODIFY_TIME = LINKER.downcallHandle(loaded.lockup.lookup("pfs_element_get_last_modify_time").orElseThrow(),
				FunctionDescriptor.of(LONG, INT));
		PFS_ELEMENT_SET_CREATE_TIME      = LINKER.downcallHandle(loaded.lockup.lookup("pfs_element_set_create_time").orElseThrow(),
				FunctionDescriptor.of(INT, INT, LONG));
		PFS_ELEMENT_SET_LAST_MODIFY_TIME = LINKER.downcallHandle(loaded.lockup.lookup("pfs_element_set_last_modify_time").orElseThrow(),
				FunctionDescriptor.of(INT, INT, LONG));
		PFS_ELEMENT_GET_NAME             = LINKER.downcallHandle(loaded.lockup.lookup("pfs_element_get_name").orElseThrow(),
				FunctionDescriptor.of(INT, INT, PNTR, PNTR));
		PFS_ELEMENT_SET_NAME             = LINKER.downcallHandle(loaded.lockup.lookup("pfs_element_set_name").orElseThrow(),
				FunctionDescriptor.of(INT, INT, PNTR));
		PFS_ELEMENT_SET_PARENT           = LINKER.downcallHandle(loaded.lockup.lookup("pfs_element_set_parent").orElseThrow(),
				FunctionDescriptor.of(INT, INT, INT));
		PFS_ELEMENT_MOVE                 = LINKER.downcallHandle(loaded.lockup.lookup("pfs_element_move").orElseThrow(),
				FunctionDescriptor.of(INT, INT, INT, PNTR));
		PFS_ELEMENT_SAME                 = LINKER.downcallHandle(loaded.lockup.lookup("pfs_element_same").orElseThrow(),
				FunctionDescriptor.of(INT, INT, INT));
	}
	
	public final int handle;
	
	public PatrFSElement(int handle) {
		this.handle = handle;
	}
	
	@Override
	public Folder parent() throws IOException {
		try {
			int res = (int) PFS_ELEMENT_PARENT.invoke(this.handle);
			if (res == -1) { throw thrw(loaded.lockup, "get parent"); }
			return new PatrFolder(res);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public int flags() throws IOException {
		try {
			int res = (int) PFS_ELEMENT_GET_FLAGS.invoke(this.handle);
			if (res == -1) { throw thrw(loaded.lockup, "get flags"); }
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void flag(int add, int rem) throws IOException {
		try {
			if (0 == (int) PFS_ELEMENT_MODIFY_FLAGS.invoke(this.handle, add, rem)) { throw thrw(loaded.lockup, "modify flags"); }
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public long lastModTime() throws IOException {
		try {
			long res = (long) PFS_ELEMENT_GET_LAST_MODIFY_TIME.invoke(this.handle);
			if (res == -1) { throw thrw(loaded.lockup, "get last modify time"); }
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void lastModTime(long time) throws IOException {
		try {
			if (0 == (int) PFS_ELEMENT_SET_LAST_MODIFY_TIME.invoke(this.handle, time)) { throw thrw(loaded.lockup, "set last modify time"); }
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public long createTime() throws IOException {
		try {
			long res = (long) PFS_ELEMENT_GET_CREATE_TIME.invoke(this.handle);
			if (res == -1) { throw thrw(loaded.lockup, "get create time"); }
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void createTime(long time) throws IOException {
		try {
			if (0 == (int) PFS_ELEMENT_SET_CREATE_TIME.invoke(this.handle, time)) { throw thrw(loaded.lockup, "set create time"); }
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public String name() throws IOException {
		try (MemorySession ses = MemorySession.openShared()) {
			MemorySegment data = ses.allocate(16);
			data.set(PNTR, 0L, MemoryAddress.NULL);
			data.set(LONG, 8L, 0L);
			if (0 == (int) PFS_ELEMENT_GET_NAME.invoke(this.handle, data, data.asSlice(8L, 8L))) { throw thrw(loaded.lockup, "get name"); }
			return data.get(PNTR, 0L).getUtf8String(0L);
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void name(String name) throws IOException {
		try (MemorySession ses = MemorySession.openConfined()) {
			if (0 == (int) PFS_ELEMENT_SET_NAME.invoke(this.handle, ses.allocateUtf8String(name))) { throw thrw(loaded.lockup, "set name"); }
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void parent(Folder parent) throws IOException {
		try {
			if (parent instanceof PatrFolder p) {
				if (0 == (int) PFS_ELEMENT_SET_PARENT.invoke(this.handle, p.handle)) { throw thrw(loaded.lockup, "set parent"); }
			} else {
				throw new ClassCastException("parent is not of class PatrFolder, but of " + parent.getClass());
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void move(Folder parent, String name) throws IOException {
		try (MemorySession ses = MemorySession.openConfined()) {
			if (parent instanceof PatrFolder p) {
				if (0 == (int) PFS_ELEMENT_MOVE.invoke(this.handle, p.handle, ses.allocateUtf8String(name))) { throw thrw(loaded.lockup, "move"); }
			} else {
				throw new ClassCastException("parent is not of class PatrFolder, but of " + parent.getClass());
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void delete() throws IOException {
		try {
			if (0 == (int) PFS_ELEMENT_DELETE.invoke(this.handle)) { throw thrw(loaded.lockup, "delete"); }
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void close() throws IOException {
		try {
			if (0 == (int) PFS_ELEMENT_CLOSE.invoke(this.handle)) { throw thrw(loaded.lockup, "close"); }
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Folder getFolder() throws IOException {
		if (this instanceof Folder res) {
			return res;
		} else if (isFolder()) {
			return new PatrFolder(handle);
		} else {
			throw new IllegalStateException("this is no folder!");
		}
	}
	
	@Override
	public File getFile() throws IOException {
		if (this instanceof File res) {
			return res;
		} else if (isFile()) {
			return new PatrFile(handle);
		} else {
			throw new IllegalStateException("this is no file!");
		}
	}
	
	@Override
	public Pipe getPipe() throws IOException {
		if (this instanceof Pipe res) {
			return res;
		} else if (isPipe()) {
			return new PatrPipe(handle);
		} else {
			throw new IllegalStateException("this is no pipe!");
		}
	}
	
	@Override
	public boolean equals(FSElement other) throws IOException {
		try {
			if (other instanceof PatrFSElement pe) {
				int res = (int) PFS_ELEMENT_SAME.invoke(this.handle, pe.handle);
				return switch (res) {
				case 0 -> false;
				case 1 -> true;
				case -1 -> throw thrw(loaded.lockup, "same");
				default -> throw thrw(loaded.lockup, "same (unknown result: " + res + ")");
				};
			} else {
				throw new ClassCastException("other is not of class PatrFSElement, but of " + other.getClass());
			}
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
	
}

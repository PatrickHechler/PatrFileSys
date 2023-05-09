package de.hechler.patrick.zeugs.pfs.impl.pfs;

import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.INT;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.LINKER;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.LOCKUP;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.LONG;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.PNTR;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider.thrw;

import java.io.IOError;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.nio.channels.ClosedChannelException;

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
		PFS_ELEMENT_CLOSE = LINKER.downcallHandle(LOCKUP.find("pfs_element_close").orElseThrow(),
				FunctionDescriptor.of(INT, INT));
		PFS_ELEMENT_PARENT = LINKER.downcallHandle(LOCKUP.find("pfs_element_parent").orElseThrow(),
				FunctionDescriptor.of(INT, INT));
		PFS_ELEMENT_DELETE = LINKER.downcallHandle(LOCKUP.find("pfs_element_delete").orElseThrow(),
				FunctionDescriptor.of(INT, INT));
		PFS_ELEMENT_GET_FLAGS = LINKER.downcallHandle(LOCKUP.find("pfs_element_get_flags").orElseThrow(),
				FunctionDescriptor.of(INT, INT));
		PFS_ELEMENT_MODIFY_FLAGS = LINKER.downcallHandle(LOCKUP.find("pfs_element_modify_flags").orElseThrow(),
				FunctionDescriptor.of(INT, INT, INT, INT));
		PFS_ELEMENT_GET_CREATE_TIME = LINKER.downcallHandle(LOCKUP.find("pfs_element_get_create_time").orElseThrow(),
				FunctionDescriptor.of(LONG, INT));
		PFS_ELEMENT_GET_LAST_MODIFY_TIME = LINKER.downcallHandle(
				LOCKUP.find("pfs_element_get_last_modify_time").orElseThrow(), FunctionDescriptor.of(LONG, INT));
		PFS_ELEMENT_SET_CREATE_TIME = LINKER.downcallHandle(LOCKUP.find("pfs_element_set_create_time").orElseThrow(),
				FunctionDescriptor.of(INT, INT, LONG));
		PFS_ELEMENT_SET_LAST_MODIFY_TIME = LINKER.downcallHandle(
				LOCKUP.find("pfs_element_set_last_modify_time").orElseThrow(), FunctionDescriptor.of(INT, INT, LONG));
		PFS_ELEMENT_GET_NAME = LINKER.downcallHandle(LOCKUP.find("pfs_element_get_name").orElseThrow(),
				FunctionDescriptor.of(INT, INT, PNTR, PNTR));
		PFS_ELEMENT_SET_NAME = LINKER.downcallHandle(LOCKUP.find("pfs_element_set_name").orElseThrow(),
				FunctionDescriptor.of(INT, INT, PNTR));
		PFS_ELEMENT_SET_PARENT = LINKER.downcallHandle(LOCKUP.find("pfs_element_set_parent").orElseThrow(),
				FunctionDescriptor.of(INT, INT, INT));
		PFS_ELEMENT_MOVE = LINKER.downcallHandle(LOCKUP.find("pfs_element_move").orElseThrow(),
				FunctionDescriptor.of(INT, INT, INT, PNTR));
		PFS_ELEMENT_SAME = LINKER.downcallHandle(LOCKUP.find("pfs_element_same").orElseThrow(),
				FunctionDescriptor.of(INT, INT, INT));
	}
	
	public final int  handle;
	protected boolean closed;
	
	public PatrFSElement(int handle) {
		this.handle = handle;
	}
	
	protected void ensureOpen() throws ClosedChannelException {
		if (closed) { throw new ClosedChannelException(); }
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
			return data.get(PNTR, 0L).getUtf8String(0L);
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
			if (0 == (int) PFS_ELEMENT_DELETE.invoke(this.handle)) { throw thrw(PFSErrorCause.DELETE_ELEMENT, null); }
			closed = true;
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
	public void close() throws IOException {
		if (closed) { return; }
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
			if (other instanceof PatrFSElement pe) {
				int res = (int) PFS_ELEMENT_SAME.invoke(this.handle, pe.handle);
				return switch (res) {
				case 0 -> false;
				case 1 -> true;
				case -1 -> throw thrw(PFSErrorCause.SAME, null);
				default -> throw thrw(PFSErrorCause.SAME, res);
				};
			} else {
				return false;
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
	
	@Override
	public String toString() {
		FSElement e = this;
		try {
			StringBuilder b = new StringBuilder();
			if (isFolder()) {
				b.append('/');
			}
			while (true) {
				String n = e.name();
				if (n.isEmpty()) { // root has an empty name
					break;
				}
				b.insert(0, '/').insert(0, n);
				FSElement p = e.parent();
				if (e != this) {
					e.close();
				}
				e = p;
			}
			if (e != this) {
				e.close();
			}
			return b.toString();
		} catch (IOException ioe) {
			if (e != this) {
				try {
					e.close();
				} catch (IOException ioe2) {/**/}
			}
			return "<error: could not get path: " + ioe + ">";
		}
	}
	
}

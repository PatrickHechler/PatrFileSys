package de.hechler.patrick.pfs.element.impl;

import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSys.pfsErrno;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSys.t;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Constants.EH_SIZE;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Element.DELETE;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Element.GET_CREATE_TIME;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Element.GET_FLAGS;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Element.GET_LAST_MOD_TIME;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Element.GET_NAME;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Element.GET_NAME_LENGTH;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Element.GET_PARENT;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Element.MODIFY_FLAGS;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Element.SET_CREATE_TIME;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Element.SET_NAME;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Element.SET_PAREMT;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.FileSys.FS;
import static jdk.incubator.foreign.ValueLayout.ADDRESS;
import static jdk.incubator.foreign.ValueLayout.JAVA_INT;
import static jdk.incubator.foreign.ValueLayout.JAVA_LONG;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import de.hechler.patrick.pfs.element.PFSElement;
import de.hechler.patrick.pfs.exceptions.PFSErr;
import de.hechler.patrick.pfs.file.PFSFile;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.folder.impl.NativePatrFileSysFolder;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSys;
import de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Constants;
import de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Errno;
import de.hechler.patrick.pfs.pipe.PFSPipe;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;

public abstract class NativePatrFileSysElement implements PFSElement {
	
	@SuppressWarnings("unchecked")
	public static final Reference <NativePatrFileSysFolder> EMPTY_REF = (Reference <
		NativePatrFileSysFolder>) (Reference <?>) JavaPatrFileSysElement.EMPTY_REF;
	
	public final NativePatrFileSys             pfs;
	public final MemorySegment                 eh;
	public Reference <NativePatrFileSysFolder> parent;
	
	public NativePatrFileSysElement(NativePatrFileSys pfs, MemorySegment root, Reference <
		NativePatrFileSysFolder> parent) {
		this.pfs = pfs;
		this.eh = root;
		this.parent = parent;
	}
	
	@Override
	public int flags() throws IOException {
		try {
			FS.set(ADDRESS, 0L, pfs.bm);
			int flags = (int) GET_FLAGS.invoke(eh);
			if (flags == -1) {
				throw PFSErr.createAndThrow(pfsErrno(), "get flags");
			}
			return flags;
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void modifyFlags(int addFlags, int remFlags) throws IOException {
		try {
			FS.set(ADDRESS, 0L, pfs.bm);
			if (0 == (int) MODIFY_FLAGS.invoke(eh, addFlags, remFlags)) {
				throw PFSErr.createAndThrow(pfsErrno(), "modify flags");
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public String name() throws IOException {
		try {
			FS.set(ADDRESS, 0L, pfs.bm);
			long len = (long) GET_NAME_LENGTH.invoke(eh);
			if (len == -1L) {
				throw PFSErr.createAndThrow(pfsErrno(), "get name");
			}
			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator alloc = SegmentAllocator.nativeAllocator(scope);
				MemorySegment mem = alloc.allocate(len + 17L);
				MemoryAddress lenAddr = mem.address().addOffset(len + 1L);
				MemoryAddress memPntr = lenAddr.addOffset(8L);
				lenAddr.set(JAVA_LONG, 0L, len + 1);
				memPntr.set(ADDRESS, 0L, memPntr);
				if (0 == (int) GET_NAME.invoke(eh, memPntr, lenAddr)) {
					throw PFSErr.createAndThrow(pfsErrno(), "get name");
				}
				assert memPntr.get(ADDRESS, 0L).toRawLongValue() == mem.address().toRawLongValue();
				return mem.getUtf8String(0L);
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void name(String newName) throws IOException {
		try {
			if (newName == null) {
				throw PFSErr.createAndThrow(Errno.ILLEGAL_ARG, "newName is null");
			}
			FS.set(ADDRESS, 0L, pfs.bm);
			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator alloc = SegmentAllocator.nativeAllocator(scope);
				MemorySegment newUtf8Name = alloc.allocateUtf8String(newName);
				if (0 == (int) SET_NAME.invoke(eh, newUtf8Name)) {
					throw PFSErr.createAndThrow(pfsErrno(), "set name");
				}
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public long createTime() throws IOException {
		try {
			FS.set(ADDRESS, 0L, pfs.bm);
			long time = (long) GET_CREATE_TIME.invoke(eh);
			if (time == -1L) {
				int err = pfsErrno();
				if (err != Errno.NONE) {
					throw PFSErr.createAndThrow(err, "get create time");
				}
			}
			return time;
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void createTime(long ct) throws IOException {
		try {
			FS.set(ADDRESS, 0L, pfs.bm);
			if (0 == (int) SET_CREATE_TIME.invoke(eh, ct)) {
				throw PFSErr.createAndThrow(pfsErrno(), "set create time");
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public long lastModTime() throws IOException {
		try {
			FS.set(ADDRESS, 0L, pfs.bm);
			long time = (long) GET_LAST_MOD_TIME.invoke(eh);
			if (time == -1L) {
				int err = pfsErrno();
				if (err != Errno.NONE) {
					throw PFSErr.createAndThrow(err, "get last mode time");
				}
			}
			return time;
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void lastModTime(long lmt) throws IOException {
		try {
			FS.set(ADDRESS, 0L, pfs.bm);
			if (0 == (int) SET_CREATE_TIME.invoke(eh, lmt)) {
				throw PFSErr.createAndThrow(pfsErrno(), "set create time");
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void delete() throws IOException {
		try {
			FS.set(ADDRESS, 0L, pfs.bm);
			if (0 == (int) DELETE.invoke(eh)) {
				throw PFSErr.createAndThrow(pfsErrno(), "delete");
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public PFSFolder parent() throws IOException {
		try {
			NativePatrFileSysFolder p = parent.get();
			if (p == null) {
				MemorySegment mem = pfs.alloc.allocate(Constants.EH_SIZE);
				p = new NativePatrFileSysFolder(pfs, mem, EMPTY_REF);
				parent = new SoftReference <NativePatrFileSysFolder>(p);
			}
			p.eh.copyFrom(eh);
			FS.set(ADDRESS, 0L, pfs.bm);
			if (0 == (int) GET_PARENT.invoke(p.eh)) {
				throw PFSErr.createAndThrow(pfsErrno(), "get parent");
			}
			return p;
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void parent(PFSFolder newParent) throws IOException {
		try {
			if (newParent == null) {
				throw PFSErr.createAndThrow(Errno.ILLEGAL_ARG, "new parent is null");
			}
			if ( ! (newParent instanceof NativePatrFileSysFolder)) {
				throw PFSErr.createAndThrow(Errno.ILLEGAL_ARG,
					"new parent belongs to a diffrent file system");
			}
			NativePatrFileSysFolder np = (NativePatrFileSysFolder) newParent;
			if (np.pfs != pfs) {
				throw PFSErr.createAndThrow(Errno.ILLEGAL_ARG,
					"new parent belongs to a diffrent file system");
			}
			FS.set(ADDRESS, 0L, pfs.bm);
			if (0 == (int) SET_PAREMT.invoke(eh, np.eh)) {
				throw PFSErr.createAndThrow(pfsErrno(), "delete");
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void move(PFSFolder newParent, String newName) throws IOException {
		try {
			if (newParent == null) {
				throw PFSErr.createAndThrow(Errno.ILLEGAL_ARG, "new parent is null");
			}
			if ( ! (newParent instanceof NativePatrFileSysFolder)) {
				throw PFSErr.createAndThrow(Errno.ILLEGAL_ARG,
					"new parent belongs to a diffrent file system");
			}
			NativePatrFileSysFolder np = (NativePatrFileSysFolder) newParent;
			if (np.pfs != pfs) {
				throw PFSErr.createAndThrow(Errno.ILLEGAL_ARG,
					"new parent belongs to a diffrent file system");
			}
			if (newName == null) {
				throw PFSErr.createAndThrow(Errno.ILLEGAL_ARG, "newName is null");
			}
			FS.set(ADDRESS, 0L, pfs.bm);
			if (0 == (int) SET_PAREMT.invoke(eh, np.eh)) {
				throw PFSErr.createAndThrow(pfsErrno(), "delete");
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public PFSFolder toFolder() throws IllegalStateException {
		throw new IllegalStateException("this element is no folder");
	}
	
	@Override
	public PFSFile toFile() throws IllegalStateException {
		throw new IllegalStateException("this element is no file");
	}
	
	@Override
	public PFSPipe toPipe() throws IllegalStateException {
		throw new IllegalStateException("this element is no pipe");
	}
	
	@Override
	public boolean isFolder() {
		return false;
	}
	
	@Override
	public boolean isFile() {
		return false;
	}
	
	@Override
	public boolean isPipe() {
		return false;
	}
	
	@Override
	public boolean isRoot() {
		return false;
	}
	
	@Override
	public int hashCode() {
		int hash = 0;
		for (long off = 0; off < EH_SIZE; off += 4) {
			hash ^= eh.get(JAVA_INT, off);
		}
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null) return false;
		if (obj.getClass() != getClass()) return false;
		NativePatrFileSysElement other = (NativePatrFileSysElement) obj;
		for (long off = 0; off < EH_SIZE; off += 4) {
			if (other.eh.get(JAVA_INT, off) != eh.get(JAVA_INT, off)) {
				return false;
			}
		}
		return true;
	}
	
}

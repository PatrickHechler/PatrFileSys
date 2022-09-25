package de.hechler.patrick.pfs.fs.impl;

import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSys.pfsErrno;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSys.t;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Element.DELETE;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Element.*;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Element.GET_FLAGS;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Element.GET_LAST_MOD_TIME;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Element.GET_NAME;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Element.GET_NAME_LENGTH;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Element.MODIFY_FLAGS;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Element.SET_CREATE_TIME;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Element.SET_NAME;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.FileSys.FS;
import static jdk.incubator.foreign.ValueLayout.ADDRESS;
import static jdk.incubator.foreign.ValueLayout.JAVA_LONG;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import de.hechler.patrick.pfs.element.PFSElement;
import de.hechler.patrick.pfs.exceptions.PFSErr;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.folder.impl.NativePatrFileSysFolder;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Constants;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Errno;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;

public class NativePatrFileSysElement implements PFSElement {
	
	private static final Reference <NativePatrFileSysFolder> EMPTY_REF = new PhantomReference <NativePatrFileSysFolder>(null, null);
	
	protected final NativePatrFileSys             pfs;
	protected final MemorySegment                 eh;
	protected Reference <NativePatrFileSysFolder> parent;
	
	public NativePatrFileSysElement(NativePatrFileSys pfs, MemorySegment root,
		Reference <NativePatrFileSysFolder> parent) {
		this.pfs = pfs;
		this.eh = root;
		this.parent = parent;
	}
	
	@Override
	public int flags() throws PatrFileSysException {
		try {
			FS.set(ADDRESS, 0L, pfs.bm);
			int flags = (int) GET_FLAGS.invoke(eh);
			if (flags == -1) {
				throw PFSErr.create(pfsErrno(), "get flags");
			}
			return flags;
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void modifyFlags(int addFlags, int remFlags) throws PatrFileSysException {
		try {
			FS.set(ADDRESS, 0L, pfs.bm);
			if (0 == (int) MODIFY_FLAGS.invoke(eh, addFlags, remFlags)) {
				throw PFSErr.create(pfsErrno(), "modify flags");
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public String name() throws PatrFileSysException {
		try {
			FS.set(ADDRESS, 0L, pfs.bm);
			long len = (long) GET_NAME_LENGTH.invoke(eh);
			if (len == -1L) {
				throw PFSErr.create(pfsErrno(), "get name");
			}
			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator alloc = SegmentAllocator.nativeAllocator(scope);
				MemorySegment mem = alloc.allocate(len + 17L);
				MemoryAddress lenAddr = mem.address().addOffset(len + 1L);
				MemoryAddress memPntr = lenAddr.addOffset(8L);
				lenAddr.set(JAVA_LONG, 0L, len + 1);
				memPntr.set(ADDRESS, 0L, memPntr);
				if (0 == (int) GET_NAME.invoke(eh, memPntr, lenAddr)) {
					throw PFSErr.create(pfsErrno(), "get name");
				}
				assert memPntr.get(ADDRESS, 0L).toRawLongValue() == mem.address().toRawLongValue();
				return mem.getUtf8String(0L);
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void name(String newName) throws PatrFileSysException {
		try {
			if (newName == null) {
				throw PFSErr.create(Errno.ILLEGAL_ARG, "newName is null");
			}
			FS.set(ADDRESS, 0L, pfs.bm);
			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator alloc = SegmentAllocator.nativeAllocator(scope);
				MemorySegment newUtf8Name = alloc.allocateUtf8String(newName);
				if (0 == (int) SET_NAME.invoke(eh, newUtf8Name)) {
					throw PFSErr.create(pfsErrno(), "set name");
				}
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public long createTime() throws PatrFileSysException {
		try {
			FS.set(ADDRESS, 0L, pfs.bm);
			long time = (long) GET_CREATE_TIME.invoke(eh);
			if (time == -1L) {
				int err = pfsErrno();
				if (err != Errno.NONE) {
					throw PFSErr.create(err, "get create time");
				}
			}
			return time;
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void createTime(long ct) throws PatrFileSysException {
		try {
			FS.set(ADDRESS, 0L, pfs.bm);
			if (0 == (int) SET_CREATE_TIME.invoke(eh, ct)) {
				throw PFSErr.create(pfsErrno(), "set create time");
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public long lastModTime() throws PatrFileSysException {
		try {
			FS.set(ADDRESS, 0L, pfs.bm);
			long time = (long) GET_LAST_MOD_TIME.invoke(eh);
			if (time == -1L) {
				int err = pfsErrno();
				if (err != Errno.NONE) {
					throw PFSErr.create(err, "get last mode time");
				}
			}
			return time;
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void lastModTime(long lmt) throws PatrFileSysException {
		try {
			FS.set(ADDRESS, 0L, pfs.bm);
			if (0 == (int) SET_CREATE_TIME.invoke(eh, lmt)) {
				throw PFSErr.create(pfsErrno(), "set create time");
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void delete() throws PatrFileSysException {
		try {
			FS.set(ADDRESS, 0L, pfs.bm);
			if (0 == (int) DELETE.invoke(eh)) {
				throw PFSErr.create(pfsErrno(), "delete");
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public PFSFolder parent() throws PatrFileSysException {
		try {
			NativePatrFileSysFolder p = parent.get();
			if (p != null) {
				MemorySegment mem = pfs.alloc.allocate(Constants.EH_SIZE);
				p = new NativePatrFileSysFolder(pfs, mem, EMPTY_REF);
				parent = new SoftReference <NativePatrFileSysFolder>(p);
			}
			p.eh.copyFrom(eh);
			FS.set(ADDRESS, 0L, pfs.bm);
			if (0 == (int) GET_PARENT.invoke(p.eh)) {
				throw PFSErr.create(pfsErrno(), "get parent");
			}
			return p;
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void parent(PFSFolder newParent) throws PatrFileSysException {
		try {
			if (newParent == null) {
				throw new PatrFileSysException(Errno.ILLEGAL_ARG, "new parent is null");
			}
			if ( ! (newParent instanceof NativePatrFileSysFolder)) {
				throw new PatrFileSysException(Errno.ILLEGAL_ARG, "new parent belongs to a diffrent file system");
			}
			NativePatrFileSysFolder np = (NativePatrFileSysFolder) newParent;
			if (np.pfs != pfs) {
				throw new PatrFileSysException(Errno.ILLEGAL_ARG, "new parent belongs to a diffrent file system");
			}
			FS.set(ADDRESS, 0L, pfs.bm);
			if (0 == (int) SET_PAREMT.invoke(eh, np.eh)) {
				throw PFSErr.create(pfsErrno(), "delete");
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void move(PFSFolder newParent, String newName) throws PatrFileSysException {
		try {
			if (newParent == null) {
				throw PFSErr.create(Errno.ILLEGAL_ARG, "new parent is null");
			}
			if ( ! (newParent instanceof NativePatrFileSysFolder)) {
				throw PFSErr.create(Errno.ILLEGAL_ARG, "new parent belongs to a diffrent file system");
			}
			NativePatrFileSysFolder np = (NativePatrFileSysFolder) newParent;
			if (np.pfs != pfs) {
				throw PFSErr.create(Errno.ILLEGAL_ARG, "new parent belongs to a diffrent file system");
			}
			if (newName == null) {
				throw PFSErr.create(Errno.ILLEGAL_ARG, "newName is null");
			}
			FS.set(ADDRESS, 0L, pfs.bm);
			if (0 == (int) SET_PAREMT.invoke(eh, np.eh)) {
				throw PFSErr.create(pfsErrno(), "delete");
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
}

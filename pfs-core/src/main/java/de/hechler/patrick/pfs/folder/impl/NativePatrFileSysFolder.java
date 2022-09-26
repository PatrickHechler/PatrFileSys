package de.hechler.patrick.pfs.folder.impl;

import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSys.pfsErrno;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSys.t;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Constants.EH_SIZE;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Constants.FI_SIZE;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Constants.FLAGS_ESSENTIAL_FLAGS;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Constants.FLAGS_FILE;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Constants.FLAGS_FOLDER;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Element.GET_FLAGS;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.FileSys.ERRNO;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Folder.*;
import static jdk.incubator.foreign.ValueLayout.JAVA_INT;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import de.hechler.patrick.pfs.element.PFSElement;
import de.hechler.patrick.pfs.exceptions.PFSErr;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.file.PFSFile;
import de.hechler.patrick.pfs.file.impl.NativePatrFileSysFile;
import de.hechler.patrick.pfs.folder.FolderIter;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSys;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Errno;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSysElement;
import de.hechler.patrick.pfs.pipe.PFSPipe;
import de.hechler.patrick.pfs.pipe.impl.NativePatrFileSysPipe;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;

@SuppressWarnings("exports")
public class NativePatrFileSysFolder extends NativePatrFileSysElement implements PFSFolder {
	
	public NativePatrFileSysFolder(NativePatrFileSys pfs, MemorySegment root, Reference <NativePatrFileSysFolder> parent) {
		super(pfs, root, parent);
	}
	
	@Override
	public FolderIter iterator(boolean showHidden) throws PatrFileSysException {
		try {
			MemorySegment ehSeg = pfs.alloc.allocate(EH_SIZE);
			MemorySegment fiSeg = pfs.alloc.allocate(FI_SIZE);
			ehSeg.copyFrom(ehSeg);
			if (0 == (int) FILL_ITERATOR.invoke(ehSeg, fiSeg, showHidden ? 1 : 0)) {
				throw PFSErr.create(pfsErrno(), "fill folder iter");
			}
			return new NativeFolderIter(ehSeg, fiSeg);
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	private class NativeFolderIter implements FolderIter {
		
		private final MemorySegment fieh;
		private final MemorySegment fi;
		
		private PFSElement next;
		
		public NativeFolderIter(MemorySegment fieh, MemorySegment fi) {
			this.fieh = fieh;
			this.fi = fi;
		}
		
		@Override
		public PFSElement getNext() throws PatrFileSysException {
			try {
				if (0 == (int) ITER_NEXT.invoke(fi)) {
					int errno = pfsErrno();
					if (errno == Errno.NO_MORE_ELEMNETS) {
						ERRNO.set(JAVA_INT, 0L, Errno.NONE);
						return null;
					}
					throw PFSErr.create(errno, "folder iter get next");
				}
				MemorySegment ehSeg = pfs.alloc.allocate(EH_SIZE);
				ehSeg.copyFrom(fieh);
				return newElement(ehSeg);
			} catch (Throwable e) {
				throw t(e);
			}
		}
		
		@Override
		public boolean hasNextElement() throws PatrFileSysException {
			next = getNext();
			return next != null;
		}
		
	}
	
	@Override
	public long childCount() throws PatrFileSysException {
		try {
			long val = (long) CHILD_COUNT.invoke(eh);
			if (val == -1L) {
				throw PFSErr.create(pfsErrno(), "get child count");
			}
			return val;
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public PFSElement element(String childName) throws PatrFileSysException {
		try {
			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator alloc = SegmentAllocator.nativeAllocator(scope);
				MemorySegment u8str = alloc.allocateUtf8String(childName);
				MemorySegment ehSeg = pfs.alloc.allocate(EH_SIZE);
				ehSeg.copyFrom(eh);
				if (0 == (int) CHILD_FROM_NAME.invoke(ehSeg, u8str)) {
					throw PFSErr.create(pfsErrno(), "get folder child");
				}
				return newElement(ehSeg);
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public PFSFolder folder(String childName) throws PatrFileSysException {
		try {
			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator alloc = SegmentAllocator.nativeAllocator(scope);
				MemorySegment u8str = alloc.allocateUtf8String(childName);
				MemorySegment ehSeg = pfs.alloc.allocate(EH_SIZE);
				ehSeg.copyFrom(eh);
				if (0 == (int) FOLDER_CHILD_FROM_NAME.invoke(ehSeg, u8str)) {
					throw PFSErr.create(pfsErrno(), "get folder child");
				}
				return new NativePatrFileSysFolder(pfs, ehSeg, new SoftReference <NativePatrFileSysFolder>(this));
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public PFSFile file(String childName) throws PatrFileSysException {
		try {
			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator alloc = SegmentAllocator.nativeAllocator(scope);
				MemorySegment u8str = alloc.allocateUtf8String(childName);
				MemorySegment ehSeg = pfs.alloc.allocate(EH_SIZE);
				ehSeg.copyFrom(eh);
				if (0 == (int) FILE_CHILD_FROM_NAME.invoke(ehSeg, u8str)) {
					throw PFSErr.create(pfsErrno(), "get folder child");
				}
				return new NativePatrFileSysFile(pfs, ehSeg, new SoftReference <NativePatrFileSysFolder>(this));
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public PFSPipe pipe(String childName) throws PatrFileSysException {
		try {
			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator alloc = SegmentAllocator.nativeAllocator(scope);
				MemorySegment u8str = alloc.allocateUtf8String(childName);
				MemorySegment ehSeg = pfs.alloc.allocate(EH_SIZE);
				ehSeg.copyFrom(eh);
				if (0 == (int) PIPE_CHILD_FROM_NAME.invoke(ehSeg, u8str)) {
					throw PFSErr.create(pfsErrno(), "get folder child");
				}
				return new NativePatrFileSysPipe(pfs, ehSeg, new SoftReference <NativePatrFileSysFolder>(this));
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public PFSFolder addFolder(String newChildName) throws PatrFileSysException {
		try {
			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator alloc = SegmentAllocator.nativeAllocator(scope);
				MemorySegment u8str = alloc.allocateUtf8String(newChildName);
				MemorySegment ehSeg = pfs.alloc.allocate(EH_SIZE);
				ehSeg.copyFrom(eh);
				if (0 == (int) CREATE_FOLDER.invoke(ehSeg, eh, u8str)) {
					throw PFSErr.create(pfsErrno(), "get folder child");
				}
				return new NativePatrFileSysFolder(pfs, ehSeg, new SoftReference <NativePatrFileSysFolder>(this));
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public PFSFile addFile(String newChildName) throws PatrFileSysException {
		try {
			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator alloc = SegmentAllocator.nativeAllocator(scope);
				MemorySegment u8str = alloc.allocateUtf8String(newChildName);
				MemorySegment ehSeg = pfs.alloc.allocate(EH_SIZE);
				ehSeg.copyFrom(eh);
				if (0 == (int) CREATE_FOLDER.invoke(ehSeg, eh, u8str)) {
					throw PFSErr.create(pfsErrno(), "get folder child");
				}
				return new NativePatrFileSysFile(pfs, ehSeg, new SoftReference <NativePatrFileSysFolder>(this));
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public PFSPipe addPipe(String newChildName) throws PatrFileSysException {
		try {
			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator alloc = SegmentAllocator.nativeAllocator(scope);
				MemorySegment u8str = alloc.allocateUtf8String(newChildName);
				MemorySegment ehSeg = pfs.alloc.allocate(EH_SIZE);
				ehSeg.copyFrom(eh);
				if (0 == (int) CREATE_FOLDER.invoke(ehSeg, eh, u8str)) {
					throw PFSErr.create(pfsErrno(), "get folder child");
				}
				return new NativePatrFileSysPipe(pfs, ehSeg, new SoftReference <NativePatrFileSysFolder>(this));
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public PFSFolder toFolder() throws IllegalStateException {
		return this;
	}
	
	private PFSElement newElement(MemorySegment ehSeg) throws Throwable, InternalError {
		int flags = (int) GET_FLAGS.invoke(ehSeg);
		switch (flags & FLAGS_ESSENTIAL_FLAGS) {
		case FLAGS_FOLDER:
			return new NativePatrFileSysFolder(pfs, ehSeg, new SoftReference <NativePatrFileSysFolder>(NativePatrFileSysFolder.this));
		case FLAGS_FILE:
			return new NativePatrFileSysFile(pfs, ehSeg, new SoftReference <NativePatrFileSysFolder>(NativePatrFileSysFolder.this));
		case PFS_FLAGS_PIPE:
			return new NativePatrFileSysPipe(pfs, ehSeg, new SoftReference <NativePatrFileSysFolder>(NativePatrFileSysFolder.this));
		default:
			throw new InternalError("unknown internal flag compination! flags=0x" + Integer.toHexString(flags));
		}
	}
	
}

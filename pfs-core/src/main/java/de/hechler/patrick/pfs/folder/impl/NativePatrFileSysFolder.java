package de.hechler.patrick.pfs.folder.impl;

import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSys.pfsErrno;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSys.t;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Constants.EH_SIZE;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Constants.FI_SIZE;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Constants.FLAGS_ESSENTIAL_FLAGS;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Element.GET_FLAGS;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.FileSys.ERRNO;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Folder.CHILD_COUNT;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Folder.CHILD_FROM_NAME;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Folder.CREATE_FOLDER;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Folder.FILE_CHILD_FROM_NAME;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Folder.FILL_ITERATOR;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Folder.FOLDER_CHILD_FROM_NAME;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Folder.ITER_NEXT;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Folder.PIPE_CHILD_FROM_NAME;
import static jdk.incubator.foreign.ValueLayout.JAVA_INT;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import de.hechler.patrick.pfs.SoftRef;
import de.hechler.patrick.pfs.element.PFSElement;
import de.hechler.patrick.pfs.element.impl.NativePatrFileSysElement;
import de.hechler.patrick.pfs.exceptions.PFSErr;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.file.PFSFile;
import de.hechler.patrick.pfs.file.impl.NativePatrFileSysFile;
import de.hechler.patrick.pfs.folder.FolderIter;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSys;
import de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Errno;
import de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry.Flags;
import de.hechler.patrick.pfs.pipe.PFSPipe;
import de.hechler.patrick.pfs.pipe.impl.NativePatrFileSysPipe;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;

@SuppressWarnings("exports")
public class NativePatrFileSysFolder extends NativePatrFileSysElement implements PFSFolder {
	
	public final Map <Object, SoftRef <NativePatrFileSysElement>> childs;
	
	public NativePatrFileSysFolder(NativePatrFileSys pfs, MemorySegment root, Reference <
		NativePatrFileSysFolder> parent) {
		super(pfs, root, parent);
		this.childs = new HashMap <>();
	}
	
	@Override
	public FolderIter iterator(boolean showHidden) throws PatrFileSysException {
		try {
			MemorySegment ehSeg = pfs.alloc.allocate(EH_SIZE);
			MemorySegment fiSeg = pfs.alloc.allocate(FI_SIZE);
			ehSeg.copyFrom(ehSeg);
			if (0 == (int) FILL_ITERATOR.invoke(ehSeg, fiSeg, showHidden ? 1 : 0)) {
				throw PFSErr.createAndThrow(pfsErrno(), "fill folder iter");
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
					throw PFSErr.createAndThrow(errno, "folder iter get next");
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
				throw PFSErr.createAndThrow(pfsErrno(), "get child count");
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
					throw PFSErr.createAndThrow(pfsErrno(), "get folder child");
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
					throw PFSErr.createAndThrow(pfsErrno(), "get folder child");
				}
				NativePatrFileSysFolder result = new NativePatrFileSysFolder(pfs, ehSeg,
					new SoftReference <NativePatrFileSysFolder>(this));
				return get(result);
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
					throw PFSErr.createAndThrow(pfsErrno(), "get folder child");
				}
				NativePatrFileSysFile result = new NativePatrFileSysFile(pfs, ehSeg,
					new SoftReference <NativePatrFileSysFolder>(this));
				return get(result);
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
					throw PFSErr.createAndThrow(pfsErrno(), "get folder child");
				}
				NativePatrFileSysPipe result = new NativePatrFileSysPipe(pfs, ehSeg,
					new SoftReference <NativePatrFileSysFolder>(this));
				return get(result);
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
					throw PFSErr.createAndThrow(pfsErrno(), "get folder child");
				}
				NativePatrFileSysFolder result = new NativePatrFileSysFolder(pfs, ehSeg,
					new SoftReference <NativePatrFileSysFolder>(this));
				SoftRef <NativePatrFileSysElement> ref = new SoftRef <>(result);
				childs.put(ref, ref);
				return result;
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
					throw PFSErr.createAndThrow(pfsErrno(), "get folder child");
				}
				NativePatrFileSysFile result = new NativePatrFileSysFile(pfs, ehSeg,
					new SoftReference <NativePatrFileSysFolder>(this));
				SoftRef <NativePatrFileSysElement> ref = new SoftRef <>(result);
				childs.put(ref, ref);
				return result;
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
					throw PFSErr.createAndThrow(pfsErrno(), "get folder child");
				}
				NativePatrFileSysPipe result = new NativePatrFileSysPipe(pfs, ehSeg,
					new SoftReference <NativePatrFileSysFolder>(this));
				SoftRef <NativePatrFileSysElement> ref = new SoftRef <>(result);
				childs.put(ref, ref);
				return result;
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public PFSFolder toFolder() throws IllegalStateException {
		return this;
	}
	
	@Override
	public boolean isFolder() {
		return true;
	}
	
	@Override
	public boolean isRoot() {
		return this == pfs.root;
	}
	
	private PFSElement newElement(MemorySegment ehSeg) throws Throwable, InternalError {
		int flags = (int) GET_FLAGS.invoke(ehSeg);
		if (flags == -1) {
			throw PFSErr.createAndThrow(pfsErrno(), "get flags");
		}
		NativePatrFileSysElement result;
		switch (flags & FLAGS_ESSENTIAL_FLAGS) {
		case Flags.FOLDER:
			result = new NativePatrFileSysFolder(pfs, ehSeg, new SoftReference <
				NativePatrFileSysFolder>(NativePatrFileSysFolder.this));
			break;
		case Flags.FILE:
			result = new NativePatrFileSysFile(pfs, ehSeg, new SoftReference <
				NativePatrFileSysFolder>(NativePatrFileSysFolder.this));
			break;
		case Flags.PIPE:
			result = new NativePatrFileSysPipe(pfs, ehSeg, new SoftReference <
				NativePatrFileSysFolder>(NativePatrFileSysFolder.this));
			break;
		default:
			throw new InternalError("unknown internal flag compination! flags=0x" + Integer
				.toHexString(flags));
		}
		return get(result);
	}
	
	@SuppressWarnings("unchecked")
	private <E extends NativePatrFileSysElement> E get(E result) {
		SoftRef <NativePatrFileSysElement> ref = childs.get(result);
		if (ref != null) {
			NativePatrFileSysElement r = ref.get();
			if (r != null) {
				return (E) r;
			}
		}
		ref = new SoftRef <NativePatrFileSysElement>(result);
		childs.put(ref, ref);
		return result;
	}
	
}

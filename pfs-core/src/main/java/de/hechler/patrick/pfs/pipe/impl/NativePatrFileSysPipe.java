package de.hechler.patrick.pfs.pipe.impl;

import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSys.t;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSys.pfsErrno;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Pipe.APPEND;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Pipe.LENGTH;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Pipe.READ;
import static jdk.incubator.foreign.ValueLayout.JAVA_BYTE;

import java.lang.ref.Reference;

import de.hechler.patrick.pfs.exceptions.PFSErr;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.folder.impl.NativePatrFileSysFolder;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSys;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Errno;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSysElement;
import de.hechler.patrick.pfs.pipe.PFSPipe;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;

@SuppressWarnings("exports")
public class NativePatrFileSysPipe extends NativePatrFileSysElement implements PFSPipe {
	
	public NativePatrFileSysPipe(NativePatrFileSys pfs, MemorySegment eh, Reference <NativePatrFileSysFolder> parent) {
		super(pfs, eh, parent);
	}
	
	@Override
	public byte[] read(int length) throws PatrFileSysException {
		try {
			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator alloc = SegmentAllocator.nativeAllocator(scope);
				MemorySegment seg = alloc.allocate(length);
				if (0 == (int) READ.invoke(eh, seg, (long) length)) {
					throw PFSErr.create(pfsErrno(), "read from pipe");
				}
				return seg.toArray(JAVA_BYTE);
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void read(int byteoff, byte[] data, int length) throws PatrFileSysException {
		try {
			if (data.length - byteoff - length < 0) {
				throw PFSErr.create(Errno.ILLEGAL_ARG, "array length is not large enugh");
			}
			if (byteoff < 0 || length < 0) {
				throw PFSErr.create(Errno.ILLEGAL_ARG, "byteOff (" + byteoff + ") or length (" + length + ") < 0");
			}
			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator alloc = SegmentAllocator.nativeAllocator(scope);
				MemorySegment seg = alloc.allocate(length);
				if (0 == (int) READ.invoke(eh, seg, (long) length)) {
					throw PFSErr.create(pfsErrno(), "read from pipe");
				}
				seg.asByteBuffer().get(data, byteoff, length);
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void append(int byteOff, byte[] data, int length) throws PatrFileSysException {
		try {
			if (data.length - byteOff - length < 0) {
				throw PFSErr.create(Errno.ILLEGAL_ARG, "array length is not large enugh");
			}
			if (byteOff < 0 || length < 0) {
				throw PFSErr.create(Errno.ILLEGAL_ARG, "byteOff (" + byteOff + ") or length (" + length + ") < 0");
			}
			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator alloc = SegmentAllocator.nativeAllocator(scope);
				MemorySegment seg = alloc.allocate(length);
				seg.asByteBuffer().put(byteOff, data, 0, length);
				if (0 == (int) APPEND.invoke(eh, seg, (long) length)) {
					throw PFSErr.create(pfsErrno(), "append to pipe");
				}
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public long length() throws PatrFileSysException {
		try {
			long len = (long) LENGTH.invoke(eh);
			if (len == -1L) {
				throw PFSErr.create(pfsErrno(), "get length of pipe");
			}
			return len;
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public PFSPipe toPipe() throws IllegalStateException {
		return this;
	}
	
}

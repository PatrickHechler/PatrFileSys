package de.hechler.patrick.pfs.file.impl;

import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSys.pfsErrno;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSys.t;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.File.*;
import static jdk.incubator.foreign.ValueLayout.JAVA_BYTE;

import java.lang.ref.Reference;

import de.hechler.patrick.pfs.exceptions.PFSErr;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.file.PFSFile;
import de.hechler.patrick.pfs.folder.impl.NativePatrFileSysFolder;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSys;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSysElement;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Errno;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;


@SuppressWarnings("exports")
public class NativePatrFileSysFile extends NativePatrFileSysElement implements PFSFile {
	
	public NativePatrFileSysFile(NativePatrFileSys pfs, MemorySegment eh, Reference <NativePatrFileSysFolder> parent) {
		super(pfs, eh, parent);
	}
	
	@Override
	public byte[] read(long position, int length) throws PatrFileSysException {
		try {
			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				SegmentAllocator alloc = SegmentAllocator.nativeAllocator(scope);
				MemorySegment seg = alloc.allocate(length);
				if (0 == (int) READ.invoke(eh, seg, (long) length)) {
					throw PFSErr.create(pfsErrno(), "read from file");
				}
				return seg.toArray(JAVA_BYTE);
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void read(long position, int byteOff, byte[] data, int length) throws PatrFileSysException {
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
				if (0 == (int) READ.invoke(eh, seg, (long) length)) {
					throw PFSErr.create(pfsErrno(), "read from file");
				}
				seg.asByteBuffer().get(byteOff, data, 0, length);
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void overwrite(long position, int byteOff, byte[] data, int length) throws PatrFileSysException {
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
				if (0 == (int) WRITE.invoke(eh, seg, (long) length)) {
					throw PFSErr.create(pfsErrno(), "write to file");
				}
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
					throw PFSErr.create(pfsErrno(), "append to file");
				}
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void truncate(long newLength) throws PatrFileSysException {
		try {
			if (0 == (int) LENGTH.invoke(eh)) {
				throw PFSErr.create(pfsErrno(), "truncate file");
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
				throw PFSErr.create(pfsErrno(), "get length of file");
			}
			return len;
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public PFSFile toFile() throws IllegalStateException {
		return this;
	}
	
}

package de.hechler.patrick.pfs.file.impl;

import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSys.pfsErrno;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSys.t;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.File.APPEND;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.File.LENGTH;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.File.READ;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.File.WRITE;

import java.lang.ref.Reference;
import java.nio.ByteBuffer;

import de.hechler.patrick.pfs.element.impl.NativePatrFileSysElement;
import de.hechler.patrick.pfs.exceptions.PFSErr;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.file.PFSFile;
import de.hechler.patrick.pfs.folder.impl.NativePatrFileSysFolder;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSys;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Errno;
import jdk.incubator.foreign.MemorySegment;


@SuppressWarnings("exports")
public class NativePatrFileSysFile extends NativePatrFileSysElement implements PFSFile {
	
	public NativePatrFileSysFile(NativePatrFileSys pfs, MemorySegment eh, Reference <
		NativePatrFileSysFolder> parent) {
		super(pfs, eh, parent);
	}
	
	@Override
	public ByteBuffer read(long position, int length) throws PatrFileSysException {
		try {
			ByteBuffer res = ByteBuffer.allocateDirect(length);
			MemorySegment seg = MemorySegment.ofByteBuffer(res);
			if (0 == (int) READ.invoke(eh, seg, (long) length)) {
				throw PFSErr.createAndThrow(pfsErrno(), "read from file");
			}
			return seg.asByteBuffer();
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void read(long position, ByteBuffer data, int length) throws PatrFileSysException {
		try {
			if (data.capacity() < length) {
				throw PFSErr.createAndThrow(Errno.ILLEGAL_ARG, "buffer length is not large enugh");
			}
			if (length < 0) {
				throw PFSErr.createAndThrow(Errno.ILLEGAL_ARG, "length (" + length + ") < 0");
			}
			MemorySegment seg = MemorySegment.ofByteBuffer(data);
			if (0 == (int) READ.invoke(eh, seg, (long) length)) {
				throw PFSErr.createAndThrow(pfsErrno(), "read from file");
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void overwrite(long position, ByteBuffer data, int length) throws PatrFileSysException {
		try {
			if (data.capacity() < length) {
				throw PFSErr.createAndThrow(Errno.ILLEGAL_ARG, "buffer length is not large enugh");
			}
			if (length < 0) {
				throw PFSErr.createAndThrow(Errno.ILLEGAL_ARG, "length (" + length + ") < 0");
			}
			MemorySegment seg = MemorySegment.ofByteBuffer(data);
			if (0 == (int) WRITE.invoke(eh, seg, (long) length)) {
				throw PFSErr.createAndThrow(pfsErrno(), "write to file");
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void append(ByteBuffer data, int length) throws PatrFileSysException {
		try {
			if (data.capacity() < 0) {
				throw PFSErr.createAndThrow(Errno.ILLEGAL_ARG, "array length is not large enugh");
			}
			if (length < 0) {
				throw PFSErr.createAndThrow(Errno.ILLEGAL_ARG, "length (" + length + ") < 0");
			}
			MemorySegment seg = MemorySegment.ofByteBuffer(data);
			if (0 == (int) APPEND.invoke(eh, seg, (long) length)) {
				throw PFSErr.createAndThrow(pfsErrno(), "append to file");
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void truncate(long newLength) throws PatrFileSysException {
		try {
			if (0 == (int) LENGTH.invoke(eh)) {
				throw PFSErr.createAndThrow(pfsErrno(), "truncate file");
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
				throw PFSErr.createAndThrow(pfsErrno(), "get length of file");
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

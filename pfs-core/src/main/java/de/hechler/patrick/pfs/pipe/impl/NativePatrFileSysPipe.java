package de.hechler.patrick.pfs.pipe.impl;

import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSys.pfsErrno;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSys.t;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Pipe.APPEND;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Pipe.LENGTH;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Pipe.READ;

import java.io.IOException;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;

import de.hechler.patrick.pfs.element.impl.NativePatrFileSysElement;
import de.hechler.patrick.pfs.exceptions.PFSErr;
import de.hechler.patrick.pfs.folder.impl.NativePatrFileSysFolder;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSys;
import de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Errno;
import de.hechler.patrick.pfs.pipe.PFSPipe;
import jdk.incubator.foreign.MemorySegment;

@SuppressWarnings("exports")
public class NativePatrFileSysPipe extends NativePatrFileSysElement implements PFSPipe {
	
	public NativePatrFileSysPipe(NativePatrFileSys pfs, MemorySegment eh, Reference <
		NativePatrFileSysFolder> parent) {
		super(pfs, eh, parent);
	}
	
	@Override
	public ByteBuffer read(int length) throws IOException {
		try {
			ByteBuffer res = ByteBuffer.allocateDirect(length);
			MemorySegment seg = MemorySegment.ofByteBuffer(res);
			if (0 == (int) READ.invoke(eh, seg, (long) length)) {
				throw PFSErr.createAndThrow(pfsErrno(), "read from pipe");
			}
			return res;
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void read(ByteBuffer data, int length) throws IOException {
		try {
			if (data.capacity() < length) {
				throw PFSErr.createAndThrow(Errno.ILLEGAL_ARG, "buffer length is not large enugh");
			}
			if (length < 0) {
				throw PFSErr.createAndThrow(Errno.ILLEGAL_ARG, "length (" + length + ") < 0");
			}
			MemorySegment seg = MemorySegment.ofByteBuffer(data);
			if (0 == (int) READ.invoke(eh, seg, (long) length)) {
				throw PFSErr.createAndThrow(pfsErrno(), "read from pipe");
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void append(ByteBuffer data, int length) throws IOException {
		try {
			if (data.capacity() < length) {
				throw PFSErr.createAndThrow(Errno.ILLEGAL_ARG, "buffer length is not large enugh");
			}
			if (length < 0) {
				throw PFSErr.createAndThrow(Errno.ILLEGAL_ARG, "length (" + length + ") < 0");
			}
			MemorySegment seg = MemorySegment.ofByteBuffer(data);
			if (0 == (int) APPEND.invoke(eh, seg, (long) length)) {
				throw PFSErr.createAndThrow(pfsErrno(), "append to pipe");
			}
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public long length() throws IOException {
		try {
			long len = (long) LENGTH.invoke(eh);
			if (len == -1L) {
				throw PFSErr.createAndThrow(pfsErrno(), "get length of pipe");
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

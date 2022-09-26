package de.hechler.patrick.pfs.fs.impl;

import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.LINKER;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.handle;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.BlockManager.NEW_FILE;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.BlockManager.OFFSET_CLOSE_BM;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Constants.B0_OFFSET_BLOCK_SIZE;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Constants.EH_SIZE;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Constants.MAGIC_START;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Errno.ILLEGAL_ARG;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Errno.IO_ERR;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.FileSys.BLOCK_COUNT;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.FileSys.BLOCK_SIZE;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.FileSys.ERRNO;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.FileSys.FILL_ROOT;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.FileSys.FORMAT;
import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.FileSys.FS;
import static jdk.incubator.foreign.ValueLayout.ADDRESS;
import static jdk.incubator.foreign.ValueLayout.JAVA_INT;
import static jdk.incubator.foreign.ValueLayout.JAVA_LONG;

import java.lang.invoke.MethodHandle;

import de.hechler.patrick.pfs.exceptions.PFSErr;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.folder.impl.NativePatrFileSysFolder;
import de.hechler.patrick.pfs.fs.PFS;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.NativeSymbol;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;

@SuppressWarnings("exports")
public class NativePatrFileSys implements PFS {
	
	private static final int O_RDWR  = 02;
	private static final int O_CREAT = 0100;
	
	private static final int SEEK_SET = 0;
	// private static final int SEEK_CUR = 1;
	// private static final int SEEK_END = 2;
	
	public final ResourceScope           scope;
	public final SegmentAllocator        alloc;
	public final MemoryAddress           bm;
	public final NativePatrFileSysFolder root;
	
	private NativePatrFileSys(ResourceScope scope, MemoryAddress pfs, MemorySegment root) {
		this.scope = scope;
		this.alloc = SegmentAllocator.nativeAllocator(scope);
		this.bm = pfs;
		this.root = new NativePatrFileSysFolder(this, root, null);
	}
	
	public static RuntimeException t(Throwable t) throws PatrFileSysException {
		if (t instanceof Error) {
			throw (Error) t;
		} else if (t instanceof RuntimeException) {
			throw (RuntimeException) t;
		} else if (t instanceof PatrFileSysException) {
			throw (PatrFileSysException) t;
		} else {
			throw new RuntimeException(t);
		}
	}
	
	public static NativePatrFileSys load(String pfsFile) throws PatrFileSysException {
		try {
			return newImpl(pfsFile, -1L, -1);
		} catch (Throwable t) {
			throw t(t);
		}
	}
	
	public static NativePatrFileSys create(String pfsFile, long blockCount, int blockSize) throws PatrFileSysException {
		try {
			if (blockCount == -1L) {
				throw PFSErr.create(ILLEGAL_ARG, "blockCount is -1");
			}
			return newImpl(pfsFile, blockCount, blockSize);
		} catch (Throwable t) {
			throw t(t);
		}
	}
	
	private static NativePatrFileSys newImpl(String pfsFile, long blockCount, int blockSize) throws Throwable {
		MemorySegment mem = SegmentAllocator.implicitAllocator().allocateUtf8String(pfsFile);
		MethodHandle openHandle = handle("open", JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT);
		int fd = (int) openHandle.invoke(mem, blockCount == -1 ? O_RDWR : (O_RDWR | O_CREAT), 0666);
		if (fd == -1) {
			throw PFSErr.create(IO_ERR, "open");
		}
		if (blockCount == -1L) {
			MethodHandle seekHandle = handle("lseek", JAVA_INT, JAVA_INT, JAVA_LONG, JAVA_INT);
			MethodHandle readHandle = handle("read", JAVA_LONG, JAVA_INT, ADDRESS, JAVA_LONG);
			mem = SegmentAllocator.implicitAllocator().allocate(8);
			read64(mem, readHandle, fd);
			long value = mem.get(JAVA_LONG, 0L);
			if (value != MAGIC_START) {
				throw PFSErr.create(ILLEGAL_ARG, "magic does not match (expected 0x"
					+ Long.toHexString(MAGIC_START) + " got 0x" + Long.toHexString(value) + ")");
			}
			long pos = (long) seekHandle.invoke(fd, B0_OFFSET_BLOCK_SIZE, SEEK_SET);
			if (pos != B0_OFFSET_BLOCK_SIZE) {
				throw PFSErr.create(IO_ERR, "lseek");
			}
			read32(mem, readHandle, fd);
			blockSize = mem.get(JAVA_INT, 0L);
			if (blockSize <= 0) {
				throw PFSErr.create(ILLEGAL_ARG, "block size <= 0 (blockSize=" + blockSize + ')');
			}
		}
		MemoryAddress bmAddr = (MemoryAddress) NEW_FILE.invoke(fd, blockSize);
		if (bmAddr.toRawLongValue() == MemoryAddress.NULL.toRawLongValue()) {
			throw PFSErr.create(pfsErrno(), "new file block manager");
		}
		FS.set(ADDRESS, 0L, bmAddr);
		if (blockCount != -1L) {
			if (0 == (int) FORMAT.invoke(blockCount)) {
				throw PFSErr.create(pfsErrno(), "format");
			}
		}
		MemorySegment root = SegmentAllocator.implicitAllocator().allocate(EH_SIZE);
		if (0 == (int) FILL_ROOT.invoke(root)) {
			throw PFSErr.create(pfsErrno(), "get root");
		}
		ResourceScope scope = ResourceScope.newConfinedScope();
		return new NativePatrFileSys(scope, bmAddr, root);
	}
	
	public static int pfsErrno() {
		return ERRNO.get(JAVA_INT, 0L);
	}
	
	public static void read32(MemorySegment mem, MethodHandle readHandle, int fd)
		throws PatrFileSysException, Throwable {
		read(mem, readHandle, fd, 4);
	}
	
	public static void read64(MemorySegment mem, MethodHandle readHandle, int fd)
		throws PatrFileSysException, Throwable {
		read(mem, readHandle, fd, 8);
	}
	
	public static void read(MemorySegment mem, MethodHandle readHandle, int fd, long len)
		throws Throwable, PatrFileSysException {
		MemoryAddress addr = mem.address();
		for (long reat = 0; reat < len;) {
			long r = (long) readHandle.invoke(fd, addr.addOffset(reat), 8L - reat);
			if (r <= 0) {
				throw PFSErr.create(IO_ERR, "read");
			}
			reat += r;
		}
	}
	
	@Override
	public void format(long blockCount) throws PatrFileSysException {
		try {
			FS.set(ADDRESS, 0L, bm);
			if (0 == (int) FORMAT.invoke(blockCount)) {
				throw PFSErr.create(pfsErrno(), "format");
			}
			if (0 == (int) FILL_ROOT.invoke(root.eh)) {
				throw PFSErr.create(pfsErrno(), "get the root");
			}
		} catch (Throwable t) {
			throw t(t);
		}
	}
	
	@Override
	public PFSFolder root() throws PatrFileSysException {
		try {
			FS.set(ADDRESS, 0L, bm);
			if (0 == (int) FILL_ROOT.invoke(root.eh)) {
				throw PFSErr.create(pfsErrno(), "get the root");
			}
			return root;
		} catch (Throwable t) {
			throw t(t);
		}
	}
	
	@Override
	public long blockCount() throws PatrFileSysException {
		try {
			FS.set(ADDRESS, 0L, bm);
			long blockCount = (long) BLOCK_COUNT.invoke(root.eh);
			if ( -1L == blockCount) {
				throw PFSErr.create(pfsErrno(), "get the block count");
			}
			return blockCount;
		} catch (Throwable t) {
			throw t(t);
		}
	}
	
	@Override
	public int blockSize() throws PatrFileSysException {
		try {
			FS.set(ADDRESS, 0L, bm);
			int blockCount = (int) BLOCK_SIZE.invoke();
			if ( -1L == blockCount) {
				throw PFSErr.create(pfsErrno(), "get the block size");
			}
			return blockCount;
		} catch (Throwable t) {
			throw t(t);
		}
	}
	
	private static final MethodHandle G_CLOSE = LINKER.downcallHandle(FunctionDescriptor.of(JAVA_INT, ADDRESS));
	
	@Override
	public void close() throws PatrFileSysException {
		try {
			FS.set(ADDRESS, 0L, bm);
			MemoryAddress closeBmAddr = bm.get(ADDRESS, OFFSET_CLOSE_BM);
			NativeSymbol close_bm = NativeSymbol.ofAddress("close_bm", closeBmAddr, scope);
			try (ResourceScope scope = ResourceScope.newConfinedScope()) {
				if (0 == (int) G_CLOSE.invoke(close_bm, bm)) {
					throw PFSErr.create(pfsErrno(), "close");
				}
			}
		} catch (Throwable t) {
			throw t(t);
		}
	}
	
}

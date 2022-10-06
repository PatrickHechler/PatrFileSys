package de.hechler.patrick.pfs.fs.impl;

import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.LINKER;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.handle;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.BlockManager.MINIMAL_SIZE;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.BlockManager.NEW_FILE;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.BlockManager.OFFSET_BLOCK_FLAG_BITS;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.BlockManager.OFFSET_BLOCK_SIZE;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.BlockManager.OFFSET_CLOSE_BM;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.BlockManager.OFFSET_DELETE_ALL_FLAGS;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.BlockManager.OFFSET_FIRST_ZERO_FLAGGED_BLOCK;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.BlockManager.OFFSET_GET;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.BlockManager.OFFSET_GET_FLAGS;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.BlockManager.OFFSET_LOADED_ENTRIES;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.BlockManager.OFFSET_LOADED_ENTRYCOUNT;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.BlockManager.OFFSET_LOADED_EQUALIZER;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.BlockManager.OFFSET_LOADED_HASHMAKER;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.BlockManager.OFFSET_LOADED_SETSIZE;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.BlockManager.OFFSET_SET;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.BlockManager.OFFSET_SET_FLAGS;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.BlockManager.OFFSET_SYNC_BM;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.BlockManager.OFFSET_UNGET;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Constants.B0_OFFSET_BLOCK_SIZE;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Constants.EH_SIZE;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Constants.MAGIC_START;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Errno.ILLEGAL_ARG;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.Errno.IO_ERR;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.FileSys.BLOCK_COUNT;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.FileSys.BLOCK_SIZE;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.FileSys.ERRNO;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.FileSys.FILL_ROOT;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.FileSys.FORMAT;
import static de.hechler.patrick.pfs.other.NativePatrFileSysDefines.FileSys.FS;
import static jdk.incubator.foreign.ValueLayout.ADDRESS;
import static jdk.incubator.foreign.ValueLayout.JAVA_INT;
import static jdk.incubator.foreign.ValueLayout.JAVA_LONG;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;

import de.hechler.patrick.pfs.bm.BlockManager;
import de.hechler.patrick.pfs.exceptions.PFSErr;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.folder.impl.NativePatrFileSysFolder;
import de.hechler.patrick.pfs.fs.PFS;
import jdk.incubator.foreign.Addressable;
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
	private boolean                      closed = false;
	
	private NativePatrFileSys(ResourceScope scope, SegmentAllocator alloc, MemoryAddress pfs,
		MemorySegment root) {
		this.scope = scope;
		this.alloc = alloc;
		this.bm = pfs;
		this.root = new NativePatrFileSysFolder(this, root, null);
	}
	
	public static RuntimeException t(Throwable t) throws IOException {
		if (t instanceof Error) {
			throw (Error) t;
		} else if (t instanceof RuntimeException) {
			throw (RuntimeException) t;
		} else if (t instanceof IOException) {
			throw (IOException) t;
		} else {
			throw new RuntimeException(t);
		}
	}
	
	public static NativePatrFileSys load(String pfsFile) throws IOException {
		try {
			return newImpl(pfsFile, -1L, -1);
		} catch (Throwable t) {
			throw t(t);
		}
	}
	
	public static NativePatrFileSys create(String pfsFile, long blockCount, int blockSize)
		throws IOException {
		try {
			if (blockCount == -1L) {
				throw PFSErr.createAndThrow(ILLEGAL_ARG, "blockCount is -1");
			}
			return newImpl(pfsFile, blockCount, blockSize);
		} catch (Throwable t) {
			throw t(t);
		}
	}
	
	public static NativePatrFileSys load(BlockManager bm) throws IOException {
		try {
			return newImpl(bm, -1L);
		} catch (Throwable t) {
			throw t(t);
		}
	}
	
	public static NativePatrFileSys create(BlockManager bm, long blockCount) throws IOException {
		try {
			if (blockCount == -1L) {
				throw PFSErr.createAndThrow(ILLEGAL_ARG, "blockCount is -1");
			}
			return newImpl(bm, -1L);
		} catch (Throwable t) {
			throw t(t);
		}
	}
	
	private static NativePatrFileSys newImpl(String pfsFile, long blockCount, int blockSize)
		throws Throwable {
		MemorySegment mem = SegmentAllocator.implicitAllocator().allocateUtf8String(pfsFile);
		MethodHandle openHandle = handle("open", JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT);
		int fd = (int) openHandle.invoke(mem, blockCount == -1 ? O_RDWR : (O_RDWR | O_CREAT), 0666);
		if (fd == -1) {
			throw PFSErr.createAndThrow(IO_ERR, "open");
		}
		if (blockCount == -1L) {
			MethodHandle seekHandle = handle("lseek", JAVA_INT, JAVA_INT, JAVA_LONG, JAVA_INT);
			MethodHandle readHandle = handle("read", JAVA_LONG, JAVA_INT, ADDRESS, JAVA_LONG);
			mem = SegmentAllocator.implicitAllocator().allocate(8);
			read64(mem, readHandle, fd);
			long value = mem.get(JAVA_LONG, 0L);
			if (value != MAGIC_START) {
				throw PFSErr.createAndThrow(ILLEGAL_ARG, "magic does not match (expected 0x" + Long
					.toHexString(MAGIC_START) + " got 0x" + Long.toHexString(value) + ")");
			}
			long pos = (long) seekHandle.invoke(fd, B0_OFFSET_BLOCK_SIZE, SEEK_SET);
			if (pos != B0_OFFSET_BLOCK_SIZE) {
				throw PFSErr.createAndThrow(IO_ERR, "lseek");
			}
			read32(mem, readHandle, fd);
			blockSize = mem.get(JAVA_INT, 0L);
			if (blockSize <= 0) {
				throw PFSErr.createAndThrow(ILLEGAL_ARG, "block size <= 0 (blockSize=" + blockSize
					+ ')');
			}
		}
		MemoryAddress bmAddr = (MemoryAddress) NEW_FILE.invoke(fd, blockSize);
		if (bmAddr.toRawLongValue() == MemoryAddress.NULL.toRawLongValue()) {
			throw PFSErr.createAndThrow(pfsErrno(), "new file block manager");
		}
		FS.set(ADDRESS, 0L, bmAddr);
		if (blockCount != -1L) {
			if (0 == (int) FORMAT.invoke(blockCount)) {
				throw PFSErr.createAndThrow(pfsErrno(), "format");
			}
		}
		ResourceScope scope = ResourceScope.newConfinedScope();
		SegmentAllocator alloc = SegmentAllocator.nativeAllocator(scope);
		MemorySegment root = alloc.allocate(EH_SIZE);
		if (0 == (int) FILL_ROOT.invoke(root)) {
			throw PFSErr.createAndThrow(pfsErrno(), "get root");
		}
		return new NativePatrFileSys(scope, alloc, bmAddr, root);
	}
	
	@SuppressWarnings("unused")
	private static class NativePatrFileSysWithJBM extends NativePatrFileSys {
		
		private final BlockManager jbm;
		
		private NativePatrFileSysWithJBM(ResourceScope scope, SegmentAllocator alloc,
			MemoryAddress pfs, MemorySegment root, BlockManager jbm) {
			super(scope, alloc, pfs, root);
			this.jbm = jbm;
		}
		
		private Addressable get(MemoryAddress addr, long block) {
			checkAddr(addr);
			try {
				ByteBuffer bb = jbm.get(block);
				return MemorySegment.ofByteBuffer(bb);
			} catch (IOException e) {
				setErrno(e);
				return MemoryAddress.NULL;
			}
		}
		
		private int unget(MemoryAddress addr, long block) {
			checkAddr(addr);
			try {
				jbm.unget(block);
				return 1;
			} catch (IOException e) {
				setErrno(e);
				return 0;
			}
		}
		
		private int set(MemoryAddress addr, long block) {
			checkAddr(addr);
			try {
				jbm.set(block);
				return 1;
			} catch (IOException e) {
				setErrno(e);
				return 0;
			}
		}
		
		private int sync_bm(MemoryAddress addr) {
			checkAddr(addr);
			try {
				jbm.sync();
				return 1;
			} catch (IOException e) {
				setErrno(e);
				return 0;
			}
		}
		
		private int close_bm(MemoryAddress addr) {
			checkAddr(addr);
			try {
				jbm.sync();
				return 1;
			} catch (IOException e) {
				setErrno(e);
				return 0;
			}
		}
		
		private long get_flags(MemoryAddress addr, long block) {
			checkAddr(addr);
			try {
				return jbm.flags(block);
			} catch (IOException e) {
				setErrno(e);
				return 0;
			}
		}
		
		private int set_flags(MemoryAddress addr, long block, long flags) {
			checkAddr(addr);
			try {
				jbm.flag(block, flags);
				return 1;
			} catch (IOException e) {
				setErrno(e);
				return 0;
			}
		}
		
		private long first_zero_flagged_block(MemoryAddress addr) {
			checkAddr(addr);
			try {
				return jbm.firstZeroFlaggedBlock();
			} catch (IOException e) {
				setErrno(e);
				return -1;
			}
		}
		
		private int delete_all_flags(MemoryAddress addr) {
			checkAddr(addr);
			try {
				jbm.deleteAllFlags();
				return 1;
			} catch (IOException e) {
				setErrno(e);
				return 0;
			}
		}
		
		private void setErrno(IOException e) {
			if ( ! (e instanceof PFSErr)) {
				// a throw would cause the VM to print the stack trace and exit (with a non-zero
				// value)
				System.err.println("my block manager threw a non PFSErr exception!");
				System.err.println("block manager: (class) " + bm.getClass().getName());
				System.err.println("block manager: (toString) " + bm);
				e.printStackTrace();
				System.exit(1);
			}
			ERRNO.set(JAVA_INT, 0L, ((PFSErr) e).pfs_errno());
		}
		
		private void checkAddr(MemoryAddress addr) {
			if (addr.toRawLongValue() != bm.toRawLongValue()) {
				// a throw would cause the VM to print the stack trace and exit (with a non-zero
				// value)
				System.err.println("addresses does not match");
				System.err.println("bm=0x" + Long.toHexString(bm.toRawLongValue()));
				System.err.println("got=0x" + Long.toHexString(addr.toRawLongValue()));
				System.err.println("stack trace:");
				new Throwable().printStackTrace();
				System.exit(1);
			}
		}
		
		public NativePatrFileSys fill() throws Throwable {
			Lookup lookup = MethodHandles.lookup();
			bm.set(JAVA_INT, OFFSET_LOADED_ENTRYCOUNT, 0);
			bm.set(JAVA_INT, OFFSET_LOADED_SETSIZE, 0);
			bm.set(ADDRESS, OFFSET_LOADED_EQUALIZER, MemoryAddress.NULL);
			bm.set(ADDRESS, OFFSET_LOADED_HASHMAKER, MemoryAddress.NULL);
			bm.set(ADDRESS, OFFSET_LOADED_ENTRIES, MemoryAddress.NULL);
			MethodHandle handle = lookup.findSpecial(NativePatrFileSysWithJBM.class, "get",
				MethodType.methodType(Addressable.class, MemoryAddress.class, long.class),
				NativePatrFileSys.class);
			handle = MethodHandles.insertArguments(handle, 0, this);
			NativeSymbol symbol = LINKER.upcallStub(handle, FunctionDescriptor.of(ADDRESS, ADDRESS,
				JAVA_LONG), scope);
			bm.set(ADDRESS, OFFSET_GET, symbol);
			handle = lookup.findSpecial(NativePatrFileSysWithJBM.class, "unget", MethodType
				.methodType(int.class, MemoryAddress.class, long.class), NativePatrFileSys.class);
			handle = MethodHandles.insertArguments(handle, 0, this);
			symbol = LINKER.upcallStub(handle, FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG),
				scope);
			bm.set(ADDRESS, OFFSET_UNGET, symbol);
			handle = lookup.findSpecial(NativePatrFileSysWithJBM.class, "set", MethodType
				.methodType(int.class, MemoryAddress.class, long.class), NativePatrFileSys.class);
			handle = MethodHandles.insertArguments(handle, 0, this);
			symbol = LINKER.upcallStub(handle, FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG),
				scope);
			bm.set(ADDRESS, OFFSET_SET, symbol);
			handle = lookup.findSpecial(NativePatrFileSysWithJBM.class, "sync_bm", MethodType
				.methodType(int.class, MemoryAddress.class), NativePatrFileSys.class);
			handle = MethodHandles.insertArguments(handle, 0, this);
			symbol = LINKER.upcallStub(handle, FunctionDescriptor.of(JAVA_INT, ADDRESS), scope);
			bm.set(ADDRESS, OFFSET_SYNC_BM, symbol);
			handle = lookup.findSpecial(NativePatrFileSysWithJBM.class, "close_bm", MethodType
				.methodType(int.class, MemoryAddress.class), NativePatrFileSys.class);
			handle = MethodHandles.insertArguments(handle, 0, this);
			symbol = LINKER.upcallStub(handle, FunctionDescriptor.of(JAVA_INT, ADDRESS), scope);
			bm.set(ADDRESS, OFFSET_CLOSE_BM, symbol);
			int bs = jbm.blockSize();
			if (bs <= 0) {
				throw PFSErr.createAndThrow(ILLEGAL_ARG, "block size <= 0 (" + bs + ")");
			}
			bm.set(JAVA_INT, OFFSET_BLOCK_SIZE, bs);
			int fpb = jbm.flagsPerBlock();
			if (fpb < 0 || fpb >= 64) {
				throw PFSErr.createAndThrow(ILLEGAL_ARG,
					"flagsPerBlock returned an invalid value (<0 or >=64) (" + fpb + ")");
			}
			bm.set(JAVA_INT, OFFSET_BLOCK_FLAG_BITS, fpb);
			handle = lookup.findSpecial(NativePatrFileSysWithJBM.class, "get_flags", MethodType
				.methodType(long.class, MemoryAddress.class, long.class), NativePatrFileSys.class);
			handle = MethodHandles.insertArguments(handle, 0, this);
			symbol = LINKER.upcallStub(handle, FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_LONG),
				scope);
			bm.set(ADDRESS, OFFSET_GET_FLAGS, symbol);
			handle = lookup.findSpecial(NativePatrFileSysWithJBM.class, "set_flags", MethodType
				.methodType(int.class, MemoryAddress.class, long.class, long.class),
				NativePatrFileSys.class);
			handle = MethodHandles.insertArguments(handle, 0, this);
			symbol = LINKER.upcallStub(handle, FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG,
				JAVA_LONG), scope);
			bm.set(ADDRESS, OFFSET_SET_FLAGS, symbol);
			handle = lookup.findSpecial(NativePatrFileSysWithJBM.class, "first_zero_flagged_block",
				MethodType.methodType(long.class, MemoryAddress.class), NativePatrFileSys.class);
			handle = MethodHandles.insertArguments(handle, 0, this);
			symbol = LINKER.upcallStub(handle, FunctionDescriptor.of(JAVA_LONG, ADDRESS), scope);
			bm.set(ADDRESS, OFFSET_FIRST_ZERO_FLAGGED_BLOCK, symbol);
			handle = lookup.findSpecial(NativePatrFileSysWithJBM.class, "delete_all_flags",
				MethodType.methodType(int.class, MemoryAddress.class), NativePatrFileSys.class);
			handle = MethodHandles.insertArguments(handle, 0, this);
			symbol = LINKER.upcallStub(handle, FunctionDescriptor.of(JAVA_INT, ADDRESS), scope);
			bm.set(ADDRESS, OFFSET_DELETE_ALL_FLAGS, symbol);
			FS.set(ADDRESS, 0, bm);
			if (0 == (int) FILL_ROOT.invoke(root)) {
				throw PFSErr.createAndThrow(pfsErrno(), "fill root");
			}
			return this;
		}
		
	}
	
	private static NativePatrFileSys newImpl(BlockManager jbm, long blockCount) throws Throwable {
		ResourceScope scope = ResourceScope.newConfinedScope();
		SegmentAllocator alloc = SegmentAllocator.nativeAllocator(scope);
		MemorySegment bm = alloc.allocate(MINIMAL_SIZE);
		MemorySegment root = alloc.allocate(EH_SIZE);
		return new NativePatrFileSysWithJBM(scope, alloc, bm.address(), root, jbm).fill();
	}
	
	public static int pfsErrno() {
		return ERRNO.get(JAVA_INT, 0L);
	}
	
	public static void read32(MemorySegment mem, MethodHandle readHandle, int fd)
		throws IOException, Throwable {
		read(mem, readHandle, fd, 4);
	}
	
	public static void read64(MemorySegment mem, MethodHandle readHandle, int fd)
		throws IOException, Throwable {
		read(mem, readHandle, fd, 8);
	}
	
	public static void read(MemorySegment mem, MethodHandle readHandle, int fd, long len)
		throws Throwable, IOException {
		MemoryAddress addr = mem.address();
		for (long reat = 0; reat < len;) {
			long r = (long) readHandle.invoke(fd, addr.addOffset(reat), 8L - reat);
			if (r <= 0) {
				throw PFSErr.createAndThrow(IO_ERR, "read");
			}
			reat += r;
		}
	}
	
	@Override
	public void format(long blockCount) throws IOException {
		try {
			FS.set(ADDRESS, 0L, bm);
			if (0 == (int) FORMAT.invoke(blockCount)) {
				throw PFSErr.createAndThrow(pfsErrno(), "format");
			}
			if (0 == (int) FILL_ROOT.invoke(root.eh)) {
				throw PFSErr.createAndThrow(pfsErrno(), "get the root");
			}
		} catch (Throwable t) {
			throw t(t);
		}
	}
	
	@Override
	public PFSFolder root() throws IOException {
		try {
			FS.set(ADDRESS, 0L, bm);
			if (0 == (int) FILL_ROOT.invoke(root.eh)) {
				throw PFSErr.createAndThrow(pfsErrno(), "get the root");
			}
			return root;
		} catch (Throwable t) {
			throw t(t);
		}
	}
	
	@Override
	public long blockCount() throws IOException {
		try {
			FS.set(ADDRESS, 0L, bm);
			long blockCount = (long) BLOCK_COUNT.invoke(root.eh);
			if ( -1L == blockCount) {
				throw PFSErr.createAndThrow(pfsErrno(), "get the block count");
			}
			return blockCount;
		} catch (Throwable t) {
			throw t(t);
		}
	}
	
	@Override
	public int blockSize() throws IOException {
		try {
			if (closed) {
				throw PFSErr.createAndThrow(PFSErr.PFS_ERR_CLOSED, "closed");
			}
			FS.set(ADDRESS, 0L, bm);
			int blockCount = (int) BLOCK_SIZE.invoke();
			if ( -1L == blockCount) {
				throw PFSErr.createAndThrow(pfsErrno(), "get the block size");
			}
			return blockCount;
		} catch (Throwable t) {
			throw t(t);
		}
	}
	
	private static final MethodHandle G_CLOSE = LINKER.downcallHandle(FunctionDescriptor.of(
		JAVA_INT, ADDRESS));
	
	@Override
	public void close() throws IOException {
		try {
			if (closed) {
				return;
			}
			FS.set(ADDRESS, 0L, bm);
			MemoryAddress closeBmAddr = bm.get(ADDRESS, OFFSET_CLOSE_BM);
			NativeSymbol close_bm = NativeSymbol.ofAddress("close_bm", closeBmAddr, scope);
			if (0 == (int) G_CLOSE.invoke(close_bm, bm)) {
				throw PFSErr.createAndThrow(pfsErrno(), "close");
			}
			scope.close();
		} catch (Throwable t) {
			throw t(t);
		}
	}
	
}

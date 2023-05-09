package de.hechler.patrick.zeugs.pfs.impl.pfs;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import de.hechler.patrick.zeugs.pfs.FSProvider;
import de.hechler.patrick.zeugs.pfs.interfaces.FS;
import de.hechler.patrick.zeugs.pfs.interfaces.FSOptions;
import de.hechler.patrick.zeugs.pfs.opts.PatrFSOptions;
import de.hechler.patrick.zeugs.pfs.opts.PatrRamFSOpts;

/**
 * this class implements the File System interfaces by delegating to the linux native Patr-FileSystem API <br>
 * the {@link #name()} of this provider is {@value FSProvider#PATR_FS_PROVIDER_NAME}
 * 
 * @author pat
 * 
 * @see FSProvider#PATR_FS_PROVIDER_NAME
 */
public class PatrFSProvider extends FSProvider {
	
	private static final long MAGIC_START       = 0x31756539FC422698L;
	private static final long OFFSET_BLOCK_SIZE = 20;
	
	private static final int SEEK_SET = 0;
	private static final int O_RDWR   = 02;
	private static final int O_CREAT  = 0100;
	
	private static final SymbolLookup GLIBC_LIBARY_LOCKUP = SymbolLookup.libraryLookup("/lib/libc.so.6", SegmentScope.global());
	
	static volatile PatrFS loaded;
	
	private volatile boolean exists = false;
	
	public PatrFSProvider() {
		super(FSProvider.PATR_FS_PROVIDER_NAME, 1);
		synchronized (PatrFSProvider.class) {
			if (exists) { throw new IllegalStateException("this class is only allowed to be created once"); }
			exists = true;
		}
	}
	
	@Override
	public FS loadFS(FSOptions fso) throws IOException {
		if (fso == null) { throw new NullPointerException("options are null!"); }
		synchronized (this) {
			if (loaded != null) {
				throw new IllegalStateException("maximum amount of file systems has been loaded! (max amount: 1)");
			}
			Arena session = Arena.openConfined();
			try {
				Linker linker = Linker.nativeLinker();
				try (Arena local = Arena.openConfined()) {
					if (fso instanceof PatrFSOptions opts) {
						loadPatrOpts(linker, local, opts);
					} else if (fso instanceof PatrRamFSOpts opts) {
						loadPatrRamOpts(linker, opts);
					} else {
						throw new IllegalArgumentException("FSOptions of an unknown " + fso.getClass());
					}
				}
				loaded = new PatrFS(session);
			} catch (Throwable e) {
				session.close();
				throw thrw(e);
			}
		}
		return loaded;
	}
	
	private static void loadPatrRamOpts(Linker linker, PatrRamFSOpts opts) throws Throwable {
		MethodHandle  newBm            = linker.downcallHandle(PatrFS.LOCKUP.find("bm_new_ram_block_manager").orElseThrow(),
			FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
		MethodHandle  pfsLoadAndFormat = linker.downcallHandle(PatrFS.LOCKUP.find("pfs_load_and_format").orElseThrow(),
			FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
		MemorySegment bm               = (MemorySegment) newBm.invoke(opts.blockCount(), opts.blockSize());
		if (0 == (int) pfsLoadAndFormat.invoke(bm, opts.blockCount())) {
			throw thrw(PFSErrorCause.LOAD_PFS_AND_FORMAT, opts);
		}
	}
	
	private static void loadPatrOpts(Linker linker, Arena local, PatrFSOptions opts) throws Throwable {
		MethodHandle  open  = linker.downcallHandle(GLIBC_LIBARY_LOCKUP.find("open").orElseThrow(),
			FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
		MethodHandle  newBm = linker.downcallHandle(PatrFS.LOCKUP.find("bm_new_file_block_manager").orElseThrow(),
			FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
		MemorySegment path  = local.allocateUtf8String(opts.path());
		int           fd    = (int) open.invoke(path, opts.format() ? O_RDWR | O_CREAT : O_RDWR, 0666);
		if (fd == -1) {
			throw new IOException("could not open the pfs '" + opts.path() + "'");
		}
		if (opts.format()) {
			loadWithFormat(linker, opts, newBm, fd);
		} else {
			loadWithoutFormat(linker, local, opts, newBm, fd);
		}
	}
	
	private static void loadWithFormat(Linker linker, PatrFSOptions opts, MethodHandle newBm, int fd) throws Throwable {
		MethodHandle  pfsLoadAndFormat = linker.downcallHandle(PatrFS.LOCKUP.find("pfs_load_and_format").orElseThrow(),
			FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
		MemorySegment bm               = (MemorySegment) newBm.invoke(fd, opts.blockSize());
		if (0 == (int) pfsLoadAndFormat.invoke(bm, opts.blockCount())) {
			throw thrw(PFSErrorCause.LOAD_PFS_AND_FORMAT, opts);
		}
	}
	
	private static void loadWithoutFormat(Linker linker, Arena local, PatrFSOptions opts, MethodHandle newBm, int fd) throws Throwable {
		MethodHandle  pfsLoad = linker.downcallHandle(PatrFS.LOCKUP.find("pfs_load").orElseThrow(),
			FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
		MethodHandle  lseek   = linker.downcallHandle(GLIBC_LIBARY_LOCKUP.find("lseek").orElseThrow(),
			FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
		MethodHandle  read    = linker.downcallHandle(GLIBC_LIBARY_LOCKUP.find("read").orElseThrow(),
			FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
		MemorySegment buf     = local.allocate(8);
		if (8L != (long) read.invoke(fd, buf, 8L)) { throw new IOException("error on read"); }
		if (MAGIC_START != buf.get(ValueLayout.JAVA_LONG, 0)) {
			throw new IOException("the file system does not start with my magic!");
		}
		if (OFFSET_BLOCK_SIZE != (long) lseek.invoke(fd, OFFSET_BLOCK_SIZE, SEEK_SET)) {
			throw new IOException("error on lseek");
		}
		if (4L != (long) read.invoke(fd, buf, 4L)) { throw new IOException("error on read"); }
		int           bs = buf.get(ValueLayout.JAVA_INT, 0);
		MemorySegment bm = (MemorySegment) newBm.invoke(fd, bs);
		if (0 == (int) pfsLoad.invoke(bm, MemorySegment.NULL)) { throw thrw(PFSErrorCause.LOAD_PFS, opts.path()); }
	}
	
	public static IOException thrw(Throwable t) throws IOException {
		if (t instanceof IOException ioe) {
			throw ioe;
		} else if (t instanceof Error err) {
			throw err;
		} else if (t instanceof RuntimeException re) {
			throw re;
		} else {
			throw new AssertionError(t);
		}
	}
	
	public static IOException thrw(PFSErrorCause cause, Object info) throws IOException {
		int    pfsErrno = pfsErrno();
		String msg      = cause.str.apply(info);
		throw cause.func.apply(msg, pfsErrno);
	}
	
	private static MemorySegment cach;
	private static MemorySegment cach2;
	
	public static int pfsErrno() {
		MemorySegment val = cach;
		if (val == null) {
			val  = MemorySegment.ofAddress(PatrFS.LOCKUP.find("pfs_err_loc").orElseThrow().address(), 8L);
			cach = val;
		}
		long addr = val.get(ValueLayout.JAVA_LONG, 0L);
		if (cach2 == null || addr != cach2.address()) {
			val   = MemorySegment.ofAddress(addr, 4);
			cach2 = val;
		}
		return val.get(ValueLayout.JAVA_INT, 0);
	}
	
	public static void unload(FS loaded) {
		if (loaded != PatrFSProvider.loaded) { throw new AssertionError(); }
		PatrFSProvider.loaded = null;
	}
	
	@Override
	public Collection<? extends FS> loadedFS() {
		if (loaded != null) {
			return Arrays.asList(loaded);
		} else {
			return Collections.emptyList();
		}
	}
	
}

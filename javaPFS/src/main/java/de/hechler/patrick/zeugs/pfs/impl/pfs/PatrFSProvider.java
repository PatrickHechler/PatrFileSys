package de.hechler.patrick.zeugs.pfs.impl.pfs;

import java.io.IOError;
import java.io.IOException;
import java.lang.foreign.Addressable;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import de.hechler.patrick.zeugs.pfs.FSProvider;
import de.hechler.patrick.zeugs.pfs.interfaces.FS;
import de.hechler.patrick.zeugs.pfs.interfaces.FSElement;
import de.hechler.patrick.zeugs.pfs.interfaces.FSOptions;
import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Folder;
import de.hechler.patrick.zeugs.pfs.interfaces.Pipe;
import de.hechler.patrick.zeugs.pfs.interfaces.WriteStream;
import de.hechler.patrick.zeugs.pfs.opts.PatrFSOptions;
import de.hechler.patrick.zeugs.pfs.opts.PatrRamFSOpts;

public class PatrFSProvider extends FSProvider {
	
	private static final long MAGIC_START       = 0xF17565393C422698L;
	private static final long OFFSET_BLOCK_SIZE = 20;
	
	private static final int SEEK_SET = 0;
	private static final int O_RDWR   = 02;
	private static final int O_CREAT  = 0100;
	
	private static final SymbolLookup GLIBC_LIBARY_LOCKUP = SymbolLookup.libraryLookup("/lib/libc.so.6", MemorySession.global());
	
	static volatile PatrFS loaded;
	
	private volatile boolean exists = false;
	
	public PatrFSProvider() {
		super(FSProvider.PATR_FS_PROVIDER_NAME, 1);
		synchronized (PatrFSProvider.class) {
			if (exists) { throw new IllegalStateException("this class is only allowed to be created once"); }
			exists = true;
		}
	}
	
	public static void main(String[] args) throws NoSuchProviderException, IOException {
		System.out.println(Runtime.version());
		FSProvider myProv = FSProvider.ofName(PATR_FS_PROVIDER_NAME);
		System.out.println("got porvider");
		FSOptions opts = new PatrFSOptions("./testout/test.pfs", 1024L, 1024);
		System.out.println("created load option: " + opts);
		try (FS fs = myProv.loadFS(opts)) {
			System.out.print("loaded fs");
			print("/", fs.folder("/"));
			File evilFile = fs.folder(".").createFile("evil file");
			System.out.println("created the evil file");
			Folder evilPipe = fs.folder(".").createFolder("evil pipe");
			System.out.println("created the evil folder/pipe");
			Pipe thisIsEvil = evilPipe.createPipe("this\n is \t evil");
			System.out.println("created the evil");
			evilFile.close();
			System.out.println("colsed evil file");
			WriteStream stream = thisIsEvil.openWrite();
			System.out.println("opened evel write stream");
			stream.write("hello world, this is the evel content of the evil pipe\nthis is very evil!".getBytes(StandardCharsets.UTF_8));
			System.out.println("wrote some data to the evel stream");
			thisIsEvil.close();
			System.out.println("colsed this is evil");
			stream.close();
			System.out.println("colsed the evil stream");
			evilPipe.close();
			System.out.println("colsed evil pipe");
			print("", fs.folder("/"));
		}
		System.out.println("FINISH");
	}
	
	private static void print(String prefix, Folder folder) throws IOException {
		try (folder) {
			String newPrefix = prefix + folder.name().replace("\\", "\\\\").replace("\t", "\\t").replace("\b", "\\b").replace("\n", "\\n")
					.replace("\r", "\\r").replace("\f", "\\f").replace(" ", "\\ ").replace("\'", "\\'").replace("\"", "\\\"") + '/';
			System.out.println("/  has " + folder.childCount() + " children");
			for (Iterator<FSElement> iter = folder.iter(true); iter.hasNext();) {
				FSElement element = iter.next();
				System.out.print(newPrefix + element.name().replace("\\", "\\\\").replace("\t", "\\t").replace("\b", "\\b").replace("\n", "\\n")
						.replace("\r", "\\r").replace("\f", "\\f").replace(" ", "\\ ").replace("\'", "\\'").replace("\"", "\\\""));
				if (element.isFolder()) {
					print(newPrefix, element.getFolder());
				} else if (element.isFile()) {
					System.out.println("  has " + element.getFile().length() + " bytes in the file");
					element.close();
				} else if (element.isPipe()) {
					System.out.println("  has " + element.getPipe().length() + " bytes in the pipe");
					element.close();
				}
			}
		}
	}
	
	@Override
	public FS loadFS(FSOptions fso) throws IOException {
		if (fso == null) { throw new NullPointerException("options are null!"); }
		synchronized (this) {
			if (loaded != null) { throw new IllegalStateException("maximum amount of file systems has been loaded! (max amount: 1)"); }
			MemorySession session = MemorySession.openConfined();
			try {
				Linker linker = Linker.nativeLinker();
				try (MemorySession local = MemorySession.openConfined()) {
					if (fso instanceof PatrFSOptions opts) {
						loadPatrOpts(linker, PatrFS.LOCKUP, local, opts);
					} else if (fso instanceof PatrRamFSOpts opts) {
						loadPatrRamOpts(linker, PatrFS.LOCKUP, opts);
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
	
	private static void loadPatrRamOpts(Linker linker, SymbolLookup loockup, PatrRamFSOpts opts) throws Throwable {
		MethodHandle newBm            = linker.downcallHandle(loockup.lookup("bm_new_ram_block_manager").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
		MethodHandle pfsLoadAndFormat = linker.downcallHandle(loockup.lookup("pfs_load_and_format").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
		Addressable  bm               = (Addressable) newBm.invoke(opts.blockCount(), opts.blockSize());
		if (0 == (int) pfsLoadAndFormat.invoke(bm, opts.blockCount())) { throw thrw(loockup, "load and format ram PFS (" + opts + ")"); }
	}
	
	private static void loadPatrOpts(Linker linker, SymbolLookup loockup, MemorySession local, PatrFSOptions opts) throws Throwable {
		MethodHandle  open  = linker.downcallHandle(GLIBC_LIBARY_LOCKUP.lookup("open").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
		MethodHandle  newBm = linker.downcallHandle(loockup.lookup("bm_new_file_block_manager").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
		MemorySegment path  = local.allocateUtf8String(opts.path());
		int           fd    = (int) open.invoke(path, O_RDWR | O_CREAT, 0666);
		if (opts.format()) {
			loadWithFormat(linker, loockup, opts, newBm, fd);
		} else {
			loadWithoutFormat(linker, loockup, local, opts, newBm, fd);
		}
	}
	
	private static void loadWithFormat(Linker linker, SymbolLookup loockup, PatrFSOptions opts, MethodHandle newBm, int fd) throws Throwable {
		MethodHandle pfsLoadAndFormat = linker.downcallHandle(loockup.lookup("pfs_load_and_format").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
		Addressable  bm               = (Addressable) newBm.invoke(fd, opts.blockSize());
		if (0 == (int) pfsLoadAndFormat.invoke(bm, opts.blockCount())) { throw thrw(loockup, "load and format PFS (" + opts.path() + ")"); }
	}
	
	private static void loadWithoutFormat(Linker linker, SymbolLookup loockup, MemorySession local, PatrFSOptions opts, MethodHandle newBm, int fd)
			throws Throwable {
		MethodHandle  pfsLoad = linker.downcallHandle(loockup.lookup("pfs_load").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
		MethodHandle  lseek   = linker.downcallHandle(GLIBC_LIBARY_LOCKUP.lookup("lseek").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT));
		MethodHandle  read    = linker.downcallHandle(GLIBC_LIBARY_LOCKUP.lookup("read").orElseThrow(),
				FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
		MemorySegment buf     = local.allocate(8);
		if (8L != (long) read.invoke(fd, buf, 8)) { throw new IOException("error on read"); }
		if (MAGIC_START != buf.get(ValueLayout.JAVA_LONG, 0)) { throw new IOException("the file system does not start with my magic!"); }
		if (OFFSET_BLOCK_SIZE != (long) lseek.invoke(fd, OFFSET_BLOCK_SIZE, SEEK_SET)) { throw new IOException("error on lseek"); }
		if (4L != (long) read.invoke(fd, buf, 4)) { throw new IOException("error on read"); }
		int         bs = buf.get(ValueLayout.JAVA_INT, 0);
		Addressable bm = (Addressable) newBm.invoke(fd, bs);
		if (0 == (int) pfsLoad.invoke(bm, MemoryAddress.NULL)) { throw thrw(loockup, "load PFS (" + opts.path() + ")"); }
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
	
	public static IOException thrw(SymbolLookup loockup, String msg) throws IOException {
		int pfsErrno = loockup.lookup("pfs_errno").orElseThrow().address().get(ValueLayout.JAVA_INT, 0);
		switch (pfsErrno) {
		case 0: /* if pfs_errno is not set/no error occurred */
			throw new AssertionError("no error: " + msg);
		case 1: /* if an operation failed because of an unknown/unspecified error */
			throw new IOException("unknown error: " + msg);
		case 2: /* if the iterator has no next element */
			throw new NoSuchElementException("no more elements: " + msg);
		case 3: /*
				 * if an IO operation failed because the element is not of the correct type
				 * (file expected, but folder or reverse)
				 */
			throw new IOException("the element has not the expected type: " + msg);
		case 4: /* if an IO operation failed because the element does not exist */
			throw new NoSuchFileException("there is no such file: " + msg);
		case 5: /* if an IO operation failed because the element already existed */
			throw new FileAlreadyExistsException("the file exists already: " + msg);
		case 6: /*
				 * if an IO operation failed because there was not enough space in the file
				 * system
				 */
			throw new IOError(new IOException("out of space: " + msg));
		case 7: /* if an unspecified IO error occurred */
			throw new IOException("IO error: " + msg);
		case 8: /* if there was at least one invalid argument */
			throw new IllegalArgumentException("illegal argument: " + msg);
		case 9: /* if there was an invalid magic value */
			throw new IOException("invalid magic: " + msg);
		case 10: /* if an IO operation failed because there was not enough memory available */
			throw new OutOfMemoryError("out of memory: " + msg);
		case 11: /* if an IO operation failed because the root folder has some restrictions */
			throw new IOException("root folder restrictions: " + msg);
		case 12: /*
					 * if an folder can not be moved because the new child (maybe a deep/indirect
					 * child) is a child of the folder
					 */
			throw new IOException("I won't move a folder to a child of it self: " + msg);
		default:
			throw new IOException("unknown errno (" + pfsErrno + "): " + msg);
		}
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
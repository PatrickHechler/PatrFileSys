package de.hechler.patrick.pfs.fs.impl;

import static de.hechler.patrick.exceptions.PFSErr.*;
import de.hechler.patrick.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.fs.PFS;

import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.SymbolLookup;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.NativeSymbol;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import jdk.incubator.foreign.VaList;

import java.io.IOError;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Paths;
import java.util.Arrays;

import static jdk.incubator.foreign.ValueLayout.*;


public class NativePatrFileSys implements PFS {
	
	private static final int O_RDWR = 02;
	
	private static final int SEEK_SET = 0;
//	private static final int SEEK_CUR = 1;
//	private static final int SEEK_END = 2;
	
	private NativePatrFileSys() {}
	
	static {
		System.load(Paths.get("../native-pfs/native-core/exports/libpfs.so").toAbsolutePath().toString());
	}
	
	public static CLinker             LINKER  = CLinker.systemCLinker();
	private static final SymbolLookup LOOCKUP = SymbolLookup.loaderLookup();
	
	private static RuntimeException t(Throwable e) {
		if (e instanceof Error) {
			throw (Error) e;
		} else if (e instanceof RuntimeException) {
			throw (RuntimeException) e;
		} else {
			throw new RuntimeException(e);
		}
	}
	
	public NativePatrFileSys create(String pfsFile) throws IOException {
		MemorySegment mem = SegmentAllocator.implicitAllocator().allocateUtf8String(pfsFile);
		MethodHandle openHandle = handle("open", JAVA_INT, ADDRESS, JAVA_INT);
		MethodHandle seekHandle = handle("lseek", JAVA_INT, JAVA_INT, JAVA_LONG, JAVA_INT);
		MethodHandle readHandle = handle("read", JAVA_LONG, JAVA_INT, ADDRESS, JAVA_LONG);
		int fd = invokeInt(openHandle, mem, O_RDWR);
		if (fd == -1) {
			throw new PatrFileSysException(PFS_ERRNO_IO_ERR, "open");
		}
		long pos = invokeLong(seekHandle, fd, 0, SEEK_SET);
		if (pos != 0L) {
			throw new PatrFileSysException(PFS_ERRNO_IO_ERR, "lseek");
		}
		mem = SegmentAllocator.implicitAllocator().allocate(8);
		for (int reat = 0; reat < 8;) {
			long r = invokeLong(readHandle, fd, mem, 8L - reat);
			if (r <= 0) {
				throw new PatrFileSysException(PFS_ERRNO_IO_ERR, "read");
			}
			reat -= r;
		}
		throw new UnsupportedOperationException();
	}
	
	private static MethodHandle handle(String name, MemoryLayout returnType, MemoryLayout... argTypes) {
		return LINKER.downcallHandle(LOOCKUP.lookup(name).orElseThrow(), FunctionDescriptor.of(returnType, argTypes));
	}
	
	private static int invokeInt(MethodHandle met, Object... args) {
		try {
			return (int) met.invoke(args);
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	private static long invokeLong(MethodHandle met, Object... args) {
		try {
			return (long) met.invoke(args);
		} catch (Throwable e) {
			throw t(e);
		}
	}
	
	@Override
	public void format(long blockCount) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public PFSFolder root() throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public long blockCount() throws PatrFileSysException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int blockSize() throws PatrFileSysException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public void close() throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}
	
}

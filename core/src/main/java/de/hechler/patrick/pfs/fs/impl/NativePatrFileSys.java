package de.hechler.patrick.pfs.fs.impl;

import java.lang.annotation.Native;

import de.hechler.patrick.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.bm.BlockManager;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.fs.PFS;

public class NativePatrFileSys implements PFS {
	
	/** used as a lock during file system operations, to be thread safe */
	@Native
	public static final Object LOCK = new Object();
	
	public static final int PFS_OPEN_TRUNCATE = 0100;
	public static final int PFS_OPEN_CREATE   = 01000;
	
	@Native
	private long      val1;
	@Native
	private long      val2;
	@Native
	private PFSFolder root;
	
	private NativePatrFileSys() {}
	
	public static native NativePatrFileSys create(String pfsFile) throws PatrFileSysException, NullPointerException;
	
	public static native NativePatrFileSys create(String pfsFile, long blockCount, int blockSize, int openMode, int openPermissions) throws PatrFileSysException, NullPointerException;
	
	public static native NativePatrFileSys createRamFs(long blockCount, int blockSize);
	
	public static native NativePatrFileSys create(BlockManager bm) throws PatrFileSysException;
	
	@Override
	public native void format(long blockCount) throws PatrFileSysException;
	
	@Override
	public native PFSFolder root() throws PatrFileSysException;
	
	@Override
	public native long blockCount() throws PatrFileSysException;
	
	@Override
	public native int blockSize() throws PatrFileSysException;
	
	@Override
	public native void close() throws PatrFileSysException;
	
	@Override
	@Deprecated
	protected native void finalize() throws Throwable;
	
}

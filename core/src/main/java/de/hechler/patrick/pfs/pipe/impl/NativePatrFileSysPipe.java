package de.hechler.patrick.pfs.pipe.impl;

import java.lang.annotation.Native;

import de.hechler.patrick.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.pipe.PFSPipe;

public class NativePatrFileSysPipe implements PFSPipe {
	
	@Native
	private long value;
	
	private NativePatrFileSysPipe(long value) {
		this.value = value;
	}
	
	@Override
	public native long getFlags() throws PatrFileSysException;
	
	@Override
	public native void modifyFlags(long addFlags, long remFlags) throws PatrFileSysException;
	
	@Override
	public native String name() throws PatrFileSysException;
	
	@Override
	public native void name(String newName) throws PatrFileSysException;
	
	@Override
	public native long createTime() throws PatrFileSysException;
	
	@Override
	public native void createTime(long ct) throws PatrFileSysException;
	
	@Override
	public native long lastModTime() throws PatrFileSysException;
	
	@Override
	public native void lastModTime(long lmt) throws PatrFileSysException;
	
	@Override
	public native void delete() throws PatrFileSysException;
	
	@Override
	public native PFSFolder parent() throws PatrFileSysException;
	
	@Override
	public native void parent(PFSFolder newParent) throws PatrFileSysException;
	
	@Override
	public native void move(PFSFolder newParent, String newName) throws PatrFileSysException;
	
	@Override
	public native void read(byte[] data, int length) throws PatrFileSysException;
	
	@Override
	public native void append(byte[] data, int length) throws PatrFileSysException;
	
	@Override
	public native long length() throws PatrFileSysException;
	
}

package de.hechler.patrick.pfs.folder.impl;

import java.lang.annotation.Native;

import de.hechler.patrick.exceptions.PatrFileSysException;
import de.hechler.patrick.interfaces.ThrowableIter;
import de.hechler.patrick.pfs.element.PFSElement;
import de.hechler.patrick.pfs.file.PFSFile;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.pipe.PFSPipe;


public class NativePatrFileSysFolder implements PFSFolder {
	
	@Native
	private long value;
	
	public NativePatrFileSysFolder(long value) {
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
	public native ThrowableIter <PatrFileSysException, PFSElement> iterator() throws PatrFileSysException;
	
	@Override
	public native long childCount() throws PatrFileSysException;
	
	@Override
	public native PFSElement element(String childName) throws PatrFileSysException;
	
	@Override
	public native PFSFolder folder(String childName) throws PatrFileSysException;
	
	@Override
	public native PFSFile file(String childName) throws PatrFileSysException;
	
	@Override
	public native PFSPipe pipe(String childName) throws PatrFileSysException;
	
	@Override
	public native PFSFolder addFolder(String newChildName) throws PatrFileSysException;
	
	@Override
	public native PFSFile addFile(String newChildName) throws PatrFileSysException;
	
	@Override
	public native PFSPipe addPipe(String newChildName) throws PatrFileSysException;
	
	@Override
	@Deprecated
	protected native void finalize() throws Throwable;
	
}

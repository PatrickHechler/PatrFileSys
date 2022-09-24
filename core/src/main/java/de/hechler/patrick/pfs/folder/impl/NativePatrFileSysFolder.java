package de.hechler.patrick.pfs.folder.impl;

import de.hechler.patrick.exceptions.PatrFileSysException;
import de.hechler.patrick.interfaces.ThrowableIter;
import de.hechler.patrick.pfs.element.PFSElement;
import de.hechler.patrick.pfs.file.PFSFile;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.pipe.PFSPipe;


public class NativePatrFileSysFolder implements PFSFolder {

	@Override
	public long getFlags() throws PatrFileSysException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void modifyFlags(long addFlags, long remFlags) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String name() throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void name(String newName) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long createTime() throws PatrFileSysException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void createTime(long ct) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long lastModTime() throws PatrFileSysException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void lastModTime(long lmt) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete() throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PFSFolder parent() throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void parent(PFSFolder newParent) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void move(PFSFolder newParent, String newName) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ThrowableIter <PatrFileSysException, PFSElement> iterator() throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long childCount() throws PatrFileSysException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public PFSElement element(String childName) throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PFSFolder folder(String childName) throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PFSFile file(String childName) throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PFSPipe pipe(String childName) throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PFSFolder addFolder(String newChildName) throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PFSFile addFile(String newChildName) throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PFSPipe addPipe(String newChildName) throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}
}

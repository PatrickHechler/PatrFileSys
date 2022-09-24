package de.hechler.patrick.pfs.pipe.impl;

import de.hechler.patrick.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.pipe.PFSPipe;


public class NativePatrFileSysPipe implements PFSPipe {

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
	public void read(byte[] data, int length) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void append(byte[] data, int length) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long length() throws PatrFileSysException {
		// TODO Auto-generated method stub
		return 0;
	}
}

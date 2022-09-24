package de.hechler.patrick.pfs.element;

import de.hechler.patrick.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.folder.PFSFolder;

public interface PFSElement {
	
	long getFlags() throws PatrFileSysException;
	
	void modifyFlags(long addFlags, long remFlags) throws PatrFileSysException;
	
	String name() throws PatrFileSysException;
	
	void name(String newName) throws PatrFileSysException;
	
	long createTime() throws PatrFileSysException;
	
	void createTime(long ct) throws PatrFileSysException;
	
	long lastModTime() throws PatrFileSysException;
	
	void lastModTime(long lmt) throws PatrFileSysException;
	
	void delete() throws PatrFileSysException;
	
	PFSFolder parent() throws PatrFileSysException;
	
	void parent(PFSFolder newParent) throws PatrFileSysException;
	
	void move(PFSFolder newParent, String newName) throws PatrFileSysException;
	
}

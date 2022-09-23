package de.hechler.patrick.pfs.element;


public interface PatrFileSysElement {
	
	long getFlags();
	
	void modifyFlags(long addFlags, long remFlags);
	
	int nameLength();
	
	String name();
	
	void name(String newName);
	
	long createTime();
	
	void createTime(long ct);
	
	long lastModTime();
	
	void lastModTime(long lmt);
	
}

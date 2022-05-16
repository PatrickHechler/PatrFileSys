package de.hechler.patrick.pfs.objects.jfs;

import java.nio.file.attribute.FileTime;


public interface PatrFileAttributeView {
	
	FileTime lastModifiedTime();
	
	FileTime creationTime();
	
	boolean isRegularFile();
	
	boolean isDirectory();
	
	boolean isSymbolicLink();
	
	boolean isOther();
	
	long size();
	
	Object fileKey();
	
	boolean isReadOnly();
	
	boolean isHidden();
	
	boolean isExecutable();
	
}

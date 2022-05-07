package de.hechler.patrick.pfs.objects.jfs;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;


public class PFSBasicFileAttributesImpl implements BasicFileAttributes {
	
	private final long     size;
	private final boolean  isFile;
	private final FileTime createTime;
	private final FileTime accessTime;
	private final FileTime lastMod;
	
	public PFSBasicFileAttributesImpl(long size, boolean isFile, long createTime, long accessTime, long lastMod) {
		this.size = size;
		this.isFile = isFile;
		this.createTime = FileTime.fromMillis(createTime);
		this.accessTime = FileTime.fromMillis(accessTime);
		this.lastMod = FileTime.fromMillis(lastMod);
	}
	
	@Override
	public FileTime lastModifiedTime() {
		return lastMod;
	}
	
	@Override
	public FileTime lastAccessTime() {
		return accessTime;
	}
	
	@Override
	public FileTime creationTime() {
		return createTime;
	}
	
	@Override
	public boolean isRegularFile() {
		return isFile;
	}
	
	@Override
	public boolean isDirectory() {
		return !isFile;
	}
	
	@Override
	public boolean isSymbolicLink() {
		return false;
	}
	
	@Override
	public boolean isOther() {
		return false;
	}
	
	@Override
	public long size() {
		return size;
	}
	
	@Override
	public Object fileKey() {
		return null;
	}
	
}

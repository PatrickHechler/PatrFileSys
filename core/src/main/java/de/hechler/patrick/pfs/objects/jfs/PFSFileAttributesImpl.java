package de.hechler.patrick.pfs.objects.jfs;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;

import de.hechler.patrick.pfs.interfaces.PatrFileAttributeView;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;


public class PFSFileAttributesImpl implements BasicFileAttributes, DosFileAttributes, PatrFileAttributeView {
	
	private final long     size;
	private final boolean  isFile;
	private final boolean  isFolder;
	private final boolean  isOther;
	private final boolean  isSymLink;
	private final boolean  isReadOnly;
	private final boolean  isHidden;
	private final boolean  isExecutable;
	private final FileTime createTime;
	private final FileTime accessTime;
	private final FileTime lastMod;
	
	public PFSFileAttributesImpl(long size, boolean isFile, boolean isFolder, boolean isOther, boolean isSymLink, boolean isReadOnly, boolean isHidden, boolean isExecutable, long createTime, long lastMod) {
		this.size = size;
		this.isFile = isFile;
		this.isFolder = isFolder;
		this.isOther = isOther;
		this.isSymLink = isSymLink;
		this.isReadOnly = isReadOnly;
		this.isHidden = isHidden;
		this.isExecutable = isExecutable;
		this.createTime = FileTime.fromMillis(createTime);
		this.accessTime = FileTime.fromMillis(PatrFileSysConstants.NO_TIME);
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
		return isFolder;
	}
	
	@Override
	public boolean isSymbolicLink() {
		return isSymLink;
	}
	
	@Override
	public boolean isOther() {
		return isOther;
	}
	
	@Override
	public long size() {
		return size;
	}
	
	@Override
	public Object fileKey() {
		return null;
	}
	
	@Override
	public boolean isReadOnly() {
		return isReadOnly;
	}
	
	@Override
	public boolean isHidden() {
		return isHidden;
	}
	
	@Override
	public boolean isArchive() {
		return false;
	}
	
	@Override
	public boolean isSystem() {
		return false;
	}
	
	@Override
	public boolean isExecutable() {
		return isExecutable;
	}
	
}

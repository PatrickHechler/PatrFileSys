package de.hechler.patrick.pfs.objects.java;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFolder;

public class PFSBasicFileAttributeViewImpl implements BasicFileAttributeView {
	
	private PatrFileSysElement element;
	
	
	
	public PFSBasicFileAttributeViewImpl(PatrFileSysElement element) {
		this.element = element;
	}
	
	
	public void setElement(PatrFileSysElement element) {
		this.element = element;
	}
	
	@Override
	public String name() {
		return PFSFileSystemImpl.ATTR_VIEW_BASIC;
	}
	
	@Override
	public BasicFileAttributes readAttributes() throws IOException {
		boolean isFile = element.isFile();
		long createTime = element.getCreateTime(), lastModTime = element.getLastModTime();
		long size;
		if (isFile) {
			size = element.getFile().length();
		} else {
			size = sizeOf(element.getFolder());
		}
		return new PFSBasicFileAttributes(size, isFile, createTime, -1L, lastModTime);
	}
	
	private static long sizeOf(PatrFolder folder) throws IOException {
		long size = 0L;
		for (PatrFileSysElement child : folder) {
			if (child.isFile()) {
				size += child.getFile().length();
			} else {
				size += sizeOf(child.getFolder());
			}
		}
		return size;
	}
	
	
	@Override
	public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
		if (lastModifiedTime == null && lastAccessTime == null && createTime == null) {
			return;
		}
		throw new IOException("can't set times");
	}
	
}

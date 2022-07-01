package de.hechler.patrick.pfs.objects.jfs;

import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.*;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.*;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;

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
		return ATTR_VIEW_BASIC;
	}
	
	@Override
	public PFSFileAttributesImpl readAttributes() throws IOException {
		return readAttributes(element);
	}
	
	public static PFSFileAttributesImpl readAttributes(PatrFileSysElement element) throws IOException {
		return element.withLock(() -> {
			boolean isFile = element.isFile();
			long createTime = element.getCreateTime(), lastModTime = element.getLastModTime();
			long size;
			try {
				if (isFile) {
					size = element.getFile().length(NO_LOCK);
				} else {
					size = sizeOf(element.getFolder());
				}
			} catch (ElementLockedException e) {
				size = -1;
			}
			boolean link = element.isLink();
			boolean readOnly = element.isReadOnly();
			boolean hidden = element.isHidden();
			boolean executable = element.isExecutable();
			return new PFSFileAttributesImpl(size, isFile, !isFile, link, readOnly, hidden, executable, false, createTime, lastModTime);
		});
	}
	
	public static long sizeOf(PatrFolder folder) throws IOException, ElementLockedException {
		long size = 0L;
		for (PatrFileSysElement child : folder) {
			if (child.isFile()) {
				size += child.getFile().length(NO_LOCK);
			} else {
				size += sizeOf(child.getFolder());
			}
		}
		return size;
	}
	
	@Override
	public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
		if (lastAccessTime != null) {
			throw new UnsupportedOperationException("can't set access time (access time is not supported)");
		}
		if (createTime != null) {
			element.setCreateTime(createTime.toMillis(), PatrFileSysConstants.NO_LOCK);
		}
		if (lastModifiedTime != null) {
			element.setLastModTime(lastModifiedTime.toMillis(), PatrFileSysConstants.NO_LOCK);
		}
	}
	
}

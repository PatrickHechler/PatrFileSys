package de.hechler.patrick.pfs.objects.java;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.nio.file.attribute.FileTime;

import de.hechler.patrick.pfs.interfaces.PatrFileSystem;


public class PFSFileStoreImpl extends FileStore {
	
	private final PatrFileSystem fileSys;
	
	public PFSFileStoreImpl(PatrFileSystem fileSys) {
		this.fileSys = fileSys;
	}
	
	@Override
	public String name() {
		try {
			return "patr_file_sys:store:" + fileSys.blockCount() + "*" + fileSys.blockSize();
		} catch (IOException e) {
			return "patr_file_sys:store";
		}
	}
	
	@Override
	public String type() {
		return "patr_file_sys:store";
	}
	
	@Override
	public boolean isReadOnly() {
		return false;
	}
	
	@Override
	public long getTotalSpace() throws IOException {
		return fileSys.totalSpace();
	}
	
	@Override
	public long getUsableSpace() throws IOException {
		return fileSys.freeSpace();
	}
	
	@Override
	public long getUnallocatedSpace() throws IOException {
		return fileSys.freeSpace();
	}
	
	@Override
	public boolean supportsFileAttributeView(Class <? extends FileAttributeView> type) {
		if (type.isAssignableFrom(PFSBasicFileAttributeViewImpl.class)) {
			return true;
		}
		return false;
	}
	
	@Override
	public boolean supportsFileAttributeView(String name) {
		return PFSFileSystemImpl.ATTR_VIEW_BASIC.equalsIgnoreCase(name);
	}
	
	@Override
	public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class <V> type) {
		if (type == FileStoreAttributeView.class) {
			return type.cast((FileStoreAttributeView) () -> "patr_file_store_attr_view");
		}
		return null;
	}
	
	@Override
	public Object getAttribute(String attribute) throws IOException {
		String[] strs = attribute.split("\\:", 2);
		if ( !supportsFileAttributeView(strs[0])) {
			throw new IllegalArgumentException("attribut from unsupported view!");
		}
		switch (strs[1]) {
		case "lastModifiedTime":
			return FileTime.fromMillis(fileSys.getRoot().getLastModTime());
		case "lastAccessTime":
			return FileTime.fromMillis( -1L);
		case "creationTime":
			return FileTime.fromMillis(fileSys.getRoot().getCreateTime());
		case "size":
			return Long.valueOf(fileSys.usedSpace());
		case "isRegularFile":
		case "isSymbolicLink":
		case "isOther":
			return Boolean.FALSE;
		case "isDirectory":
			return Boolean.TRUE;
		case "fileKey":
			return null;
		}
		throw new IllegalArgumentException("unknown value");
	}
	
}

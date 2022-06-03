package de.hechler.patrick.pfs.objects.jfs;

import static de.hechler.patrick.pfs.utils.JavaPFSConsants.ATTR_VIEW_BASIC;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_CREATION_TIME;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_FILE_KEY;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_IS_DIRECTORY;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_IS_OTHER;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_IS_REGULAR_FILE;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_LAST_ACCESS_TIME;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_LAST_MODIFIED_TIME;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.BASIC_ATTRIBUTE_SIZE;
import static de.hechler.patrick.pfs.utils.JavaPFSConsants.URI_SHEME;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.nio.file.attribute.FileTime;

import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;


public class PFSFileStoreImpl extends FileStore {
	
	private final PatrFileSystem fileSys;
	
	public PFSFileStoreImpl(PatrFileSystem fileSys) {
		this.fileSys = fileSys;
	}
	
	@Override
	public String name() {
		try {
			return URI_SHEME + ":store:" + fileSys.blockCount() + "*" + fileSys.blockSize();
		} catch (IOException e) {
			return URI_SHEME + ":store";
		}
	}
	
	@Override
	public String type() {
		return URI_SHEME + ":store";
	}
	
	@Override
	public boolean isReadOnly() {
		return false;
	}
	
	@Override
	public long getTotalSpace() throws IOException {
		try {
			return Math.multiplyExact(fileSys.blockCount(), fileSys.blockSize());
		} catch (ArithmeticException e) {
			return Long.MAX_VALUE;
		}
	}
	
	@Override
	public long getUsableSpace() throws IOException {
		try {
			return Math.multiplyExact(fileSys.freeBlocks(), fileSys.blockSize());
		} catch (ArithmeticException e) {
			return Long.MAX_VALUE;
		}
	}
	
	@Override
	public long getUnallocatedSpace() throws IOException {
		try {
			return Math.multiplyExact(fileSys.freeBlocks(), fileSys.blockSize());
		} catch (ArithmeticException e) {
			return Long.MAX_VALUE;
		}
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
		return ATTR_VIEW_BASIC.equalsIgnoreCase(name);
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
		case BASIC_ATTRIBUTE_LAST_MODIFIED_TIME:
			return FileTime.fromMillis(fileSys.getRoot().getLastModTime());
		case BASIC_ATTRIBUTE_LAST_ACCESS_TIME:
			return FileTime.fromMillis(PatrFileSysConstants.NO_TIME);
		case BASIC_ATTRIBUTE_CREATION_TIME:
			return FileTime.fromMillis(fileSys.getRoot().getCreateTime());
		case BASIC_ATTRIBUTE_SIZE:
			try {
				return Long.valueOf(Math.multiplyExact(fileSys.freeBlocks(), fileSys.blockSize()));
			} catch (ArithmeticException e) {
				return Long.valueOf(Long.MAX_VALUE);
			}
		case BASIC_ATTRIBUTE_IS_REGULAR_FILE:
			return Boolean.FALSE;
		case BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK:
			return Boolean.FALSE;
		case BASIC_ATTRIBUTE_IS_OTHER:
			return Boolean.FALSE;
		case BASIC_ATTRIBUTE_IS_DIRECTORY:
			return Boolean.TRUE;
		case BASIC_ATTRIBUTE_FILE_KEY:
			return null;
		}
		throw new IllegalArgumentException("unknown value");
	}
	
}

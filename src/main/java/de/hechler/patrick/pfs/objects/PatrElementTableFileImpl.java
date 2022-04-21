package de.hechler.patrick.pfs.objects;

import java.io.IOException;

import de.hechler.patrick.pfs.interfaces.BlockManager;
import de.hechler.patrick.pfs.interfaces.PatrFolder;

public class PatrElementTableFileImpl extends PatrFileImpl {
	
	public PatrElementTableFileImpl(PatrFileSysImpl fs, long startTime, BlockManager bm, long id) {
		super(fs, startTime, bm, id);
	}
	
	@Override
	public PatrFolderImpl getParent() throws IllegalStateException, IOException {
		throw new IllegalStateException("the root dos not have a Parent Folder");
	}
	
	@Override
	public void setParent(PatrFolder newParent, long myLock, long oldParentLock, long newParentLock) throws IllegalStateException, IOException {
		throw new IllegalStateException("the root can not have a Parent Folder");
	}
	
	@Override
	public String getName() throws IOException {
		return "";
	}
	
	@Override
	protected int getNameByteCount() throws IOException, IllegalStateException {
		return 0;
	}
	
}

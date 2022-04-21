package de.hechler.patrick.pfs.objects;

import java.io.IOException;

import de.hechler.patrick.pfs.interfaces.BlockManager;
import de.hechler.patrick.pfs.interfaces.PatrFolder;

public class PatrRootFolderImpl extends PatrFolderImpl {
	
	public PatrRootFolderImpl(PatrFileSysImpl fs, long startTime, BlockManager bm, long id) {
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
	public boolean isRoot() {
		return true;
	}
	
	@Override
	public void setName(String name, long lock) throws NullPointerException, IllegalStateException {
		throw new IllegalStateException("the root folder can not have a name!");
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

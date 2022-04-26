package de.hechler.patrick.pfs.objects.bfs;

import java.io.IOException;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.interfaces.PatrLink;


public class BufferedFolderImpl extends BufferedFileSysElementImpl implements PatrFolder {
	
	public BufferedFolderImpl(PatrFileSysElementBuffer buffer) {
		super(buffer);
	}
	
	@Override
	public PatrFolder addFolder(String name, long lock) throws IOException, NullPointerException, ElementLockedException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public PatrFile addFile(String name, long lock) throws IOException, NullPointerException, ElementLockedException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public PatrLink addLink(String name, PatrFileSysElement target, long lock) throws IOException, NullPointerException, ElementLockedException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean isRoot() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public int elementCount(long lock) throws ElementLockedException, IOException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public PatrFileSysElement getElement(int index, long lock) throws ElementLockedException, IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
}

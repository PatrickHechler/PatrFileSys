package de.hechler.patrick.ownfs.objects;

import static de.hechler.patrick.ownfs.utils.ConvertNumByteArr.*;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.*;

import java.io.IOException;
import java.util.Iterator;

import de.hechler.patrick.ownfs.interfaces.BlockAccessor;
import de.hechler.patrick.ownfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.ownfs.interfaces.PatrFolder;


public class PatrFolderImpl extends PatrFSElement implements PatrFolder {
	
	public PatrFolderImpl(BlockAccessor ba, long block, int pos) {
		super(ba, block, pos);
	}

	@Override
	public Iterator <PatrFileSysElement> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addFolder(String name) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addFile(String name) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete() throws IllegalStateException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canDeleteWithContent() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRoot() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int elementCount() {
		// TODO Auto-generated method stub
		return 0;
	}
}

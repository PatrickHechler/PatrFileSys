package de.hechler.patrick.ownfs.objects;

import de.hechler.patrick.ownfs.interfaces.BlockManager;
import de.hechler.patrick.ownfs.interfaces.PatrFolder;


public class PatrFolderImpl extends PatrFileSysElementImpl implements PatrFolder {

	public PatrFolderImpl(long startTime, BlockManager bm, long block, int pos) {
		super(startTime, bm, block, pos);
	}
	
}

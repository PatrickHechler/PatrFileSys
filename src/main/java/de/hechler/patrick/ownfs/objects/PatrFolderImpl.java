package de.hechler.patrick.ownfs.objects;

import de.hechler.patrick.ownfs.interfaces.BlockManager;
import de.hechler.patrick.ownfs.interfaces.PatrFolder;


public class PatrFolderImpl extends PatrFSElement implements PatrFolder {
	
	public PatrFolderImpl(BlockManager bm, long block, int pos) {
		super(bm, block, pos);
	}

}

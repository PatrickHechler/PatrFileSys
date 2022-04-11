package de.hechler.patrick.ownfs.objects;

import de.hechler.patrick.ownfs.interfaces.BlockManager;
import de.hechler.patrick.ownfs.interfaces.PatrFile;


public class PatrFileImpl extends PatrFSElement implements PatrFile {

	public PatrFileImpl(BlockManager bm, long block, int pos) {
		super(bm, block, pos);
	}

}

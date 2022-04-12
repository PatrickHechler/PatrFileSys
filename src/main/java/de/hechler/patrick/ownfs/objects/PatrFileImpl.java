package de.hechler.patrick.ownfs.objects;

import de.hechler.patrick.ownfs.interfaces.BlockManager;
import de.hechler.patrick.ownfs.interfaces.PatrFile;


public class PatrFileImpl extends PatrFileSysElementImpl implements PatrFile {
	
	public PatrFileImpl(long startTime, BlockManager bm, long block, int pos) {
		super(startTime, bm, block, pos);
	}
	
}

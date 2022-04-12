package de.hechler.patrick.ownfs.objects;

import static de.hechler.patrick.ownfs.utils.ConvertNumByteArr.*;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.*;

import java.io.IOException;

import de.hechler.patrick.ownfs.interfaces.BlockManager;
import de.hechler.patrick.ownfs.interfaces.PatrFolder;

public class PatrRootFolderImpl extends PatrFolderImpl {
	
	public PatrRootFolderImpl(long startTime, BlockManager bm, long block, int pos) {
		super(startTime, bm, block, pos);
	}
	
	@Override
	public PatrFolder getParent() throws IllegalStateException, IOException {
		throw new IllegalStateException("the root dos not have a Parent Folder");
	}
	
	@Override
	public boolean isRoot() {
		return true;
	}
	
	@Override
	public void setName(String name) throws NullPointerException, IllegalStateException {
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
	
	@Override
	protected void setNewPosToParent(long oldBlock, int oldPos, long newBlock, int newPos) throws IOException {
		byte[] bytes = bm.getBlock(0L);
		try {
			longToByteArr(bytes, FB_ROOT_BLOCK_OFFSET, newBlock);
			longToByteArr(bytes, FB_ROOT_POS_OFFSET, newPos);
		} finally {
			bm.setBlock(0L);
		}
	}
	
}

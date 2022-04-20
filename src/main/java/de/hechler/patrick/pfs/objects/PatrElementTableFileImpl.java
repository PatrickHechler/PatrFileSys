package de.hechler.patrick.pfs.objects;

import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.longToByteArr;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_TABLE_FILE_BLOCK_OFFSET;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_TABLE_FILE_POS_OFFSET;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

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
	
	@Override
	protected void setNewPosToOthers(long oldBlock, int oldPos, long newBlock, int newPos) throws IOException {
		simpleWithLock(bm, () -> executeSetNewPosToOthers(newBlock, newPos));
	}
	
	private void executeSetNewPosToOthers(long newBlock, int newPos) throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(0L);
		try {
			longToByteArr(bytes, FB_TABLE_FILE_BLOCK_OFFSET, newBlock);
			longToByteArr(bytes, FB_TABLE_FILE_POS_OFFSET, newPos);
		} finally {
			bm.setBlock(0L);
		}
	}
}

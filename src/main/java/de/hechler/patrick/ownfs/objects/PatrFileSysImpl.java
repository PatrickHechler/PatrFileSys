package de.hechler.patrick.ownfs.objects;

import static de.hechler.patrick.ownfs.utils.ConvertNumByteArr.byteArrToInt;
import static de.hechler.patrick.ownfs.utils.ConvertNumByteArr.byteArrToLong;
import static de.hechler.patrick.ownfs.utils.ConvertNumByteArr.intToByteArr;
import static de.hechler.patrick.ownfs.utils.ConvertNumByteArr.longToByteArr;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.ELEMENT_FLAG_FOLDER;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_CREATE_TIME;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_FLAGS;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LAST_MOD_TIME;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LOCK_VALUE;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_NAME;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_OWNER;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_PARENT_BLOCK;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_PARENT_POS;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.FB_BLOCK_COUNT_OFFSET;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.FB_BLOCK_LENGTH_OFFSET;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.FB_ROOT_BLOCK_OFFSET;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.FB_ROOT_POS_OFFSET;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.FB_START_ROOT_POS;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.FOLDER_OFFSET_ELEMENT_COUNT;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.FOLDER_OFFSET_FOLDER_ELEMENTS;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.LOCK_NO_LOCK;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.OWNER_NO_OWNER;

import java.io.IOException;

import de.hechler.patrick.ownfs.interfaces.BlockAccessor;
import de.hechler.patrick.ownfs.interfaces.BlockManager;
import de.hechler.patrick.ownfs.interfaces.PatrFileSystem;
import de.hechler.patrick.ownfs.interfaces.PatrFolder;
import de.hechler.patrick.ownfs.objects.ba.BlockManagerImpl;


public class PatrFileSysImpl implements PatrFileSystem {
	
	private final BlockManager bm;
	private volatile long      startTime;
	
	public PatrFileSysImpl(BlockAccessor ba) {
		this(new BlockManagerImpl(ba));
	}
	
	public PatrFileSysImpl(BlockManager bm) {
		this.bm = bm;
		this.startTime = System.currentTimeMillis();
	}
	
	
	
	public long getStartTime() {
		return startTime;
	}
	
	@Override
	public PatrFolder getRoot() throws IOException {
		byte[] bytes = bm.getBlock(0L);
		try {
			return new PatrFolderImpl(startTime, bm, byteArrToLong(bytes, FB_ROOT_BLOCK_OFFSET), byteArrToInt(bytes, FB_ROOT_POS_OFFSET)) {
				
				@Override
				public void setName(String name) throws NullPointerException, IllegalStateException {
					throw new IllegalStateException("the root folder can not have a name!");
				}
				
				@Override
				public String getName() throws IOException {
					return "";
				}
				
			};
		} finally {
			bm.ungetBlock(0L);
		}
	}
	
	@Override
	public void close() throws IOException {
		bm.close();
	}
	
	@Override
	public void format() throws IOException {
		format(Long.MAX_VALUE);
	}
	
	public void format(long blockCount) throws IOException {
		byte[] bytes = bm.getBlock(0L);
		try {
			int blockSize = bm.blockSize();
			assert blockSize == bytes.length;
			initBlock(bytes, FB_START_ROOT_POS + FOLDER_OFFSET_FOLDER_ELEMENTS);
			
			intToByteArr(bytes, FB_BLOCK_LENGTH_OFFSET, blockSize);
			longToByteArr(bytes, FB_BLOCK_COUNT_OFFSET, blockCount);
			longToByteArr(bytes, FB_ROOT_BLOCK_OFFSET, 0L);
			intToByteArr(bytes, FB_ROOT_POS_OFFSET, FB_START_ROOT_POS);
			
			startTime = System.currentTimeMillis();
			longToByteArr(bytes, FB_START_ROOT_POS + ELEMENT_OFFSET_CREATE_TIME, startTime);
			intToByteArr(bytes, FB_START_ROOT_POS + ELEMENT_OFFSET_FLAGS, ELEMENT_FLAG_FOLDER);
			longToByteArr(bytes, FB_START_ROOT_POS + ELEMENT_OFFSET_LAST_MOD_TIME, startTime);
			longToByteArr(bytes, FB_START_ROOT_POS + ELEMENT_OFFSET_LOCK_VALUE, LOCK_NO_LOCK);
			intToByteArr(bytes, FB_START_ROOT_POS + ELEMENT_OFFSET_NAME, -1);
			intToByteArr(bytes, FB_START_ROOT_POS + ELEMENT_OFFSET_OWNER, OWNER_NO_OWNER);
			longToByteArr(bytes, FB_START_ROOT_POS + ELEMENT_OFFSET_PARENT_BLOCK, -1L);
			intToByteArr(bytes, FB_START_ROOT_POS + ELEMENT_OFFSET_PARENT_POS, -1);
			intToByteArr(bytes, FB_START_ROOT_POS + FOLDER_OFFSET_ELEMENT_COUNT, 0);
		} finally {
			bm.setBlock(0L);
		}
		bytes = bm.getBlock(1L);
		try {
			longToByteArr(bytes, 0, 0L);
			longToByteArr(bytes, 8, 2L);
		} finally {
			bm.setBlock(1L);
		}
	}
	
	public static void initBlock(byte[] bytes, int startLen) {
		int blockSize = bytes.length;
		final int tableStart = blockSize - 20;
		if (blockSize < startLen - 32) {
			throw new OutOfMemoryError("the block is not big enugh! blockSize=" + blockSize + " startLen=" + startLen + " (note, that the table also needs " + (blockSize - tableStart) + " bytes)");
		}
		intToByteArr(bytes, blockSize - 4, tableStart);
		intToByteArr(bytes, blockSize - 8, blockSize);
		intToByteArr(bytes, blockSize - 12, tableStart);
		intToByteArr(bytes, blockSize - 16, startLen);
		intToByteArr(bytes, tableStart, 0);
	}
	
}

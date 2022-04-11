package de.hechler.patrick.ownfs.objects.old;

import static de.hechler.patrick.ownfs.utils.ConvertNumByteArr.*;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.*;

import java.io.IOException;

import de.hechler.patrick.ownfs.interfaces.BlockAccessor;
import de.hechler.patrick.ownfs.interfaces.BlockManager;
import de.hechler.patrick.ownfs.interfaces.PatrFileSystem;
import de.hechler.patrick.ownfs.interfaces.PatrFolder;
import de.hechler.patrick.ownfs.objects.ba.BlockManagerImpl;


public class PatrFileSysImpl implements PatrFileSystem {
	
	private final BlockManager bm;
	
	public PatrFileSysImpl(BlockAccessor ba) {
		this(new BlockManagerImpl(ba));
	}
	
	public PatrFileSysImpl(BlockManager bm) {
		this.bm = bm;
	}
	
	@Override
	public PatrFolder getRoot() throws IOException {
		byte[] bytes = bm.getBlock(0L);
		try {
			return new PatrFolderImpl(bm, byteArrToLong(bytes, FB_ROOT_BLOCK_OFFSET), byteArrToInt(bytes, FB_ROOT_POS_OFFSET));
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
			
			intToByteArr(bytes, FB_START_ROOT_POS + ELEMENT_OFFSET_FLAGS, ELEMENT_FLAG_FOLDER);
			intToByteArr(bytes, FB_START_ROOT_POS + ELEMENT_OFFSET_METADATA_LENGTH, 0);
			intToByteArr(bytes, FB_START_ROOT_POS + ELEMENT_OFFSET_METADATA_POS, -1);
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
		if (blockSize < startLen) {
			throw new OutOfMemoryError("the block is not big enugh! blockSize=" + blockSize + " startLen=" + startLen);
		}
		intToByteArr(bytes, blockSize - 4, blockSize);
		intToByteArr(bytes, blockSize - 8, blockSize - 32);
		intToByteArr(bytes, blockSize - 16, startLen);
		intToByteArr(bytes, blockSize - 24, 0);
	}
	
}

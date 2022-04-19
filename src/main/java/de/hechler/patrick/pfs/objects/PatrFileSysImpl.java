package de.hechler.patrick.pfs.objects;

import static de.hechler.patrick.pfs.objects.PatrFileSysElementImpl.simpleWithLock;
import static de.hechler.patrick.pfs.objects.PatrFileSysElementImpl.simpleWithLockInt;
import static de.hechler.patrick.pfs.objects.PatrFileSysElementImpl.simpleWithLockLong;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToInt;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToLong;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.intToByteArr;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.longToByteArr;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_FOLDER;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_CREATE_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_FLAGS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LAST_MOD_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LOCK_VALUE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_NAME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_OWNER;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_PARENT_BLOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_PARENT_POS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_BLOCK_COUNT_OFFSET;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_BLOCK_LENGTH_OFFSET;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_ROOT_BLOCK_OFFSET;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_ROOT_POS_OFFSET;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_START_ROOT_POS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_OFFSET_ELEMENT_COUNT;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_OFFSET_FOLDER_ELEMENTS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.OWNER_NO_OWNER;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

import de.hechler.patrick.pfs.interfaces.BlockAccessor;
import de.hechler.patrick.pfs.interfaces.BlockManager;
import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.objects.ba.BlockManagerImpl;


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
		return simpleWithLock(bm, this::executeGetRoot, 0, 0L);
	}
	
	private PatrFolder executeGetRoot() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(0L);
		try {
			long rootblock = byteArrToLong(bytes, FB_ROOT_BLOCK_OFFSET);
			int rootpos = byteArrToInt(bytes, FB_ROOT_POS_OFFSET);
			return new PatrRootFolderImpl(startTime, bm, rootblock, rootpos);
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
		simpleWithLock(bm, () -> executeFormat(blockCount), 0, 0L, 1L);
	}
	
	public void executeFormat(long blockCount) throws IOException {
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
			intToByteArr(bytes, bytes.length - 4, 16);
		} finally {
			bm.setBlock(1L);
		}
	}
	
	@Override
	public long totalSpace() throws IOException {
		return simpleWithLockLong(bm, this::executeTotalSpace, 0, 0L);
	}
	
	private long executeTotalSpace() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(0L);
		try {
			long space = byteArrToLong(bytes, FB_BLOCK_COUNT_OFFSET);
			if (space == Long.MAX_VALUE) {
				return Long.MAX_VALUE;
			}
			space *= (long) byteArrToInt(bytes, FB_BLOCK_LENGTH_OFFSET);
			if (space < 0) {
				return Long.MAX_VALUE;
			} else {
				return space;
			}
		} finally {
			bm.ungetBlock(0L);
		}
	}
	
	@Override
	public long freeSpace() throws IOException {
		return simpleWithLockLong(bm, this::executeFreeSpace, 0, 0L, 1L);
	}
	
	private long executeFreeSpace() throws IOException {
		long space = executeTotalSpace();
		if (space == Long.MAX_VALUE) {
			return Long.MAX_VALUE;
		} else {
			space -= executeUsedSpace();
			return space;
		}
	}
	
	@Override
	public long usedSpace() throws IOException {
		return simpleWithLockLong(bm, this::executeUsedSpace, 0, 1L);
	}
	
	private long executeUsedSpace() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(1L);
		try {
			long used = 0;
			int end = byteArrToInt(bytes, bytes.length - 4);
			for (int off = 0; off < end; off += 16) {
				long startBlock = byteArrToLong(bytes, off),
					endBlock = byteArrToLong(bytes, off + 8),
					count = endBlock - startBlock;
				used += count * (long) bytes.length;
				if (used < 0L) {
					return Long.MAX_VALUE;
				}
			}
			return used;
		} finally {
			bm.ungetBlock(1L);
		}
	}
	
	@Override
	public int blockSize() throws IOException {
		return simpleWithLockInt(bm, this::executeBlockSize, 0, 0L);
	}
	
	public static int blockSize(byte[] bytes, int off) throws IOException {
		return byteArrToInt(bytes, FB_BLOCK_LENGTH_OFFSET - off);
	}
	
	public static long blockCount(byte[] bytes, int off) throws IOException {
		return byteArrToLong(bytes, FB_BLOCK_COUNT_OFFSET - off);
	}
	
	private int executeBlockSize() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(0L);
		try {
			return byteArrToInt(bytes, FB_BLOCK_LENGTH_OFFSET);
		} finally {
			bm.ungetBlock(0L);
		}
	}
	
	@Override
	public long blockCount() throws IOException {
		return simpleWithLockLong(bm, this::executeBlockCount, 0, 0L);
	}
	
	private long executeBlockCount() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(0L);
		try {
			return byteArrToLong(bytes, FB_BLOCK_COUNT_OFFSET);
		} finally {
			bm.ungetBlock(0L);
		}
	}
	
	public static void initBlock(byte[] bytes, int startLen) {
		final int blockSize = bytes.length,
			tableStart = blockSize - 20;
		if (tableStart < 0) {
			throw new OutOfMemoryError("the block is not big enugh! blockSize=" + blockSize + " startLen=" + startLen + " (note, that the table also needs " + (blockSize - tableStart) + " bytes)");
		}
		intToByteArr(bytes, blockSize - 4, tableStart);
		intToByteArr(bytes, tableStart + 12, blockSize);
		intToByteArr(bytes, tableStart + 8, tableStart);
		intToByteArr(bytes, tableStart + 4, startLen);
		intToByteArr(bytes, tableStart, 0);
	}
	
}

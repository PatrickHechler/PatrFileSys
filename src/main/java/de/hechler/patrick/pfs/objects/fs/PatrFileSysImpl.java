package de.hechler.patrick.pfs.objects.fs;

import static de.hechler.patrick.pfs.objects.fs.PatrFileSysElementImpl.simpleWithLock;
import static de.hechler.patrick.pfs.objects.fs.PatrFileSysElementImpl.simpleWithLockInt;
import static de.hechler.patrick.pfs.objects.fs.PatrFileSysElementImpl.simpleWithLockLong;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToInt;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToLong;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.intToByteArr;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.longToByteArr;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_FOLDER;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_CREATE_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_FLAGS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_ID;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LAST_MOD_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LOCK_VALUE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_NAME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_OWNER;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_PARENT_ID;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_TABLE_ELEMENT_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_TABLE_FILE_ID;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_TABLE_OFFSET_BLOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_TABLE_OFFSET_POS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_BLOCK_COUNT_OFFSET;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_BLOCK_LENGTH_OFFSET;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_ROOT_BLOCK_OFFSET;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_ROOT_POS_OFFSET;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_START_ROOT_POS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_TABLE_FILE_BLOCK_OFFSET;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_TABLE_FILE_POS_OFFSET;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_DATA_TABLE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_ELEMENT_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_OFFSET_ELEMENT_COUNT;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_OFFSET_FOLDER_ELEMENTS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_ID;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_OWNER;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ROOT_FOLDER_ID;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;

import de.hechler.patrick.pfs.exception.OutOfSpaceException;
import de.hechler.patrick.pfs.interfaces.BlockAccessor;
import de.hechler.patrick.pfs.interfaces.BlockManager;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.objects.LongInt;
import de.hechler.patrick.pfs.objects.ba.BlockManagerImpl;

public class PatrFileSysImpl implements PatrFileSystem {

	private final BlockManager bm;
	private volatile long startTime;
	private PatrFileImpl blockTable;
	private PatrFolder root;

	public PatrFileSysImpl(BlockAccessor ba) {
		this(new BlockManagerImpl(ba));
	}

	public PatrFileSysImpl(BlockManager bm) {
		this.bm = bm;
		this.startTime = System.currentTimeMillis();
		this.blockTable = new PatrElementTableFileImpl(this, startTime, bm, ELEMENT_TABLE_FILE_ID);
		this.root = new PatrFolderImpl(this, startTime, bm, ROOT_FOLDER_ID);
	}

	public BlockManager getBlockManager() {
		return bm;
	}

	public long getStartTime() {
		return startTime;
	}

	@Override
	public PatrFolder getRoot() throws IOException {
		return root;
	}

	@Override
	public PatrFileSysElement fromID(Object id) throws IOException, IllegalArgumentException, NullPointerException {
		return simpleWithLock(bm, () -> executeFromID(id));
	}

	private PatrFileSysElement executeFromID(Object id) throws IllegalAccessError {
		if (id == null) {
			throw new NullPointerException("id is null");
		}
		if (!(id instanceof PatrID)) {
			throw new IllegalAccessError("the given id is invalid (class: '" + id.getClass() + "' tos: '" + id + "')");
		}
		PatrID pid = (PatrID) id;
		if (pid.startTime != startTime || pid.fs.bm != bm || pid.fs != this) {
			throw new IllegalAccessError("the given id does not belong th this file system");
		}
		return new PatrFileSysElementImpl(this, pid.startTime, pid.fs.bm, pid.id);
	}

	@Override
	public void close() throws IOException {
		bm.close();
	}

	public void format(long blockCount) throws IOException {
		format(blockCount, bm.blockSize());
	}

	@Override
	public void format(long blockCount, int blockSize) throws IOException {
		simpleWithLock(bm, () -> executeFormat(blockCount, blockSize));
	}

	public void executeFormat(long blockCount, int blockSize) throws IOException {
		if (blockSize != bm.blockSize()) {
			throw new IllegalStateException("the given size does not match the size of my block manager!");
		}
		byte[] bytes = bm.getBlock(1L);
		try {
			longToByteArr(bytes, 0, 0L);
			longToByteArr(bytes, 8, 2L);
			intToByteArr(bytes, bytes.length - 4, 16);
		} finally {
			bm.setBlock(1L);
		}
		bytes = bm.getBlock(0L);
		try {
			assert blockSize == bytes.length;
			initBlock(bytes, FB_START_ROOT_POS + FOLDER_OFFSET_FOLDER_ELEMENTS + FOLDER_ELEMENT_LENGTH);
			longToByteArr(bytes, FB_BLOCK_COUNT_OFFSET, blockCount);
			intToByteArr(bytes, FB_BLOCK_LENGTH_OFFSET, blockSize);
			longToByteArr(bytes, FB_ROOT_BLOCK_OFFSET, 0L);
			intToByteArr(bytes, FB_ROOT_POS_OFFSET, FB_START_ROOT_POS);
			startTime = System.currentTimeMillis();
			blockTable = new PatrElementTableFileImpl(this, startTime, bm, ELEMENT_TABLE_FILE_ID);
			root = new PatrFolderImpl(this, startTime, bm, ROOT_FOLDER_ID);
			longToByteArr(bytes, FB_START_ROOT_POS + ELEMENT_OFFSET_CREATE_TIME, startTime);
			intToByteArr(bytes, FB_START_ROOT_POS + ELEMENT_OFFSET_FLAGS, ELEMENT_FLAG_FOLDER);
			longToByteArr(bytes, FB_START_ROOT_POS + ELEMENT_OFFSET_LAST_MOD_TIME, startTime);
			longToByteArr(bytes, FB_START_ROOT_POS + ELEMENT_OFFSET_LOCK_VALUE, NO_LOCK);
			intToByteArr(bytes, FB_START_ROOT_POS + ELEMENT_OFFSET_NAME, -1);
			intToByteArr(bytes, FB_START_ROOT_POS + ELEMENT_OFFSET_OWNER, NO_OWNER);
			longToByteArr(bytes, FB_START_ROOT_POS + ELEMENT_OFFSET_ID, ROOT_FOLDER_ID);
			longToByteArr(bytes, FB_START_ROOT_POS + ELEMENT_OFFSET_PARENT_ID, NO_ID);
			intToByteArr(bytes, FB_START_ROOT_POS + FOLDER_OFFSET_ELEMENT_COUNT, 0);
			int blockTablePos = blockTable.allocate(0L, FILE_OFFSET_FILE_DATA_TABLE);
			PatrFolderImpl.initChild(bm, NO_ID, NO_OWNER, false, blockTablePos, -1, 0L, null, null);
			longToByteArr(bytes, FB_TABLE_FILE_BLOCK_OFFSET, 0L);
			longToByteArr(bytes, FB_TABLE_FILE_POS_OFFSET, blockTablePos);
		} finally {
			bm.setBlock(0L);
		}
	}

	public static void setBlockAndPos(PatrID id, long block, int pos) throws IOException {
		simpleWithLock(id.fs.bm, () -> executeSetBlockAndPos(id, block, pos));
	}

	private static void executeSetBlockAndPos(PatrID id, long block, int pos)
			throws IOException, IllegalArgumentException {
		if (id.id == ROOT_FOLDER_ID) {
			byte[] bytes = id.fs.bm.getBlock(0L);
			try {
				longToByteArr(bytes, FB_ROOT_BLOCK_OFFSET, block);
				longToByteArr(bytes, FB_ROOT_POS_OFFSET, pos);
			} finally {
				id.fs.bm.setBlock(0L);
			}
		} else if (id.id == ELEMENT_TABLE_FILE_ID) {
			byte[] bytes = id.fs.bm.getBlock(0L);
			try {
				longToByteArr(bytes, FB_TABLE_FILE_BLOCK_OFFSET, block);
				longToByteArr(bytes, FB_TABLE_FILE_POS_OFFSET, pos);
			} finally {
				id.fs.bm.setBlock(0L);
			}
		} else if (id.id >= 0L) {
			byte[] bytes = new byte[ELEMENT_TABLE_ELEMENT_LENGTH];
			longToByteArr(bytes, ELEMENT_TABLE_OFFSET_BLOCK, block);
			intToByteArr(bytes, ELEMENT_TABLE_OFFSET_POS, pos);
			id.fs.blockTable.setContent(bytes, id.id, 0, ELEMENT_TABLE_ELEMENT_LENGTH, NO_LOCK);
		} else {
			throw new IllegalArgumentException("illegal id; id=" + id.id);
		}
	}

	public static LongInt getBlockAndPos(PatrID id) throws IllegalArgumentException, IOException {
		if (id.startTime != id.fs.startTime) {
			throw new IllegalStateException("the start time of the given id is invalid!");
		}
		return simpleWithLock(id.fs.bm, () -> id.fs.executeGetBlockAndPos(id.id));
	}

	public LongInt getBlockAndPos(long id) throws IllegalArgumentException, IOException {
		return simpleWithLock(bm, () -> executeGetBlockAndPos(id));
	}

	private LongInt executeGetBlockAndPos(long id) throws IllegalArgumentException, IOException {
		long block;
		int pos;
		if (id == ELEMENT_TABLE_FILE_ID) {
			byte[] bytes = bm.getBlock(0L);
			try {
				block = byteArrToLong(bytes, FB_TABLE_FILE_BLOCK_OFFSET);
				pos = byteArrToInt(bytes, FB_TABLE_FILE_POS_OFFSET);
			} finally {
				bm.ungetBlock(0L);
			}
		} else if (id == ROOT_FOLDER_ID) {
			byte[] bytes = bm.getBlock(0L);
			try {
				block = byteArrToLong(bytes, FB_ROOT_BLOCK_OFFSET);
				pos = byteArrToInt(bytes, FB_ROOT_POS_OFFSET);
			} finally {
				bm.ungetBlock(0L);
			}
		} else if (id >= 0L) {
			byte[] bytes = new byte[ELEMENT_TABLE_ELEMENT_LENGTH];
			blockTable.getContent(bytes, id, 0, ELEMENT_TABLE_ELEMENT_LENGTH, NO_LOCK);
			block = byteArrToLong(bytes, ELEMENT_TABLE_OFFSET_BLOCK);
			pos = byteArrToInt(bytes, ELEMENT_TABLE_OFFSET_POS);
		} else {
			throw new IllegalArgumentException("invalid id! id=" + id);
		}
		return new LongInt(block, pos);
	}

	public static long generateID(PatrID parent, long block, int pos) throws IOException {
		return simpleWithLockLong(parent.fs.bm, () -> executeGenerateID(parent, block, pos));
	}

	private static long executeGenerateID(PatrID parent, long block, int pos) throws IOException {
		long len = parent.fs.blockTable.length(), off = 0L;
		byte[] bytes = new byte[Math.max(ELEMENT_TABLE_ELEMENT_LENGTH,
				(int) Math.min(1 << 16 - 1 << 16 % ELEMENT_TABLE_ELEMENT_LENGTH, len))];
		for (int cpy; off < len; off += cpy) {
			cpy = (int) Math.min(len - off, bytes.length);
			parent.fs.blockTable.getContent(bytes, off, 0, cpy, NO_LOCK);
			for (int i = ELEMENT_TABLE_OFFSET_POS; i < bytes.length; i += ELEMENT_TABLE_ELEMENT_LENGTH) {
				if (bytes[i] == -1) {
					i -= ELEMENT_TABLE_OFFSET_POS;
					longToByteArr(bytes, i + ELEMENT_TABLE_OFFSET_BLOCK, block);
					intToByteArr(bytes, i + ELEMENT_TABLE_OFFSET_POS, pos);
					return (long) i;
				}
			}
		}
		longToByteArr(bytes, ELEMENT_TABLE_OFFSET_BLOCK, block);
		intToByteArr(bytes, ELEMENT_TABLE_OFFSET_POS, pos);
		parent.fs.blockTable.appendContent(bytes, 0, ELEMENT_TABLE_ELEMENT_LENGTH, NO_LOCK);
		return len;
	}

	public static void removeID(PatrID id) throws IOException {
		long len = id.fs.blockTable.length();
		if (id.startTime != id.fs.startTime) {
			assert id.startTime < id.fs.startTime;
			throw new IllegalStateException("the id has the wrong startTime!");
		}
		assert len > id.id + ELEMENT_TABLE_ELEMENT_LENGTH;
		byte[] bytes = new byte[ELEMENT_TABLE_ELEMENT_LENGTH];
		Arrays.fill(bytes, (byte) -1);
		id.fs.blockTable.setContent(bytes, id.id, 0, ELEMENT_TABLE_ELEMENT_LENGTH, NO_LOCK);
	}

	@Override
	public long totalSpace() throws IOException {
		return simpleWithLockLong(bm, this::executeTotalSpace);
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
		return simpleWithLockLong(bm, this::executeFreeSpace);
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
		return simpleWithLockLong(bm, this::executeUsedSpace);
	}

	private long executeUsedSpace() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(1L);
		try {
			long used = 0;
			int end = byteArrToInt(bytes, bytes.length - 4);
			for (int off = 0; off < end; off += 16) {
				long startBlock = byteArrToLong(bytes, off), endBlock = byteArrToLong(bytes, off + 8),
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
		return simpleWithLockInt(bm, this::executeBlockSize);
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
		return simpleWithLockLong(bm, this::executeBlockCount);
	}

	private long executeBlockCount() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(0L);
		try {
			return byteArrToLong(bytes, FB_BLOCK_COUNT_OFFSET);
		} finally {
			bm.ungetBlock(0L);
		}
	}

	public static void initBlock(byte[] bytes, int startLen) throws OutOfSpaceException {
		final int blockSize = bytes.length, tableStart = blockSize - 20;
		if (tableStart < startLen) {
			throw new OutOfSpaceException("the block is not big enugh! blockSize=" + blockSize + " startLen=" + startLen
					+ " (note, that the table also needs " + (blockSize - tableStart) + " bytes)");
		}
		intToByteArr(bytes, blockSize - 4, tableStart);
		intToByteArr(bytes, tableStart + 12, blockSize);
		intToByteArr(bytes, tableStart + 8, tableStart);
		intToByteArr(bytes, tableStart + 4, startLen);
		intToByteArr(bytes, tableStart, 0);
	}

}
package de.hechler.patrick.pfs.objects.fs;

import static de.hechler.patrick.pfs.objects.fs.PatrFileSysElementImpl.executeEnsureAccess;
import static de.hechler.patrick.pfs.objects.fs.PatrFileSysElementImpl.executeLock;
import static de.hechler.patrick.pfs.objects.fs.PatrFileSysElementImpl.executeRemoveLock;
import static de.hechler.patrick.pfs.objects.fs.PatrFileSysElementImpl.withLock;
import static de.hechler.patrick.pfs.objects.fs.PatrFileSysElementImpl.withLockInt;
import static de.hechler.patrick.pfs.objects.fs.PatrFileSysElementImpl.withLockLong;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToInt;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToLong;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.intToByteArr;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.longToByteArr;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_TABLE_ELEMENT_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_TABLE_FILE_ID;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_TABLE_OFFSET_BLOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_TABLE_OFFSET_POS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_BLOCK_COUNT_OFFSET;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_BLOCK_LENGTH_OFFSET;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_FILE_SYS_LOCK_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_FILE_SYS_LOCK_VALUE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_FILE_SYS_STATE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_ROOT_BLOCK_OFFSET;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_ROOT_POS_OFFSET;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_START_ROOT_POS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_TABLE_FILE_BLOCK_OFFSET;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_TABLE_FILE_POS_OFFSET;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_DATA_TABLE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_ELEMENT_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_OFFSET_FOLDER_ELEMENTS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_DELETE_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_ID;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ROOT_FOLDER_ID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.exception.ElementReadOnlyException;
import de.hechler.patrick.pfs.exception.OutOfSpaceException;
import de.hechler.patrick.pfs.interfaces.BlockAccessor;
import de.hechler.patrick.pfs.interfaces.BlockManager;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.objects.LongInt;
import de.hechler.patrick.pfs.objects.ba.BlockManagerImpl;

public class PatrFileSysImpl implements PatrFileSystem {
	
	public final Random       rnd;
	public final BlockManager bm;
	private volatile long     startTime;
	private volatile long     lock;
	public final boolean      readOnly;
	private PatrFile          elementTable;
	private PatrFolder        root;
	
	public PatrFileSysImpl(BlockAccessor ba) {
		this(new Random(), new BlockManagerImpl(ba), false);
	}
	
	public PatrFileSysImpl(BlockManager bm) {
		this(new Random(), bm, false);
	}
	
	public PatrFileSysImpl(Random rnd, BlockAccessor ba) {
		this(rnd, new BlockManagerImpl(ba), false);
	}
	
	public PatrFileSysImpl(Random rnd, BlockManager bm) {
		this(rnd, bm, false);
	}
	
	public PatrFileSysImpl(Random rnd, BlockManager bm, boolean readOnly) {
		this.rnd = rnd;
		this.bm = bm;
		this.startTime = bootTime(System.currentTimeMillis());
		this.lock = NO_LOCK;
		this.readOnly = false;
		this.elementTable = new PatrFileImpl(this, startTime, bm, ELEMENT_TABLE_FILE_ID);
		this.root = new PatrFolderImpl(this, startTime, bm, ROOT_FOLDER_ID);
	}
	
	/**
	 * tries to find the boot time of the system.<br>
	 * if the boot time can not be found out, {@code fallback} will be returned.
	 * 
	 * @return the boot time if the system
	 */
	public final static long bootTime(long fallback) {
		long current = System.currentTimeMillis();
		try (Scanner sc = new Scanner(Files.newInputStream(Paths.get("/proc/uptime")), StandardCharsets.UTF_8)) {
			String str = sc.next();
			int i = str.indexOf('.');
			if (i != -1) {
				StringBuilder time = new StringBuilder(str.substring(0, i));
				String end = str.substring(i + 1);
				time.append(end);
				int ii;
				for (ii = end.length(); ii < 3; ii ++ ) {
					time.append('0');
				}
				if (ii > 3) {
					str = null;
				} else {
					str = time.toString();
				}
			}
			if (str != null) {
				long on = Long.parseLong(str);
				return current - on;
			}
		} catch (IOException | /* NumberFormatException | */ RuntimeException e) {}
		try {
			current = System.currentTimeMillis();
			Process process = Runtime.getRuntime().exec("net stats srv");
			BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ( (line = in.readLine()) != null) {
				if (line.startsWith("Statistics since")) {
					SimpleDateFormat format = new SimpleDateFormat("'Statistics since' MM/dd/yyyy hh:mm:ss a");
					Date boottime = format.parse(line);
					return current - boottime.getTime();
				}
			}
		} catch (IOException | ParseException | RuntimeException e) {}
		return fallback;
	}
	
	/**
	 * locks the file system
	 * 
	 * @param lock
	 *            the lock data for the new lock
	 * @return the new lock
	 * @throws ElementLockedException
	 *             if the file system is already locked
	 * @throws IOException
	 *             if an IO error occurs
	 * @see PatrFileSysElement#lock(long)
	 */
	public long lock(long lock) throws ElementLockedException, IOException {
		synchronized (bm) {
			byte[] bytes = bm.getBlock(0L);
			return executeLock(() -> {
				long time = byteArrToLong(bytes, FB_FILE_SYS_LOCK_TIME);
				if (time == NO_TIME || time < startTime) {
					return NO_LOCK;
				}
				return byteArrToLong(bytes, FB_FILE_SYS_LOCK_VALUE);
			}, setLock -> {
				bm.getBlock(0L);
				try {
					longToByteArr(bytes, FB_FILE_SYS_LOCK_VALUE, setLock);
					longToByteArr(bytes, FB_FILE_SYS_LOCK_TIME, System.currentTimeMillis());
				} finally {
					bm.setBlock(0L);
				}
			}, () -> "[FILE-SYSTEM]", rnd, lock);
		}
	}
	
	/**
	 * removes the given lock from the file system
	 * 
	 * @param lock
	 *            the lock to remove
	 * @throws IOException
	 *             if an IO error occurs
	 * @see PatrFileSysElement#removeLock(long)
	 */
	public void removeLock(long lock) throws IOException {
		synchronized (bm) {
			byte[] bytes = bm.getBlock(0L);
			executeRemoveLock(() -> byteArrToLong(bytes, FB_FILE_SYS_LOCK_VALUE), () -> byteArrToLong(bytes, FB_FILE_SYS_LOCK_TIME), (newLock, newTime) -> {
				bm.getBlock(0L);
				try {
					longToByteArr(bytes, FB_FILE_SYS_LOCK_VALUE, newLock);
					longToByteArr(bytes, FB_FILE_SYS_LOCK_TIME, newTime);
				} finally {
					bm.setBlock(0L);
				}
			}, lock);
			bm.ungetBlock(0L);
		}
	}
	
	public boolean isReadOnly() {
		return readOnly;
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
		return withLock(this, () -> executeFromID(id), 0L);
	}
	
	private PatrFileSysElement executeFromID(Object id) throws IllegalAccessError {
		if (id == null) {
			throw new NullPointerException("id is null");
		}
		if ( ! (id instanceof PatrID)) {
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
		synchronized (bm) {
			int key = lockFS();
			ensureAccess(LOCK_NO_DELETE_ALLOWED_LOCK, true);
			try {
				executeFormat(blockCount, blockSize);
			} finally {
				unlockFS(key);
			}
		}
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
			root = new PatrFolderImpl(this, startTime, bm, ROOT_FOLDER_ID);
			elementTable = new PatrFileImpl(this, startTime, bm, ELEMENT_TABLE_FILE_ID);
			assert blockSize == bytes.length;
			initBlock(bytes, FB_START_ROOT_POS + FOLDER_OFFSET_FOLDER_ELEMENTS + FOLDER_ELEMENT_LENGTH);
			longToByteArr(bytes, FB_BLOCK_COUNT_OFFSET, blockCount);
			intToByteArr(bytes, FB_BLOCK_LENGTH_OFFSET, blockSize);
			longToByteArr(bytes, FB_ROOT_BLOCK_OFFSET, 0L);
			longToByteArr(bytes, FB_FILE_SYS_LOCK_VALUE, NO_LOCK);
			intToByteArr(bytes, FB_ROOT_POS_OFFSET, FB_START_ROOT_POS);
			startTime = System.currentTimeMillis();
			PatrFolderImpl.initChild(bm, NO_ID, ROOT_FOLDER_ID, true, FB_START_ROOT_POS, -1, 0L, null, null);
			int blockTablePos = PatrFileSysElementImpl.allocate(bm, 0L, FILE_OFFSET_FILE_DATA_TABLE);
			PatrFolderImpl.initChild(bm, NO_ID, ELEMENT_TABLE_FILE_ID, false, blockTablePos, -1, 0L, null, null);
			longToByteArr(bytes, FB_TABLE_FILE_BLOCK_OFFSET, 0L);
			intToByteArr(bytes, FB_TABLE_FILE_POS_OFFSET, blockTablePos);
		} finally {
			bm.setBlock(0L);
		}
	}
	
	public static void setBlockAndPos(PatrID id, long block, int pos) throws IOException {
		withLock(id.fs, () -> executeSetBlockAndPos(id, block, pos), 0L);
	}
	
	private static void executeSetBlockAndPos(PatrID id, long block, int pos) throws IOException, IllegalArgumentException {
		assert id.fs.startTime == id.startTime;
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
			id.fs.elementTable.setContent(bytes, id.id, 0, ELEMENT_TABLE_ELEMENT_LENGTH, NO_LOCK);
		} else {
			throw new IllegalArgumentException("illegal id; id=" + id.id);
		}
	}
	
	
	public void updateBlockAndPos(PatrFileSysElementImpl element) throws IOException {
		assert element.fs == this;
		withLock(this, () -> {
			assert element.startTime == startTime : "mystart=" + startTime + " elementstart=" + element.startTime + " dif=" + (startTime - element.startTime);
			if (element.id == ELEMENT_TABLE_FILE_ID) {
				byte[] bytes = bm.getBlock(0L);
				try {
					element.block = byteArrToLong(bytes, FB_TABLE_FILE_BLOCK_OFFSET);
					element.pos = byteArrToInt(bytes, FB_TABLE_FILE_POS_OFFSET);
				} finally {
					bm.ungetBlock(0L);
				}
			} else if (element.id == ROOT_FOLDER_ID) {
				byte[] bytes = bm.getBlock(0L);
				try {
					element.block = byteArrToLong(bytes, FB_ROOT_BLOCK_OFFSET);
					element.pos = byteArrToInt(bytes, FB_ROOT_POS_OFFSET);
				} finally {
					bm.ungetBlock(0L);
				}
			} else if (element.id >= 0L) {
				byte[] bytes = new byte[ELEMENT_TABLE_ELEMENT_LENGTH];
				elementTable.getContent(bytes, element.id, 0, ELEMENT_TABLE_ELEMENT_LENGTH, NO_LOCK);
				element.block = byteArrToLong(bytes, ELEMENT_TABLE_OFFSET_BLOCK);
				element.pos = byteArrToInt(bytes, ELEMENT_TABLE_OFFSET_POS);
			} else {
				throw new IllegalArgumentException("invalid id! id=" + element.id);
			}
		}, 0L);
	}
	
	public LongInt getBlockAndPos(long id) throws IllegalArgumentException, IOException {
		return withLock(this, () -> {
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
				elementTable.getContent(bytes, id, 0, ELEMENT_TABLE_ELEMENT_LENGTH, NO_LOCK);
				block = byteArrToLong(bytes, ELEMENT_TABLE_OFFSET_BLOCK);
				pos = byteArrToInt(bytes, ELEMENT_TABLE_OFFSET_POS);
			} else {
				throw new IllegalArgumentException("invalid id! id=" + id);
			}
			return new LongInt(block, pos);
		}, 0L);
	}
	
	public static long generateID(PatrID parent, long block, int pos) throws IOException {
		return withLockLong(parent.fs, () -> executeGenerateID(parent, block, pos), 0L);
	}
	
	private static long executeGenerateID(PatrID parent, long block, int pos) throws IOException {
		long len = parent.fs.elementTable.length(), off = 0L;
		byte[] bytes = new byte[Math.max(ELEMENT_TABLE_ELEMENT_LENGTH,
			(int) Math.min(1 << 16 - 1 << 16 % ELEMENT_TABLE_ELEMENT_LENGTH, len))];
		for (int cpy; off < len; off += cpy) {
			cpy = (int) Math.min(len - off, bytes.length);
			parent.fs.elementTable.getContent(bytes, off, 0, cpy, NO_LOCK);
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
		parent.fs.elementTable.appendContent(bytes, 0, ELEMENT_TABLE_ELEMENT_LENGTH, NO_LOCK);
		return len;
	}
	
	public void remove(PatrID id) throws IOException {
		assert this == id.fs;
		assert startTime == id.startTime;
		long len = elementTable.length();
		assert len > id.id : "invalid element ID: maxID=" + (len - ELEMENT_TABLE_ELEMENT_LENGTH) + " id=" + id.id;
		assert id.id >= 0L : "can not remove negative IDs id=" + id.id;
		assert (id.id % ELEMENT_TABLE_ELEMENT_LENGTH) == 0L : "invalid id (not aligned) id=" + id.id + " miss alignment: " + (id.id % ELEMENT_TABLE_ELEMENT_LENGTH);
		byte[] bytes = new byte[ELEMENT_TABLE_ELEMENT_LENGTH];
		Arrays.fill(bytes, (byte) -1);
		elementTable.setContent(bytes, id.id, 0, ELEMENT_TABLE_ELEMENT_LENGTH, NO_LOCK);
	}
	
	@Override
	public long totalSpace() throws IOException {
		return withLockLong(this, this::executeTotalSpace, 0L);
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
		return withLockLong(this, this::executeFreeSpace, 0L);
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
		return withLockLong(this, this::executeUsedSpace, 0L);
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
		return withLockInt(this, this::executeBlockSize, 0L);
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
		return withLockLong(this, this::executeBlockCount, 0L);
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
	
	public void ensureAccess(long forbiddenBits, boolean readOnlyForbidden) throws ElementReadOnlyException, ElementLockedException, IOException {
		synchronized (bm) {
			executeEnsureAccess(() -> readOnly, () -> {
				byte[] block = bm.getBlock(0L);
				try {
					return byteArrToLong(block, FB_FILE_SYS_LOCK_VALUE);
				} finally {
					bm.ungetBlock(0L);
				}
			}, () -> "[FILE-SYSTEM]", lock, forbiddenBits, readOnlyForbidden);
		}
	}
	
	/**
	 * locks the file system, to complete an operation.<br>
	 * after this call {@link #unlockFS(int)} should be called with the return value of this call as argument.
	 * <p>
	 * if this method gets called twice without {@link #unlockFS(int)} the behavior is undefined
	 * 
	 * @return the argument for {@link #unlockFS(int)}
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             if the file system is locked
	 */
	public int lockFS() throws IOException, ElementLockedException {
		while (true) {
			byte[] bytes = bm.getBlock(0L);
			try {
				int state = byteArrToInt(bytes, FB_FILE_SYS_STATE);
				if (state == 0) {
					state = rnd.nextInt() | 1;
					intToByteArr(bytes, FB_FILE_SYS_STATE, state);
					bm.setBlock(0L);
					try {
						Thread.sleep(100L);
					} catch (InterruptedException e) {} finally {
						bytes = bm.getBlock(0L);
					}
					int newState = byteArrToInt(bytes, FB_FILE_SYS_STATE);
					if (state == newState) {
						return state;
					}
				}
			} finally {
				bm.ungetBlock(0L);
			}
		}
		
	}
	
	public void unlockFS(int key) throws IOException {
		byte[] bytes = bm.getBlock(0L);
		try {
			int state = byteArrToInt(bytes, FB_FILE_SYS_STATE);
			assert state == key;
			bm.getBlock(0L);
			try {// discard block if key does not match
				intToByteArr(bytes, FB_FILE_SYS_STATE, 0);
			} finally {
				bm.setBlock(0L);
			}
		} finally {
			bm.ungetBlock(0L);
		}
	}
	
}

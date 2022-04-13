package de.hechler.patrick.pfs.objects;

import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToInt;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToLong;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.intToByteArr;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.longToByteArr;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_EXECUTABLE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_FOLDER;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_HIDDEN;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_READ_ONLY;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_CREATE_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_FLAGS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LAST_META_MOD_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LAST_MOD_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LOCK_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LOCK_VALUE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_NAME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_OWNER;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_PARENT_BLOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_PARENT_POS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FB_BLOCK_COUNT_OFFSET;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_DATA_TABLE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_ELEMENT_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_ELEMENT_OFFSET_BLOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_ELEMENT_OFFSET_POS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_OFFSET_ELEMENT_COUNT;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_OFFSET_FOLDER_ELEMENTS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_DATA;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_META_CHANGE_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_WRITE_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_SHARED_COUNTER_AND;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_SHARED_COUNTER_MAX_VALUE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_SHARED_COUNTER_SHIFT;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_SHARED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_USER_MAX_VALUE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_USER_SHIFT;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_TIME;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.interfaces.BlockManager;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingBooleanSupplier;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingIntSupplier;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingLongSupplier;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingRunnable;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingSupplier;
import de.hechler.patrick.pfs.objects.java.PFSFileSystemProviderImpl;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;

public class PatrFileSysElementImpl implements PatrFileSysElement {
	
	private static Map <BlockLock, WeakReference <Object>> looks = new HashMap <>();
	
	protected final long         startTime;
	protected final BlockManager bm;
	protected long               block;
	protected int                pos;
	
	public PatrFileSysElementImpl(long startTime, BlockManager bm, long block, int pos) {
		this.startTime = startTime;
		this.bm = bm;
		this.block = block;
		this.pos = pos;
	}
	
	@Override
	public PatrFolderImpl getParent() throws IllegalStateException, IOException {
		return simpleWithLock(() -> {
			byte[] bytes = bm.getBlock(block);
			try {
				long pblock = byteArrToLong(bytes, pos + ELEMENT_OFFSET_PARENT_BLOCK);
				int ppos = byteArrToInt(bytes, pos + ELEMENT_OFFSET_PARENT_POS);
				return new PatrFolderImpl(startTime, bm, pblock, ppos);
			} finally {
				bm.ungetBlock(block);
			}
		});
	}
	
	@Override
	public void setParent(PatrFolder newParent, long lock, long oldParentLock, long newParentLock) throws IllegalStateException, IOException {
		if ( ! (newParent instanceof PatrFileSysImpl)) {
			throw new IllegalArgumentException("I can not set my parent to an folder of an unknown implementation");
		}
		PatrFileSysElementImpl np = (PatrFileSysElementImpl) newParent;
		if (np.bm != bm) {
			throw new IllegalArgumentException("I can not set my parent to an folder of an other fils system");
		}
		simpleWithLock((ThrowingRunnable <IOException>) () -> newParent.simpleWithLock((ThrowingRunnable <IOException>) () -> getParent().simpleWithLock(() -> executeSetParent(np, lock, oldParentLock, newParentLock))));
	}
	
	private void executeSetParent(PatrFileSysElementImpl np, long lock, long oldParentLock, long newParentLock) throws IllegalStateException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			byte[] pbytes = bm.getBlock(np.block);
			try {
				PatrFolderImpl oldParent = getParent();
				bm.getBlock(oldParent.block);
				try {
					oldParent.ensureAccess(oldParentLock, LOCK_NO_WRITE_ALLOWED_LOCK, false);
					np.ensureAccess(oldParentLock, LOCK_NO_WRITE_ALLOWED_LOCK, false);
					ensureAccess(oldParentLock, LOCK_NO_META_CHANGE_ALLOWED_LOCK, false);
					oldParent.modify(false);
				} finally {
					bm.ungetBlock(oldParent.block);
				}
				deleteFromParent();
				final int pelementcount = byteArrToInt(pbytes, np.pos + FOLDER_OFFSET_ELEMENT_COUNT),
					oldLen = FOLDER_OFFSET_FOLDER_ELEMENTS + pelementcount * FOLDER_ELEMENT_LENGTH,
					newLen = oldLen + FOLDER_ELEMENT_LENGTH,
					addPos = np.pos + oldLen;
				reallocate(np.block, np.pos, oldLen, newLen, true);
				longToByteArr(bytes, addPos + FOLDER_ELEMENT_OFFSET_BLOCK, block);
				intToByteArr(bytes, addPos + FOLDER_ELEMENT_OFFSET_POS, pos);
				longToByteArr(bytes, pos + ELEMENT_OFFSET_PARENT_BLOCK, np.block);
				intToByteArr(bytes, pos + ELEMENT_OFFSET_PARENT_POS, np.pos);
				np.modify(false);
				modify(true);
			} finally {
				bm.setBlock(np.block);
			}
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public PatrFolder getFolder() throws IllegalStateException, IOException {
		if ( !isFolder()) {
			throw new IllegalStateException("this is no folder!");
		}
		if (this instanceof PatrFolder) {
			return (PatrFolder) this;
		}
		return new PatrFolderImpl(startTime, bm, block, pos);
	}
	
	@Override
	public PatrFile getFile() throws IllegalStateException, IOException {
		if ( !isFile()) {
			throw new IllegalStateException("this is no file!");
		}
		if (this instanceof PatrFile) {
			return (PatrFile) this;
		}
		return new PatrFileImpl(startTime, bm, block, pos);
	}
	
	@Override
	public boolean isFolder() throws IOException {
		return (getFlags() & ELEMENT_FLAG_FOLDER) != 0;
	}
	
	@Override
	public boolean isFile() throws IOException {
		return (getFlags() & ELEMENT_FLAG_FOLDER) == 0;
	}
	
	@Override
	public boolean isExecutable() throws IOException {
		return (getFlags() & ELEMENT_FLAG_EXECUTABLE) != 0;
	}
	
	@Override
	public boolean isHidden() throws IOException {
		return (getFlags() & ELEMENT_FLAG_HIDDEN) != 0;
	}
	
	@Override
	public boolean isReadOnly() throws IOException {
		return (getFlags() & ELEMENT_FLAG_READ_ONLY) != 0;
	}
	
	@Override
	public void setExecutable(boolean isExecutale, long lock) throws IOException, ElementLockedException {
		simpleWithLock(() -> executeSetExecutable(isExecutale, lock));
	}
	
	private void executeSetExecutable(boolean isExecutale, long lock) throws IOException, ElementLockedException {
		bm.getBlock(block);
		try {
			ensureAccess(lock, LOCK_NO_META_CHANGE_ALLOWED_LOCK, false);
			flag(ELEMENT_FLAG_EXECUTABLE, isExecutale);
			modify(true);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public void setHidden(boolean isHidden, long lock) throws IOException, ElementLockedException {
		simpleWithLock(() -> executeSetHidden(isHidden, lock));
	}
	
	private void executeSetHidden(boolean isHidden, long lock) throws IOException, ElementLockedException {
		bm.getBlock(block);
		try {
			ensureAccess(lock, LOCK_NO_META_CHANGE_ALLOWED_LOCK, false);
			flag(ELEMENT_FLAG_HIDDEN, isHidden);
			modify(true);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public void setReadOnly(boolean isReadOnly, long lock) throws IOException, ElementLockedException {
		simpleWithLock(() -> executeSetReadOnly(isReadOnly, lock));
	}
	
	private void executeSetReadOnly(boolean isReadOnly, long lock) throws IOException, ElementLockedException {
		bm.getBlock(block);
		try {
			ensureAccess(lock, LOCK_NO_META_CHANGE_ALLOWED_LOCK, false);
			flag(ELEMENT_FLAG_READ_ONLY, isReadOnly);
			modify(true);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	public int getFlags() throws IOException {
		return simpleWithLockInt(this::executeGetFlags);
	}
	
	private int executeGetFlags() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			return byteArrToInt(bytes, pos + ELEMENT_OFFSET_FLAGS);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	/**
	 * sets or removes the given flag
	 * 
	 * @param flag
	 *            the flag to modify
	 * @param isSet
	 *            if the flag should be set or cleared
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected void flag(int flag, boolean isSet) throws IOException {
		int flags = getFlags();
		if (isSet) {
			flags |= flag;
		} else {
			flags &= ~flag;
		}
		setFlags(flags);
	}
	
	/**
	 * sets the flags of this element
	 * <p>
	 * this method should only be used with great care, since the flags also decide, which element is a file and which is a folder!<br>
	 * the preferred method to modify to modify the flags is {@link #flag(int, boolean)}.
	 * 
	 * @param flags
	 *            the new flags of this element
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected void setFlags(int flags) throws IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			intToByteArr(bytes, pos + ELEMENT_OFFSET_FLAGS, flags);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public int getOwner() throws IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			return byteArrToInt(bytes, pos + ELEMENT_OFFSET_OWNER);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public void setOwner(int owner, long lock) throws IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			ensureAccess(lock, LOCK_NO_META_CHANGE_ALLOWED_LOCK, false);
			intToByteArr(bytes, pos + ELEMENT_OFFSET_OWNER, owner);
			modify(true);
		} finally {
			bm.setBlock(block);
		}
	}
	
	@Override
	public long getCreateTime() throws IOException {
		return simpleWithLockLong(this::executeGetCreateTime);
	}
	
	private long executeGetCreateTime() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			return byteArrToLong(bytes, pos + ELEMENT_OFFSET_CREATE_TIME);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public long getLastModTime() throws IOException {
		return simpleWithLockLong(this::executeGetLastModTime);
	}
	
	private long executeGetLastModTime() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			return byteArrToLong(bytes, pos + ELEMENT_OFFSET_LAST_MOD_TIME);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public long getLastMetaModTime() throws IOException {
		return simpleWithLockLong(this::executeGetLastMetaModTime);
	}
	
	private long executeGetLastMetaModTime() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			return byteArrToLong(bytes, pos + ELEMENT_OFFSET_LAST_META_MOD_TIME);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public long getLockData() throws IOException {
		return simpleWithLockLong(this::executeGetLockData);
	}
	
	private long executeGetLockData() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			getLockTime();
			long lock = byteArrToLong(bytes, pos + ELEMENT_OFFSET_LOCK_VALUE);
			return lock & LOCK_DATA;
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public long getLockTime() throws IOException, IllegalStateException {
		return simpleWithLockLong(this::executeGetLockTime);
	}
	
	private long executeGetLockTime() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			long lockTime = byteArrToLong(bytes, pos + ELEMENT_OFFSET_LOCK_TIME);
			if (lockTime < startTime) {
				removeLock(LOCK_NO_LOCK);
				return NO_TIME;
			}
			return lockTime;
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public void removeLock(long lock) throws IOException, IllegalStateException {
		simpleWithLock(() -> executeRemoveLock(lock));
	}
	
	private void executeRemoveLock(long lock) throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			long newLock = LOCK_NO_LOCK, newTime = NO_TIME;
			if (lock != LOCK_NO_LOCK) {
				long currentLock = getLock();
				if ( (currentLock & ~LOCK_SHARED_COUNTER_AND) != lock) {
					throw new IllegalStateException(
						"the locks are diffrent! I am locked with: " + Long.toHexString(currentLock & (LOCK_DATA | (LOCK_USER_MAX_VALUE << LOCK_USER_SHIFT))) + " (data and user only) but the lock: "
							+ Long.toHexString(lock) + " should be removed");
				}
				if ( (currentLock & LOCK_SHARED_LOCK) != 0) {
					long cnt = (currentLock & LOCK_SHARED_COUNTER_AND) >>> LOCK_SHARED_COUNTER_SHIFT;
					cnt -- ;
					if (cnt <= 0L) {
						newLock = LOCK_NO_LOCK;
						newTime = NO_TIME;
					} else {
						newLock = cnt << LOCK_SHARED_COUNTER_SHIFT;
						newLock |= currentLock & ~LOCK_SHARED_COUNTER_AND;
					}
				}
			}
			longToByteArr(bytes, pos + ELEMENT_OFFSET_LOCK_VALUE, newLock);
			longToByteArr(bytes, pos + ELEMENT_OFFSET_LOCK_TIME, newTime);
			modify(true);
		} finally {
			bm.setBlock(block);
		}
	}
	
	/**
	 * returns the current lock of this element or {@link PatrFileSysConstants#LOCK_NO_LOCK} if no lock is set
	 * 
	 * @return the current lock of this element or {@link PatrFileSysConstants#LOCK_NO_LOCK} if no lock is set
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected long getLock() throws IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			getLockTime();
			return byteArrToLong(bytes, pos + ELEMENT_OFFSET_LOCK_VALUE);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public long lock(long newLock) throws IOException, IllegalArgumentException, ElementLockedException {
		if (newLock == LOCK_NO_LOCK) {
			throw new IllegalArgumentException("can't lock with the LOCK_NO_LOCK (val=" + Long.toHexString(newLock) + ")");
		}
		return simpleWithLockLong(() -> executeLock(newLock));
	}
	
	private long executeLock(long newLock) throws ClosedChannelException, IOException, ElementLockedException {
		byte[] bytes = bm.getBlock(block);
		try {
			long myLock = getLock();
			if (myLock != LOCK_NO_LOCK) {
				if ( (myLock & LOCK_SHARED_LOCK) != 0) {
					throw new ElementLockedException(PFSFileSystemProviderImpl.buildName(this), "this element is locked with a non shared lock!");
				} else if ( (newLock & LOCK_SHARED_LOCK) != 0) {
					throw new ElementLockedException(PFSFileSystemProviderImpl.buildName(this), "this element is locked with a shared lock!");
				} else if ( (myLock & LOCK_DATA) != newLock) {
					throw new ElementLockedException(PFSFileSystemProviderImpl.buildName(this), "this element is locked with a shared lock which has diffrent data flags!");
				}
				long cnt = (myLock & LOCK_SHARED_COUNTER_AND) >>> LOCK_SHARED_COUNTER_SHIFT;
				cnt ++ ;
				if (cnt > LOCK_SHARED_COUNTER_MAX_VALUE) {
					throw new ElementLockedException(PFSFileSystemProviderImpl.buildName(this), "this element is locked with a shared lock which has already been shared too often");
				}
				newLock = myLock & ~LOCK_SHARED_COUNTER_AND;
				myLock = newLock | (cnt << LOCK_SHARED_COUNTER_SHIFT);
			}
			long time = System.currentTimeMillis();
			longToByteArr(bytes, pos + ELEMENT_OFFSET_LOCK_VALUE, myLock);
			longToByteArr(bytes, pos + ELEMENT_OFFSET_LOCK_TIME, time);
			modify(true);
			return newLock;
		} finally {
			bm.setBlock(block);
		}
	}
	
	@Override
	public void ensureAccess(long lock, long forbiddenBits, boolean readOnlyForbidden) throws IOException, ElementLockedException, IllegalArgumentException {
		long check = forbiddenBits & LOCK_DATA;
		if (check != forbiddenBits) {
			throw new IllegalArgumentException("the forbidden bits are not allowed to contain non data bits!");
		}
		withLock(() -> executeEnsureAccess(lock, forbiddenBits, readOnlyForbidden));
	}
	
	public void executeEnsureAccess(long lock, long forbiddenBits, boolean readOnlyForbidden) throws IOException, ElementLockedException, IllegalArgumentException {
		if (readOnlyForbidden) {
			if (isReadOnly()) {
				throw new ElementLockedException(PFSFileSystemProviderImpl.buildName(this), "this element is read only, but non read access was requested!");
			}
		}
		long myLock = getLock();
		if (lock != LOCK_NO_LOCK) {
			if (myLock != lock) {
				throw new ElementLockedException(PFSFileSystemProviderImpl.buildName(this), "the lock is not LOCK_NO_LOCK, but also not my lock!");
			} else if ( (myLock & LOCK_SHARED_LOCK) != 0) {
				if ( (myLock & forbiddenBits) != 0) {
					throw new ElementLockedException(PFSFileSystemProviderImpl.buildName(this), "the lock is a shared lock and I have at least one of the forbidden bits set in my lock!");
				}
			}
		} else {
			if ( (myLock & forbiddenBits) != 0) {
				throw new ElementLockedException(PFSFileSystemProviderImpl.buildName(this), "the lock is LOCK_NO_LOCK, but I have at least one of the forbidden bits set in my lock!");
			}
		}
	}
	
	protected int getNameByteCount() throws IOException, IllegalStateException {
		byte[] bytes = bm.getBlock(block);
		try {
			int np = byteArrToInt(bytes, pos + ELEMENT_OFFSET_NAME);
			int len;
			for (len = 0; bytes[np + len] != 0 || bytes[np + len + 1] != 0; len += 2) {}
			return len + 2;
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public void setName(String name, long lock) throws IOException, NullPointerException, IllegalStateException, ElementLockedException {
		Objects.requireNonNull(name, "element names can not be null!");
		simpleWithLock(() -> executeSetName(name, lock));
	}
	
	private void executeSetName(String name, long lock) throws ClosedChannelException, IOException, ElementLockedException, OutOfMemoryError {
		byte[] bytes = bm.getBlock(block);
		try {
			ensureAccess(lock, LOCK_NO_META_CHANGE_ALLOWED_LOCK, false);
			int oldlen = getNameByteCount();
			int np = byteArrToInt(bytes, pos + ELEMENT_OFFSET_NAME);
			byte[] namebytes = name.getBytes(StandardCharsets.UTF_16);
			np = reallocate(block, np, oldlen, namebytes.length, false);
			System.arraycopy(namebytes, 0, bytes, np, namebytes.length);
			modify(true);
		} finally {
			bm.setBlock(block);
		}
	}
	
	@Override
	public String getName() throws IOException {
		return simpleWithLock(() -> executeGetName());
	}
	
	private String executeGetName() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			int len = getNameByteCount();
			int np = byteArrToInt(bytes, pos + ELEMENT_OFFSET_NAME);
			return new String(bytes, np, len - 2, StandardCharsets.UTF_16);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	/**
	 * frees the name from the block intern memory table
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected void freeName() throws IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			int nameLen = getNameByteCount();
			int namePos = byteArrToInt(bytes, pos + ELEMENT_OFFSET_NAME);
			reallocate(block, namePos, nameLen, 0, false);
		} finally {
			bm.setBlock(block);
		}
	}
	
	/**
	 * sets the last modified time of this element.
	 * <p>
	 * this method will not be called from non public methods declared in this class, even if they modify this element
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected void modify(boolean onlyMetadata) throws IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			long time = System.currentTimeMillis();
			if ( !onlyMetadata) {
				longToByteArr(bytes, pos + ELEMENT_OFFSET_LAST_MOD_TIME, time);
			}
			longToByteArr(bytes, pos + ELEMENT_OFFSET_LAST_META_MOD_TIME, time);
		} finally {
			bm.setBlock(block);
		}
	}
	
	/**
	 * removes this element from the parent element
	 * <p>
	 * this method should only be called when the entire element gets deleted
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected void deleteFromParent() throws IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			long pblock = byteArrToLong(bytes, pos + ELEMENT_OFFSET_PARENT_BLOCK);
			int ppos = byteArrToInt(bytes, pos + ELEMENT_OFFSET_PARENT_POS);
			bytes = bm.getBlock(pblock);
			try {
				int index = indexInParentList(pblock, ppos),
					imel = index * FOLDER_ELEMENT_LENGTH,
					off = ppos + FOLDER_OFFSET_FOLDER_ELEMENTS + imel,
					oldElementCount = byteArrToInt(bytes, ppos + FOLDER_OFFSET_ELEMENT_COUNT),
					oldElementSize = oldElementCount * FOLDER_ELEMENT_LENGTH,
					copySize = oldElementSize - imel - FOLDER_ELEMENT_LENGTH;
				System.arraycopy(bytes, off + FOLDER_ELEMENT_LENGTH, bytes, off, copySize);
				intToByteArr(bytes, ppos + FOLDER_OFFSET_ELEMENT_COUNT, oldElementCount - 1);
			} finally {
				bm.setBlock(pblock);
			}
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	/**
	 * sets the new position of this element on all related position, like the parent or if this element is a folder it's children<br>
	 * called when this element changes it's position<br>
	 * when this method is called, no changes has been made.
	 * <p>
	 * this method should only be called indirectly using {@link #relocate()}
	 * 
	 * @param oldBlock
	 *            the old block
	 * @param oldPos
	 *            the old position in the old block
	 * @param newBlock
	 *            the new block
	 * @param newBlockNum
	 *            the new position in the new block
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected void setNewPosToOthers(long oldBlock, int oldPos, long newBlock, int newPos) throws IOException {
		byte[] bytes = bm.getBlock(oldBlock);
		try {
			long pblock = byteArrToLong(bytes, oldPos + ELEMENT_OFFSET_PARENT_BLOCK);
			int ppos = byteArrToInt(bytes, oldPos + ELEMENT_OFFSET_PARENT_POS);
			bytes = bm.getBlock(pblock);
			try {
				int index = indexInParentList(pblock, ppos);
				int offset = ppos + FOLDER_OFFSET_FOLDER_ELEMENTS + index * 8;
				longToByteArr(bytes, offset + FOLDER_ELEMENT_OFFSET_BLOCK, newBlock);
				intToByteArr(bytes, offset + FOLDER_ELEMENT_OFFSET_POS, newPos);
			} finally {
				bm.ungetBlock(pblock);
			}
		} finally {
			bm.ungetBlock(oldBlock);
		}
	}
	
	/**
	 * helper method, which should not directly be called
	 * 
	 * @param pblock
	 *            the block from the parent
	 * @param ppos
	 *            the position of the parent
	 * @return the index in the parents child list
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected int indexInParentList(long pblock, int ppos) throws IOException {
		byte[] bytes = bm.getBlock(pblock);
		try {
			final int len = byteArrToInt(bytes, ppos + FOLDER_OFFSET_ELEMENT_COUNT),
				off = ppos + FOLDER_OFFSET_FOLDER_ELEMENTS;
			for (int i = 0; i < len; i ++ ) {
				final int im8 = i * FOLDER_ELEMENT_LENGTH;
				long cblock = byteArrToLong(bytes, off + FOLDER_ELEMENT_OFFSET_BLOCK + im8);
				if (cblock != block) {
					continue;
				}
				int cpos = byteArrToInt(bytes, off + FOLDER_ELEMENT_OFFSET_POS + im8);
				if (cpos != pos) {
					continue;
				}
				return i;
			}
			throw new InternalError("could not find me in my parent");
		} finally {
			bm.ungetBlock(pblock);
		}
	}
	
	/**
	 * relocates this element to a new allocated block.
	 * 
	 * @param oldBolck
	 *            the bytes of the old block. they should be saved after this operation
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected void relocate() throws IOException {
		final long oldBlockNum = block;
		final int oldPos = pos;
		long newBlockNum;
		newBlockNum = allocateOneBlock();
		byte[] oldBlock = bm.getBlock(oldBlockNum);
		try {
			int mylen,
				oldnamepos, namelen;
			if (isFolder()) {
				mylen = byteArrToInt(oldBlock, oldPos + FOLDER_OFFSET_ELEMENT_COUNT);
				mylen *= FOLDER_ELEMENT_LENGTH;
				mylen += FOLDER_OFFSET_FOLDER_ELEMENTS;
			} else {
				long blocks = byteArrToLong(oldBlock, oldPos + FILE_OFFSET_FILE_LENGTH);
				for (mylen = FILE_OFFSET_FILE_DATA_TABLE; blocks > 0; mylen += 16) {
					long start = byteArrToLong(oldBlock, oldPos + mylen);
					long end = byteArrToLong(oldBlock, oldPos + mylen + 8);
					blocks -= end - start;
				}
				assert blocks == 0L;
			}
			oldnamepos = byteArrToInt(oldBlock, oldPos + ELEMENT_OFFSET_NAME);
			namelen = getNameByteCount();
			byte[] newBlock = bm.getBlock(newBlockNum);
			try {
				final int myNewPos = namelen, nameNewPos = 0;
				setNewPosToOthers(oldBlockNum, oldPos, newBlockNum, myNewPos);
				PatrFileSysImpl.initBlock(newBlock, mylen + namelen);
				System.arraycopy(oldBlock, oldPos, newBlock, myNewPos, mylen);
				if (namelen > 0) {
					System.arraycopy(oldBlock, oldnamepos, newBlock, nameNewPos, namelen);
					intToByteArr(newBlock, namelen + ELEMENT_OFFSET_NAME, 0);
				}
				pos = myNewPos;
				block = newBlockNum;
			} finally {
				bm.setBlock(newBlockNum);
			}
		} finally {
			bm.setBlock(oldBlockNum);
		}
	}
	
	/**
	 * allocates one block.<br>
	 * this is effectively the same as <code>allocate(1L)[0].startBlock</code>
	 * 
	 * @return the allocated block
	 * @throws OutOfMemoryError
	 * @throws IOException
	 */
	protected long allocateOneBlock() throws OutOfMemoryError, IOException {
		long newBlockNum;
		AllocatedBlocks[] allocate = allocate(1L);
		assert allocate.length == 1;
		assert allocate[0].count == 1;
		newBlockNum = allocate[0].startBlock;
		return newBlockNum;
	}
	
	/**
	 * allocates the given number of blocks
	 * 
	 * @param len
	 *            the number of blocks to be allocated
	 * @return the array containing the allocated blocks
	 * @throws OutOfMemoryError
	 *             if there is not enough memory
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected AllocatedBlocks[] allocate(long len) throws OutOfMemoryError, IOException {
		long blockCount;
		byte[] bytes = bm.getBlock(0L);
		try {
			blockCount = byteArrToLong(bytes, FB_BLOCK_COUNT_OFFSET);
		} finally {
			bm.ungetBlock(0L);
		}
		bytes = bm.getBlock(1L);
		try {
			List <AllocatedBlocks> result = new ArrayList <>();
			int end = byteArrToInt(bytes, bytes.length - 4);
			final int oldEnd = end;
			while (end >= 24) {
				if (len <= 0) {
					break;
				}
				long endBlock = byteArrToLong(bytes, 8);
				long startBlock = byteArrToLong(bytes, 16);
				long free = endBlock - startBlock;
				long allocate;
				if (free <= len) {
					end -= 16;
					System.arraycopy(bytes, 24, bytes, 8, end);
					allocate = free;
				} else {
					long newEndBlock = endBlock + len;
					longToByteArr(bytes, 8, newEndBlock);
					allocate = len;
				}
				len -= allocate;
				result.add(new AllocatedBlocks(endBlock, allocate));
			}
			if (len > 0) {
				long endBlock = byteArrToLong(bytes, end - 8);
				endBlock += len;
				len -= len;
				if (endBlock > blockCount) {
					for (AllocatedBlocks ab : result) {
						free(ab);
					}
					throw new OutOfMemoryError("not enugh blocks (blockCount=" + Long.toHexString(blockCount) + " endBlock=" + Long.toHexString(endBlock) + ") (I freed the new allocated blocks)");
				}
				longToByteArr(bytes, end - 8, endBlock);
			}
			if (oldEnd != end) {
				intToByteArr(bytes, bytes.length - 4, end);
			}
			assert len == 0;
			return result.toArray(new AllocatedBlocks[result.size()]);
		} finally {
			bm.setBlock(1L);
		}
	}
	
	/**
	 * removes the given blocks from the block allocation table
	 * 
	 * @param remove
	 *            the blocks to remove
	 * @throws OutOfMemoryError
	 *             if there is not enough memory
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected void free(AllocatedBlocks remove) throws OutOfMemoryError, IOException {
		byte[] bytes = bm.getBlock(1L);
		try {
			int end = byteArrToInt(bytes, bytes.length - 4),
				min = 0, max = end - 16, mid;
			final int oldend = end;
			long startBlock, count;
			while (true) {
				mid = (min + max) >>> 2;
				mid &= 0xFFFFFFF0;
				startBlock = byteArrToLong(bytes, mid);
				count = byteArrToLong(bytes, mid + 8) - startBlock;
				if (remove.hasOverlapp(startBlock, count)) {
					break;
				}
				if (remove.startBlock > startBlock) {
					min = mid + 16;
				} else {
					max = mid - 16;
				}
				if (min > max) {
					throw new AssertionError("did not found the allocated blocks to remove!");
				}
			}
			AllocatedBlocks[] stay = remove.remove(startBlock, count);
			switch (stay.length) {
			case 0:
				System.arraycopy(bytes, mid + 16, bytes, mid, end - mid - 16);
				end -= 16;
				break;
			case 1:
				break;
			case 2:
				if (end + 12 > bytes.length) {
					throw new OutOfMemoryError("there is not enough memory in this block to free/remove the given block sequence!");
				}
				System.arraycopy(bytes, mid + 16, bytes, mid + 32, end - mid - 16);
				end += 16;
				break;
			default:
				throw new InternalError("the removed array is too long!");
			}
			for (int i = 0; i < stay.length; i ++ , mid += 16) {
				longToByteArr(bytes, mid, stay[i].startBlock);
				longToByteArr(bytes, mid + 8, stay[i].startBlock + stay[i].count);
			}
			if (oldend != end) {
				intToByteArr(bytes, bytes.length - 4, end);
			}
			if (startBlock > remove.startBlock || startBlock + count < remove.startBlock + remove.count) {
				throw new AssertionError("not all blocks to remove were allocated!");
			}
		} finally {
			bm.setBlock(1L);
		}
	}
	
	/**
	 * allocate a sequence of bytes in this block
	 * 
	 * @param block
	 *            the given block
	 * @param len
	 *            the number of bytes in the new block
	 * @return the pos of the new allocated bytes
	 * @throws OutOfMemoryError
	 *             if there is not enough memory
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected int allocate(long block, int len) throws OutOfMemoryError, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			int tablestart = byteArrToInt(bytes, bytes.length - 4),
				entrycount = (bytes.length - 4 - tablestart) >>> 3;
			if (byteArrToInt(bytes, tablestart) >= len) {
				if (byteArrToInt(bytes, tablestart) == len) {
					intToByteArr(bytes, tablestart, 0);
				} else {
					tableGrow(block, 0, 0, len);
				}
				return 0;
			}
			for (int i = 0; i < entrycount - 1; i ++ ) {
				int off = tablestart + i * 8,
					thisend = byteArrToInt(bytes, off + 4),
					nextstart = byteArrToInt(bytes, off + 8),
					free = nextstart - thisend;
				if (free >= len) {
					int space = (free - len) >>> 1,
						mystart = thisend + space,
						myend = mystart + len;
					if (free == len) {
						int newstart = byteArrToInt(bytes, off);
						boolean blockAlive = tableShrink(block, i, false);
						assert blockAlive;
						tablestart = byteArrToInt(bytes, bytes.length - 4);
						intToByteArr(bytes, off + 8, newstart);
					} else if (space == 0) {
						intToByteArr(bytes, off + 4, myend);
					} else {
						tableGrow(block, i + 1, mystart, myend);
					}
					return mystart;
				}
			}
			throw new OutOfMemoryError("could not find enough memory! (len=" + len + ")");
		} finally {
			bm.setBlock(block);
		}
	}
	
	/**
	 * reallocates the given bytes to the new len (or frees them if the new len is zero).<br>
	 * returns the new position of the bytes (or {@code -1} if they have been freed and {@code -2} if the block has been freed from the block allocation table)<br>
	 * if {@code copy} is <code>true</code> and the new position of the bytes is not the old position this operation will copy the bytes from the old to the new position.
	 * 
	 * @param block
	 *            the given block
	 * @param pos
	 *            the old position of the allocated bytes
	 * @param oldLen
	 *            the old number of bytes
	 * @param newLen
	 *            the new number of bytes
	 * @param copy
	 *            <code>true</code> if the bytes should be copied to the new position if the relocation was not in place
	 * @return the new pos of the relocated bytes or {@code -1} if the bytes have been freed and {@code -2} if the block has been removed from the block allocation table
	 * @throws OutOfMemoryError
	 *             if there is not enough memory
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected int reallocate(long block, int pos, int oldLen, int newLen, boolean copy) throws OutOfMemoryError, IOException {
		byte[] bytes = bm.getBlock(block);
		if (newLen < 0 || oldLen < 0) {
			throw new IllegalArgumentException("len is smaller than zero! (newLen=" + newLen + ", odLen=" + oldLen + ")");
		}
		try {
			int tablestart = byteArrToInt(bytes, bytes.length - 4),
				entrycount = (bytes.length - 4 - tablestart) >>> 3,
				min = 0,
				max = entrycount - 1,
				mid, midoff;
			while (true) {
				mid = (min + max) >>> 1;
				midoff = tablestart + (mid * 8);
				int start = byteArrToInt(bytes, midoff),
					end = byteArrToInt(bytes, midoff + 4);
				if (pos < start) {
					min = mid + 1;
				} else if (end < pos) {
					max = mid - 1;
				} else if (oldLen > newLen) {
					int ret = remove(block, pos + newLen, oldLen - newLen, mid, true);
					if (ret == -2) {
						return -2;
					} else if (newLen == 0) {
						return -1;
					} else {
						return pos;
					}
				} else {
					boolean needNewPlace = false;
					if (mid < entrycount - 1 && end == pos + oldLen) {
						int nextStart = byteArrToInt(bytes, midoff + 8),
							free = end - nextStart;
						if (free < newLen - oldLen) {
							needNewPlace = true;
						}
					} else {
						needNewPlace = true;
					}
					if (needNewPlace) {
						int newPos;
						if (remove(block, pos, oldLen, mid, false) == -2) {
							PatrFileSysImpl.initBlock(bytes, newLen);
							newPos = 0;
						} else {
							newPos = allocate(block, newLen);
						}
						if (copy) {
							System.arraycopy(bytes, pos, bytes, newPos, oldLen);
						}
						return newPos;
					} else {
						intToByteArr(bytes, midoff, pos + newLen);
						return pos;
					}
				}
				if (min > max) {
					throw new AssertionError("did not found the allocated blocks to remove!");
				}
			}
		} finally {
			bm.setBlock(block);
		}
	}
	
	/**
	 * removes a part of an entry or an complete entry from the block intern table.<br>
	 * this method will return {@code 0} if the entry has been partly been removed, {@code -1} if the entry has been completely removed.<br>
	 * {@code -2} will be returned when the entry would have been removed, but it was the last effective entry and so the block has been freed or nothing has been done (depends on the value of
	 * {@code allowFreeBlock}.
	 * <p>
	 * this method should only be called indirectly by using the methods {@link #allocate(long, int)} and {@link #reallocate(long, int, int, int, boolean)}
	 * 
	 * @param block
	 *            the block on which should be operated
	 * @param remFrom
	 *            the start of the part to remove
	 * @param remTo
	 *            the end of the part to remove
	 * @param index
	 *            the index of the entry which should be partly/completely removed
	 * @param allowFreeBlock
	 *            if the block should be freed, when the last effective entry gets removed
	 * @return {@code 0} if the entry has been partly removed, {@code -1} if the entry has been completely removed and {@code -2} if the block has/would have been freed
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws OutOfMemoryError
	 *             if the entry gets separated and there is not enough memory for the table to grow
	 */
	private int remove(long block, int remFrom, int remTo, int index, boolean allowFreeBlock) throws IOException, OutOfMemoryError {
		byte[] bytes = bm.getBlock(block);
		try {
			int tablestart = byteArrToInt(bytes, bytes.length - 4),
				offset = tablestart + index * 8,
				start = byteArrToInt(bytes, offset),
				end = byteArrToInt(bytes, offset + 4);
			AllocatedBlocks[] stay = new AllocatedBlocks(start, end - start).remove(remFrom, remTo);
			switch (stay.length) {
			case 0:
				boolean blockAlive = tableShrink(block, index, allowFreeBlock);
				if (blockAlive) {
					return -1;
				} else {
					return -2;
				}
			case 1:
				start = (int) stay[0].startBlock;
				int count = (int) stay[0].count;
				assert start == stay[0].startBlock;
				assert count == stay[0].count;
				end = start + count;
				intToByteArr(bytes, offset, start);
				intToByteArr(bytes, offset, end);
				break;
			case 2:
				int start0 = (int) stay[0].startBlock,
					count0 = (int) stay[0].count,
					end0 = start0 + count0,
					start1 = (int) stay[1].startBlock,
					count1 = (int) stay[1].count,
					end1 = start1 + count1;
				assert start0 == stay[0].startBlock;
				assert start1 == stay[1].startBlock;
				assert count0 == stay[0].count;
				assert count1 == stay[1].count;
				assert start == start0;
				assert end == end1;
				intToByteArr(bytes, offset, start1);
				tableGrow(block, index, start0, end0);
				break;
			default:
				throw new InternalError("illegal length of returned array from AllocatedBlocks.remove(...)");
			}
			return 0;
		} finally {
			bm.setBlock(block);
		}
	}
	
	/**
	 * helper method to remove an entry from the memory table.
	 * <p>
	 * this method will automatically free the complete block from the block allocation table, when {@code allowFreeBlock} is <code>true</code> and the effective last entry gets removed.<br>
	 * If that happens <code>false</code> will be returned and the table will not be modified, since it is in a dead block.<br>
	 * if {@code allowFreeBloc} is <code>false</code> and the effective last entry should be removed nothing is done and <code>false</code> is returned.<br>
	 * In all other cases the return value will be <code>true</code>.
	 * <p>
	 * this method should only be called indirectly by using the methods {@link #allocate(long, int)} and {@link #reallocate(long, int, int, int, boolean)}
	 * 
	 * @param block
	 *            the block on which should be worked
	 * @param remindex
	 *            the index of the entry to remove
	 * @param allowFreeBlock
	 *            if <code>true</code> the block will be removed from the block allocation table, when the effective last entry is removed from the intern table.
	 * @throws IOException
	 *             if an IO error occurs
	 * @return <code>false</code> if the block was removed from the block allocation table and <code>true</code> if not.
	 */
	protected boolean tableShrink(long block, int remindex, boolean allowFreeBlock) throws IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			int tablestart = byteArrToInt(bytes, bytes.length - 4);
			int lastStart = byteArrToInt(bytes, bytes.length - 12);
			if (tablestart != lastStart) {
				System.arraycopy(bytes, remindex + 8, bytes, remindex, bytes.length - 20 - remindex);
				intToByteArr(bytes, bytes.length - 12, tablestart);// the only case to produce to entries where end=start
				intToByteArr(bytes, bytes.length - 16, tablestart);
			} else if (tablestart + 8 >= bytes.length - 12) {
				assert tablestart + 8 >= bytes.length - 12;
				if (allowFreeBlock) {
					free(new AllocatedBlocks(block, 1L));
				}
				return false;
			} else {
				System.arraycopy(bytes, tablestart, bytes, tablestart + 8, remindex - tablestart - 8);
				intToByteArr(bytes, bytes.length - 12, tablestart + 8);
				intToByteArr(bytes, bytes.length - 4, tablestart + 8);
			}
			return true;
		} finally {
			bm.setBlock(block);
		}
	}
	
	/**
	 * helper method to add an entry to the memory table.
	 * <p>
	 * this method should only be called indirectly by using the methods {@link #allocate(long, int)} and {@link #reallocate(long, int, int, int, boolean)}
	 * 
	 * @param block
	 *            the block on which should be worked
	 * @param addindex
	 *            the index of the higher element of the element to be inserted
	 * @param start
	 *            the start pointer of the element
	 * @param end
	 *            the end pointer of the element
	 * @throws OutOfMemoryError
	 *             if there is not enough memory for the table to grow
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected void tableGrow(long block, int addindex, int start, int end) throws OutOfMemoryError, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			int tablestart = byteArrToInt(bytes, bytes.length - 4),
				lastStart = byteArrToInt(bytes, bytes.length - 12),
				addm8 = addindex * 8,
				addoff = tablestart + addm8;
			if (tablestart - 8 > lastStart) {
				System.arraycopy(bytes, tablestart, bytes, tablestart - 8, addm8);
				intToByteArr(bytes, bytes.length - 4, tablestart - 8);
				intToByteArr(bytes, bytes.length - 12, tablestart - 8);
				intToByteArr(bytes, addoff - 8, start);
				intToByteArr(bytes, addoff - 8, end);
				assert byteArrToInt(bytes, addm8 - 4) < start;
				assert end < byteArrToInt(bytes, addm8 + 8);
			} else if (tablestart - 8 == lastStart) {
				System.arraycopy(bytes, addoff, bytes, addoff + 8, bytes.length - addoff - 12);
				intToByteArr(bytes, addoff, start);
				intToByteArr(bytes, addoff, end);
			} else {
				throw new OutOfMemoryError("there is not enugh memory for the table to grow");
			}
		} finally {
			bm.setBlock(block);
		}
	}
	
	@Override
	public <T extends Throwable> void withLock(ThrowingRunnable <T> exec) throws T, IOException {
		Object elementLock = getElementLock();
		if (Thread.holdsLock(elementLock)) {
			final long myBlock = block;
			bm.getBlock(myBlock);
			try {
				exec.execute();
			} finally {
				bm.ungetBlock(myBlock);
			}
		} else {
			synchronized (elementLock) {
				final long myBlock = block;
				bm.getBlock(myBlock);
				try {
					exec.execute();
				} finally {
					bm.ungetBlock(myBlock);
				}
			}
		}
	}
	
	public static <T extends Throwable> void withLock(BlockManager bm, long lockBlock, ThrowingRunnable <T> exec) throws T, IOException {
		Object elementLock = getBlockLock(bm, lockBlock);
		if (Thread.holdsLock(elementLock)) {
			bm.getBlock(lockBlock);
			try {
				exec.execute();
			} finally {
				bm.ungetBlock(lockBlock);
			}
		} else {
			synchronized (elementLock) {
				bm.getBlock(lockBlock);
				try {
					exec.execute();
				} finally {
					bm.ungetBlock(lockBlock);
				}
			}
		}
	}
	
	public static <T extends Throwable, R> R withLock(BlockManager bm, long lockBlock, ThrowingSupplier <T, R> exec) throws T, IOException {
		Object elementLock = getBlockLock(bm, lockBlock);
		if (Thread.holdsLock(elementLock)) {
			bm.getBlock(lockBlock);
			try {
				return exec.supply();
			} finally {
				bm.ungetBlock(lockBlock);
			}
		} else {
			synchronized (elementLock) {
				bm.getBlock(lockBlock);
				try {
					return exec.supply();
				} finally {
					bm.ungetBlock(lockBlock);
				}
			}
		}
	}
	
	public static <T extends Throwable> long withLockLong(BlockManager bm, long lockBlock, ThrowingLongSupplier <T> exec) throws T, IOException {
		Object elementLock = getBlockLock(bm, lockBlock);
		if (Thread.holdsLock(elementLock)) {
			bm.getBlock(lockBlock);
			try {
				return exec.supply();
			} finally {
				bm.ungetBlock(lockBlock);
			}
		} else {
			synchronized (elementLock) {
				bm.getBlock(lockBlock);
				try {
					return exec.supply();
				} finally {
					bm.ungetBlock(lockBlock);
				}
			}
		}
	}
	
	@Override
	public <T extends Throwable, R> R withLock(ThrowingSupplier <T, R> exec) throws T, IOException {
		Object elementLock = getElementLock();
		if (Thread.holdsLock(elementLock)) {
			final long myBlock = block;
			bm.getBlock(myBlock);
			try {
				return exec.supply();
			} finally {
				bm.ungetBlock(myBlock);
			}
		} else {
			synchronized (elementLock) {
				final long myBlock = block;
				bm.getBlock(myBlock);
				try {
					return exec.supply();
				} finally {
					bm.ungetBlock(myBlock);
				}
			}
		}
	}
	
	@Override
	public <T extends Throwable> int withLockInt(ThrowingIntSupplier <T> exec) throws T, IOException {
		Object elementLock = getElementLock();
		if (Thread.holdsLock(elementLock)) {
			final long myBlock = block;
			bm.getBlock(myBlock);
			try {
				return exec.supply();
			} finally {
				bm.ungetBlock(myBlock);
			}
		} else {
			synchronized (elementLock) {
				final long myBlock = block;
				bm.getBlock(myBlock);
				try {
					return exec.supply();
				} finally {
					bm.ungetBlock(myBlock);
				}
			}
		}
	}
	
	@Override
	public <T extends Throwable> long withLockLong(ThrowingLongSupplier <T> exec) throws T, IOException {
		Object elementLock = getElementLock();
		if (Thread.holdsLock(elementLock)) {
			final long myBlock = block;
			bm.getBlock(myBlock);
			try {
				return exec.supply();
			} finally {
				bm.ungetBlock(myBlock);
			}
		} else {
			synchronized (elementLock) {
				final long myBlock = block;
				bm.getBlock(myBlock);
				try {
					return exec.supply();
				} finally {
					bm.ungetBlock(myBlock);
				}
			}
		}
	}
	
	@Override
	public <T extends Throwable> boolean withLockBoolean(ThrowingBooleanSupplier <T> exec) throws T, IOException {
		Object elementLock = getElementLock();
		if (Thread.holdsLock(elementLock)) {
			final long myBlock = block;
			bm.getBlock(myBlock);
			try {
				return exec.supply();
			} finally {
				bm.ungetBlock(myBlock);
			}
		} else {
			synchronized (elementLock) {
				final long myBlock = block;
				bm.getBlock(myBlock);
				try {
					return exec.supply();
				} finally {
					bm.ungetBlock(myBlock);
				}
			}
		}
	}
	
	public static <T extends Throwable> void simpleWithLock(BlockManager bm, long block, ThrowingRunnable <T> exec) throws T {
		Object elementLock = getBlockLock(bm, block);
		if (Thread.holdsLock(elementLock)) {
			exec.execute();
		} else {
			synchronized (elementLock) {
				exec.execute();
			}
		}
	}
	
	public static <T extends Throwable, R> R simpleWithLock(BlockManager bm, long block, ThrowingSupplier <T, R> exec) throws T {
		Object elementLock = getBlockLock(bm, block);
		if (Thread.holdsLock(elementLock)) {
			return exec.supply();
		} else {
			synchronized (elementLock) {
				return exec.supply();
			}
		}
	}
	
	public static <T extends Throwable> int simpleWithLockInt(BlockManager bm, long block, ThrowingIntSupplier <T> exec) throws T {
		Object elementLock = getBlockLock(bm, block);
		if (Thread.holdsLock(elementLock)) {
			return exec.supply();
		} else {
			synchronized (elementLock) {
				return exec.supply();
			}
		}
	}
	
	public static <T extends Throwable> long simpleWithLockLong(BlockManager bm, long block, ThrowingLongSupplier <T> exec) throws T {
		Object elementLock = getBlockLock(bm, block);
		if (Thread.holdsLock(elementLock)) {
			return exec.supply();
		} else {
			synchronized (elementLock) {
				return exec.supply();
			}
		}
	}
	
	@Override
	public <T extends Throwable> void simpleWithLock(ThrowingRunnable <T> exec) throws T {
		Object elementLock = getElementLock();
		if (Thread.holdsLock(elementLock)) {
			exec.execute();
		} else {
			synchronized (elementLock) {
				exec.execute();
			}
		}
	}
	
	@Override
	public <T extends Throwable, R> R simpleWithLock(ThrowingSupplier <T, R> exec) throws T {
		Object elementLock = getElementLock();
		if (Thread.holdsLock(elementLock)) {
			return exec.supply();
		} else {
			synchronized (elementLock) {
				return exec.supply();
			}
		}
	}
	
	@Override
	public <T extends Throwable> int simpleWithLockInt(ThrowingIntSupplier <T> exec) throws T {
		Object elementLock = getElementLock();
		if (Thread.holdsLock(elementLock)) {
			return exec.supply();
		} else {
			synchronized (elementLock) {
				return exec.supply();
			}
		}
	}
	
	@Override
	public <T extends Throwable> long simpleWithLockLong(ThrowingLongSupplier <T> exec) throws T {
		Object elementLock = getElementLock();
		if (Thread.holdsLock(elementLock)) {
			return exec.supply();
		} else {
			synchronized (elementLock) {
				return exec.supply();
			}
		}
	}
	
	@Override
	public <T extends Throwable> boolean simpleWithLockBoolean(ThrowingBooleanSupplier <T> exec) throws T {
		Object elementLock = getElementLock();
		if (Thread.holdsLock(elementLock)) {
			return exec.supply();
		} else {
			synchronized (elementLock) {
				return exec.supply();
			}
		}
	}
	
	protected static Object getBlockLock(BlockManager bm, long block) {
		synchronized (looks) {
			BlockLock key = new BlockLock(block, bm, null);
			WeakReference <Object> look = looks.get(key);
			if (look != null) {
				Object obj = look.get();
				if (obj != null) {
					return obj;
				}
			}
			key.obj = new WeakReference <Object>(new Object());
			looks.put(key, key.obj);
			return looks.get(key);
		}
	}
	
	protected Object getElementLock() {
		synchronized (looks) {
			BlockLock key = new BlockLock(block, bm, null);
			WeakReference <?> look = looks.get(key);
			if (look != null) {
				Object obj = look.get();
				if (obj != null) {
					return obj;
				}
			}
			key.obj = new WeakReference <>(new Object());
			looks.put(key, key.obj);
			return looks.get(key);
		}
	}
	
	public static class BlockLock {
		
		public final long                         block;
		public final WeakReference <BlockManager> bm;
		
		public WeakReference <Object> obj;
		
		public BlockLock(long block, BlockManager bm, Object obj) {
			this.block = block;
			this.bm = new WeakReference <>(bm);
			this.obj = new WeakReference <>(obj);
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (block ^ (block >>> 32));
			result = prime * result + ( (bm == null) ? 0 : bm.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			else if (obj == null) return false;
			else if (getClass() != obj.getClass()) return false;
			BlockLock other = (BlockLock) obj;
			if (block != other.block) return false;
			else if ( !bm.equals(other.bm)) return false;
			else return true;
		}
		
		@Override
		protected void finalize() throws Throwable {
			synchronized (looks) {
				WeakReference <Object> ref = looks.remove(this);
				Object otehr = ref.get();
				if (otehr != null && otehr != this.obj) {
					this.obj = ref;
					looks.put(this, ref);
				}
			}
		}
		
	}
	
}

package de.hechler.patrick.pfs.objects.fs;

import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToInt;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToLong;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.intToByteArr;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.longToByteArr;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_EXECUTABLE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_FILE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_FOLDER;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_HIDDEN;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_LINK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_READ_ONLY;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_CREATE_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_FLAGS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LAST_META_MOD_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LAST_MOD_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LOCK_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LOCK_VALUE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_NAME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_OWNER;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_PARENT_ID;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_TABLE_FILE_ID;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_DATA_TABLE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_ELEMENT_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_OFFSET_ELEMENT_COUNT;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_OFFSET_FOLDER_ELEMENTS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LINK_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LINK_OFFSET_TARGET_ID;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_DATA;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_DELETE_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_META_CHANGE_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_WRITE_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_SHARED_COUNTER_AND;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_SHARED_COUNTER_MAX_VALUE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_SHARED_COUNTER_SHIFT;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_SHARED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_USER_MAX_VALUE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_USER_SHIFT;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_ID;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ROOT_FOLDER_ID;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.exception.OutOfSpaceException;
import de.hechler.patrick.pfs.interfaces.BlockManager;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.interfaces.PatrLink;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingBooleanSupplier;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingIntSupplier;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingLongSupplier;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingRunnable;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingSupplier;
import de.hechler.patrick.pfs.objects.AllocatedBlocks;
import de.hechler.patrick.pfs.objects.LongInt;
import de.hechler.patrick.pfs.objects.java.PFSFileSystemProviderImpl;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;

public class PatrFileSysElementImpl extends PatrID implements PatrFileSysElement {
	
	// private static Map <BlockLock, WeakReference <Object>> looks = new HashMap <>();
	
	public final BlockManager bm;
	
	public PatrFileSysElementImpl(PatrFileSysImpl fs, long startTime, BlockManager bm, long id) {
		super(fs, id, startTime);
		this.bm = bm;
	}
	
	protected long block;
	protected int  pos;
	
	
	protected void updatePosAndBlock() throws IOException {
		LongInt blockAndPos = PatrFileSysImpl.getBlockAndPos(this);
		block = blockAndPos.l;
		pos = blockAndPos.i;
	}
	
	@Override
	public PatrFolderImpl getParent() throws IllegalStateException, IOException {
		if (id == ROOT_FOLDER_ID) {
			throw new IllegalStateException("this is the root folder!");
		}
		return simpleWithLock(() -> {
			updatePosAndBlock();
			return executeGetParent();
		});
	}
	
	private PatrFolderImpl executeGetParent() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			long pid = byteArrToLong(bytes, pos + ELEMENT_OFFSET_PARENT_ID);
			if (pid == NO_ID) throw new InternalError("this element has no parent!");
			else return new PatrFolderImpl(fs, startTime, bm, pid);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public void setParent(PatrFolder newParent, long lock, long oldParentLock, long newParentLock) throws IllegalStateException, IOException {
		if ( ! (newParent instanceof PatrFileSysImpl)) {
			throw new IllegalArgumentException("I can not set my parent to an folder of an unknown implementation");
		}
		if (id == ROOT_FOLDER_ID) {
			throw new IllegalStateException("this is the root folder!");
		}
		PatrFolderImpl np = (PatrFolderImpl) newParent;
		if (np.bm != bm) {
			throw new IllegalArgumentException("I can not set my parent to an folder of an other fils system");
		}
		simpleWithLock(() -> {
			updatePosAndBlock();
			executeSetParent(np, lock, oldParentLock, newParentLock);
		});
	}
	
	private void executeSetParent(PatrFolderImpl np, long lock, long oldParentLock, long newParentLock) throws IllegalStateException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			np.updatePosAndBlock();
			long npblock = np.block;
			byte[] pbytes = bm.getBlock(npblock);
			try {
				PatrFolderImpl oldParent = getParent();
				oldParent.updatePosAndBlock();
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
				np.resize(oldLen, newLen);
				longToByteArr(bytes, addPos, id);
				longToByteArr(bytes, pos + ELEMENT_OFFSET_PARENT_ID, np.id);
				np.modify(false);
				modify(true);
			} finally {
				bm.setBlock(npblock);
			}
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public PatrFolder getFolder() throws IllegalStateException, IOException {
		return withLock(() -> {
			updatePosAndBlock();
			return executeGetFolder();
		});
	}
	
	protected PatrFolder executeGetFolder() throws IOException {
		int flags = executeGetFlags();
		if (0 == (ELEMENT_FLAG_FOLDER & flags)) {
			throw new IllegalStateException("this is no folder!");
		}
		if (this instanceof PatrFolder) {
			return (PatrFolder) this;
		} else if (0 != (ELEMENT_FLAG_LINK & flags)) {
			byte[] bytes = bm.getBlock(block);
			try {
				long tid = byteArrToLong(bytes, pos + LINK_OFFSET_TARGET_ID);
				return new PatrFolderImpl(fs, startTime, bm, tid);
			} finally {
				bm.ungetBlock(block);
			}
		}
		return new PatrFolderImpl(fs, startTime, bm, id);
	}
	
	@Override
	public PatrFile getFile() throws IllegalStateException, IOException {
		return withLock(() -> {
			updatePosAndBlock();
			return executeGetFile();
		});
	}
	
	protected PatrFile executeGetFile() throws IOException {
		int flags = executeGetFlags();
		if (0 == (ELEMENT_FLAG_FILE & flags)) {
			throw new IllegalStateException("this is no file!");
		}
		if (this instanceof PatrFile) {
			return (PatrFile) this;
		} else if (0 != (ELEMENT_FLAG_LINK & flags)) {
			byte[] bytes = bm.getBlock(block);
			try {
				long tid = byteArrToLong(bytes, pos + LINK_OFFSET_TARGET_ID);
				return new PatrFileImpl(fs, startTime, bm, tid);
			} finally {
				bm.ungetBlock(block);
			}
		}
		return new PatrFileImpl(fs, startTime, bm, id);
	}
	
	@Override
	public PatrLink getLink() throws IllegalStateException, IOException {
		return withLock(() -> {
			updatePosAndBlock();
			return executeGetLink();
		});
	}
	
	private PatrLink executeGetLink() throws IOException {
		if (0 == (ELEMENT_FLAG_LINK & executeGetFlags())) {
			throw new IllegalStateException("this is no link!");
		}
		if (this instanceof PatrLink) {
			return (PatrLink) this;
		}
		return new PatrLinkImpl(fs, startTime, bm, id);
	}
	
	@Override
	public boolean isFolder() throws IOException {
		return (getFlags() & ELEMENT_FLAG_FOLDER) != 0;
	}
	
	@Override
	public boolean isFile() throws IOException {
		return (getFlags() & ELEMENT_FLAG_FILE) != 0;
	}
	
	@Override
	public boolean isLink() throws IOException {
		return (getFlags() & ELEMENT_FLAG_LINK) != 0;
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
		setFlag(isExecutale, lock, ELEMENT_FLAG_EXECUTABLE);
	}
	
	@Override
	public void setHidden(boolean isHidden, long lock) throws IOException, ElementLockedException {
		setFlag(isHidden, lock, ELEMENT_FLAG_HIDDEN);
	}
	
	@Override
	public void setReadOnly(boolean isReadOnly, long lock) throws IOException, ElementLockedException {
		setFlag(isReadOnly, lock, ELEMENT_FLAG_READ_ONLY);
	}
	
	private void setFlag(boolean doSet, long lock, int flag) throws IOException {
		withLock(() -> {
			updatePosAndBlock();
			ensureAccess(lock, LOCK_NO_META_CHANGE_ALLOWED_LOCK, false);
			flag(flag, doSet);
			modify(true);
		});
	}
	
	public int getFlags() throws IOException {
		return simpleWithLockInt(() -> {
			updatePosAndBlock();
			return executeGetFlags();
		});
	}
	
	private int executeGetFlags() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			return byteArrToInt(bytes, pos + ELEMENT_OFFSET_FLAGS);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public Object getID() throws IOException {
		return this;
	}
	
	/**
	 * sets or removes the given flags
	 * 
	 * @param flags
	 *            the flags to modify
	 * @param doSet
	 *            if the flags should be set or cleared
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected void flag(int flags, boolean doSet) throws IOException {
		if (doSet) flag(flags, 0);
		else flag(0, flags);
	}
	
	/**
	 * sets and removes the given flags
	 * <p>
	 * if ({@code addFlags & remFlags}) is not zero the behavior is undefined
	 * 
	 * @param addFlags
	 *            the flags to add/set
	 * @param remFlags
	 *            the flags to remove
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected void flag(int addFlags, int remFlags) throws IOException {
		simpleWithLock(() -> {
			executeFlag(addFlags, remFlags);
		});
	}
	
	protected void executeFlag(int addFlags, int remFlags) throws IOException {
		int flags = getFlags();
		flags |= addFlags;
		flags &= ~remFlags;
		setFlags(flags);
	}
	
	/**
	 * sets the flags of this element
	 * <p>
	 * this method should only be used with great care, since the flags also decide, which element is a file, folder and link!<br>
	 * the preferred method to modify to modify the flags is {@link #flag(int, boolean)} and {@link #flag(int, int)}.
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
		return simpleWithLockInt(() -> {
			updatePosAndBlock();
			return executeGetOwner();
		});
	}
	
	private int executeGetOwner() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			return byteArrToInt(bytes, pos + ELEMENT_OFFSET_OWNER);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public void setOwner(int owner, long lock) throws IOException {
		simpleWithLock(() -> {
			updatePosAndBlock();
			executeSetOwner(owner, lock);
		});
	}
	
	private void executeSetOwner(int owner, long lock) throws ClosedChannelException, IOException, ElementLockedException {
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
		return simpleWithLockLong(() -> {
			updatePosAndBlock();
			return executeGetCreateTime();
		});
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
		return simpleWithLockLong(() -> {
			updatePosAndBlock();
			return executeGetLastModTime();
		});
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
		return simpleWithLockLong(() -> {
			updatePosAndBlock();
			return executeGetLastMetaModTime();
		});
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
		return simpleWithLockLong(() -> {
			updatePosAndBlock();
			return executeGetLockData();
		});
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
		return simpleWithLockLong(() -> {
			updatePosAndBlock();
			return executeGetLockTime();
		});
	}
	
	private long executeGetLockTime() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			long lockTime = byteArrToLong(bytes, pos + ELEMENT_OFFSET_LOCK_TIME);
			if (lockTime < startTime && lockTime != NO_TIME) {
				executeRemoveLock(LOCK_NO_LOCK);
				return NO_TIME;
			}
			return lockTime;
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public void removeLock(long lock) throws IOException, IllegalStateException {
		withLock(() -> {
			updatePosAndBlock();
			executeRemoveLock(lock);
			modify(true);
		});
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
					if (cnt > 0L) {
						newLock = cnt << LOCK_SHARED_COUNTER_SHIFT;
						newLock |= currentLock & ~LOCK_SHARED_COUNTER_AND;
						newTime = executeGetLockTime();
					}
				}
			}
			longToByteArr(bytes, pos + ELEMENT_OFFSET_LOCK_VALUE, newLock);
			longToByteArr(bytes, pos + ELEMENT_OFFSET_LOCK_TIME, newTime);
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
			long lockTime = byteArrToLong(bytes, pos + ELEMENT_OFFSET_LOCK_TIME);
			if (lockTime != NO_TIME && lockTime < startTime) {
				longToByteArr(bytes, pos + ELEMENT_OFFSET_LOCK_TIME, NO_TIME);
				longToByteArr(bytes, pos + ELEMENT_OFFSET_LOCK_VALUE, LOCK_NO_LOCK);
				return LOCK_NO_LOCK;
			}
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
		return simpleWithLockLong(() -> {
			updatePosAndBlock();
			return executeLock(newLock);
		});
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
		simpleWithLock(() -> {
			updatePosAndBlock();
			executeEnsureAccess(lock, forbiddenBits, readOnlyForbidden);
		});
	}
	
	protected void executeEnsureAccess(long lock, long forbiddenBits, boolean readOnlyForbidden) throws IOException, ElementLockedException, IllegalArgumentException {
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
		} else if ( (myLock & forbiddenBits) != 0) {
			throw new ElementLockedException(PFSFileSystemProviderImpl.buildName(this), "the lock is LOCK_NO_LOCK, but I have at least one of the forbidden bits set in my lock!");
		}
	}
	
	protected int getNameByteCount() throws IOException, IllegalStateException {
		byte[] bytes = bm.getBlock(block);
		try {
			int np = byteArrToInt(bytes, pos + ELEMENT_OFFSET_NAME);
			if (np == -1) {
				assert id == ROOT_FOLDER_ID || id == ELEMENT_TABLE_FILE_ID;
				return 0;
			}
			int byteCount;
			for (byteCount = 0; bytes[np + byteCount] != 0 || bytes[np + byteCount + 1] != 0; byteCount += 2) {}
			return byteCount + 2;
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public void setName(String name, long lock) throws IOException, NullPointerException, IllegalStateException, ElementLockedException {
		Objects.requireNonNull(name, "element names can not be null!");
		if (id == ROOT_FOLDER_ID) {
			throw new IllegalStateException("this element is the root folder!");
		}
		simpleWithLock(() -> {
			updatePosAndBlock();
			executeSetName(name, lock);
		});
	}
	
	private void executeSetName(String name, long lock) throws ClosedChannelException, IOException, ElementLockedException, OutOfSpaceException {
		long oldblock = block;
		byte[] bytes = bm.getBlock(oldblock);
		try {
			ensureAccess(lock, LOCK_NO_META_CHANGE_ALLOWED_LOCK, false);
			int oldlen = getNameByteCount();
			int np = byteArrToInt(bytes, pos + ELEMENT_OFFSET_NAME);
			byte[] namebytes = name.getBytes(StandardCharsets.UTF_16);
			try {
				np = reallocate(oldblock, np, oldlen, namebytes.length + 2, false);
			} catch (OutOfSpaceException e) {
				int myLen = myLength();
				relocate(myLen, myLen, namebytes.length + 2);
				np = byteArrToInt(bytes, pos + ELEMENT_OFFSET_NAME);
				try {
					np = reallocate(oldblock, np, oldlen, namebytes.length + 2, false);
				} catch (OutOfSpaceException e1) {
					e.addSuppressed(e1);
					throw e;
				}
			}
			bytes = bm.getBlock(block);
			try {
				System.arraycopy(namebytes, 0, bytes, np, namebytes.length);
				bytes[np + namebytes.length] = 0;
				bytes[np + namebytes.length + 1] = 0;
				modify(true);
			} finally {
				bm.setBlock(block);
			}
		} finally {
			bm.setBlock(oldblock);
		}
	}
	
	@Override
	public String getName() throws IOException {
		if (id == ROOT_FOLDER_ID) {
			return "";
		}
		return simpleWithLock(() -> {
			updatePosAndBlock();
			return executeGetName();
		});
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
	
	// /**
	// * frees the name from the block intern memory table
	// *
	// * @throws IOException
	// * if an IO error occurs
	// */
	// protected void freeName() throws IOException {
	// byte[] bytes = bm.getBlock(block);
	// try {
	// int nameLen = getNameByteCount();
	// int namePos = byteArrToInt(bytes, pos + ELEMENT_OFFSET_NAME);
	// reallocate(block, namePos, nameLen, 0, false);
	// } finally {
	// bm.setBlock(block);
	// }
	// }
	//
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
	
	@Override
	public void delete(long myLock, long parentLock) throws IOException, IllegalStateException, ElementLockedException {
		withLock(() -> executeDelete(myLock, parentLock));
	}
	
	private void executeDelete(long myLock, long parentLock) throws ElementLockedException, IllegalStateException, IOException {
		if (isFolder()) {
			if (getFolder().elementCount(myLock) > 0) {
				throw new IllegalStateException("this element is a non empty folder!");
			}
		}
		ensureAccess(myLock, LOCK_NO_DELETE_ALLOWED_LOCK, true);
		PatrFolderImpl parent = getParent();
		parent.ensureAccess(parentLock, LOCK_NO_WRITE_ALLOWED_LOCK, true);
		deleteFromParent();
		int myLen = myLength();
		reallocate(block, pos, myLen, 0, false);
		parent.modify(false);
		PatrFileSysImpl.removeID(this);
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
			long pid = byteArrToLong(bytes, pos + ELEMENT_OFFSET_PARENT_ID);
			LongInt parent = PatrFileSysImpl.getBlockAndPos(new PatrID(fs, pid, startTime));
			bytes = bm.getBlock(parent.l);
			try {
				int index = indexInParentList(parent.l, parent.i),
					imel = index * FOLDER_ELEMENT_LENGTH,
					off = parent.i + FOLDER_OFFSET_FOLDER_ELEMENTS + imel,
					oldElementCount = byteArrToInt(bytes, parent.i + FOLDER_OFFSET_ELEMENT_COUNT),
					oldElementSize = oldElementCount * FOLDER_ELEMENT_LENGTH,
					copySize = oldElementSize - imel - FOLDER_ELEMENT_LENGTH;
				System.arraycopy(bytes, off + FOLDER_ELEMENT_LENGTH, bytes, off, copySize);
				intToByteArr(bytes, parent.i + FOLDER_OFFSET_ELEMENT_COUNT, oldElementCount - 1);
			} finally {
				bm.setBlock(parent.l);
			}
		} finally {
			bm.ungetBlock(block);
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
				final int imfel = i * FOLDER_ELEMENT_LENGTH;
				long cid = byteArrToLong(bytes, off + imfel);
				if (cid != id) {
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
	 * returns the length of this element
	 * 
	 * @return the length of this element
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected int myLength() throws IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			int mylen;
			if (isLink()) {
				mylen = LINK_LENGTH;
			} else if (isFolder()) {
				mylen = byteArrToInt(bytes, pos + FOLDER_OFFSET_ELEMENT_COUNT);
				mylen *= FOLDER_ELEMENT_LENGTH;
				mylen += FOLDER_OFFSET_FOLDER_ELEMENTS;
			} else {
				long byteLen = byteArrToLong(bytes, pos + FILE_OFFSET_FILE_LENGTH),
					blocks = (byteLen + ((long) bytes.length) - 1L) / bytes.length;
				for (mylen = FILE_OFFSET_FILE_DATA_TABLE; blocks > 0; mylen += 16) {
					long start = byteArrToLong(bytes, pos + mylen);
					long end = byteArrToLong(bytes, pos + mylen + 8);
					blocks -= end - start;
				}
				assert blocks == 0L;
			}
			return mylen;
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	/**
	 * resizes this element.
	 * 
	 * @param oldSize
	 *            the old size of this element
	 * @param newSize
	 *            the new size of this element
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected void resize(int oldSize, int newSize) throws IOException {
		try {
			final long oldPos = pos;
			pos = reallocate(block, pos, oldSize, newSize, true);
			if (pos != oldPos) {
				PatrFileSysImpl.setBlockAndPos(this, block, pos);
			}
		} catch (OutOfSpaceException e) {
			try {
				relocate(oldSize, newSize, -1);
			} catch (OutOfSpaceException e1) {
				e.addSuppressed(e1);
				throw e;
			}
		}
	}
	
	/**
	 * relocates this element to a new block.
	 * <p>
	 * if {@code newNameLen} is {@code -1} the name will be copied to the new name position.<br>
	 * if {@code newNameLen} is not {@code -1} the memory for the name will be allocated, but not be touched.
	 * 
	 * @param myoldlen
	 *            the old length of this element wich should be freed in the old block
	 * @param mynewlen
	 *            the new length of this element wich should be allocated in the new block
	 * @param newNameLen
	 *            the new length of the new name or -1 if the name remains unchanged.
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws OutOfSpaceException
	 *             if there is not enough memory
	 */
	protected void relocate(int myoldlen, int mynewlen, int newNameLen) throws IOException, OutOfSpaceException {
		final long myNewBlockNum = allocateOneBlock(),
			myOldBlockNum = block;
		final int myOldPos = pos;
		int oldnamepos;
		int namelen;
		byte[] oldBlock = bm.getBlock(myOldBlockNum);
		try {
			oldnamepos = byteArrToInt(oldBlock, myOldPos + ELEMENT_OFFSET_NAME);
			namelen = getNameByteCount();
			byte[] newBlock = bm.getBlock(myNewBlockNum);
			try {
				PatrFileSysImpl.initBlock(newBlock, mynewlen + newNameLen);
				final int myNewPos = namelen, nameNewPos = 0;
				try {
					reallocate(myOldBlockNum, myOldPos, myoldlen, 0, false);
					if (namelen > 0) {
						try {
							reallocate(myOldBlockNum, oldnamepos, namelen, 0, false);
						} catch (OutOfSpaceException e) {
							allocate(myOldBlockNum, myoldlen);
							throw e;
						}
					}
				} catch (OutOfSpaceException e) {
					free(new AllocatedBlocks(myNewBlockNum, 1L));
					throw e;
				}
				System.arraycopy(oldBlock, myOldPos, newBlock, myNewPos, myoldlen);
				if (namelen > 0) {
					if (namelen == -1) {
						System.arraycopy(oldBlock, oldnamepos, newBlock, nameNewPos, namelen);
					}
					intToByteArr(newBlock, namelen + ELEMENT_OFFSET_NAME, 0);
				}
				pos = myNewPos;
				block = myNewBlockNum;
				PatrFileSysImpl.setBlockAndPos(this, myNewBlockNum, myNewPos);
			} finally {
				bm.setBlock(myNewBlockNum);
			}
		} finally {
			bm.setBlock(myOldBlockNum);
		}
	}
	
	/**
	 * allocates one block.<br>
	 * this is effectively the same as <code>allocate(1L)[0].startBlock</code>
	 * 
	 * @return the allocated block
	 * @throws OutOfSpaceException
	 *             if there is not enough memory to allocate one block
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected long allocateOneBlock() throws OutOfSpaceException, IOException {
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
	 * @throws OutOfSpaceException
	 *             if there is not enough memory
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected AllocatedBlocks[] allocate(long len) throws OutOfSpaceException, IOException {
		byte[] bytes = bm.getBlock(1L);
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
				result.add(new AllocatedBlocks(endBlock, len));
				endBlock += len;
				len -= len;
				final long blockCount = fs.blockCount();
				if (endBlock > blockCount) {
					for (AllocatedBlocks ab : result) {
						free(ab);
					}
					throw new OutOfSpaceException("not enugh blocks (blockCount=" + Long.toHexString(blockCount) + " endBlock=" + Long.toHexString(endBlock) + ") (I freed the new allocated blocks)");
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
	 * @throws OutOfSpaceException
	 *             if there is not enough memory
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected void free(AllocatedBlocks remove) throws OutOfSpaceException, IOException {
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
					throw new OutOfSpaceException("there is not enough memory in this block to free/remove the given block sequence!");
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
	 * @throws OutOfSpaceException
	 *             if there is not enough memory
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected int allocate(long block, int len) throws OutOfSpaceException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			int tablestart = byteArrToInt(bytes, bytes.length - 4),
				entrycount = (bytes.length - 4 - tablestart) >>> 3;
			if (byteArrToInt(bytes, tablestart) >= len) {
				int start = byteArrToInt(bytes, tablestart);
				if (start == len) {
					intToByteArr(bytes, tablestart, 0);
				} else {
					try {
						tableGrow(block, 0, 0, len);
					} catch (OutOfSpaceException e) {
						start -= len;
						intToByteArr(bytes, tablestart, start);
					}
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
			throw new OutOfSpaceException("could not find enough memory! (len=" + len + ")");
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
	 * @throws OutOfSpaceException
	 *             if there is not enough memory
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected int reallocate(long block, int pos, int oldLen, int newLen, boolean copy) throws OutOfSpaceException, IOException {
		byte[] bytes = bm.getBlock(block);
		if (newLen < 0 || oldLen < 0 || pos < 0) {
			throw new IllegalArgumentException("len is smaller than zero! (newLen=" + newLen + ", odLen=" + oldLen + ")");
		}
		try {
			int tablestart = byteArrToInt(bytes, bytes.length - 4),
				entrycount = (bytes.length - 4 - tablestart) / 8,
				min = 0,
				max = entrycount - 1,
				mid, midoff;
			while (true) {
				mid = (min + max) >>> 1;
				midoff = tablestart + (mid * 8);
				int start = byteArrToInt(bytes, midoff),
					end = byteArrToInt(bytes, midoff + 4);
				if (pos < start) {
					max = mid - 1;
				} else if (end < pos) {
					min = mid + 1;
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
							free = nextStart - end;
						if (free < newLen - oldLen) {
							needNewPlace = true;
						}
					} else {
						needNewPlace = true;
					}
					if (needNewPlace) {
						int newPos;
						if (remove(block, pos, pos + oldLen, mid, false) == -2) {
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
						intToByteArr(bytes, midoff + 4, pos + newLen);
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
	 * @throws OutOfSpaceException
	 *             if the entry gets separated and there is not enough memory for the table to grow
	 */
	private int remove(long block, int remFrom, int remTo, int index, boolean allowFreeBlock) throws IOException, OutOfSpaceException {
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
				if (end == bytes.length) {
					if (start == bytes.length - 20) {
						if (allowFreeBlock) {
							free(new AllocatedBlocks(block, 1L));
						}
						return -2;
					}
				}
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
			final int tablestart = byteArrToInt(bytes, bytes.length - 4),
				lastStart = byteArrToInt(bytes, bytes.length - 12),
				rim8 = remindex * 8,
				remoff = tablestart + rim8;
			assert remoff >= tablestart;
			assert remoff <= bytes.length - 12;
			if (remoff == bytes.length - 12) {
				assert tablestart == remoff : "tablestart=" + tablestart + " remoff=" + remoff + " remindex=" + remindex;
				if (allowFreeBlock) {
					free(new AllocatedBlocks(block, 1L));
				}
				return true;
			}
			if (lastStart == tablestart) {
				System.arraycopy(bytes, tablestart, bytes, tablestart + 8, remoff - tablestart);
				intToByteArr(bytes, bytes.length - 12, tablestart + 8);
				intToByteArr(bytes, bytes.length - 4, tablestart + 8);
			} else {
				System.arraycopy(bytes, remoff + 8, bytes, remoff, bytes.length - 12 - remoff - 8);
				intToByteArr(bytes, bytes.length - 12, tablestart);
				intToByteArr(bytes, bytes.length - 16, tablestart);
			}
			return true;
			// if (tablestart != lastStart) {
			// System.arraycopy(bytes, remoff + 8, bytes, remoff, bytes.length - 20 - rim8);
			// intToByteArr(bytes, bytes.length - 12, tablestart);// one of two cases to produce to entries where prev.end=next.start
			// intToByteArr(bytes, bytes.length - 16, tablestart);
			// } else if (tablestart + 8 >= bytes.length - 12) {
			// if (allowFreeBlock) {
			// free(new AllocatedBlocks(block, 1L));
			// }
			// return false;
			// } else {
			// System.arraycopy(bytes, tablestart, bytes, tablestart + 8, rim8);
			// intToByteArr(bytes, bytes.length - 12, tablestart + 8);
			// intToByteArr(bytes, bytes.length - 4, tablestart + 8);
			// }
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
	 * @throws OutOfSpaceException
	 *             if there is not enough memory for the table to grow
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected void tableGrow(long block, int addindex, int start, int end) throws OutOfSpaceException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			int tablestart = byteArrToInt(bytes, bytes.length - 4),
				lastStart = byteArrToInt(bytes, bytes.length - 12),
				addoff = tablestart + addindex * 8;
			if (tablestart == lastStart) {
				int prevLastEnd = byteArrToInt(bytes, bytes.length - 16);
				if (addoff == bytes.length - 12) {
					prevLastEnd = end;
				}
				if (tablestart - 8 < prevLastEnd) {
					throw new OutOfSpaceException("there is not enugh memory for the table to grow");
				} else if (tablestart - 8 == prevLastEnd) {
					System.arraycopy(bytes, addoff, bytes, addoff + 8, bytes.length - addoff - 8);
					intToByteArr(bytes, addoff, start);
					intToByteArr(bytes, addoff, end);
					intToByteArr(bytes, bytes.length - 4, tablestart - 8);
				} else {
					System.arraycopy(bytes, tablestart, bytes, tablestart - 8, addoff - tablestart);
					intToByteArr(bytes, addoff - 8, start);
					intToByteArr(bytes, addoff - 4, end);
					intToByteArr(bytes, bytes.length - 4, tablestart - 8);
					intToByteArr(bytes, bytes.length - 12, tablestart - 8);
				}
			} else {
				throw new OutOfSpaceException("there is not enugh memory for the table to grow");
			}
		} finally {
			bm.setBlock(block);
		}
	}
	
	/**
	 * locks the array block manager
	 * 
	 * @param <T>
	 *            the exception type which can be thrown from {@code exec}
	 * @param bm
	 *            the block manager
	 * @param exec
	 *            the runnable to be executed
	 * @throws T
	 *             the exception type possibly thrown by runnable
	 */
	protected static <T extends Throwable> void simpleWithLock(BlockManager bm, ThrowingRunnable <T> exec) throws T {
		synchronized (bm) {
			exec.execute();
		}
	}
	
	/**
	 * lock the block manager
	 * <p>
	 * in addition to {@link #simpleWithLock(BlockManager, ThrowingRunnable, int, long...)} this method returns the value returned by {@code exec}
	 * 
	 * @param <T>
	 *            the exception type which can be thrown from {@code exec}
	 * @param <R>
	 *            the return type from {@code exec}
	 * @param bm
	 *            the block manager
	 * @param exec
	 *            the runnable to be executed
	 * @return the value returned from {@code Patrick exec}
	 * @throws T
	 *             the exception type possibly thrown by runnable
	 */
	protected static <T extends Throwable, R> R simpleWithLock(BlockManager bm, ThrowingSupplier <T, R> exec) throws T {
		synchronized (bm) {
			return exec.supply();
		}
	}
	
	/**
	 * locks the block manager
	 * <p>
	 * in addition to {@link #simpleWithLock(BlockManager, ThrowingRunnable, int, long...)} this method returns the value returned by {@code exec}
	 * 
	 * @param <T>
	 *            the exception type which can be thrown from {@code exec}
	 * @param bm
	 *            the block manager
	 * @param exec
	 *            the runnable to be executed
	 * @return the value returned from {@code Patrick exec}
	 * @throws T
	 *             the exception type possibly thrown by runnable
	 */
	protected static <T extends Throwable> int simpleWithLockInt(BlockManager bm, ThrowingIntSupplier <T> exec) throws T {
		synchronized (bm) {
			return exec.supply();
		}
	}
	
	/**
	 * locks the block manager
	 * <p>
	 * in addition to {@link #simpleWithLock(BlockManager, ThrowingRunnable, int, long...)} this method returns the value returned by {@code exec}
	 * 
	 * @param <T>
	 *            the exception type which can be thrown from {@code exec}
	 * @param bm
	 *            the block manager
	 * @param exec
	 *            the runnable to be executed
	 * @return the value returned from {@code Patrick exec}
	 * @throws T
	 *             the exception type possibly thrown by runnable
	 */
	protected static <T extends Throwable> long simpleWithLockLong(BlockManager bm, ThrowingLongSupplier <T> exec) throws T {
		synchronized (bm) {
			return exec.supply();
		}
	}
	
	/**
	 * locks the block manager
	 * <p>
	 * in addition to {@link #simpleWithLock(BlockManager, ThrowingRunnable, int, long...)} this method returns the value returned by {@code exec}
	 * 
	 * @param <T>
	 *            the exception type which can be thrown from {@code exec}
	 * @param bm
	 *            the block manager
	 * @param exec
	 *            the runnable to be executed
	 * @return the value returned from {@code Patrick exec}
	 * @throws T
	 *             the exception type possibly thrown by runnable
	 */
	protected static <T extends Throwable> boolean simpleWithLockBoolean(BlockManager bm, ThrowingBooleanSupplier <T> exec) throws T {
		synchronized (bm) {
			return exec.supply();
		}
	}
	
	@Override
	public <T extends Throwable> void simpleWithLock(ThrowingRunnable <T> exec) throws T {
		synchronized (bm) {
			exec.execute();
		}
	}
	
	@Override
	public <T extends Throwable, R> R simpleWithLock(ThrowingSupplier <T, R> exec) throws T {
		synchronized (bm) {
			return exec.supply();
		}
	}
	
	@Override
	public <T extends Throwable> int simpleWithLockInt(ThrowingIntSupplier <T> exec) throws T {
		synchronized (bm) {
			return exec.supply();
		}
	}
	
	@Override
	public <T extends Throwable> long simpleWithLockLong(ThrowingLongSupplier <T> exec) throws T {
		synchronized (bm) {
			return exec.supply();
		}
	}
	
	@Override
	public <T extends Throwable> boolean simpleWithLockBoolean(ThrowingBooleanSupplier <T> exec) throws T {
		synchronized (bm) {
			return exec.supply();
		}
	}
	
	/**
	 * locks the block manager
	 * <p>
	 * in addition to {@link #simpleWithLock(BlockManager, ThrowingRunnable, int, long...)} this method also loads the blocks from the arrays.<br>
	 * So this is the better method, when the blocks are often loaded and unloaded
	 * 
	 * 
	 * @param <T>
	 *            the exception type which can be thrown from {@code exec}
	 * @param bm
	 *            the block manager
	 * @param exec
	 *            the runnable to be executed
	 * @param block
	 *            the block to be loaded
	 * @return the value returned from {@code Patrick exec}
	 * @throws T
	 *             the exception type possibly thrown by runnable
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected static <T extends Throwable> void withLock(BlockManager bm, ThrowingRunnable <T> exec, long block) throws T, IOException {
		synchronized (bm) {
			long oldblock = block;
			bm.getBlock(oldblock);
			try {
				exec.execute();
			} finally {
				bm.ungetBlock(oldblock);
			}
		}
	}
	
	/**
	 * locks the block manager
	 * <p>
	 * in addition to {@link #simpleWithLock(BlockManager, ThrowingRunnable, int, long...)} this method also loads the blocks from the arrays.<br>
	 * So this is the better method, when the blocks are often loaded and unloaded
	 * <p>
	 * in addition to {@link #withLock(BlockManager, ThrowingRunnable, int, long...)} this method returns the value returned by {@code exec}
	 * 
	 * @param <T>
	 *            the exception type which can be thrown from {@code exec}
	 * @param <R>
	 *            the return type from {@code exec}
	 * @param bm
	 *            the block manager
	 * @param exec
	 *            the runnable to be executed
	 * @param block
	 *            the block to be loaded
	 * @return the value returned from {@code Patrick exec}
	 * @throws T
	 *             the exception type possibly thrown by runnable
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected static <T extends Throwable, R> R withLock(BlockManager bm, ThrowingSupplier <T, R> exec, long block) throws T, IOException {
		synchronized (bm) {
			long oldblock = block;
			bm.getBlock(oldblock);
			try {
				return exec.supply();
			} finally {
				bm.ungetBlock(oldblock);
			}
		}
	}
	
	/**
	 * locks the array of blocks from {@code i} to to len<br>
	 * the blocks need to be sorted in ascending order. (multiple same blocks are permitted)
	 * <p>
	 * in addition to {@link #simpleWithLock(BlockManager, ThrowingRunnable, int, long...)} this method also loads the blocks from the arrays.<br>
	 * So this is the better method, when the blocks are often loaded and unloaded
	 * <p>
	 * in addition to {@link #withLock(BlockManager, ThrowingRunnable, int, long...)} this method returns the value returned by {@code exec}
	 * 
	 * @param <T>
	 *            the exception type which can be thrown from {@code exec}
	 * @param <R>
	 *            the return type from {@code exec}
	 * @param bm
	 *            the block manager
	 * @param exec
	 *            the runnable to be executed
	 * @param block
	 *            the block to be loaded
	 * @return the value returned from {@code Patrick exec}
	 * @throws T
	 *             the exception type possibly thrown by runnable
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected static <T extends Throwable> int withLockInt(BlockManager bm, ThrowingIntSupplier <T> exec, long block) throws T, IOException {
		synchronized (bm) {
			long oblock = block;
			bm.getBlock(oblock);
			try {
				return exec.supply();
			} finally {
				bm.ungetBlock(oblock);
			}
		}
	}
	
	/**
	 * locks the array of blocks from {@code i} to to len<br>
	 * the blocks need to be sorted in ascending order. (multiple same blocks are permitted)
	 * <p>
	 * in addition to {@link #simpleWithLock(BlockManager, ThrowingRunnable, int, long...)} this method also loads the blocks from the arrays.<br>
	 * So this is the better method, when the blocks are often loaded and unloaded
	 * <p>
	 * in addition to {@link #withLock(BlockManager, ThrowingRunnable, int, long...)} this method returns the value returned by {@code exec}
	 * 
	 * @param <T>
	 *            the exception type which can be thrown from {@code exec}
	 * @param <R>
	 *            the return type from {@code exec}
	 * @param bm
	 *            the block manager
	 * @param exec
	 *            the runnable to be executed
	 * @param block
	 *            the block to be loaded
	 * @return the value returned from {@code Patrick exec}
	 * @throws T
	 *             the exception type possibly thrown by runnable
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected static <T extends Throwable> long withLockLong(BlockManager bm, ThrowingLongSupplier <T> exec, long block) throws T, IOException {
		synchronized (bm) {
			long oblock = block;
			bm.getBlock(oblock);
			try {
				return exec.supply();
			} finally {
				bm.ungetBlock(oblock);
			}
		}
	}
	
	/**
	 * locks the array of blocks from {@code i} to to len<br>
	 * the blocks need to be sorted in ascending order. (multiple same blocks are permitted)
	 * <p>
	 * in addition to {@link #simpleWithLock(BlockManager, ThrowingRunnable, int, long...)} this method also loads the blocks from the arrays.<br>
	 * So this is the better method, when the blocks are often loaded and unloaded
	 * <p>
	 * in addition to {@link #withLock(BlockManager, ThrowingRunnable, int, long...)} this method returns the value returned by {@code exec}
	 * 
	 * @param <T>
	 *            the exception type which can be thrown from {@code exec}
	 * @param <R>
	 *            the return type from {@code exec}
	 * @param bm
	 *            the block manager
	 * @param exec
	 *            the runnable to be executed
	 * @param block
	 *            the block to be loaded
	 * @return the value returned from {@code Patrick exec}
	 * @throws T
	 *             the exception type possibly thrown by runnable
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected static <T extends Throwable> boolean withLockBoolean(BlockManager bm, ThrowingBooleanSupplier <T> exec, long block) throws T, IOException {
		synchronized (bm) {
			long oblock = block;
			bm.getBlock(oblock);
			try {
				return exec.supply();
			} finally {
				bm.ungetBlock(oblock);
			}
		}
	}
	
	@Override
	public <T extends Throwable> void withLock(ThrowingRunnable <T> exec) throws T, IOException {
		synchronized (bm) {
			long oldblock = block;
			bm.getBlock(oldblock);
			try {
				exec.execute();
			} finally {
				bm.ungetBlock(oldblock);
			}
		}
	}
	
	@Override
	public <T extends Throwable, R> R withLock(ThrowingSupplier <T, R> exec) throws T, IOException {
		synchronized (bm) {
			long oblock = block;
			bm.getBlock(oblock);
			try {
				return exec.supply();
			} finally {
				bm.ungetBlock(oblock);
			}
		}
	}
	
	@Override
	public <T extends Throwable> int withLockInt(ThrowingIntSupplier <T> exec) throws T, IOException {
		synchronized (bm) {
			long oblock = block;
			bm.getBlock(oblock);
			try {
				return exec.supply();
			} finally {
				bm.ungetBlock(oblock);
			}
		}
	}
	
	@Override
	public <T extends Throwable> long withLockLong(ThrowingLongSupplier <T> exec) throws T, IOException {
		synchronized (bm) {
			long oblock = block;
			bm.getBlock(oblock);
			try {
				return exec.supply();
			} finally {
				bm.ungetBlock(oblock);
			}
		}
	}
	
	@Override
	public <T extends Throwable> boolean withLockBoolean(ThrowingBooleanSupplier <T> exec) throws T, IOException {
		synchronized (bm) {
			long oblock = block;
			bm.getBlock(oblock);
			try {
				return exec.supply();
			} finally {
				bm.ungetBlock(oblock);
			}
		}
	}
	
}

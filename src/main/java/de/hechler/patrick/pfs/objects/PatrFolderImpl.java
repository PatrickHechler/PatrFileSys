package de.hechler.patrick.pfs.objects;

import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToInt;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToLong;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.intToByteArr;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.longToByteArr;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.*;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_FOLDER;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_LINK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_CREATE_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_FLAGS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LAST_META_MOD_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LAST_MOD_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LOCK_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LOCK_VALUE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_NAME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_OWNER;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_PARENT_ID;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_DATA_TABLE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_ELEMENT_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_OFFSET_ELEMENT_COUNT;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_OFFSET_FOLDER_ELEMENTS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LINK_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LINK_OFFSET_TARGET_ID;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_DELETE_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_READ_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_WRITE_ALLOWED_LOCK;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.interfaces.BlockManager;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.interfaces.PatrLink;


public class PatrFolderImpl extends PatrFileSysElementImpl implements PatrFolder {
	
	public PatrFolderImpl(PatrFileSysImpl fs, long startTime, BlockManager bm, long id) {
		super(fs, startTime, bm, id);
	}
	
	@Override
	public PatrFolder addFolder(String name, long lock) throws IOException, NullPointerException, ElementLockedException {
		Objects.requireNonNull(name, "null names are not permitted!");
		return simpleWithLock(() -> (PatrFolder) addElement(name, true, null, lock));
	}
	
	@Override
	public PatrFile addFile(String name, long lock) throws IOException, NullPointerException, ElementLockedException {
		Objects.requireNonNull(name, "null names are not permitted!");
		return simpleWithLock(() -> (PatrFile) addElement(name, false, null, lock));
	}
	
	@Override
	public PatrLink addLink(String name, PatrFileSysElement target, long lock) throws IOException, NullPointerException, ElementLockedException, IllegalArgumentException {
		Objects.requireNonNull(name, "null names are not permitted!");
		Objects.requireNonNull(target, "can not create a link with a null target!");
		if ( ! (target instanceof PatrFileSysElementImpl)) {
			throw new IllegalArgumentException("the tareget is of an unknown class: " + target.getClass().getName() + "");
		}
		PatrFileSysElementImpl t = (PatrFileSysElementImpl) target;
		return simpleWithLock(() -> (PatrLink) addElement(name, target.isFolder(), t, lock));
	}
	
	private PatrFileSysElement addElement(String name, boolean isFolder, PatrFileSysElementImpl target, long lock) throws NullPointerException, IOException, ElementLockedException {
		updatePosAndBlock();
		final long oldBlock = block;
		byte[] bytes = bm.getBlock(oldBlock);
		try {
			if (target != null && target.isLink()) {
				throw new IllegalArgumentException("i will not create a link to a link!");
			}
			ensureAccess(lock, LOCK_NO_WRITE_ALLOWED_LOCK, true);
			final int oldElementCount = getElementCount(),
				oldSize = oldElementCount * FOLDER_ELEMENT_LENGTH + FOLDER_OFFSET_FOLDER_ELEMENTS,
				newSize = oldSize + FOLDER_ELEMENT_LENGTH;
			try {
				final long oldPos = pos;
				pos = reallocate(oldBlock, pos, oldSize, newSize, true);
				if (pos != oldPos) {
					PatrFileSysImpl.setBlockAndPos(this, block, pos);
				}
			} catch (OutOfMemoryError e) {
				relocate();
			}
			bytes = bm.getBlock(block);
			try {
				int childPos = -1, childNamePos,
					childLen = target == null ? isFolder ? FOLDER_OFFSET_FOLDER_ELEMENTS : FILE_OFFSET_FILE_DATA_TABLE : LINK_LENGTH;
				long childBlock = block;
				byte[] childNameBytes = name.getBytes(StandardCharsets.UTF_16);
				try {
					childPos = allocate(childBlock, childLen);
					childNamePos = allocate(childBlock, childNameBytes.length + 2);
				} catch (OutOfMemoryError e) {
					if (childPos != -1) {
						reallocate(childBlock, childPos, childLen, 0, false);
					}
					childBlock = allocateOneBlock();
					byte[] cbytes = bm.getBlock(childBlock);
					try {
						PatrFileSysImpl.initBlock(cbytes, childLen + childNameBytes.length + 2);
						childPos = childNameBytes.length + 2;
						childNamePos = 0;
					} finally {
						bm.setBlock(childBlock);
					}
				}
				initChild(bm, id, getOwner(), isFolder, childPos, childNamePos, childBlock, childNameBytes, target);
				final long cid = PatrFileSysImpl.generateID(this, childBlock, childPos);
				int childOff = pos + FOLDER_OFFSET_FOLDER_ELEMENTS + oldElementCount * FOLDER_ELEMENT_LENGTH;
				longToByteArr(bytes, childOff, cid);
				intToByteArr(bytes, pos + FOLDER_OFFSET_ELEMENT_COUNT, oldElementCount + 1);
				modify(false);
				if (isFolder) {
					return new PatrFolderImpl(fs, startTime, bm, cid);
				} else {
					return new PatrFileImpl(fs, startTime, bm, cid);
				}
			} finally {
				bm.setBlock(block);
			}
		} finally {
			bm.setBlock(oldBlock);
		}
	}
	
	/**
	 * Initializes a new element.
	 * 
	 * @param bm
	 *            the block manager to be used
	 * @param parentID
	 *            the id of the parent element
	 * @param owner
	 *            the owner of the new element
	 * @param isFolder
	 *            if the new element is a folder
	 * @param childPos
	 *            the pos of the new element
	 * @param childNamePos
	 *            the pos of the name of the new element
	 * @param childBlock
	 *            the block number of the new element
	 * @param childNameBytes
	 *            the name of the new element without the null terminating character
	 * @param target
	 *            the target of the new link or <code>null</code> if the element to init is no link
	 * @throws IOException
	 *             if an IO error occurs
	 */
	public static void initChild(BlockManager bm, long parentID, int owner, boolean isFolder, int childPos, int childNamePos, long childBlock, byte[] childNameBytes, PatrFileSysElementImpl target) throws IOException {
		byte[] cbytes = bm.getBlock(childBlock);
		try {
			long time = System.currentTimeMillis();
			longToByteArr(cbytes, childPos + ELEMENT_OFFSET_CREATE_TIME, time);
			intToByteArr(cbytes, childPos + ELEMENT_OFFSET_FLAGS, (isFolder ? ELEMENT_FLAG_FOLDER : ELEMENT_FLAG_FILE) | (target == null ? 0 : ELEMENT_FLAG_LINK));
			longToByteArr(cbytes, childPos + ELEMENT_OFFSET_LAST_META_MOD_TIME, time);
			longToByteArr(cbytes, childPos + ELEMENT_OFFSET_LAST_MOD_TIME, time);
			longToByteArr(cbytes, childPos + ELEMENT_OFFSET_LOCK_TIME, -1);
			longToByteArr(cbytes, childPos + ELEMENT_OFFSET_LOCK_VALUE, LOCK_NO_LOCK);
			intToByteArr(cbytes, childPos + ELEMENT_OFFSET_NAME, childNamePos);
			intToByteArr(cbytes, childPos + ELEMENT_OFFSET_OWNER, owner);
			longToByteArr(cbytes, childPos + ELEMENT_OFFSET_PARENT_ID, parentID);
			if (childNameBytes != null) {
				System.arraycopy(childNameBytes, 0, cbytes, childNamePos, childNameBytes.length);
				cbytes[childNamePos + childNameBytes.length] = 0;
				cbytes[childNamePos + childNameBytes.length + 1] = 0;
			}
			if (target != null) {
				longToByteArr(cbytes, childPos + LINK_OFFSET_TARGET_ID, target.id);
			} else if (isFolder) {
				intToByteArr(cbytes, childPos + FOLDER_OFFSET_ELEMENT_COUNT, 0);
			} else {
				longToByteArr(cbytes, childPos + FILE_OFFSET_FILE_LENGTH, 0);
			}
		} finally {
			bm.setBlock(childBlock);
		}
	}
	
	@Override
	public void delete(long myLock, long parentLock) throws IllegalStateException, IOException, ElementLockedException {
		withLock(() -> {
			updatePosAndBlock();
			executeDelete(myLock, parentLock);
		});
	}
	
	private void executeDelete(long myLock, long parentLock) throws ClosedChannelException, IOException, ElementLockedException, OutOfMemoryError {
		ensureAccess(myLock, LOCK_NO_DELETE_ALLOWED_LOCK, true);
		getParent().ensureAccess(parentLock, LOCK_NO_WRITE_ALLOWED_LOCK, true);
		if (getElementCount() > 0) {
			throw new IllegalStateException("I can not delet myself, when I still have children");
		}
		deleteFromParent();
		freeName();
		reallocate(block, pos, FOLDER_OFFSET_FOLDER_ELEMENTS, 0, false);
		getParent().modify(false);
		PatrFileSysImpl.removeID(this);
	}
	
	@Override
	public boolean isRoot() {
		return id == ROOT_FOLDER_ID;
	}
	
	@Override
	public int elementCount(long lock) throws IOException {
		return withLockInt(() -> {
			updatePosAndBlock();
			ensureAccess(lock, LOCK_NO_READ_ALLOWED_LOCK, false);
			return getElementCount();
		});
	}
	
	protected int getElementCount() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			return byteArrToInt(bytes, pos + FOLDER_OFFSET_ELEMENT_COUNT);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public PatrFileSysElement getElement(int index, long lock) throws IOException, ElementLockedException {
		if (index < 0) {
			throw new IndexOutOfBoundsException("index is smaller than 0: index=" + index);
		}
		return simpleWithLock(() -> {
			updatePosAndBlock();
			return executeGetElement(index, lock);
		});
	}
	
	private PatrFileSysElement executeGetElement(int index, long lock) throws ClosedChannelException, IOException, ElementLockedException {
		byte[] bytes = bm.getBlock(block);
		try {
			ensureAccess(lock, LOCK_NO_READ_ALLOWED_LOCK, false);
			if (index >= getElementCount()) {
				throw new IndexOutOfBoundsException("the index is greather the my element count: index=" + index + " elementCount=" + getElementCount());
			}
			int off = FOLDER_OFFSET_FOLDER_ELEMENTS + index * FOLDER_ELEMENT_LENGTH;
			long cid = byteArrToLong(bytes, pos + off);
			return new PatrFileSysElementImpl(fs, startTime, bm, cid);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
}

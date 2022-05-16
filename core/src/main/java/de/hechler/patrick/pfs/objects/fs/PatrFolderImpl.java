package de.hechler.patrick.pfs.objects.fs;

import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToInt;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToLong;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.intToByteArr;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.longToByteArr;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.CHARSET;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_FILE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_FOLDER;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_FOLDER_SORTED;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_LINK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_CREATE_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_FLAGS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LAST_META_MOD_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LAST_MOD_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LOCK_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LOCK_VALUE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_NAME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_PARENT_ID;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_DATA_TABLE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_HASH_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_ELEMENT_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_OFFSET_ELEMENT_COUNT;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_OFFSET_FOLDER_ELEMENTS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LINK_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LINK_OFFSET_TARGET_ID;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_READ_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_WRITE_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ROOT_FOLDER_ID;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.exception.OutOfSpaceException;
import de.hechler.patrick.pfs.interfaces.BlockManager;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.interfaces.PatrLink;
import de.hechler.patrick.pfs.objects.LongInt;
import de.hechler.patrick.pfs.objects.jfs.PFSFileSystemProviderImpl;


public class PatrFolderImpl extends PatrFileSysElementImpl implements PatrFolder {
	
	public PatrFolderImpl(PatrFileSysImpl fs, long startTime, BlockManager bm, long id) {
		super(fs, startTime, bm, id);
	}
	
	@Override
	public PatrFolder addFolder(String name, long lock) throws IOException, NullPointerException, ElementLockedException {
		Objects.requireNonNull(name, "null names are not permitted!");
		return withLock(() -> (PatrFolder) executeAddElement(name, true, null, lock));
	}
	
	@Override
	public PatrFile addFile(String name, long lock) throws IOException, NullPointerException, ElementLockedException {
		Objects.requireNonNull(name, "null names are not permitted!");
		return withLock(() -> (PatrFile) executeAddElement(name, false, null, lock));
	}
	
	@Override
	public PatrLink addLink(String name, PatrFileSysElement target, long lock) throws IOException, NullPointerException, ElementLockedException, IllegalArgumentException {
		Objects.requireNonNull(name, "null names are not permitted!");
		Objects.requireNonNull(target, "can not create a link with a null target!");
		if ( ! (target instanceof PatrFileSysElementImpl)) {
			throw new IllegalArgumentException("the tareget is of an unknown class: " + target.getClass().getName() + "");
		}
		PatrFileSysElementImpl t = (PatrFileSysElementImpl) target;
		return withLock(() -> (PatrLink) executeAddElement(name, target.isFolder(), t, lock));
	}
	
	private PatrFileSysElement executeAddElement(String name, boolean isFolder, PatrFileSysElementImpl target, long lock) throws NullPointerException, IOException, ElementLockedException {
		fs.updateBlockAndPos(this);
		byte[] childNameBytes = name.getBytes(CHARSET);
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
			for (int i = 0, off = pos + FOLDER_OFFSET_FOLDER_ELEMENTS; i < oldElementCount; i ++ , off += FOLDER_ELEMENT_LENGTH) {
				long ocid = byteArrToLong(bytes, off);
				LongInt oc = fs.getBlockAndPos(ocid);
				byte[] ocbytes = bm.getBlock(oc.l);
				try {
					int ocnamepos = byteArrToInt(ocbytes, oc.i + ELEMENT_OFFSET_NAME);
					for (int ii = 0;; ii += 2) {
						if (ii >= childNameBytes.length) {
							if (ocbytes[ocnamepos + ii] == 0 && ocbytes[ocnamepos + ii + 1] == 0) {
								String fullName = PFSFileSystemProviderImpl.buildName(this) + '/' + name;
								throw new FileAlreadyExistsException(fullName, null, "there is already an element with the given name (name='" + name + "', fullName='" + fullName + "')!");
							}
						}
						if (ocbytes[ocnamepos + ii] != childNameBytes[ii]) {
							break;
						}
						if (ocbytes[ocnamepos + ii + 1] != childNameBytes[ii + 1]) {
							break;
						}
					}
				} finally {
					bm.ungetBlock(oc.l);
				}
			}
			resize(oldSize, newSize);
			bytes = bm.getBlock(block);
			try {
				int childPos = -1, childNamePos,
					childLen = target != null ? LINK_LENGTH : (isFolder ? FOLDER_OFFSET_FOLDER_ELEMENTS : FILE_OFFSET_FILE_DATA_TABLE);
				long childBlock = block;
				try {
					childPos = allocate(bm, childBlock, childLen);
					childNamePos = allocate(bm, childBlock, childNameBytes.length + 2);
				} catch (OutOfSpaceException e) {
					if (childPos != -1) {
						reallocate(childBlock, childPos, childLen, 0, false);
					}
					try {
						childBlock = allocateOneBlock();
					} catch (OutOfSpaceException e1) {
						resize(newSize, oldSize);
						throw e1;
					}
					byte[] cbytes = bm.getBlock(childBlock);
					try {
						PatrFileSysImpl.initBlock(cbytes, childLen + childNameBytes.length + 2);
						childPos = childNameBytes.length + 2;
						childNamePos = 0;
					} finally {
						bm.setBlock(childBlock);
					}
				}
				final long cid = PatrFileSysImpl.generateID(this, childBlock, childPos);
				initChild(bm, id, cid, isFolder, childPos, childNamePos, childBlock, childNameBytes, target);
				int childOff = pos + FOLDER_OFFSET_FOLDER_ELEMENTS + oldElementCount * FOLDER_ELEMENT_LENGTH;
				longToByteArr(bytes, childOff, cid);
				intToByteArr(bytes, pos + FOLDER_OFFSET_ELEMENT_COUNT, oldElementCount + 1);
				executeFlag(0, ELEMENT_FLAG_FOLDER_SORTED);
				executeModify(false);
				PatrFileSysElement result;
				if (target != null) result = new PatrLinkImpl(fs, startTime, bm, cid);
				else if (isFolder) result = new PatrFolderImpl(fs, startTime, bm, cid);
				else result = new PatrFileImpl(fs, startTime, bm, cid);
				return result;
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
	public static void initChild(BlockManager bm, long parentID, long childID, boolean isFolder, final int childPos, int childNamePos, long childBlock, byte[] childNameBytes, PatrFileSysElementImpl target)
		throws IOException {
		byte[] bytes = bm.getBlock(childBlock);
		try {
			long time = System.currentTimeMillis();
			longToByteArr(bytes, childPos + ELEMENT_OFFSET_CREATE_TIME, time);
			intToByteArr(bytes, childPos + ELEMENT_OFFSET_FLAGS, target == null ? (isFolder ? ELEMENT_FLAG_FOLDER : ELEMENT_FLAG_FILE) : ELEMENT_FLAG_LINK);
			longToByteArr(bytes, childPos + ELEMENT_OFFSET_LAST_META_MOD_TIME, time);
			longToByteArr(bytes, childPos + ELEMENT_OFFSET_LAST_MOD_TIME, time);
			longToByteArr(bytes, childPos + ELEMENT_OFFSET_LOCK_TIME, NO_TIME);
			longToByteArr(bytes, childPos + ELEMENT_OFFSET_LOCK_VALUE, NO_LOCK);
			intToByteArr(bytes, childPos + ELEMENT_OFFSET_NAME, childNamePos);
			longToByteArr(bytes, childPos + ELEMENT_OFFSET_PARENT_ID, parentID);
			if (childNameBytes != null) {
				System.arraycopy(childNameBytes, 0, bytes, childNamePos, childNameBytes.length);
				bytes[childNamePos + childNameBytes.length] = 0;
				bytes[childNamePos + childNameBytes.length + 1] = 0;
			}
			if (target != null) {
				longToByteArr(bytes, childPos + LINK_OFFSET_TARGET_ID, target.id);
			} else if (isFolder) {
				intToByteArr(bytes, childPos + FOLDER_OFFSET_ELEMENT_COUNT, 0);
			} else {
				longToByteArr(bytes, childPos + FILE_OFFSET_FILE_LENGTH, 0);
				longToByteArr(bytes, childPos + FILE_OFFSET_FILE_HASH_TIME, NO_TIME);
			}
		} finally {
			bm.setBlock(childBlock);
		}
	}
	
	@Override
	public boolean isRoot() {
		return id == ROOT_FOLDER_ID;
	}
	
	@Override
	public int elementCount(long lock) throws IOException {
		synchronized (bm) {
			fs.updateBlockAndPos(this);
			ensureAccess(lock, LOCK_NO_READ_ALLOWED_LOCK, false);
			return getElementCount();
		}
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
		synchronized (bm) {
			fs.updateBlockAndPos(this);
			return executeGetElement(index, lock);
		}
	}
	
	private PatrFileSysElement executeGetElement(int index, long lock) throws ClosedChannelException, IOException, ElementLockedException {
		byte[] bytes = bm.getBlock(block);
		try {
			ensureAccess(lock, LOCK_NO_READ_ALLOWED_LOCK, false);
			if (index >= getElementCount()) {
				throw new IndexOutOfBoundsException("the index is greather (or equal) than my element count: index=" + index + " elementCount=" + getElementCount());
			}
			int off = FOLDER_OFFSET_FOLDER_ELEMENTS + index * FOLDER_ELEMENT_LENGTH;
			long cid = byteArrToLong(bytes, pos + off);
			return new PatrFileSysElementImpl(fs, startTime, bm, cid);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public PatrFileSysElement getElement(String name, long lock) throws ElementLockedException, NoSuchFileException, IOException {
		synchronized (bm) {
			fs.updateBlockAndPos(this);
			byte[] bytes = bm.getBlock(block);
			try {
				ensureAccess(lock, LOCK_NO_READ_ALLOWED_LOCK, false);
				byte[] nameBytes = name.getBytes(CHARSET);
				int off = pos + FOLDER_OFFSET_FOLDER_ELEMENTS;
				int ec = getElementCount();
				for (int i = 0; i < ec; i ++ , off += FOLDER_ELEMENT_LENGTH) {
					long cid = byteArrToLong(bytes, off);
					LongInt cbp = fs.getBlockAndPos(cid);
					byte[] cbytes = bm.getBlock(cbp.l);
					try {
						int cno = byteArrToInt(cbytes, cbp.i + ELEMENT_OFFSET_NAME);
						if (cno == -1) {
							if (nameBytes.length != 0) {
								continue;
							}
						} else if (cno + nameBytes.length + 2 <= cbytes.length) {
							if ( !Arrays.equals(nameBytes, 0, nameBytes.length - 1, bytes, cno, cno + nameBytes.length - 1)) {
								continue;
							}
							if (cbytes[cno + nameBytes.length] != 0 || cbytes[cno + nameBytes.length + 1] != 0) {
								continue;
							}
						}
					} finally {
						bm.ungetBlock(cbp.l);
					}
					return new PatrFileSysElementImpl(fs, startTime, bm, cid);
				}
				throw new NoSuchElementException("I have no child element with the name: '" + name + "'");
			} finally {
				bm.ungetBlock(block);
			}
		}
	}
	
}

package de.hechler.patrick.pfs.objects;

import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToInt;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToLong;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.intToByteArr;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.longToByteArr;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_FLAG_FOLDER;
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
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_DATA_TABLE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_ELEMENT_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_ELEMENT_OFFSET_BLOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_ELEMENT_OFFSET_POS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_OFFSET_ELEMENT_COUNT;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FOLDER_OFFSET_FOLDER_ELEMENTS;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.OWNER_NO_OWNER;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import de.hechler.patrick.pfs.interfaces.BlockManager;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFolder;


public class PatrFolderImpl extends PatrFileSysElementImpl implements PatrFolder {
	
	public PatrFolderImpl(long startTime, BlockManager bm, long block, int pos) {
		super(startTime, bm, block, pos);
	}
	
	@Override
	public PatrFolder addFolder(String name) throws IOException, NullPointerException {
		return (PatrFolder) addElement(name, true);
	}
	
	@Override
	public PatrFile addFile(String name) throws IOException, NullPointerException {
		return (PatrFile) addElement(name, false);
	}
	
	private PatrFileSysElement addElement(String name, boolean isFolder) throws IOException {
		final long oldBlock = block;
		byte[] bytes = bm.getBlock(oldBlock);
		try {
			final int oldElementCount = elementCount(),
				oldSize = oldElementCount * FOLDER_ELEMENT_LENGTH + FOLDER_OFFSET_FOLDER_ELEMENTS,
				newSize = oldSize + FOLDER_ELEMENT_LENGTH;
			try {
				pos = reallocate(oldBlock, pos, oldSize, newSize, true);
			} catch (OutOfMemoryError e) {
				relocate();
			}
			int childPos = -1, childNamePos,
				childLen = isFolder ? FOLDER_OFFSET_FOLDER_ELEMENTS : FILE_OFFSET_FILE_DATA_TABLE;
			long childBlock = block;
			byte[] childNameBytes = name.getBytes(StandardCharsets.UTF_16);
			try {
				childPos = allocate(childBlock, childLen);
				childNamePos = allocate(childBlock, childNameBytes.length + 2);
			} catch (OutOfMemoryError e) {
				if (childPos == -1) {
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
			PatrFileSysElement result = initChild(isFolder, childPos, childNamePos, childBlock, childNameBytes);
			intToByteArr(bytes, pos + FOLDER_OFFSET_ELEMENT_COUNT, oldElementCount + 1);
			modify(false);
			return result;
		} finally {
			bm.setBlock(oldBlock);
		}
	}
	
	private PatrFileSysElement initChild(boolean isFolder, int childPos, int childNamePos, long childBlock, byte[] childNameBytes) throws IOException {
		byte[] cbytes = bm.getBlock(childBlock);
		try {
			long time = System.currentTimeMillis();
			longToByteArr(cbytes, childPos + ELEMENT_OFFSET_CREATE_TIME, time);
			intToByteArr(cbytes, childPos + ELEMENT_OFFSET_FLAGS, isFolder ? ELEMENT_FLAG_FOLDER : 0);
			longToByteArr(cbytes, childPos + ELEMENT_OFFSET_LAST_META_MOD_TIME, time);
			longToByteArr(cbytes, childPos + ELEMENT_OFFSET_LAST_MOD_TIME, time);
			longToByteArr(cbytes, childPos + ELEMENT_OFFSET_LOCK_TIME, -1);
			longToByteArr(cbytes, childPos + ELEMENT_OFFSET_LOCK_VALUE, LOCK_NO_LOCK);
			intToByteArr(cbytes, childPos + ELEMENT_OFFSET_NAME, childPos);
			intToByteArr(cbytes, childPos + ELEMENT_OFFSET_OWNER, OWNER_NO_OWNER);
			longToByteArr(cbytes, childPos + ELEMENT_OFFSET_PARENT_BLOCK, block);
			intToByteArr(cbytes, childPos + ELEMENT_OFFSET_PARENT_POS, pos);
			System.arraycopy(childNameBytes, 0, cbytes, childNamePos, childNameBytes.length);
			cbytes[childNamePos + childNameBytes.length] = 0;
			cbytes[childNamePos + childNameBytes.length + 1] = 0;
			if (isFolder) {
				intToByteArr(cbytes, childPos + FOLDER_OFFSET_ELEMENT_COUNT, 0);
				return new PatrFolderImpl(startTime, bm, childBlock, childPos);
			} else {
				longToByteArr(cbytes, childPos + FILE_OFFSET_FILE_LENGTH, 0);
				return new PatrFileImpl(startTime, bm, childBlock, childPos);
			}
		} finally {
			bm.setBlock(childBlock);
		}
	}
	
	public void delete() throws IllegalStateException, IOException {
		bm.getBlock(block);
		try {
			if (elementCount() > 0) {
				throw new IllegalStateException("I can not delet myself, when I still have children");
			}
			deleteFromParent();
			freeName();
			reallocate(block, pos, FOLDER_OFFSET_FOLDER_ELEMENTS, 0, false);
			getParent().modify(false);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public boolean isRoot() {
		return false;
	}
	
	@Override
	public int elementCount() throws IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			return byteArrToInt(bytes, pos + FOLDER_OFFSET_ELEMENT_COUNT);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public PatrFileSysElement getElement(int index) throws IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			int off = FOLDER_OFFSET_FOLDER_ELEMENTS + index * FOLDER_ELEMENT_LENGTH;
			long cblock = byteArrToLong(bytes, pos + off + FOLDER_ELEMENT_OFFSET_BLOCK);
			int cpos = byteArrToInt(bytes, pos + off + FOLDER_ELEMENT_OFFSET_POS);
			return new PatrFileSysElementImpl(startTime, bm, cblock, cpos);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
}

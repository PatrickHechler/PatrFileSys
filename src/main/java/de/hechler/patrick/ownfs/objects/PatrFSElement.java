package de.hechler.patrick.ownfs.objects;

import static de.hechler.patrick.ownfs.utils.ConvertNumByteArr.byteArrToInt;
import static de.hechler.patrick.ownfs.utils.ConvertNumByteArr.byteArrToLong;
import static de.hechler.patrick.ownfs.utils.ConvertNumByteArr.intToByteArr;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.*;

import java.io.IOException;

import de.hechler.patrick.ownfs.interfaces.BlockAccessor;
import de.hechler.patrick.ownfs.interfaces.PatrFile;
import de.hechler.patrick.ownfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.ownfs.interfaces.PatrFolder;

public class PatrFSElement implements PatrFileSysElement {
	
	protected final BlockAccessor ba;
	protected long                block;
	protected int                 pos;
	
	public PatrFSElement(BlockAccessor ba, long block, int pos) {
		this.ba = ba;
		this.block = block;
		this.pos = pos;
	}
	
	@Override
	public PatrFolder getParent() throws IllegalStateException, IOException {
		byte[] bytes = ba.loadBlock(block);
		try {
			long pblock = byteArrToLong(bytes, pos + ELEMENT_OFFSET_PARENT_BLOCK);
			int ppos = byteArrToInt(bytes, pos + ELEMENT_OFFSET_PARENT_POS);
			if (pblock == -1L) {
				assert ppos == -1;
				throw new IllegalStateException("this is the root folder!");
			}
			return new PatrFolderImpl(ba, pblock, ppos);
		} finally {
			ba.discardBlock(block);
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
		return new PatrFolderImpl(ba, block, pos);
	}
	
	@Override
	public PatrFile getFile() throws IllegalStateException, IOException {
		if ( !isFile()) {
			throw new IllegalStateException("this is no file!");
		}
		if (this instanceof PatrFile) {
			return (PatrFile) this;
		}
		return new PatrFileImpl(ba, block, pos);
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
	public byte[] getMetadata() throws IOException {
		byte[] bytes = ba.loadBlock(block);
		try {
			int len = byteArrToInt(bytes, pos + ELEMENT_OFFSET_METADATA_LENGTH);
			int off = byteArrToInt(bytes, pos + ELEMENT_OFFSET_METADATA_POS);
			byte[] result = new byte[len];
			System.arraycopy(bytes, off, result, 0, len);
			return result;
		} finally {
			ba.discardBlock(block);
		}
	}
	
	@Override
	public void setMetadata(byte[] data) throws IOException {
		setMetadata(data, false);
	}
	
	public void setMetadata(byte[] data, boolean alreadyRelocated) throws IOException {
		byte[] bytes = ba.loadBlock(block);
		try {
			final int oldpos = byteArrToInt(bytes, pos + ELEMENT_OFFSET_METADATA_POS), oldlen = byteArrToInt(bytes, pos + ELEMENT_OFFSET_METADATA_LENGTH);
			int newpos;
			if (oldlen != data.length) {
				try {
					if (oldlen > 0) {
						newpos = reallocate(bytes, oldpos, oldlen, data.length, false);
					} else {
						assert oldpos == -1;
						newpos = allocate(bytes, data.length);
					}
					if (oldpos != newpos) {
						intToByteArr(bytes, pos + ELEMENT_OFFSET_METADATA_POS, newpos);
					}
					intToByteArr(bytes, pos + ELEMENT_OFFSET_METADATA_LENGTH, data.length);
				} catch (OutOfMemoryError oome) {
					if (alreadyRelocated) {
						throw oome;
					}
					relocate(bytes, false);
					setMetadata(data, true);
					return;
				}
			} else {
				newpos = oldpos;
			}
			System.arraycopy(data, 0, bytes, newpos, data.length);
		} finally {
			ba.saveBlock(bytes, block);
		}
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
	
	public int getFlags() throws IOException {
		byte[] bytes = ba.loadBlock(block);
		try {
			return getFlags(bytes);
		} finally {
			ba.discardBlock(block);
		}
	}
	
	public int getFlags(byte[] bytes) {
		return byteArrToInt(bytes, pos + ELEMENT_OFFSET_FLAGS);
	}
	
	/**
	 * relocates this element to a new allocated block.
	 * 
	 * @param oldBolck
	 *            the bytes of the old block. they should be saved after this operation
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected void relocate(byte[] oldBolck, boolean copyMetadata) throws IOException {
		long newBlockNum;
		byte[] second = ba.loadBlock(1L);
		try {
			AllocatedBlocks[] allocate = allocate(second, 1L);
			assert allocate.length == 1;
			assert allocate[0].count == 1;
			newBlockNum = allocate[0].startBlock;
		} finally {
			ba.saveBlock(second, 1L);
		}
		int oldepos = pos, elen,
			oldmdpos, mdlen;
		if ( (getFlags(oldBolck) & ELEMENT_FLAG_FOLDER) != 0) {
			elen = byteArrToInt(oldBolck, oldepos + FOLDER_OFFSET_ELEMENT_COUNT);
			elen *= FOLDER_ELEMENT_LENGTH;
			elen += FOLDER_OFFSET_FOLDER_ELEMENTS;
		} else {
			long blocks = byteArrToLong(oldBolck, oldepos + FILE_OFFSET_FILE_LENGTH);
			for (elen = FILE_OFFSET_FILE_DATA_TABLE; blocks > 0; elen += 16) {
				long start = byteArrToLong(oldBolck, oldepos + elen);
				long end = byteArrToLong(oldBolck, oldepos + elen + 8);
				blocks -= end - start;
			}
			assert blocks == 0L;
		}
		if (copyMetadata) {
			oldmdpos = byteArrToInt(oldBolck, oldepos + ELEMENT_OFFSET_METADATA_POS);
			mdlen = byteArrToInt(oldBolck, oldepos + ELEMENT_OFFSET_METADATA_LENGTH);
		} else {
			oldmdpos = -1;
			mdlen = 0;
		}
		byte[] newBlock = ba.loadBlock(newBlockNum);
		try {
			PatrFileSysImpl.initBlock(newBlock, elen + mdlen);
			System.arraycopy(oldBolck, oldepos, newBlock, mdlen, elen);
			if (mdlen > 0) {
				System.arraycopy(oldBolck, oldmdpos, newBlock, 0, mdlen);
				intToByteArr(newBlock, mdlen + ELEMENT_OFFSET_METADATA_POS, 0);
			}
			pos = mdlen;
			block = newBlockNum;
		} finally {
			ba.saveBlock(newBlock, newBlockNum);
		}
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
	protected static AllocatedBlocks[] allocate(byte[] secondBlock, long len) throws OutOfMemoryError, IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * removes the given blocks from the allocated block table
	 * 
	 * @param remove
	 *            the blocks to remove
	 * @throws OutOfMemoryError
	 *             if there is not enough memory
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected static void free(byte[] secondBlock, AllocatedBlocks remove) throws OutOfMemoryError, IOException {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * allocate a block of bytes in the given block
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
	protected static int allocate(byte[] block, int len) throws OutOfMemoryError, IOException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/**
	 * reallocates the given bytes to the new len (or frees them if the new len is zero) and returns the new position of the bytes (or {@code -1} if they have been freed).<br>
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
	 * @return the new pos of the relocated bytes
	 * @throws OutOfMemoryError
	 *             if there is not enough memory
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected static int reallocate(byte[] block, int pos, int oldLen, int newLen, boolean copy) throws OutOfMemoryError, IOException {
		// TODO Auto-generated method stub
		return 0;
	}
	
}

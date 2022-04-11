package de.hechler.patrick.ownfs.objects.old;

import static de.hechler.patrick.ownfs.utils.ConvertNumByteArr.byteArrToInt;
import static de.hechler.patrick.ownfs.utils.ConvertNumByteArr.byteArrToLong;
import static de.hechler.patrick.ownfs.utils.ConvertNumByteArr.intToByteArr;
import static de.hechler.patrick.ownfs.utils.ConvertNumByteArr.longToByteArr;
import static de.hechler.patrick.ownfs.utils.PatrFileSysConstants.*;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;

import de.hechler.patrick.ownfs.interfaces.BlockManager;
import de.hechler.patrick.ownfs.interfaces.PatrFile;
import de.hechler.patrick.ownfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.ownfs.interfaces.PatrFolder;
import de.hechler.patrick.ownfs.objects.AllocatedBlocks;

public class PatrFSElement implements PatrFileSysElement {
	
	protected final BlockManager bm;
	protected long               block;
	protected int                pos;
	
	public PatrFSElement(BlockManager bm, long block, int pos) {
		this.bm = bm;
		this.block = block;
		this.pos = pos;
	}
	
	@Override
	public PatrFolder getParent() throws IllegalStateException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			long pblock = byteArrToLong(bytes, pos + ELEMENT_OFFSET_PARENT_BLOCK);
			int ppos = byteArrToInt(bytes, pos + ELEMENT_OFFSET_PARENT_POS);
			if (pblock == -1L) {
				assert ppos == -1;
				throw new IllegalStateException("this is the root folder!");
			}
			return new PatrFolderImpl(bm, pblock, ppos);
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
		return new PatrFolderImpl(bm, block, pos);
	}
	
	@Override
	public PatrFile getFile() throws IllegalStateException, IOException {
		if ( !isFile()) {
			throw new IllegalStateException("this is no file!");
		}
		if (this instanceof PatrFile) {
			return (PatrFile) this;
		}
		return new PatrFileImpl(bm, block, pos);
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
	
	public int getFlags() throws IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			return byteArrToInt(bytes, pos + ELEMENT_OFFSET_FLAGS);
		} finally {
			bm.ungetBlock(block);
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
		long newBlockNum;
		newBlockNum = allocateOneBlock();
		byte[] oldBlock = bm.getBlock(oldBlockNum);
		try {
			int oldepos = pos, elen,
				oldnamepos, namelen;
			if ( (getFlags() & ELEMENT_FLAG_FOLDER) != 0) {
				elen = byteArrToInt(oldBlock, oldepos + FOLDER_OFFSET_ELEMENT_COUNT);
				elen *= FOLDER_ELEMENT_LENGTH;
				elen += FOLDER_OFFSET_FOLDER_ELEMENTS;
			} else {
				long blocks = byteArrToLong(oldBlock, oldepos + FILE_OFFSET_FILE_LENGTH);
				for (elen = FILE_OFFSET_FILE_DATA_TABLE; blocks > 0; elen += 16) {
					long start = byteArrToLong(oldBlock, oldepos + elen);
					long end = byteArrToLong(oldBlock, oldepos + elen + 8);
					blocks -= end - start;
				}
				assert blocks == 0L;
			}
			oldnamepos = byteArrToInt(oldBlock, oldepos + ELEMENT_OFFSET_METADATA_POS);
			namelen = byteArrToInt(oldBlock, oldepos + ELEMENT_OFFSET_METADATA_LENGTH);
			byte[] newBlock = bm.getBlock(newBlockNum);
			try {
				PatrFileSysImpl.initBlock(newBlock, elen + namelen);
				System.arraycopy(oldBlock, oldepos, newBlock, namelen, elen);
				if (namelen > 0) {
					System.arraycopy(oldBlock, oldnamepos, newBlock, 0, namelen);
					intToByteArr(newBlock, namelen + ELEMENT_OFFSET_METADATA_POS, 0);
				}
				pos = namelen;
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
				endBlock += len;
				len -= len;
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
	 * removes the given blocks from the allocated block table
	 * 
	 * @param remove
	 *            the blocks to remove
	 * @throws OutOfMemoryError
	 *             if there is not enough memory
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected void free(AllocatedBlocks remove) throws OutOfMemoryError, IOException {
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
	protected int allocate(int len) throws OutOfMemoryError, IOException {
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
	protected int reallocate(int pos, int oldLen, int newLen, boolean copy) throws OutOfMemoryError, IOException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int getOwner() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public void setOwner(int owner) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public long getCreateTime() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public long getLastModTime() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public long getLock() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public void removeLock(long lock) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	protected int getNameLen() throws IOException, IllegalStateException {
		byte[] bytes = bm.getBlock(block);
		try {
			int np = byteArrToInt(bytes, pos + ELEMENT_OFFSET_NAME);
			if (np == -1) {
				throw new IllegalStateException("this element has no name!");
			}
			int len;
			for (len = 0; bytes[np] != 0; len ++ ) {}
			return len;
		} finally {
			bm.ungetBlock(block);
		}
	}
	
}

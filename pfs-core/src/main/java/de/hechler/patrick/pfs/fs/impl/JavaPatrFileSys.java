package de.hechler.patrick.pfs.fs.impl;

import static de.hechler.patrick.pfs.other.PatrFileSysConstants.B0.MAGIC;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.B0.OFF_BLOCK_COUNT;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.B0.OFF_BLOCK_SIZE;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.B0.OFF_BLOCK_TABLE_FIRST_BLOCK;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.B0.OFF_ROOT_BLOCK;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.B0.OFF_ROOT_POS;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.B0.SIZE;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.BlockFlags.ENTRIES;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.BlockFlags.USED;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.BlockFlags.USED_BIT;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.OFF_LAST_MODIFY_TIME;

import java.io.IOException;
import java.nio.ByteBuffer;

import de.hechler.patrick.pfs.bm.BlockManager;
import de.hechler.patrick.pfs.exceptions.PFSErr;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.folder.impl.JavaPatrFileSysFolder;
import de.hechler.patrick.pfs.fs.PFS;
import de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder;
import de.hechler.patrick.pfs.other.Place;

@SuppressWarnings("exports")
public class JavaPatrFileSys implements PFS {
	
	public final BlockManager          bm;
	public final JavaPatrFileSysFolder root;
	
	private JavaPatrFileSys(BlockManager bm) {
		this.bm = bm;
		this.root = new JavaPatrFileSysFolder(this, null, new Place( -1L, -1), null, -1);
	}
	
	@Override
	public void format(long blockCount) throws IOException {
		if (bm.blockSize() < (SIZE + Folder.EMPTY_SIZE + (Folder.Entry.SIZE * 2) + 30)) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ILLEGAL_ARG, "block size is too small");
		}
		if ( (bm.blockSize() & 7) != 0) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ILLEGAL_ARG,
				"block size % 8 != 0 (blockSize=" + bm.blockSize() + ')');
		}
		if (blockCount < 2) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ILLEGAL_ARG, "block count < 2 (blockCount="
				+ blockCount + ')');
		}
		ByteBuffer b0 = bm.get(0L);
		try {
			int table_off = b0.capacity() - 20;
			b0.putInt(table_off, 0);
			b0.putInt(table_off + 4, SIZE);
			b0.putInt(table_off + 8, SIZE);
			b0.putInt(table_off + 12, SIZE + Folder.EMPTY_SIZE);
			b0.putInt(table_off + 16, table_off);
			b0.putLong(0, MAGIC);
			b0.putLong(OFF_ROOT_BLOCK, 0L);
			b0.putInt(OFF_ROOT_POS, SIZE);
			b0.putInt(OFF_BLOCK_SIZE, b0.capacity());
			b0.putLong(OFF_BLOCK_COUNT, blockCount);
			if (bm.flagsPerBlock() > USED_BIT) {
				b0.putLong(OFF_BLOCK_TABLE_FIRST_BLOCK, -1L);
				bm.deleteAllFlags();
				bm.flag(0L, USED | ENTRIES);
			} else {
				b0.putLong(OFF_BLOCK_TABLE_FIRST_BLOCK, 1L);
				ByteBuffer b1 = bm.get(1L);
				try {
					for (int i = 0; i < b1.capacity(); i += 8) {
						b1.putLong(i, 0L);
					}
					b1.put(0, (byte) 3);
					b1.putLong(b1.capacity() - 8, -1L);
				} finally {
					bm.set(1L);
				}
			}
			long time = System.currentTimeMillis() / 1000;
			b0.putLong(SIZE + OFF_LAST_MODIFY_TIME, time);
			b0.putLong(SIZE + Folder.OFF_PARENT_BLOCK, -1L);
			b0.putInt(SIZE + Folder.OFF_PARENT_POS, -1);
			b0.putInt(SIZE + Folder.OFF_DIRECT_CHILD_COUNT, 0);
			b0.putLong(SIZE + Folder.OFF_ENTRY_BLOCK, -1L);
			b0.putInt(SIZE + Folder.OFF_ENTRY_POS, -1);
			b0.putInt(SIZE + Folder.OFF_HELPER_INDEX, -1);
		} finally {
			bm.set(0L);
		}
	}
	
	@Override
	public PFSFolder root() throws IOException {
		ByteBuffer b0 = bm.get(0L);
		try {
			root.element.block = b0.getLong(OFF_ROOT_BLOCK);
			root.element.pos = b0.getInt(OFF_ROOT_POS);
			return root;
		} finally {
			bm.unget(0L);
		}
	}
	
	@Override
	public long blockCount() throws IOException {
		ByteBuffer b0 = bm.get(0L);
		try {
			return b0.getLong(OFF_BLOCK_COUNT);
		} finally {
			bm.unget(0L);
		}
	}
	
	@Override
	public int blockSize() throws IOException {
		ByteBuffer b0 = bm.get(0L);
		try {
			return b0.getInt(OFF_BLOCK_SIZE);
		} finally {
			bm.unget(0L);
		}
	}
	
	@Override
	public void close() throws IOException {
		bm.close();
	}
	
	public long allocateBlock(long flags) throws IOException {
		ByteBuffer b0 = bm.get(0L);
		try {
			long blockCount = b0.getLong(OFF_BLOCK_COUNT);
			long btfb = b0.getLong(OFF_BLOCK_TABLE_FIRST_BLOCK);
			if (btfb == -1L) {
				long block = bm.firstZeroFlaggedBlock();
				blockLimitCheck(blockCount, block);
				bm.flag(block, flags);
				return block;
			} else {
				long current = btfb;
				long block = 0L;
				ByteBuffer data = bm.get(current);
				try {
					while (true) {
						for (int off = 0; off < data.capacity() - 8;) {
							long l = data.getLong(off);
							if (l == -1L) {
								block += 64;
								off += 8;
								blockLimitCheck(blockCount, block);
								continue;
							}
							for (int i = 0;; i ++ ) {
								if (l != (l | (1 << i))) {
									blockLimitCheck(blockCount, block + i);
									data.putLong(off, l | 1 << i);
									return block + i;
								}
							}
						}
						blockLimitCheck(blockCount, block + 1);
						long next = data.getLong(data.capacity() - 8);
						if (next != -1L) {
							bm.unget(current);
							current = next;
							data = bm.get(current);
						} else {
							long old = current;
							data.putLong(data.capacity() - 8, block);
							data = bm.get(block);
							current = block;
							bm.set(old);
							for (int off = 0; off < data.capacity() - 8; off += 8) {
								data.putLong(off, 0L);
							}
							data.put((byte) 3);
							data.putLong(data.capacity() - 8, -1L);
							return block + 1;
						}
					}
				} finally {
					bm.set(current);
				}
			}
		} finally {
			bm.unget(0L);
		}
	}
	
	private static void blockLimitCheck(long blockCount, long block) throws IOException {
		if (block >= blockCount) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_OUT_OF_SPACE,
				"reached block limit (increase block count to allocate more blocks)");
		}
	}
	
	public void freeBlock(long block) throws IOException {
		ByteBuffer b0 = bm.get(0L);
		try {
			long btfb = b0.getLong(OFF_BLOCK_TABLE_FIRST_BLOCK);
			if (btfb == -1L) {
				bm.flag(block, 0L);
			} else {
				Place p = find(btfb, block >> 3);
				ByteBuffer data = bm.get(p.block);
				try {
					bm.unget(p.block); // find does not release the last block
					assert p.pos < data.capacity() - 8;
					byte b = data.get(p.pos);
					b &= ~ (1 << (block & 3));
					data.put(p.pos, b);
				} finally {
					bm.set(p.block);
				}
			}
		} finally {
			bm.unget(0L);
		}
	}
	
	public Place find(long firstBlock, long length) throws IOException {
		Place current = new Place(firstBlock, 0);
		int dataLen = bm.blockSize() - 8;
		while (true) {
			ByteBuffer data = bm.get(current.block);
			try {
				length -= dataLen;
				long next = data.getLong(dataLen);
				if (length > 0L || next != -1L && length == 0L) {
					bm.unget(current.block);
					current.block = next;
					bm.get(current.block);
				} else {
					current.pos = (int) (length + dataLen);
					return current;
				}
			} catch (Throwable t) {
				bm.unget(current.block);
				throw t;
			}
		}
	}
	
}

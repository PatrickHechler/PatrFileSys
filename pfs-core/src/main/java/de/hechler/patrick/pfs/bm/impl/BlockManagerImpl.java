package de.hechler.patrick.pfs.bm.impl;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import de.hechler.patrick.pfs.bm.BlockAccessor;
import de.hechler.patrick.pfs.bm.BlockManager;
import de.hechler.patrick.pfs.exceptions.PFSErr;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;

public class BlockManagerImpl implements BlockManager {
	
	private final BlockAccessor           ba;
	private final Map <Long, LoadedBlock> blocks;
	private volatile boolean              closed;
	
	public BlockManagerImpl(BlockAccessor ba) {
		this.ba = ba;
		this.blocks = new HashMap <>();
	}
	
	@Override
	public int blockSize() {
		return ba.blockSize();
	}
	
	@Override
	public ByteBuffer get(long block) throws PatrFileSysException {
		synchronized (blocks) {
			ensureOpen();
			LoadedBlock lb = blocks.get(block);
			if (lb != null) {
				lb.loadCount ++ ;
				return lb.value;
			}
			ByteBuffer data = ba.loadBlock(block);
			lb = new LoadedBlock(data);
			blocks.put(block, lb);
			return data;
		}
	}
	
	@Override
	public void unget(long block) throws IllegalStateException, PatrFileSysException {
		synchronized (blocks) {
			ensureOpen();
			Long blockObj = (Long) block;
			LoadedBlock lb = blocks.get(blockObj);
			if (lb == null) {
				throw new IllegalStateException("this block is not loaded: " + block);
			}
			lb.loadCount -- ;
			if (lb.loadCount <= 0) {
				if (lb.needSave) {
					ba.saveBlock(lb.value, block);
				} else {
					ba.discardBlock(block);
				}
				assert lb.loadCount == 0;
				LoadedBlock rem = blocks.remove(blockObj);
				assert rem == lb;
			}
		}
	}
	
	@Override
	public void set(long block) throws PatrFileSysException, IllegalStateException {
		synchronized (blocks) {
			ensureOpen();
			Long blockObj = (Long) block;
			LoadedBlock lb = blocks.get(blockObj);
			if (lb == null) {
				throw new IllegalStateException("this block is not loaded: " + block);
			}
			lb.loadCount -- ;
			lb.needSave = true;
			if (lb.loadCount <= 0) {
				ba.saveBlock(lb.value, block);
				assert lb.loadCount == 0;
				LoadedBlock rem = blocks.remove(blockObj);
				assert rem == lb;
			}
		}
	}
	
	@Override
	public void close() throws PatrFileSysException {
		synchronized (blocks) {
			if (closed) {
				return;
			}
			ba.close();
		}
	}
	
	public void ensureOpen() throws PatrFileSysException {
		if (closed) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_CLOSED, "block manager is closed");
		}
	}
	
	@Override
	public void sync() throws PatrFileSysException {
		ba.sync();
	}
	
	@Override
	public long flags(long block) throws PatrFileSysException {
		return ba.flags(block);
	}
	
	@Override
	public void flag(long block, long flags) throws PatrFileSysException {
		ba.flag(block, flags);
	}
	
	@Override
	public long firstZeroFlaggedBlock() throws PatrFileSysException {
		return ba.firstZeroFlaggedBlock();
	}
	
	@Override
	public void deleteAllFlags() throws PatrFileSysException {
		ba.deleteAllFlags();
	}
	
	private static class LoadedBlock {
		
		private final ByteBuffer value;
		private volatile int     loadCount;
		private volatile boolean needSave;
		
		public LoadedBlock(ByteBuffer value) {
			this.value = value;
			this.loadCount = 1;
			this.needSave = false;
		}
		
	}
	
}


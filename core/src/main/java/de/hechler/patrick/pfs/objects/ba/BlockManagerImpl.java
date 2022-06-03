package de.hechler.patrick.pfs.objects.ba;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.hechler.patrick.pfs.interfaces.BlockAccessor;
import de.hechler.patrick.pfs.interfaces.BlockManager;


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
	public byte[] getBlock(long block) throws ClosedChannelException, IOException {
		synchronized (blocks) {
			ensureOpen();
			LoadedBlock lb = blocks.get(block);
			if (lb != null) {
				lb.loadCount ++ ;
				return lb.value;
			}
			byte[] bytes = ba.loadBlock(block);
			lb = new LoadedBlock(bytes);
			blocks.put(block, lb);
			return bytes;
		}
	}
	
	@Override
	public void ungetBlock(long block) throws IllegalStateException, IOException, ClosedChannelException {
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
	public void setBlock(long block) throws ClosedChannelException, IOException, IllegalStateException {
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
	public void close() throws IOException {
		synchronized (blocks) {
			if (closed) {
				return;
			}
			ba.close();
			blocks.clear();
		}
	}
	
	@Override
	public void saveAll() throws ClosedChannelException, IOException {
		synchronized (blocks) {
			ensureOpen();
			for (Entry <Long, LoadedBlock> entry : blocks.entrySet()) {
				long block = entry.getKey();
				LoadedBlock lb = entry.getValue();
				ba.saveBlock(lb.value, block);
			}
			blocks.clear();
		}
	}
	
	@Override
	public void discardAll() throws ClosedChannelException {
		synchronized (blocks) {
			ensureOpen();
			ba.discardAll();
			blocks.clear();
		}
	}
	
	public void ensureOpen() throws ClosedChannelException {
		if (closed) {
			throw new ClosedChannelException();
		}
	}
	
	private static class LoadedBlock {
		
		private final byte[]     value;
		private volatile int     loadCount;
		private volatile boolean needSave;
		
		public LoadedBlock(byte[] value) {
			this.value = value;
			this.loadCount = 1;
			this.needSave = false;
		}

		@Override
		public String toString() {
			return "LoadedBlock [loadCount=" + loadCount + ", needSave=" + needSave + "]";
		}
		
	}
	
}

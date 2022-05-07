package de.hechler.patrick.pfs.objects.ba;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.hechler.patrick.pfs.interfaces.BlockAccessor;


public class CheckedBlockAccessor implements BlockAccessor {
	
	private final Map <Long, byte[]> loaded;
	private final BlockAccessor      ba;
	private volatile boolean         closed;
	
	public CheckedBlockAccessor(BlockAccessor ba) {
		this.loaded = new HashMap <>();
		this.ba = ba;
		this.closed = false;
	}
	
	@Override
	public int blockSize() {
		return ba.blockSize();
	}
	
	@Override
	public byte[] loadBlock(long block) throws IllegalArgumentException, IOException, ClosedChannelException {
		Long blockObj = (Long) block;
		byte[] bytes;
		synchronized (loaded) {
			checkOpen();
			if (loaded.containsKey(blockObj)) {
				throw new IllegalArgumentException("the block " + block + " is already loaded!");
			}
			bytes = ba.loadBlock(block);
			loaded.put(blockObj, bytes);
		}
		return bytes;
	}
	
	@Override
	public void saveBlock(byte[] value, long block) throws IllegalArgumentException, IOException, ClosedChannelException {
		Long blockObj = (Long) block;
		synchronized (loaded) {
			checkOpen();
			byte[] bytes = loaded.remove(blockObj);
			if (value != bytes) {
				if (bytes == null) {
					throw new IllegalArgumentException("the block " + block + " has not been loaded!");
				} else {
					throw new IllegalArgumentException("the block " + block + " has been loaded with a diffrent array!");
				}
			}
			ba.saveBlock(value, block);
		}
	}
	
	@Override
	public void discardBlock(long block) throws ClosedChannelException {
		synchronized (loaded) {
			checkOpen();
			if (loaded.remove(block) == null) {
				throw new IllegalArgumentException("the block " + block + " has not been loaded!");
			}
		}
	}
	
	@Override
	public void close() {
		if ( !closed) {
			ba.close();
			closed = true;
		}
	}
	
	@Override
	public void saveAll() throws IOException, ClosedChannelException {
		synchronized (loaded) {
			checkOpen();
			for (Entry <Long, byte[]> e : loaded.entrySet()) {
				ba.saveBlock(e.getValue(), e.getKey());
			}
			loaded.clear();
		}
	}
	
	@Override
	public void discardAll() throws ClosedChannelException {
		synchronized (loaded) {
			checkOpen();
			for (Entry <Long, byte[]> e : loaded.entrySet()) {
				ba.discardBlock(e.getKey());
			}
			loaded.clear();
		}
	}
	
	public void checkOpen() throws ClosedChannelException {
		if (closed) {
			throw new ClosedChannelException();
		}
	}
	
}

package de.hechler.patrick.ownfs.objects;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.hechler.patrick.ownfs.interfaces.BlockAccessor;

public class BlockAccessorByteArrayArrayImpl implements BlockAccessor {
	
	private byte[][]              blocks;
	private Map <Integer, byte[]> loaded;
	
	
	public BlockAccessorByteArrayArrayImpl(int blockCnt, int blockSize) {
		this.blocks = new byte[blockCnt][blockSize];
		this.loaded = new HashMap <>();
	}
	
	@Override
	public int blockSize() {
		return this.blocks[0].length;
	}
	
	@Override
	public byte[] loadBlock(final long block) throws IOException, IndexOutOfBoundsException, ClosedChannelException {
		if (this.loaded == null) {
			throw new ClosedChannelException();
		}
		if (block >= this.blocks.length || block < 0) {
			throw new IndexOutOfBoundsException("blockcount=" + this.blocks.length + " block=" + block);
		}
		final int intblock = (int) block;
		final byte[] clone = this.blocks[intblock].clone();
		final byte[] old = loaded.putIfAbsent(Integer.valueOf(intblock), clone);
		if (old != null) {
			throw new IOException("block already loaded");
		}
		return clone;
	}
	
	@Override
	public void saveBlock(byte[] value, long block) throws IOException, ClosedChannelException {
		if (this.loaded == null) {
			throw new ClosedChannelException();
		}
		if (block >= this.blocks.length) {
			throw new IndexOutOfBoundsException("blockcount=" + this.blocks.length + " block=" + block);
		}
		final int intblock = (int) block;
		final boolean removed = loaded.remove(Integer.valueOf(intblock), value);
		if ( !removed) {
			throw new IOException("block not loaded");
		}
		System.arraycopy(value, 0, this.blocks[intblock], 0, value.length);
	}
	
	@Override
	public void unloadBlock(long block) throws ClosedChannelException {
		if (this.loaded == null) {
			throw new ClosedChannelException();
		}
		if (block >= this.blocks.length) {
			throw new IndexOutOfBoundsException("blockcount=" + this.blocks.length + " block=" + block);
		}
		final int intblock = (int) block;
		final byte[] removed = loaded.remove(Integer.valueOf(intblock));
		if (removed == null) {
			throw new IllegalStateException("block not loaded");
		}
	}
	
	@Override
	public void close() {
		this.loaded = null;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void saveAll() throws IOException, ClosedChannelException {
		if (this.loaded == null) {
			throw new ClosedChannelException();
		}
		for (Entry <Integer, byte[]> e : this.loaded.entrySet().toArray(new Entry[this.loaded.size()])) {
			saveBlock(e.getValue(), e.getKey().intValue());
		}
	}
	
	@Override
	public void unloadAll() throws ClosedChannelException {
		if (this.loaded == null) {
			throw new ClosedChannelException();
		}
		this.loaded.clear();
	}
	
}

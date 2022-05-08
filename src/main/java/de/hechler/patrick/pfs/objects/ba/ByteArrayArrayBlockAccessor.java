package de.hechler.patrick.pfs.objects.ba;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

import de.hechler.patrick.pfs.interfaces.BlockAccessor;

public class ByteArrayArrayBlockAccessor implements BlockAccessor {
	
	private final byte[][] blocks;
	
	public ByteArrayArrayBlockAccessor(int blockCount, int blockSize) {
		this.blocks = new byte[blockCount][blockSize];
	}
	
	@Override
	public int blockSize() {
		return blocks[0].length;
	}
	
	@Override
	public byte[] loadBlock(long block) throws IOException, ClosedChannelException {
		return blocks[(int) block].clone();
	}
	
	@Override
	public void saveBlock(byte[] value, long block) throws IOException, ClosedChannelException {
		System.arraycopy(value, 0, blocks[(int) block], 0, blocks[0].length);
	}
	
	@Override
	public void discardBlock(long block) throws ClosedChannelException {}
	
	@Override
	public void close() {}
	
	@Override
	public void saveAll() throws IOException, ClosedChannelException {
		throw new UnsupportedOperationException("can not save all");
	}
	
	@Override
	public void discardAll() throws ClosedChannelException {}

	@Override
	public BlockAccessor subAccessor(long offset, long length) {
		return new SubAccessor(this, offset, length) {
			
			@Override
			public void saveAll() throws UnsupportedOperationException, IOException, ClosedChannelException {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public void discardAll() throws ClosedChannelException {
				throw new UnsupportedOperationException();
			}
		};
	}
	
}

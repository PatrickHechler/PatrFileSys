package de.hechler.patrick.pfs.objects.ba;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.ClosedChannelException;

import de.hechler.patrick.pfs.interfaces.BlockAccessor;


public class FileBlockAccessor implements BlockAccessor {
	
	private final int              blockSize;
	private final RandomAccessFile file;
	
	public FileBlockAccessor(int blockSize, RandomAccessFile file) {
		this.blockSize = blockSize;
		this.file = file;
	}
	
	@Override
	public int blockSize() {
		return blockSize;
	}
	
	@Override
	public byte[] loadBlock(long block) throws IOException, ClosedChannelException {
		byte[] bytes = new byte[blockSize];
		synchronized (file) {
			file.seek(block * (long) blockSize);
			file.read(bytes);
		}
		return bytes;
	}
	
	@Override
	public void saveBlock(byte[] value, long block) throws UnsupportedOperationException, IOException, ClosedChannelException {
		synchronized (file) {
			file.seek(block * (long) blockSize);
			file.write(value);
		}
	}
	
	@Override
	public void discardBlock(long block) throws ClosedChannelException {}
	
	@Override
	public void close() {
		try {
			file.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void saveAll() throws IOException, ClosedChannelException {
		throw new UnsupportedOperationException("save all is not supporteds");
	}
	
	@Override
	public void discardAll() throws ClosedChannelException {}

}

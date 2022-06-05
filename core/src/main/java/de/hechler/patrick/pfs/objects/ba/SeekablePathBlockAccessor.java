package de.hechler.patrick.pfs.objects.ba;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import de.hechler.patrick.pfs.interfaces.BlockAccessor;

public class SeekablePathBlockAccessor implements BlockAccessor {
	
	private final SeekableByteChannel channel;
	private final int                 blockSize;
	
	public SeekablePathBlockAccessor(Path path, int blockSize) throws IOException {
		this(Files.newByteChannel(path, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE), blockSize);
	}
	
	public SeekablePathBlockAccessor(SeekableByteChannel channel, int blockSize) {
		this.channel = channel;
		this.blockSize = blockSize;
	}
	
	@Override
	public int blockSize() {
		return blockSize;
	}
	
	@Override
	public byte[] loadBlock(long block) throws IOException, ClosedChannelException {
		byte[] bytes = new byte[blockSize];
		long pos = block * blockSize;
		channel.position(pos);
		channel.read(ByteBuffer.wrap(bytes));
		return bytes;
	}
	
	@Override
	public void saveBlock(byte[] value, long block) throws IOException, ClosedChannelException {
		long pos = block * blockSize;
		channel.position(pos);
		channel.write(ByteBuffer.wrap(value));
	}
	
	@Override
	public void discardBlock(long block) throws ClosedChannelException {}
	
	@Override
	public void close() {
		try {
			channel.close();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	
	@Override
	public void saveAll() throws UnsupportedOperationException, IOException, ClosedChannelException {
		throw new UnsupportedOperationException("save all");
	}
	
	@Override
	public void discardAll() throws ClosedChannelException {}
	
}

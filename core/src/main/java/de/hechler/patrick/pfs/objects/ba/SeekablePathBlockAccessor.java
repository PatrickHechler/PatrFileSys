package de.hechler.patrick.pfs.objects.ba;

import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToInt;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import de.hechler.patrick.pfs.interfaces.BlockAccessor;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;

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
	
	private static final OpenOption[] OPEN_EXISTING_READ_ONLY  = new OpenOption[] {StandardOpenOption.READ };
	private static final OpenOption[] OPEN_EXISTING_READ_WRITE = new OpenOption[] {StandardOpenOption.READ, StandardOpenOption.WRITE };
	private static final OpenOption[] OPEN_NEW_READ_ONLY       = new OpenOption[] {StandardOpenOption.CREATE_NEW, StandardOpenOption.READ };
	private static final OpenOption[] OPEN_NEW_READ_WRITE      = new OpenOption[] {StandardOpenOption.CREATE_NEW, StandardOpenOption.READ, StandardOpenOption.WRITE };
	
	/**
	 * creates a new {@link SeekablePathBlockAccessor}.
	 * <p>
	 * this method tries at first to open a byte channel to a existing file.<br>
	 * if the file already exists it tries to read the block size from the existing file system.<br>
	 * else it tries to create a new file with the <code>defaultBlockSize</code><br>
	 * if the {@code defaultBlockSize} is {@code -1} the creation of the new file will fail.
	 * 
	 * @param path
	 *            the path of the patr-file-system {@link BlockAccessor}
	 * @param defaultBlockSize
	 *            the default block size if the file does not already exist or {@code -1}
	 * @return the new created {@link SeekablePathBlockAccessor}
	 * @throws IOException
	 *             if an IO-error occurs
	 */
	public static SeekablePathBlockAccessor create(Path path, int defaultBlockSize, boolean readOnly, Bool created) throws IOException {
		try {
			SeekableByteChannel channel = Files.newByteChannel(path, readOnly ? OPEN_EXISTING_READ_ONLY : OPEN_EXISTING_READ_WRITE);
			channel.position(PatrFileSysConstants.FB_BLOCK_LENGTH_OFFSET);
			byte[] bytes = new byte[8];
			ByteBuffer buf = ByteBuffer.wrap(bytes);
			int r = channel.read(buf);
			if (r != 8) {
				throw new IOException("could not read the block size");
			}
			int bs = byteArrToInt(bytes, 0);
			if (created != null) {
				created.value = false;
			}
			return new SeekablePathBlockAccessor(channel, bs);
		} catch (IOException e) {
			if (defaultBlockSize == -1) {
				throw e;
			}
			try {
				SeekableByteChannel channel = Files.newByteChannel(path, readOnly ? OPEN_NEW_READ_ONLY : OPEN_NEW_READ_WRITE);
				if (created != null) {
					created.value = true;
				}
				return new SeekablePathBlockAccessor(channel, defaultBlockSize);
			} catch (IOException e1) {
				if (e instanceof FileNotFoundException) {
					e1.addSuppressed(e);
				}
				throw e1;
			}
		}
	}
	
	public static class Bool {
		
		public boolean value;
		
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

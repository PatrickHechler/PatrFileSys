package de.hechler.patrick.pfs.bm.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import de.hechler.patrick.pfs.bm.BlockAccessor;
import de.hechler.patrick.pfs.exceptions.PFSErr;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.fs.PFS;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines;

public class SeekableBlockAccessor implements BlockAccessor {
	
	private final SeekableByteChannel channel;
	private final int                 blockSize;
	private final boolean             direct;
	
	/**
	 * creates a new {@link SeekableBlockAccessor} with the {@code channel} as underlying storage
	 * and the given {@code blockSize}
	 * 
	 * @param channel
	 *            the underlying {@link #channel} of the new {@link SeekableBlockAccessor}
	 * @param blockSize
	 *            the {@link #blockSize} of the new {@link SeekableBlockAccessor}
	 * @param direct
	 *            if the {@link ByteBuffer} returned by the {@link #loadBlock(long)} should
	 *            {@link ByteBuffer#isDirect() direct}
	 */
	public SeekableBlockAccessor(SeekableByteChannel channel, int blockSize, boolean direct) {
		this.channel = channel;
		this.blockSize = blockSize;
		this.direct = direct;
	}
	
	/**
	 * returns a new {@link SeekableBlockAccessor} for an existing Patr-File-Sys
	 * 
	 * @param channel
	 *            the channel which should be used by the {@link BlockAccessor}
	 * @return the new created {@link BlockAccessor}
	 * @throws PatrFileSysException
	 *             if an error occurs
	 */
	public static SeekableBlockAccessor createFromPFS(SeekableByteChannel channel)
		throws PatrFileSysException {
		try {
			ByteBuffer buf = ByteBuffer.allocate(8);
			read(channel, buf);
			long magic = buf.getLong(0);
			if (magic != NativePatrFileSysDefines.Constants.MAGIC_START) {
				throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ILLEGAL_ARG, "illegal magic at start!");
			}
			buf.position(0);
			channel.position(NativePatrFileSysDefines.Constants.B0_OFFSET_BLOCK_SIZE);
			buf.limit(4);
			read(channel, buf);
			int blockSize = buf.getInt(0);
			return new SeekableBlockAccessor(channel, blockSize, true);
		} catch (IOException e) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_IO_ERR, e.toString());
		}
	}
	
	/**
	 * returns a new {@link SeekableBlockAccessor} for an existing Patr-File-Sys
	 * 
	 * @param path
	 *            the file where the Patr-File-System is saved
	 * @return the new created {@link BlockAccessor}
	 * @throws PatrFileSysException
	 *             if an error occurs
	 */
	public static SeekableBlockAccessor createFromPFS(Path path) throws PatrFileSysException {
		try {
			SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ,
				StandardOpenOption.WRITE);
			return createFromPFS(channel);
		} catch (IOException e) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_IO_ERR, e.toString());
		}
	}
	
	/**
	 * creates a new {@link SeekableBlockAccessor}.<br>
	 * this method does not check what the block size of the current {@link PFS} is, in fact it does
	 * not check if there is currently any {@link PFS} saved
	 * <p>
	 * this method should only be used when a new {@link PFS} will be created
	 * 
	 * @param path
	 *            the underlying {@link Path} of the {@link BlockAccessor}
	 * @param blockSize
	 *            the block size of the {@link BlockAccessor}
	 * @return the new created {@link BlockAccessor}
	 * @throws PatrFileSysException
	 *             if an error occurs
	 */
	public static SeekableBlockAccessor create(Path path, int blockSize)
		throws PatrFileSysException {
		try {
			SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ,
				StandardOpenOption.WRITE);
			return new SeekableBlockAccessor(channel, blockSize, true);
		} catch (IOException e) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_IO_ERR, e.toString());
		}
	}
	
	private static void read(SeekableByteChannel channel, ByteBuffer buf) throws IOException,
		PatrFileSysException {
		while (true) {
			int read = channel.read(buf);
			if (read == -1) {
				throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ILLEGAL_ARG, "reached end of file");
			}
		}
	}
	
	@Override
	public int blockSize() {
		return blockSize;
	}
	
	@Override
	public ByteBuffer loadBlock(long block) throws PatrFileSysException {
		ByteBuffer data = direct ? ByteBuffer.allocateDirect(blockSize)
			: ByteBuffer.allocate(blockSize);
		try {
			channel.position(Math.multiplyExact(block, blockSize));
			do {
				channel.read(data);
			} while (data.position() < blockSize);
			data.position(0);
			return data;
		} catch (ClosedChannelException e) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_CLOSED, e.toString());
		} catch (IOException e) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_IO_ERR, e.toString());
		}
	}
	
	@Override
	public void saveBlock(ByteBuffer data, long block) throws PatrFileSysException {
		data.position(0);
		data.limit(blockSize);
		assert data.capacity() == blockSize;
		try {
			channel.position(Math.multiplyExact(block, blockSize));
			do {
				channel.write(data);
			} while (data.position() < blockSize);
		} catch (ClosedChannelException e) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_CLOSED, e.toString());
		} catch (IOException e) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_IO_ERR, e.toString());
		}
	}
	
	@Override
	public void discardBlock(long block) throws PatrFileSysException {}
	
	@Override
	public void close() throws PatrFileSysException {
		try {
			channel.close();
		} catch (IOException e) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_IO_ERR, e.toString());
		}
	}
	
	@Override
	public void sync() throws PatrFileSysException {
		if (channel instanceof FileChannel) {
			try {
				((FileChannel) channel).force(false);
			} catch (IOException e) {
				throw PFSErr.createAndThrow(PFSErr.PFS_ERR_IO_ERR, e.toString());
			}
		}
	}
	
}

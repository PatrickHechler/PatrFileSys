package de.hechler.patrick.ownfs.objects;

import static de.hechler.patrick.zeugs.NumberConvert.byteArrToInt;
import static de.hechler.patrick.zeugs.NumberConvert.byteArrToLong;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.SyncFailedException;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.Map;

import de.hechler.patrick.ownfs.interfaces.BlockAccessor;

public class RAFBlockAccessorImpl implements BlockAccessor {
	
	public final long              blockCount;
	public final int               blockSize;
	private Map <Long, byte[]>     loaded;
	private final RandomAccessFile raf;
	
	public RAFBlockAccessorImpl(File file, int blockSize, long blockCoutn) throws FileNotFoundException {
		this(file, false, blockSize, blockCoutn);
	}
	
	public RAFBlockAccessorImpl(File file, boolean syncronized, int blockSize, long blockCoutn) throws FileNotFoundException {
		this(new RandomAccessFile(file, syncronized ? "rws" : "rw"), blockSize, blockCoutn);
	}
	
	public RAFBlockAccessorImpl(RandomAccessFile raf, int blockSize, long blockCoutn) {
		this.blockCount = blockCoutn;
		this.blockSize = blockSize;
		this.loaded = new HashMap <>();
		this.raf = raf;
	}
	
	public RAFBlockAccessorImpl createExsisting(RandomAccessFile raf) throws IOException {
		raf.seek(PatrFileSys.FIRST_BLOCK_BLOCK_SIZE_OFFSET);
		byte[] bytes = new byte[8];
		raf.read(bytes, 0, 4);
		int blockSize = byteArrToInt(bytes, 0);
		raf.seek(blockSize + PatrFileSys.SECOND_BLOCK_BLOCK_COUNT_OFFSET);
		raf.read(bytes, 0, 8);
		long blockCount = byteArrToLong(bytes, 0);
		return new RAFBlockAccessorImpl(raf, blockSize, blockCount);
	}
	
	
	
	@Override
	public int blockSize() {
		return this.blockSize;
	}
	
	@Override
	public byte[] loadBlock(long block) throws IOException, ClosedChannelException {
		if (this.loaded == null) {
			throw new ClosedChannelException();
		}
		if (block >= this.blockCount) {
			throw new IllegalArgumentException("too high block number: blockcount=" + this.blockCount + " block=" + block + " (blocksize=" + this.blockSize + ")");
		}
		byte[] bl = new byte[this.blockSize];
		this.raf.read(bl, 0, this.blockSize);
		final byte[] old = this.loaded.putIfAbsent(Long.valueOf(block), bl);
		if (old != null) {
			throw new IOException("block already loaded");
		}
		return bl;
	}
	
	@Override
	public void saveBlock(byte[] value, long block) throws IOException, ClosedChannelException {
		if (this.loaded == null) {
			throw new ClosedChannelException();
		}
		saveBlock0(value, block, true);
	}

	private void saveBlock0(byte[] value, long block, boolean remove) throws IOException {
		boolean notFound = remove ? loaded.remove(Long.valueOf(block), value) : (loaded.get(Long.valueOf(block)) == value);
		if ( !notFound) {
			throw new IOException("block not loaded");
		}
		this.raf.seek(block * this.blockSize);
		this.raf.write(value, 0, this.blockSize);
	}

	@Override
	public void unloadBlock(long block) throws ClosedChannelException, IllegalStateException {
		if (this.loaded == null) {
			throw new ClosedChannelException();
		}
		final byte[] removed = loaded.remove(Long.valueOf(block));
		if (null != removed) {
			throw new IllegalStateException("block not loaded");
		}
	}
	
	@Override
	public void close() {
		if (this.loaded != null) {
			try {
				this.loaded = null;
				this.raf.close();
			} catch (IOException e) {
				throw new IOError(e);
			}
		}
	}
	
	@Override
	public void saveAll() throws IOException, ClosedChannelException {
		if (this.loaded == null) {
			throw new ClosedChannelException();
		}
		this.loaded.forEach((v,b) -> {
			try {
				saveBlock0(b, v, false);
			} catch (IOException e1) {
				throw new IOError(e1);
			}
		});
		this.loaded.clear();
	}
	
	@Override
	public void unloadAll() throws ClosedChannelException {
		if (this.loaded == null) {
			throw new ClosedChannelException();
		}
		this.loaded.clear();
	}
	
	
	public void sync() throws SyncFailedException, IOException {
		this.raf.getChannel().force(true);
		this.raf.getFD().sync();
	}
}

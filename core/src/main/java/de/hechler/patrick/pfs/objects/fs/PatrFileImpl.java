package de.hechler.patrick.pfs.objects.fs;

import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToLong;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.longToByteArr;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LAST_META_MOD_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_DATA_TABLE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_HASH_CODE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_HASH_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_READ_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_WRITE_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_TIME;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ClosedChannelException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.exception.OutOfSpaceException;
import de.hechler.patrick.pfs.interfaces.BlockManager;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.ThrowingIterator;
import de.hechler.patrick.pfs.objects.AllocatedBlocks;


public class PatrFileImpl extends PatrFileSysElementImpl implements PatrFile {
	
	
	
	public PatrFileImpl(PatrFileSysImpl fs, long startTime, BlockManager bm, long id) {
		super(fs, startTime, bm, id);
	}
	
	@Override
	public void getContent(byte[] bytes, long offset, int bytesOff, int length, long lock) throws IllegalArgumentException, IOException {
		if (bytesOff + length > bytes.length) {
			throw new IllegalArgumentException("too large for the given byte array! (array-len=" + bytes.length + ", array-off=" + bytesOff + ", length=" + length + ")");
		}
		if (bytesOff < 0 || offset < 0 || length < 0) {
			throw new IllegalArgumentException("negative value! offset/len can not be negative! (bytesOff=" + bytesOff + ", offset=" + offset + ", length=" + length + ")");
		}
		if ((long) length + offset < 0L) {
			throw new IllegalArgumentException("owerflow detected: off=" + offset + " len=" + length + " off+len=" + (offset + (long) length));
		}
		synchronized (bm) {
			fs.updateBlockAndPos(this);
			bm.getBlock(block);
			try {
				executeEnsureAccess(lock, LOCK_NO_READ_ALLOWED_LOCK, false);
				executeRead(bytes, offset, bytesOff, length);
			} finally {
				bm.ungetBlock(block);
			}
		}
	}
	
	protected void executeRead(byte[] bytes, long offset, int bytesOff, int length) throws ClosedChannelException, IOException, ElementLockedException {
		if (offset + (long) length > executeLength()) {
			throw new IllegalArgumentException("too large for me! (offset=" + offset + ", length=" + length + ", offset+length=" + (offset + length) + ", my-length=" + length() + ")");
		}
		BlockTableIter bti;
		for (bti = new BlockTableIter(); bti.hasNext() && bti.fileoff < offset; bti.next = null);
		for (; length > 0 && bti.hasNext(); bti.next = null) {
			long startFileOff = bti.fileoff;
			startFileOff -= bti.next.count * bm.blockSize();
			long startBlock = bti.next.startBlock,
				count = bti.next.count;
			if (startFileOff < offset) {
				long dif = offset - startFileOff;
				int firstOff = (int) (dif % bm.blockSize());
				long blockSkip = (dif / bm.blockSize()) + 1L;
				count -= blockSkip;
				startBlock += blockSkip;
				long halfStartBlock = startBlock - 1L;
				byte[] blockbytes = bm.getBlock(halfStartBlock);
				try {
					int cpy = Math.min(length, blockbytes.length - firstOff);
					System.arraycopy(blockbytes, firstOff, bytes, bytesOff, cpy);
					bytesOff += cpy;
					length -= cpy;
				} finally {
					bm.ungetBlock(halfStartBlock);
				}
			}
			for (long blockAdd = 0L; blockAdd < count && length > 0; blockAdd ++ ) {
				byte[] blockbytes = bm.getBlock(startBlock + blockAdd);
				try {
					int cpy = Math.min(blockbytes.length, length);
					System.arraycopy(blockbytes, 0, bytes, bytesOff, cpy);
					bytesOff += cpy;
					length -= cpy;
				} finally {
					bm.ungetBlock(startBlock + blockAdd);
				}
			}
		}
	}
	
	@Override
	public void removeContent(long lock) throws IOException, ElementLockedException {
		withLock(() -> {
			fs.updateBlockAndPos(this);
			executeEnsureAccess(lock, LOCK_NO_WRITE_ALLOWED_LOCK, true);
			executeRemoveContext();
			executeModify(false);
		});
	}
	
	private void executeRemoveContext() throws IOException {
		for (BlockTableIter bit = new BlockTableIter(); bit.hasNext(); bit.next = null) {
			free(bm, bit.next);
		}
		int len = myLength();
		resize(len, FILE_OFFSET_FILE_DATA_TABLE);
		setLength(0L);
	}
	
	@Override
	public void truncate(long size, long lock) throws IllegalArgumentException, IOException, ElementLockedException {
		if (size < 0L) {
			throw new IllegalArgumentException("size < 0 size: " + size);
		}
		withLock(() -> {
			fs.updateBlockAndPos(this);
			if (size >= executeLength()) {
				throw new IllegalArgumentException("size >= len size=" + size + " len=" + executeLength());
			}
			executeEnsureAccess(lock, LOCK_NO_WRITE_ALLOWED_LOCK, true);
			executeTruncate(size);
			executeModify(false);
		});
	}
	
	private void executeTruncate(long size) throws IOException {
		BlockTableIter bit;
		int firstOff = -1;
		for (bit = new BlockTableIter(); bit.hasNext(); bit.next = null) {
			if (bit.fileoff > size) {
				if (firstOff == -1) {
					firstOff = bit.tableoff;
				}
				long startOff = bit.fileoff;
				startOff -= bit.next.count * bm.blockSize();
				if (startOff >= size) {
					free(bm, bit.next);
				} else {
					long removeLen = bit.fileoff - startOff;
					removeLen /= bm.blockSize();
					long removeStart = bit.next.startBlock + bit.next.count - removeLen;
					AllocatedBlocks freeBlocks = new AllocatedBlocks(removeStart, removeLen);
					free(bm, freeBlocks);
				}
			}
		}
		assert firstOff != -1L;
		int len = myLength();
		resize(len, pos - firstOff);
		setLength(size);
	}
	
	@Override
	public void setContent(final byte[] bytes, final long offset, final int bytesOff, final int length, long lock) throws IllegalArgumentException, IOException {
		if (bytesOff + length > bytes.length) {
			throw new IllegalArgumentException("too large for the given byte array! (array-len=" + bytes.length + ", array-off=" + bytesOff + ", length=" + length + ")");
		}
		if (bytesOff < 0 || offset < 0 || length < 0) {
			throw new IllegalArgumentException("negative value! offset/len can not be negative! (bytesOff=" + bytesOff + ", offset=" + offset + ", length=" + length + ")");
		}
		withLock(() -> {
			fs.updateBlockAndPos(this);
			executeEnsureAccess(lock, LOCK_NO_WRITE_ALLOWED_LOCK, true);
			executeWrite(bytes, offset, bytesOff, length);
			executeModify(false);
		});
	}
	
	private void executeWrite(final byte[] bytes, final long offset, final int bytesOff, final int length) throws ClosedChannelException, IOException {
		if (offset + (long) length > length()) {
			throw new IllegalArgumentException("too large for me! (offset=" + offset + ", length=" + length + ", offset+length=" + (offset + length) + ", my-length=" + length() + ")");
		}
		long skip = offset / bm.blockSize();
		int off = (int) (offset % (long) bm.blockSize());
		int boff = bytesOff;
		int remain = length;
		for (BlockTableIter iter = new BlockTableIter(); iter.hasNext();) {
			AllocatedBlocks p = iter.next();
			if (p.count < skip) {
				skip -= p.count;
				continue;
			}
			int blockSize = bm.blockSize();
			long blockAdd = 0;
			if (off > 0) {
				blockAdd = off / blockSize;
				off = off % blockSize;
			}
			for (; blockAdd < p.count && remain > 0; blockAdd ++ ) {
				long blockNum = p.startBlock + blockAdd;
				byte[] blockBytes = bm.getBlock(blockNum);
				try {
					int cpy = Math.min(remain, blockSize - off);
					System.arraycopy(bytes, boff, blockBytes, off, cpy);
					off = 0;
					remain -= cpy;
					boff += cpy;
				} finally {
					bm.setBlock(blockNum);
				}
			}
			if (remain > 0) {
				continue;
			} else {
				break;
			}
		}
	}
	
	@Override
	public void appendContent(byte[] bytes, int bytesOff, int length, long lock) throws IllegalArgumentException, IOException {
		if (bytesOff + length > bytes.length) {
			throw new IllegalArgumentException("too large for the given byte array! (array-len=" + bytes.length + ", array-off=" + bytesOff + ", length=" + length + ")");
		}
		withLock(() -> {
			fs.updateBlockAndPos(this);
			executeEnsureAccess(lock, LOCK_NO_WRITE_ALLOWED_LOCK, true);
			executeAppend(bytes, bytesOff, length, lock);
			executeModify(false);
		});
	}
	
	private void executeAppend(byte[] bytes, int bytesOff, int length, long lock) throws ClosedChannelException, IOException, ElementLockedException, OutOfSpaceException {
		final long oldBlock = block;
		byte[] myBlockBytes = bm.getBlock(oldBlock);
		try {
			final long myOldLen = executeLength();
			BlockTableIter bti = new BlockTableIter();
			while (bti.hasNext()) {
				bti.next = null;
			}
			int off = bti.tableoff + 16;
			final int blockSize = myBlockBytes.length;
			final long myNewLen = myOldLen + (long) length;
			int lastBlockPos = (int) (myOldLen % (long) blockSize);
			if (lastBlockPos > 0) {
				int remain = blockSize - lastBlockPos;
				long lastBlock = byteArrToLong(myBlockBytes, off - 8) - 1L;
				byte[] lbbytes = bm.getBlock(lastBlock);
				try {
					int cpy = Math.min(remain, length);
					System.arraycopy(bytes, bytesOff, lbbytes, lastBlockPos, cpy);
					length -= cpy;
					bytesOff += cpy;
				} finally {
					bm.setBlock(lastBlock);
				}
			}
			if (length > 0) {
				long neededBlockCount = ( ((long) length) + ((long) blockSize) - 1L) / (long) blockSize;
				AllocatedBlocks[] newBlocks = allocate(neededBlockCount);
				for (int i = 0; i < newBlocks.length; i ++ ) {
					for (long cpyBlockAdd = 0; cpyBlockAdd < newBlocks[i].count; cpyBlockAdd ++ ) {
						long cpyBlock = newBlocks[i].startBlock + cpyBlockAdd;
						byte[] cpyBlockBytes = bm.getBlock(cpyBlock);
						try {
							int cpyLen = Math.min(blockSize, length);
							System.arraycopy(bytes, bytesOff, cpyBlockBytes, 0, cpyLen);
							length -= cpyLen;
							bytesOff += cpyLen;
						} finally {
							bm.setBlock(cpyBlock);
						}
					}
				}
				final int relOff = off - pos;
				final boolean oldLastAndFirstNewMatches = relOff == FILE_OFFSET_FILE_DATA_TABLE ? false : newBlocks[0].startBlock == byteArrToLong(myBlockBytes, off - 8);
				final int newLen;
				if (oldLastAndFirstNewMatches) {
					newLen = (relOff + (newBlocks.length * 16)) - 16;
				} else {
					newLen = relOff + (newBlocks.length * 16);
				}
				resize(off - pos, newLen);
				off = pos + relOff;
				myBlockBytes = bm.getBlock(block);
				try {
					int i = 0;
					if (oldLastAndFirstNewMatches) {
						longToByteArr(myBlockBytes, off - 8, newBlocks[0].startBlock + newBlocks[0].count);
						i = 1;
					}
					for (; i < newBlocks.length; i ++ , off += 16) {
						longToByteArr(myBlockBytes, off, newBlocks[i].startBlock);
						longToByteArr(myBlockBytes, off + 8, newBlocks[i].startBlock + newBlocks[i].count);
					}
					setLength(myNewLen);
				} finally {
					bm.setBlock(block);
				}
			} else {
				setLength(myNewLen);
			}
		} finally {
			bm.setBlock(oldBlock);
		}
	}
	
	private void setLength(final long myNewLen) throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			longToByteArr(bytes, pos + FILE_OFFSET_FILE_LENGTH, myNewLen);
		} finally {
			bm.setBlock(block);
		}
	}
	
	@Override
	public byte[] getHashCode(long lock) throws IOException, ElementLockedException {
		synchronized (bm) {
			fs.updateBlockAndPos(this);
			return executeGetHashCode(lock);
		}
	}
	
	private byte[] executeGetHashCode(long lock) throws IOException, ElementLockedException {
		byte[] bytes = bm.getBlock(block);
		try {
			executeEnsureAccess(lock, LOCK_NO_READ_ALLOWED_LOCK, false);
			byte[] result = new byte[32];
			long lastMod = byteArrToLong(bytes, pos + ELEMENT_OFFSET_LAST_META_MOD_TIME);
			long hashTIme = byteArrToLong(bytes, pos + FILE_OFFSET_FILE_HASH_TIME);
			if (hashTIme == NO_TIME || hashTIme <= lastMod) {
				longToByteArr(bytes, pos + FILE_OFFSET_FILE_HASH_TIME, System.currentTimeMillis());
				result = executeCalcHAshCode();
				assert result.length == 32;
				System.arraycopy(result, 0, bytes, pos + FILE_OFFSET_FILE_HASH_CODE, 32);
			} else {
				System.arraycopy(bytes, pos + FILE_OFFSET_FILE_HASH_CODE, result, 0, 32);
			}
			return result;
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	// http://www.java2s.com/example/java-utility-method/sha256/sha256-final-inputstream-inputstream-82aa9.html
	private byte[] executeCalcHAshCode() throws IOException {
		try {
			byte[] buffer = new byte[1 << 16];
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			long length = length(), read;
			for (read = 0L; length - read > buffer.length; read += buffer.length) {
				executeRead(buffer, read, 0, buffer.length);
				digest.update(buffer, 0, 1 << 16);
			}
			if (length > read) {
				executeRead(buffer, 0, 0, (int) (length - read));
				digest.update(buffer, 0, (int) (length - read));
			}
			return digest.digest();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 hashing algorithm unknown in this VM.", e);
		}
	}
	
	@Override
	public long length() throws IOException {
		synchronized (bm) {
			fs.updateBlockAndPos(this);
			return executeLength();
		}
	}
	
	private long executeLength() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			return byteArrToLong(bytes, pos + FILE_OFFSET_FILE_LENGTH);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	private class BlockTableIter implements ThrowingIterator <AllocatedBlocks, IOException> {
		
		private AllocatedBlocks next     = null;
		private long            fileoff  = 0L;
		private int             tableoff = pos + FILE_OFFSET_FILE_DATA_TABLE - 16;
		
		@Override
		public boolean hasNext() throws IOException {
			if (next != null) return true;
			long oldBlock = block;
			bm.getBlock(oldBlock);
			try {
				long remain = executeLength() - fileoff;
				if (remain <= 0L) {
					return false;
				}
				tableoff += 16;
				byte[] bytes = bm.getBlock(block);
				long start, count;
				try {
					start = byteArrToLong(bytes, tableoff);
					long end = byteArrToLong(bytes, tableoff + 8);
					count = end - start;
				} finally {
					bm.ungetBlock(block);
				}
				next = new AllocatedBlocks(start, count);
				fileoff += bm.blockSize() * next.count;
				return true;
			} finally {
				bm.ungetBlock(oldBlock);
			}
		}
		
		@Override
		public AllocatedBlocks next() throws IOException {
			if ( !hasNext()) throw new NoSuchElementException("no more blocks in table");
			AllocatedBlocks n = next;
			next = null;
			return n;
		}
		
	}
	
	@Override
	public InputStream openInput(long lock) throws IOException, ElementLockedException {
		return new PatrFileImplInputStream(lock);
	}
	
	@Override
	public OutputStream openOutput(boolean append, long lock) throws IOException, ElementLockedException {
		return new PatrFileImplOutputStream(append, lock);
	}
	
	public class PatrFileImplInputStream extends PatrFileInputStream {
		
		public PatrFileImplInputStream(long lock) {
			super(PatrFileImpl.this, lock);
		}
		
		@Override
		public synchronized int read(byte[] b, int off, int len) throws IOException {
			if (off < 0 || len < 0 || off + len < 0 || off + len > b.length) {
				throw new IllegalArgumentException("bytes.length=" + b.length + " off=" + off + " len=" + len + " off+len=" + (off + len));
			}
			synchronized (bm) {
				ensureOpen();
				fs.updateBlockAndPos(PatrFileImpl.this);
				long oldBlock = block;
				bm.getBlock(oldBlock);
				try {
					PatrFileImpl.this.executeEnsureAccess(lock, LOCK_NO_READ_ALLOWED_LOCK, false);
					long remain = executeLength() - pos;
					if (remain <= 0L) {
						assert remain == 0L;
						return -1;
					}
					int r = (int) Math.min(remain, len);
					PatrFileImpl.this.executeRead(b, pos, off, r);
					pos += r;
					return r;
				} finally {
					bm.ungetBlock(oldBlock);
				}
			}
		}
		
		@Override
		public synchronized int read() throws IOException {
			synchronized (bm) {
				ensureOpen();
				fs.updateBlockAndPos(PatrFileImpl.this);
				long oldBlock = block;
				bm.getBlock(oldBlock);
				try {
					PatrFileImpl.this.executeEnsureAccess(lock, LOCK_NO_READ_ALLOWED_LOCK, false);
					long remain = executeLength() - pos;
					if (remain < 1L) {
						assert remain == 0L;
						return -1;
					}
					PatrFileImpl.this.executeRead(singleByte, pos, 0, 1);
					pos ++ ;
					return singleByte[0] & 0xFF;
				} finally {
					bm.ungetBlock(oldBlock);
				}
			}
		}
		
	}
	
	public class PatrFileImplOutputStream extends PatrFileOutputStream {
		
		public PatrFileImplOutputStream(boolean append, long lock) {
			super(PatrFileImpl.this, append, lock);
		}
		
		@Override
		public synchronized void write(byte[] b, int off, int len) throws IOException {
			if (off < 0 || len < 0 || off > b.length || b.length - off < len) {
				throw new IllegalArgumentException("bytes.length=" + b.length + " off=" + off + " len=" + len);
			}
			synchronized (bm) {
				ensureOpen();
				fs.updateBlockAndPos(PatrFileImpl.this);
				if (pos < len && !append) {
					long remain = file.length() - pos;
					int cpy = (int) Math.min(remain, len);
					executeWrite(b, pos, cpy, len);
					len -= cpy;
					off += cpy;
					pos += cpy;
				}
				executeAppend(b, off, len, lock);
				pos += len;
			}
		}
		
	}
	
}

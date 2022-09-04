package de.hechler.patrick.pfs.objects.fs;

import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToLong;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.longToByteArr;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FIRST_BLOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_READ_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_WRITE_ALLOWED_LOCK;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.interfaces.BlockManager;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.objects.AllocatedBlocks;


public class PatrFileImpl extends PatrFileSysElementImpl implements PatrFile {
	
	public PatrFileImpl(PatrFileSysImpl fs, long startTime, BlockManager bm, long id) {
		super(fs, startTime, bm, id);
	}
	
	@Override
	public void getContent(byte[] bytes, long offset, int bytesOff, int length, long lock) throws IllegalArgumentException, IOException, ElementLockedException {
		if (offset < 0L || bytesOff < 0 || length < 0 || bytes.length - bytesOff < length) {
			throw new IllegalArgumentException("offset=" + offset + " bytesOff=" + bytesOff + " length=" + length + " bytes.length=" + bytes.length);
		}
		withLock(() -> {
			fs.updateBlockAndPos(this);
			executeEnsureAccess(lock, LOCK_NO_WRITE_ALLOWED_LOCK);
			executeReadWrite(bytes, offset, bytesOff, length, false);
		});
	}
	
	@Override
	public void setContent(byte[] bytes, long offset, int bytesOff, int length, long lock) throws IllegalArgumentException, IOException, ElementLockedException {
		if (offset < 0L || bytesOff < 0 || length < 0 || bytes.length - bytesOff < length) {
			throw new IllegalArgumentException("offset=" + offset + " bytesOff=" + bytesOff + " length=" + length + " bytes.length=" + bytes.length);
		}
		withLock(() -> {
			fs.updateBlockAndPos(this);
			executeEnsureAccess(lock, LOCK_NO_WRITE_ALLOWED_LOCK);
			executeReadWrite(bytes, offset, bytesOff, length, true);
		});
	}
	
	@Override
	public void appendContent(byte[] bytes, int bytesOff, int length, long lock) throws IllegalArgumentException, IOException, ElementLockedException {
		if (bytesOff < 0 || length < 0 || bytes.length - bytesOff < length) {
			throw new IllegalArgumentException("bytesOff=" + bytesOff + " length=" + length + " bytes.length=" + bytes.length);
		}
		withLock(() -> {
			fs.updateBlockAndPos(this);
			executeEnsureAccess(lock, LOCK_NO_WRITE_ALLOWED_LOCK);
			executeAppend(bytes, bytesOff, length);
		});
	}
	
	@Override
	public void truncate(long size, long lock) throws IllegalArgumentException, IOException, ElementLockedException {
		if (size < 0) {
			throw new IllegalArgumentException("negative size: " + size);
		}
		withLock(() -> {
			fs.updateBlockAndPos(this);
			executeEnsureAccess(lock, LOCK_NO_WRITE_ALLOWED_LOCK);
			executeRemove(size);
		});
	}
	
	@Override
	public long length(final long lock) throws IOException {
		return withLockLong(() -> {
			fs.updateBlockAndPos(this);
			ensureAccess(lock, LOCK_NO_READ_ALLOWED_LOCK);
			return executeLength();
		});
	}
	
	public void executeReadWrite(byte[] bytes, long off, int bytesOff, int length, boolean write) throws IOException {
		long mylen = executeLength();
		if (mylen - off < (long) length) {
			throw new IllegalStateException("fileLength < writeLength + fileOffset (fileLength=" + mylen + ", writeLength=" + length + ", fileOffset=" + off + ")");
		}
		final int blockContent = bm.blockSize() - 8;
		BTI bti = bti();
		for (long r = off; r >= blockContent; bti.next(), r -= blockContent);
		for (int o = (int) (off % blockContent); length > 0;) {
			int cpy = Math.min(blockContent - o, length);
			if (write) {
				byte[] cur = bm.getBlock(bti.current);
				try {
					System.arraycopy(bytes, bytesOff, cur, o, cpy);
				} finally {
					bm.setBlock(bti.current);
				}
			} else {
				byte[] cur = bm.getBlock(bti.current);
				try {
					System.arraycopy(bytes, bytesOff, cur, o, cpy);
				} finally {
					bm.ungetBlock(bti.current);
				}
			}
			length -= cpy;
			off += cpy;
			o = 0;
		}
	}
	
	// public void executeWrite(byte[] bytes, long off, int bytesOff, int length) throws IOException {
	// long mylen = executeLength();
	// if (mylen - off < (long) length) {
	// throw new IllegalStateException("fileLength < writeLength + fileOffset (fileLength=" + mylen + ", writeLength=" + length + ", fileOffset=" + off + ")");
	// }
	// final int blockContent = bm.blockSize() - 8;
	// BTI bti = bti();
	// for (long r = off; r > 0; bti.next(), r -= blockContent);
	// for (int o = (int) (off % blockContent); length > 0;) {
	// int cpy = Math.min(blockContent - o, length);
	// byte[] cur = bm.getBlock(bti.current);
	// try {
	// System.arraycopy(bytes, bytesOff, cur, o, cpy);
	// } finally {
	// bm.setBlock(bti.current);
	// }
	// length -= cpy;
	// off += cpy;
	// o = 0;
	// }
	// }
	
	public void executeAppend(byte[] bytes, int bytesOff, int length) throws IOException {
		byte[] me = bm.getBlock(block);
		try {
			final int blockContent = me.length - 8;
			final long oldLength = executeLength();
			final long newLength = oldLength + length;
			BTI bti = bti();
			long last = bti.current;
			while (bti.current != -1) {
				last = bti.current;
				bti.next();
			}
			if (last == -1L) {
				assert oldLength == 0L;
				last = allocateOneBlock();
				longToByteArr(me, pos + FILE_OFFSET_FIRST_BLOCK, last);
				bti.remain = -blockContent;
			}
			int rem = -(int) bti.remain;
			long next;
			int blockOff = blockContent - rem;
			while (true) {
				byte[] cur = bm.getBlock(last);
				try {
					int cpy = Math.min(rem, length);
					System.arraycopy(bytes, bytesOff, cur, blockOff, cpy);
					length -= cpy;
					bytesOff += cpy;
					if (length <= 0) {
						assert length == 0;
						setLength(newLength);
						return;
					}
					next = allocateOneBlock();
					longToByteArr(cur, blockContent, next);
				} finally {
					bm.ungetBlock(last);
				}
				last = next;
				blockOff = 0;
				rem = blockContent;
			}
		} finally {
			bm.setBlock(block);
		}
	}
	
	public void executeRemove(long newLength) throws IOException {
		long len = executeLength();
		if (len <= newLength) {
			throw new IllegalStateException("current length is less or equal to newLength (current=" + len + " (not) new=" + newLength + ")");
		}
		final int blockContent = bm.blockSize() - 8;
		BTI bti = bti();
		for (long r = newLength; r > 0; bti.next(), r -= blockContent);
		long last = bti.current;
		while (bti.current != -1L) {
			free(bm, new AllocatedBlocks(bti.current, 1L));
			last = bti.current;
			bti.next();
		}
		if (last != -1L) {
			byte[] bytes = bm.getBlock(last);
			try {
				longToByteArr(bytes, blockContent, -1L);
			} finally {
				bm.setBlock(last);
			}
			setLength(newLength);
		}
	}
	
	private void setLength(long newLength) throws IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			longToByteArr(bytes, pos + FILE_OFFSET_FILE_LENGTH, newLength);
		} finally {
			bm.setBlock(block);
		}
	}
	
	protected long executeLength() throws IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			return byteArrToLong(bytes, pos + FILE_OFFSET_FILE_LENGTH);
		} finally {
			bm.ungetBlock(block);
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
	
	private BTI bti() throws IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			long length = byteArrToLong(bytes, pos + FILE_OFFSET_FILE_LENGTH);
			if (length == 0L) {
				return new BTI( -1L, length);
			}
			long first = byteArrToLong(bytes, pos + FILE_OFFSET_FIRST_BLOCK);
			return new BTI(first, length);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	/** BlockTableIterator (the name is too long) */
	private class BTI {
		
		long current;
		long remain;
		
		private BTI(long c, long r) {
			current = c;
			remain = r;
		}
		
		private void next() throws IOException {
			final int blockContent = bm.blockSize() - 8;
			remain -= blockContent;
			if (remain <= 0L) {
				current = -1L;
			} else {
				final long old = current;
				byte[] bytes = bm.getBlock(old);
				try {
					current = byteArrToLong(bytes, blockContent);
				} finally {
					bm.ungetBlock(old);
				}
			}
		}
		
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
					PatrFileImpl.this.executeEnsureAccess(lock, LOCK_NO_READ_ALLOWED_LOCK);
					long remain = executeLength() - pos;
					if (remain <= 0L) {
						assert remain == 0L;
						return -1;
					}
					int r = (int) Math.min(remain, len);
					PatrFileImpl.this.executeReadWrite(b, pos, off, r, false);
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
					PatrFileImpl.this.executeEnsureAccess(lock, LOCK_NO_READ_ALLOWED_LOCK);
					long remain = executeLength() - pos;
					if (remain < 1L) {
						assert remain == 0L;
						return -1;
					}
					PatrFileImpl.this.executeReadWrite(singleByte, pos, 0, 1, false);
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
				bm.getBlock(block);
				try {
					PatrFileImpl.this.executeEnsureAccess(lock, LOCK_NO_WRITE_ALLOWED_LOCK);
					if (pos < len && !append) {
						long remain = executeLength() - pos;
						int cpy = (int) Math.min(remain, len);
						executeReadWrite(b, pos, cpy, len, true);
						len -= cpy;
						off += cpy;
						pos += cpy;
					}
					if (len > 0) {
						PatrFileImpl.this.executeAppend(b, off, len);
					}
				} finally {
					bm.ungetBlock(block);
				}
				pos += len;
			}
		}
		
	}
	
}

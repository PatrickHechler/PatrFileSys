package de.hechler.patrick.pfs.objects;

import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.byteArrToLong;
import static de.hechler.patrick.pfs.utils.ConvertNumByteArr.longToByteArr;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.ELEMENT_OFFSET_LAST_META_MOD_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_DATA_TABLE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_HASH_CODE;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_HASH_TIME;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.FILE_OFFSET_FILE_LENGTH;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_DELETE_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_READ_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_WRITE_ALLOWED_LOCK;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.interfaces.BlockManager;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingBooleanFunction;


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
		withLock(() -> {
			ensureAccess(lock, LOCK_NO_READ_ALLOWED_LOCK, false);
			executeRead(bytes, offset, bytesOff, length);
		});
	}
	
	private void executeRead(byte[] bytes, long offset, int bytesOff, int length) throws ClosedChannelException, IOException, ElementLockedException {
		if (offset + (long) length > length()) {
			throw new IllegalArgumentException("too large for me! (offset=" + offset + ", length=" + length + ", offset+length=" + (offset + length) + ", my-length=" + length() + ")");
		}
		iterateBlockTable(new ThrowingBooleanFunction <AllocatedBlocks, IOException>() {
			
			private long skip   = offset / bm.blockSize();
			private int  off    = (int) (offset % (long) bm.blockSize());
			private int  boff   = bytesOff;
			private int  remain = length;
			
			@Override
			public boolean calc(AllocatedBlocks p) throws IOException {
				if (p.count < skip) {
					skip -= p.count;
					return true;
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
						System.arraycopy(blockBytes, boff, bytes, off, cpy);
						off = 0;
						remain -= cpy;
						boff += cpy;
					} finally {
						bm.ungetBlock(blockNum);
					}
				}
				if (remain > 0) {
					return true;
				} else {
					return false;
				}
			}
			
		});
	}
	
	@Override
	public void removeContent(final long offset, final long length, long lock) throws IllegalArgumentException, IOException {
		if (offset < 0L || length < 0L) {
			throw new IllegalArgumentException("negative value! offset/len can not be negative! (offset=" + offset + ", length=" + length + ")");
		}
		withLock(() -> {
			ensureAccess(lock, LOCK_NO_WRITE_ALLOWED_LOCK, true);
			executeRemove(offset, length);
			modify(false);
		});
	}
	
	private void executeRemove(final long offset, final long length) throws ClosedChannelException, IOException {
		final long myOldLen = length();
		if (offset + length > myOldLen) {
			throw new IllegalArgumentException("too large for me! (offset=" + offset + ", length=" + length + ", offset+length=" + (offset + length) + ", my-length=" + myOldLen + ")");
		}
		final int blockSize = bm.blockSize();
		iterateBlockTable(new ThrowingBooleanFunction <AllocatedBlocks, IOException>() {
			
			private int       state                = 0;
			private long      skip                 = offset / (long) blockSize;
			private final int inStartBlockOff      = (int) (offset % (long) blockSize);
			private int       startBlockTableOff   = -1;
			private long      startBlockNum        = -1L;
			private long      remove               = -1L;
			private int       lastBlockRemove      = -1;
			private long      lastBlockNum         = -1L;
			private long      firstNotRemovedBlock = -1L;
			private int       lastBlockTableOff    = -1;
			private int       inBlockOff           = -1;
			private int       currentOff           = pos + FILE_OFFSET_FILE_DATA_TABLE - 16;
			private long      remainBlocks         = (myOldLen + (long) blockSize - 1L) / blockSize;
			
			@Override
			public boolean calc(AllocatedBlocks p) throws IOException {
				currentOff += 16;
				remainBlocks -= p.count;
				switch (state) {
				case 0:
					if (p.count < skip) {
						skip -= p.count;
					} else if (remove == -1L) {
						state = 1;
						startBlockTableOff = currentOff;
						startBlockNum = p.startBlock + skip;
						lastBlockRemove = (int) ( (length - inStartBlockOff) % bm.blockSize());
						remove = (length - inStartBlockOff) / bm.blockSize();
						remove -= startBlockNum - p.startBlock;
						assert p.contains(startBlockNum);
					}
					break;
				case 1:
					if (p.count > remove) {
						remove -= p.count;
					} else {
						state = 2;
						lastBlockTableOff = currentOff;
						firstNotRemovedBlock = p.startBlock + remove;
						lastBlockNum = firstNotRemovedBlock;
						assert p.contains(lastBlockNum);
						final long sbn = startBlockNum;
						byte[] stay = bm.getBlock(sbn);
						try {
							final long lbn = lastBlockNum;
							byte[] del = bm.getBlock(lbn);
							try {
								int cpy;
								if (blockSize - inStartBlockOff < blockSize - lastBlockRemove) {
									cpy = blockSize - inStartBlockOff;
									inBlockOff = lastBlockRemove - inStartBlockOff;
									System.arraycopy(del, lastBlockRemove + cpy, del, 0, blockSize - lastBlockRemove - cpy);
									lastBlockNum = startBlockNum;
									startBlockNum = firstNotRemovedBlock;
								} else {
									cpy = blockSize - lastBlockRemove;
									inBlockOff = blockSize - lastBlockRemove + inStartBlockOff;
									firstNotRemovedBlock ++ ;
									lastBlockNum = startBlockNum;
									startBlockNum = firstNotRemovedBlock + 1;
								}
								System.arraycopy(del, lastBlockRemove, stay, inStartBlockOff, cpy);
							} finally {
								bm.setBlock(lbn);
							}
						} finally {
							bm.setBlock(sbn);
						}
						lastBlockNum ++ ;
						copy(blockSize, p.startBlock + p.count);
					}
					break;
				case 2:
					startBlockNum = p.startBlock;
					copy(blockSize, p.startBlock + p.count);
					if (remainBlocks > 0L) {
						break;
					}
					byte[] bytes = bm.getBlock(block);
					try {
						System.arraycopy(bytes, lastBlockTableOff, bytes, startBlockTableOff, currentOff - lastBlockTableOff);
						reallocate(blockSize, pos, currentOff - pos, currentOff - pos - lastBlockTableOff + startBlockTableOff, true);
						longToByteArr(bytes, pos + FILE_OFFSET_FILE_LENGTH, myOldLen - length);
					} finally {
						bm.setBlock(block);
					}
					return false;
				default:
					throw new InternalError("illegal state: " + state);
				}
				return true;
			}
			
			private void copy(final int blockSize, final long end) throws ClosedChannelException, IOException {
				for (; lastBlockNum < end; startBlockNum ++ ) {
					byte[] sbb = bm.getBlock(startBlockNum);
					try {
						byte[] lbb = bm.getBlock(lastBlockNum);
						try {
							int cpy = blockSize - inBlockOff;
							System.arraycopy(lbb, 0, sbb, inBlockOff, cpy);
						} finally {
							bm.setBlock(lastBlockNum);
						}
					} finally {
						bm.setBlock(startBlockNum);
					}
					lastBlockNum = startBlockNum;
				}
			}
			
		});
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
			ensureAccess(bytesOff, LOCK_NO_WRITE_ALLOWED_LOCK, true);
			executeWrite(bytes, offset, bytesOff, length);
			modify(false);
		});
	}
	
	private void executeWrite(final byte[] bytes, final long offset, final int bytesOff, final int length) throws ClosedChannelException, IOException {
		if (offset + (long) length > length()) {
			throw new IllegalArgumentException("too large for me! (offset=" + offset + ", length=" + length + ", offset+length=" + (offset + length) + ", my-length=" + length() + ")");
		}
		iterateBlockTable(new ThrowingBooleanFunction <AllocatedBlocks, IOException>() {
			
			private long skip   = offset / bm.blockSize();
			private int  off    = (int) (offset % (long) bm.blockSize());
			private int  boff   = bytesOff;
			private int  remain = length;
			
			@Override
			public boolean calc(AllocatedBlocks p) throws IOException {
				if (p.count < skip) {
					skip -= p.count;
					return true;
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
					return true;
				} else {
					return false;
				}
			}
			
		});
	}
	
	@Override
	public void appendContent(byte[] bytes, int bytesOff, int length, long lock) throws IllegalArgumentException, IOException {
		if (bytesOff + length > bytes.length) {
			throw new IllegalArgumentException("too large for the given byte array! (array-len=" + bytes.length + ", array-off=" + bytesOff + ", length=" + length + ")");
		}
		withLock(() -> {
			ensureAccess(lock, LOCK_NO_WRITE_ALLOWED_LOCK, true);
			executeAppend(bytes, bytesOff, length, lock);
			modify(false);
		});
	}
	
	private void executeAppend(byte[] bytes, int bytesOff, int length, long lock) throws ClosedChannelException, IOException, ElementLockedException, OutOfMemoryError {
		final long oldBlock = block;
		byte[] myBlockBytes = bm.getBlock(oldBlock);
		try {
			int off = iterateBlockTable(ac -> true);
			final int blockSize = bm.blockSize();
			final long myOldLen = length();
			final long myNewLen = myOldLen + (long) length;
			int lastBlockPos = (int) (myOldLen % (long) blockSize);
			if (lastBlockPos > 0) {
				int remain = blockSize - lastBlockPos;
				long lastBlock = byteArrToLong(bytes, off - 8) - 1L;
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
				int newLen = off - pos;
				newLen += newBlocks.length * 16;
				boolean lastOldAndFirstNewMatches = newBlocks[0].startBlock == byteArrToLong(myBlockBytes, off - 8);
				if (lastOldAndFirstNewMatches) {
					newLen -= 16;
				}
				try {
					reallocate(block, pos, off - pos, newLen, true);
				} catch (OutOfMemoryError e) {
					relocate();
				}
				myBlockBytes = bm.getBlock(block);
				try {
					longToByteArr(myBlockBytes, pos + FILE_OFFSET_FILE_LENGTH, myOldLen + (long) length);
					int i = 0;
					if (lastOldAndFirstNewMatches) {
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
		return simpleWithLock(() -> executeGetHashCode(lock));
	}
	
	private byte[] executeGetHashCode(long lock) throws IOException, ElementLockedException {
		byte[] bytes = bm.getBlock(block);
		try {
			ensureAccess(lock, LOCK_NO_READ_ALLOWED_LOCK, false);
			byte[] result = new byte[32];
			long lastMod = byteArrToLong(bytes, pos + ELEMENT_OFFSET_LAST_META_MOD_TIME);
			long hashTIme = byteArrToLong(bytes, pos + FILE_OFFSET_FILE_HASH_TIME);
			if (hashTIme <= lastMod) {
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
		return simpleWithLockLong(this::executeLength);
	}
	
	private long executeLength() throws ClosedChannelException, IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			return byteArrToLong(bytes, pos + FILE_OFFSET_FILE_LENGTH);
		} finally {
			bm.ungetBlock(block);
		}
	}
	
	@Override
	public void delete(long myLock, long paretnLock) throws IOException {
		withLock(() -> executeDelete(myLock, paretnLock));
	}
	
	private void executeDelete(long myLock, long parentLock) throws ClosedChannelException, IOException, ElementLockedException, OutOfMemoryError {
		ensureAccess(myLock, LOCK_NO_DELETE_ALLOWED_LOCK, true);
		getParent().ensureAccess(parentLock, LOCK_NO_WRITE_ALLOWED_LOCK, true);
		executeRemove(0, length());
		deleteFromParent();
		reallocate(block, pos, FILE_OFFSET_FILE_DATA_TABLE, 0, false);
		getParent().modify(false);
	}
	
	private int iterateBlockTable(ThrowingBooleanFunction <AllocatedBlocks, ? extends IOException> func) throws IOException {
		byte[] bytes = bm.getBlock(block);
		try {
			int off;
			long remain = length();
			for (off = pos + FILE_OFFSET_FILE_DATA_TABLE; remain > 0; off += 16) {
				long start = byteArrToLong(bytes, off),
					end = byteArrToLong(bytes, off + 8),
					cnt = end - start;
				remain -= cnt;
				boolean cont = func.calc(new AllocatedBlocks(start, cnt));
				if (cont) {
					continue;
				} else {
					assert remain >= 0;
					return off;
				}
			}
			assert remain == 0;
			return off;
		} finally {
			bm.ungetBlock(block);
		}
	}
	
}

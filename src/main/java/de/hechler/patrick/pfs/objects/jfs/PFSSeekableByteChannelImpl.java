package de.hechler.patrick.pfs.objects.jfs;

import static de.hechler.patrick.pfs.objects.jfs.PFSFileSystemProviderImpl.MODE_APPEND;
import static de.hechler.patrick.pfs.objects.jfs.PFSFileSystemProviderImpl.MODE_READ;
import static de.hechler.patrick.pfs.objects.jfs.PFSFileSystemProviderImpl.MODE_WRITE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;

import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingConsumer;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;

public class PFSSeekableByteChannelImpl implements SeekableByteChannel, GatheringByteChannel, ScatteringByteChannel {
	
	// @SuppressWarnings("unused")
	// private String[] path;
	private ThrowingConsumer <? extends IOException, PatrFile> onClose;
	private volatile int                                       mode;
	private long                                               lock;
	private PatrFile                                           file;
	private long                                               pos;
	private byte[]                                             buffer;
	
	public PFSSeekableByteChannelImpl(long lock, String[] path, PatrFile file, ThrowingConsumer <? extends IOException, PatrFile> onClose, int mode) {
		this(lock, path, file, onClose, mode, 1 << 16/* ca. 32k */);
	}
	
	public PFSSeekableByteChannelImpl(long lock, String[] path, PatrFile file, ThrowingConsumer <? extends IOException, PatrFile> onClose, int mode, int bufferSize) {
		this.onClose = onClose;
		this.mode = mode;
		this.lock = lock;
		// this.path = path;
		this.file = file;
		this.pos = 0L;
		this.buffer = new byte[bufferSize];
	}
	
	@Override
	public int read(ByteBuffer dst) throws IOException {
		checkRead();
		return file.withLockInt(() -> {
			final long myLen = file.length();
			long myRemain = myLen - pos;
			if (myRemain <= 0L) {
				return -1;
			}
			int remain = dst.remaining();
			int read = 0;
			while (remain > buffer.length) {
				if (myRemain < buffer.length) {
					file.getContent(buffer, pos, 0, (int) myRemain, lock);
					dst.put(buffer, 0, (int) myRemain);
					read += myRemain;
					pos += myRemain;
					return read;
				}
				file.getContent(buffer, pos, 0, buffer.length, lock);
				dst.put(buffer, 0, buffer.length);
				read += buffer.length;
				pos += buffer.length;
				remain -= buffer.length;
				myRemain -= buffer.length;
			}
			int lastRead = (int) Math.min(remain, myRemain);
			file.getContent(buffer, pos, 0, lastRead, lock);
			dst.put(buffer, 0, lastRead);
			read += lastRead;
			pos += lastRead;
			return read;
		});
	}
	
	public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
		checkRead();
		return file.withLockLong(() -> {
			final long myLen = file.length(),
				startPos = pos;
			long myRemain = myLen - startPos;
			if (myRemain <= 0L) {
				return -1;
			}
			int off = 0;
			for (int i = 0; i < dsts.length && myRemain > 0L; i ++ ) {
				ByteBuffer dest = dsts[i];
				int remain = dest.remaining();
				for (int cpy; remain > 0 && myRemain > 0L; remain -= cpy, myRemain -= cpy, pos += cpy) {
					cpy = Math.min(remain, (int) Math.min(myRemain, buffer.length - off));
					file.getContent(buffer, offset, off, cpy, lock);
					dest.put(buffer, off, cpy);
					off += cpy;
					if (off >= buffer.length) {
						assert off == buffer.length;
						off = 0;
					}
				}
			}
			return pos - startPos;
		});
	}
	
	@Override
	public long read(ByteBuffer[] dsts) throws IOException {
		return read(dsts, 0, dsts.length);
	}
	
	@Override
	public int write(ByteBuffer src) throws IOException {
		checkWrite();
		return file.withLockInt(() -> {
			if ( (mode & MODE_APPEND) != 0) {
				pos = file.length();
			}
			final long myLen = file.length();
			long myRemain = myLen - pos;
			int remain = src.remaining();
			int wrote = 0;
			while (myRemain > 0L && remain > 0) {
				int cpy = Math.min((int) Math.min(remain, myRemain), buffer.length);
				src.get(buffer, 0, cpy);
				file.setContent(buffer, pos, 0, cpy, lock);
				wrote += cpy;
				pos += cpy;
				remain -= cpy;
				myRemain -= cpy;
			}
			while (remain > 0) {
				int cpy = Math.min(remain, buffer.length);
				src.get(buffer, 0, cpy);
				file.appendContent(buffer, 0, cpy, lock);
				wrote += cpy;
				pos += cpy;
				remain -= cpy;
			}
			return wrote;
		});
	}
	
	public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
		checkWrite();
		return file.withLockLong(() -> {
			final long myLen = file.length(),
				startPos = pos,
				end = offset + length;
			long myRemain = myLen - startPos;
			if (myRemain <= 0L) {
				return -1;
			}
			int off = 0;
			int i = offset;
			for (i = 0; i < end && myRemain > 0L; i ++ ) {
				ByteBuffer src = srcs[i];
				int remain = src.remaining();
				for (int cpy; remain > 0 && myRemain > 0L; remain -= cpy, myRemain -= cpy, pos += cpy) {
					cpy = Math.min(remain, (int) Math.min(myRemain, buffer.length - off));
					src.get(buffer, off, cpy);
					file.setContent(buffer, offset, off, cpy, lock);
					off += cpy;
					if (off >= buffer.length) {
						assert off == buffer.length;
						off = 0;
					}
				}
			}
			for (; i < end; i ++ ) {
				ByteBuffer src = srcs[i];
				int remain = src.remaining();
				for (int cpy; remain > 0; remain -= cpy, pos += cpy) {
					cpy = Math.min(remain, buffer.length - off);
					src.get(buffer, 0, cpy);
					file.appendContent(buffer, 0, cpy, lock);
				}
			}
			return pos - startPos;
		});
	}
	
	@Override
	public long write(ByteBuffer[] srcs) throws IOException {
		return write(srcs, 0, srcs.length);
	}
	
	@Override
	public long position() throws IOException {
		return pos;
	}
	
	@Override
	public PFSSeekableByteChannelImpl position(long newPosition) throws IOException {
		pos = newPosition;
		return this;
	}
	
	@Override
	public long size() throws IOException {
		return file.length();
	}
	
	@Override
	public PFSSeekableByteChannelImpl truncate(long size) throws IOException {
		checkWrite();
		file.withLock(() -> {
			long length = file.length();
			if (length > size) {
				file.removeContent(size, length - size, lock);
			}
		});
		return this;
	}
	
	public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
		checkRead();
		return file.withLockLong(() -> {
			long pos = position,
				remain = Math.min(file.length() - position, count);
			ByteBuffer buf = ByteBuffer.wrap(buffer);
			while (remain > 0L) {
				int cpy = (int) Math.min(remain, buffer.length);
				file.getContent(buffer, pos, 0, cpy, lock);
				target.write(buf);
				pos += cpy;
				remain -= cpy;
			}
			return Math.min(file.length() - position, count);
		});
	}
	
	public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
		checkWrite();
		file.withLock(() -> {
			long len = file.length(),
				pos = position,
				remain = count,
				myremain = len - position;
			ByteBuffer buf = ByteBuffer.wrap(buffer);
			while (remain > 0L && myremain > 0L) {
				int cpy = (int) Math.min(buffer.length, Math.min(remain, myremain)),
					read = src.read(buf);
				cpy = Math.min(read, cpy);
				file.setContent(buffer, pos, 0, cpy, lock);
				pos += cpy;
				remain -= cpy;
				myremain -= cpy;
			}
			while (remain > 0L) {
				int cpy = (int) Math.min(buffer.length, remain),
					read = src.read(buf);
				cpy = Math.min(read, cpy);
				file.appendContent(buffer, 0, cpy, lock);
				pos += cpy;
				remain -= cpy;
			}
		});
		return count;
	}
	
	public int read(ByteBuffer dst, long position) throws IOException {
		checkRead();
		return file.withLockInt(() -> {
			long pos = position;
			final long myLen = file.length();
			long myRemain = myLen - pos;
			if (myRemain <= 0L) {
				return -1;
			}
			int remain = dst.remaining();
			int read = 0;
			while (remain > buffer.length) {
				if (myRemain < buffer.length) {
					file.getContent(buffer, pos, 0, (int) myRemain, lock);
					dst.put(buffer, 0, (int) myRemain);
					read += myRemain;
					pos += myRemain;
					return read;
				}
				file.getContent(buffer, pos, 0, buffer.length, lock);
				dst.put(buffer, 0, buffer.length);
				read += buffer.length;
				pos += buffer.length;
				remain -= buffer.length;
				myRemain -= buffer.length;
			}
			int lastRead = (int) Math.min(remain, myRemain);
			file.getContent(buffer, pos, 0, lastRead, lock);
			dst.put(buffer, 0, lastRead);
			read += lastRead;
			pos += lastRead;
			return read;
		});
	}
	
	public int write(ByteBuffer src, long position) throws IOException {
		checkWrite();
		return file.withLockInt(() -> {
			if ( (mode & MODE_APPEND) != 0) {
				pos = file.length();
			}
			final long myLen = file.length();
			long myRemain = myLen - pos;
			int remain = src.remaining();
			int wrote = 0;
			while (myRemain > 0L && remain > 0) {
				int cpy = Math.min((int) Math.min(remain, myRemain), buffer.length);
				src.get(buffer, 0, cpy);
				file.setContent(buffer, pos, 0, cpy, lock);
				wrote += cpy;
				pos += cpy;
				remain -= cpy;
				myRemain -= cpy;
			}
			while (remain > 0) {
				int cpy = Math.min(remain, buffer.length);
				src.get(buffer, 0, cpy);
				file.appendContent(buffer, 0, cpy, lock);
				wrote += cpy;
				pos += cpy;
				remain -= cpy;
			}
			return wrote;
		});
	}
	
	@Override
	public boolean isOpen() {
		return mode != 0;
	}
	
	@Override
	public void close() throws IOException {
		if (mode == 0) {
			return;
		}
		file.withLock(() -> {
			if (mode == 0) {
				return;
			}
			onClose.consumer(file);
			if (lock != PatrFileSysConstants.LOCK_NO_LOCK) {
				file.removeLock(lock);
			}
			pos = -1L;
			mode = 0;
		});
	}
	
	public void checkRead() {
		if ( (mode & MODE_READ) == 0) {
			throw new IllegalStateException("not open for read operations");
		}
	}
	
	public void checkWrite() {
		if ( (mode & MODE_WRITE) == 0) {
			throw new IllegalStateException("not open for write operations");
		}
	}
	
}

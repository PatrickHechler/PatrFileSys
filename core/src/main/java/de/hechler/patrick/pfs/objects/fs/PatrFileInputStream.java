package de.hechler.patrick.pfs.objects.fs;


import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_LOCK;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ClosedChannelException;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.interfaces.PatrFile;

public class PatrFileInputStream extends InputStream {
	
	protected final PatrFile file;
	protected long           pos        = 0L;
	protected long           mark       = -1L;
	protected long           markEnd    = -1L;
	protected byte[]         singleByte = null;
	protected final long     lock;
	
	public PatrFileInputStream(PatrFile file) {
		this(file, NO_LOCK);
	}
	
	public PatrFileInputStream(PatrFile file, long lock) {
		this.file = file;
		this.lock = lock;
	}
	
	@Override
	public synchronized int read() throws IOException {
		ensureOpen();
		if (singleByte == null) {
			singleByte = new byte[1];
		}
		int r = file.withLockInt(() -> executeSingleRead());
		pos ++ ;
		return r;
	}
	
	private int executeSingleRead() throws IOException, ElementLockedException {
		try {
			file.getContent(singleByte, pos, 0, 1, lock);
		} catch (IllegalArgumentException e) {
			if (file.length(lock) <= pos) {
				return -1;
			} else {
				throw e;
			}
		}
		return singleByte[0] & 0xFF;
	}
	
	@Override
	public synchronized int read(byte[] b, int off, int len) throws IOException {
		if (off < 0 || len < 0 || off + len < 0 || off + len > b.length) {
			throw new IllegalArgumentException("bytes.length=" + b.length + " off=" + off + " len=" + len + " off+len=" + (off + len));
		}
		ensureOpen();
		int r = file.withLockInt(() -> executeRead(b, off, len));
		pos += r;
		return r;
	}
	
	private int executeRead(byte[] b, int off, int len) throws IOException, ElementLockedException {
		int r = (int) Math.min(file.length(lock) - pos, len);
		file.getContent(b, pos, off, r, lock);
		return r;
	}
	
	@Override
	public int available() throws IOException {
		ensureOpen();
		return (int) Math.min(Integer.MAX_VALUE, file.length(lock) - pos);
	}
	
	@Override
	public boolean markSupported() {
		return true;
	}
	
	public synchronized void mark() {
		if (this.pos == -1L) {
			return;
		}
		this.mark = this.pos;
		this.markEnd = Long.MAX_VALUE;
	}
	
	public synchronized void mark(long readlimit) {
		if (readlimit < 0L) {
			throw new IllegalArgumentException("readLimit below zero: " + readlimit);
		}
		if (this.pos == -1L) {
			return;
		}
		this.mark = this.pos;
		this.markEnd = this.pos + readlimit;
	}
	
	@Override
	public synchronized void mark(int readlimit) {
		if (readlimit < 0) {
			throw new IllegalArgumentException("readLimit below zero: " + readlimit);
		}
		if (this.pos == -1L) {
			return;
		}
		this.mark = this.pos;
		this.markEnd = this.pos + readlimit;
	}
	
	@Override
	public synchronized void reset() throws IOException {
		if (this.pos > this.markEnd) {
			throw new IOException("mark-end=" + this.markEnd + "=0x" + Long.toHexString(this.markEnd) + " pos=" + this.pos + "=0x" + Long.toHexString(this.pos) + " mark="
				+ this.mark + "=0x" + Long.toHexString(this.mark));
		} else {
			ensureOpen();
			this.pos = this.mark;
		}
	}
	
	@Override
	public synchronized long skip(long n) throws IOException {
		if (n < 0L) {
			throw new IllegalArgumentException("negative skip value: " + n);
		}
		ensureOpen();
		return file.withLockLong(() -> executeSkip(n));
	}
	
	private long executeSkip(long n) throws IOException {
		long remain = file.length(lock) - this.pos;
		long skip = Math.min(n, remain);
		this.pos += skip;
		return skip;
	}
	
	@Override
	public void close() throws IOException {
		if (this.lock != NO_LOCK) {
			this.file.removeLock(this.lock);
		}
		this.pos = -1L;
	}
	
	protected void ensureOpen() throws ClosedChannelException {
		if (this.pos == -1L) {
			throw new ClosedChannelException();
		}
	}
	
}


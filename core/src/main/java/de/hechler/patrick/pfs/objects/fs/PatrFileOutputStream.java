package de.hechler.patrick.pfs.objects.fs;

import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_LOCK;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.ClosedChannelException;

import de.hechler.patrick.pfs.interfaces.PatrFile;

public class PatrFileOutputStream extends OutputStream {
	
	protected final PatrFile file;
	protected final long     lock;
	protected final boolean  append;
	protected long           pos        = 0L;
	protected byte[]         singleByte = null;
	
	public PatrFileOutputStream(PatrFile file, boolean append) {
		this(file, append, NO_LOCK);
	}
	
	public PatrFileOutputStream(PatrFile file, boolean append, long lock) {
		this.file = file;
		this.append = append;
		this.lock = lock;
	}
	
	@Override
	public synchronized void write(int b) throws IOException {
		ensureOpen();
		if (singleByte == null) {
			singleByte = new byte[1];
		}
		file.withLock(() -> executeSingleWrite());
		pos ++ ;
	}
	
	private void executeSingleWrite() throws IOException {
		long len = file.length();
		if (pos >= len || append) {
			file.appendContent(singleByte, 0, 1, lock);
		} else {
			file.setContent(singleByte, pos, 0, 1, lock);
		}
	}
	
	@Override
	public synchronized void write(byte[] b, int off, int len) throws IOException {
		if (off < 0 || len < 0 || off > b.length || b.length - off < len) {
			throw new IllegalArgumentException("bytes.length=" + b.length + " off=" + off + " len=" + len);
		}
		ensureOpen();
		file.withLock(() -> executeWrite(b, off, len));
		pos += len;
	}
	
	private void executeWrite(byte[] bytes, int off, int len) throws IOException {
		if (pos < len && !append) {
			long remain = file.length() - pos;
			int cpy = (int) Math.min(remain, len);
			file.setContent(bytes, pos, off, cpy, lock);
			len -= cpy;
			off += cpy;
		}
		file.appendContent(bytes, off, len, lock);
	}
	
	protected void ensureOpen() throws ClosedChannelException {
		if (pos == -1L) {
			throw new ClosedChannelException();
		}
	}
	
	@Override
	public void close() throws IOException {
		if (lock != NO_LOCK) {
			file.removeLock(lock);
		}
		pos = -1L;
	}
	
}

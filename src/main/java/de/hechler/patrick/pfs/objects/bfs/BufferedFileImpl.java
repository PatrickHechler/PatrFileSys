package de.hechler.patrick.pfs.objects.bfs;

import java.io.IOException;
import java.lang.ref.WeakReference;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.objects.IntHashMap;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;


public class BufferedFileImpl extends BufferedFileSysElementImpl implements PatrFile {
	
	public BufferedFileImpl(PatrFileSysElementBuffer buffer) {
		super(buffer);
	}
	
	@Override
	public void getContent(byte[] bytes, long offset, int bytesOff, int length, long lock) throws IllegalArgumentException, IOException, ElementLockedException {
		if (bytesOff < 0 || length < 0 || offset < 0L || bytesOff + length > bytes.length) {
			throw new IllegalArgumentException("bytesOff=" + bytesOff + " offset=" + offset + " length=" + length + " bytes.len=" + bytes.length);
		}
		synchronized (buffer) {
			int blockSize = buffer.fs.blockSize();
			final long mod = offset % (long) blockSize,
				key = ( (offset - mod) / blockSize);
			final int imod = (int) mod, ikey = (int) key;
			PatrFile file = file();
			if (key != ikey && length + imod <= blockSize) {
				if (buffer.value != null) {
					WeakReference <byte[]> wr = buffer.value.get(ikey);
					if (wr != null) {
						byte[] saved = wr.get();
						if (saved != null) {
							System.arraycopy(saved, imod, bytes, bytesOff, length);
							return;
						}
					}
				} else {
					buffer.value = new IntHashMap <>();
				}
				byte[] save = new byte[blockSize];
				file.getContent(bytes, offset - mod, 0, blockSize, lock);
				System.arraycopy(save, imod, bytes, bytesOff, length);
				buffer.value.put(ikey, new WeakReference <>(save));
				return;
			} else {
				file.getContent(bytes, offset, bytesOff, length, lock);
			}
		}
	}
	
	@Override
	public void removeContent(long offset, long length, long lock) throws IllegalArgumentException, IOException, ElementLockedException {
		if (length < 0 || offset < 0L) {
			throw new IllegalArgumentException("offset=" + offset + " length=" + length);
		}
		synchronized (buffer) {
			file().removeContent(offset, length, lock);
			buffer.value = null;
			buffer.dataChanged = true;
			buffer.fs.changeSize();
		}
	}
	
	@Override
	public void setContent(byte[] bytes, long offset, int bytesOff, int length, long lock) throws IllegalArgumentException, IOException, ElementLockedException {
		if (bytesOff < 0 || length < 0 || offset < 0L || bytesOff + length > bytes.length) {
			throw new IllegalArgumentException("bytesOff=" + bytesOff + " offset=" + offset + " length=" + length + " bytes.len=" + bytes.length);
		}
		synchronized (buffer) {
			final int blockSize = buffer.fs.blockSize();
			final long mod = offset % (long) blockSize,
				key = ( (offset - mod) / blockSize);
			final int imod = (int) mod, ikey = (int) key;
			file().setContent(bytes, offset, bytesOff, length, lock);
			buffer.dataChanged = true;
			if (buffer.value != null) {
				for (int block = ikey, boff = bytesOff;; block ++ ) {
					WeakReference <byte[]> wr = buffer.value.get(block);
					if (wr != null) {
						byte[] saved = wr.get();
						if (saved != null) {
							System.arraycopy(bytes, boff, saved, imod, Math.min(blockSize, bytesOff + length - boff));
						}
					}
					boff += blockSize;
					if (boff >= bytesOff + length) {
						break;
					}
				}
			}
		}
	}
	
	@Override
	public void appendContent(byte[] bytes, int bytesOff, int length, long lock) throws IllegalArgumentException, IOException, ElementLockedException {
		if (bytesOff < 0 || length < 0 || bytesOff + length > bytes.length) {
			throw new IllegalArgumentException("bytesOff=" + bytesOff + " length=" + length + " bytes.len=" + bytes.length);
		}
		synchronized (buffer) {
			PatrFile file = file();
			file.withLock(() -> {
				if (buffer.value != null) {
					final int blockSize = buffer.fs.blockSize();
					final long len = file.length(),
						mod = len % blockSize,
						block = (len - mod) / blockSize;
					final int key = (int) block;
					buffer.value.remove(key);
				}
				file.appendContent(bytes, bytesOff, length, lock);
			});
			buffer.dataChanged = true;
		}
	}
	
	@Override
	public byte[] getHashCode(long lock) throws IOException, ElementLockedException {
		synchronized (buffer) {
			byte[] hash;
			if (buffer.hash == null || buffer.dataChanged) {
				hash = file().getHashCode(lock);
				buffer.hash = new WeakReference <>(hash.clone());
			} else {
				hash = buffer.hash.get();
				if (hash != null) {
					buffer.element.ensureAccess(lock, PatrFileSysConstants.LOCK_NO_READ_ALLOWED_LOCK, false);
					return hash.clone();
				}
				hash = file().getHashCode(lock);
				buffer.hash = new WeakReference <>(hash.clone());
			}
			return hash;
		}
	}
	
	@Override
	public long length() throws IOException {
		synchronized (buffer) {
			if (buffer.length == 0L || buffer.dataChanged) {
				buffer.length = file().length();
			}
			return buffer.length;
		}
	}
	
}

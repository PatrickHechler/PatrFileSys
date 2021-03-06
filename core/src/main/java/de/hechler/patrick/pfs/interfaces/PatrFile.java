package de.hechler.patrick.pfs.interfaces;

import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_READ_ALLOWED_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_WRITE_ALLOWED_LOCK;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.objects.fs.PatrFileInputStream;
import de.hechler.patrick.pfs.objects.fs.PatrFileOutputStream;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;

public interface PatrFile extends PatrFileSysElement {
	
	/**
	 * fills the {@code bytes} from {@code bytesOff} to {@code bytesOff + length} with the content of
	 * this file from {@code offset} to {@code offset + length}.
	 * 
	 * @param bytes
	 *            the bytes to be filled with the content
	 * @param offset
	 *            the offset in the file
	 * @param bytesOff
	 *            the offset in the byte array {@code bytes}
	 * @param length
	 *            the count of bytes to be read/copied from this file to the byte array {@code bytes}
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @throws IllegalArgumentException
	 *             if {@code offset < 0} or {@code length < 0} or
	 *             <code>offset + length > {@link #length()}</code> or
	 *             {@code bytesOff + length > bytes.length}
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 */
	void getContent(byte[] bytes, long offset, int bytesOff, int length, long lock) throws IllegalArgumentException, IOException, ElementLockedException;
	
	/**
	 * removes the content from this file.<br>
	 * after this operation the files {@link #length()} will be {@code 0}
	 * <p>
	 * this method is like {@link #truncate(long, long)} with {@code 0L} as {@code size} argument.
	 * 
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 * @see #truncate(long, long)
	 */
	default void removeContent(long lock) throws IOException, ElementLockedException {
		truncate(0L, lock);
	}
	
	/**
	 * truncates the file.<br>
	 * after this operation the {@link #length()} of the file will be {@code size}.
	 * <p>
	 * if {@code size} is smaller than {@code 0} or greater than or equal to {@link #length()} an
	 * {@link IllegalArgumentException} will be thrown
	 * 
	 * @param size
	 *            the new {@link #length()}
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @throws IllegalArgumentException
	 *             if {@code size} is below {@code 0L} or larger than {@link #length()}
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 */
	void truncate(long size, long lock) throws IllegalArgumentException, IOException, ElementLockedException;
	
	/**
	 * overwrites the content from {@code offset} to {@code offset + length} with the values from
	 * {@code bytes} at the area from {@code bytesOff} to {@code bytesOff + length}
	 * 
	 * @param bytes
	 *            the byte array with the new content
	 * @param offset
	 *            the offset in the file of the new content
	 * @param bytesOff
	 *            the offset in the byte array {@code bytes} of the new content
	 * @param length
	 *            the number of bytes in the new content
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @throws IllegalArgumentException
	 *             if {@code offset < 0} or {@code length < 0} or
	 *             <code>offset + length > {@link #length()}</code> or
	 *             {@code bytesOff + length > bytes.length}
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 */
	void setContent(byte[] bytes, long offset, int bytesOff, int length, long lock) throws IllegalArgumentException, IOException, ElementLockedException;
	
	/**
	 * appends the given content in the {@code bytes} from {@code bytesOff} to {@code bytesOff + length}
	 * to this file
	 * 
	 * @param bytes
	 *            the byte array containing the bytes to be appended to this file
	 * @param bytesOff
	 *            the offset in the byte array of the content to be appended
	 * @param length
	 *            the number of bytes to be appended to this file
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @throws IllegalArgumentException
	 *             if {@code bytesOff < 0} or {@code length < 0} or
	 *             {@code bytesOff + length > bytes.length}
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 */
	void appendContent(byte[] bytes, int bytesOff, int length, long lock) throws IllegalArgumentException, IOException, ElementLockedException;
	
	/**
	 * returns the length of this file in bytes
	 * 
	 * @return the length of this file in bytes
	 * @throws IOException
	 *             if an IO error occurs
	 */
	long length(long lock) throws IOException;
	
	/**
	 * deletes this file from the file system.<br>
	 * if the file still has content, this operation will remove the content first.
	 * 
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 */
	void delete(long myLock, long parentLocks) throws IOException, ElementLockedException;
	
	@Override
	default boolean isFile() throws IOException { return true; }
	
	@Override
	default boolean isFolder() throws IOException { return false; }
	
	@Override
	default boolean isLink() throws IOException { return false; }
	
	@Override
	default PatrFile getFile() throws IllegalStateException, IOException { return this; }
	
	@Override
	default PatrFolder getFolder() throws IllegalStateException, IOException {
		throw new IllegalStateException("this is no folder!");
	}
	
	@Override
	default PatrLink getLink() throws IllegalStateException, IOException {
		throw new IllegalStateException("this is no link!");
	}
	
	/**
	 * opens a new {@link InputStream} for this file.
	 * 
	 * @param lock
	 *            the lock used by the read operations of the stream
	 * @return the input stream
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             if the element is currently locked
	 */
	default InputStream openInput(long lock) throws IOException, ElementLockedException {
		return withLock(() -> {
			ensureAccess(lock, LOCK_NO_READ_ALLOWED_LOCK, false);
			return new PatrFileInputStream(this, lock);
		});
	}
	
	default OutputStream openOutput(boolean append, long lock) throws IOException, ElementLockedException {
		return withLock(() -> {
			ensureAccess(lock, LOCK_NO_WRITE_ALLOWED_LOCK, true);
			return new PatrFileOutputStream(this, append, lock);
		});
	}
	
}

package de.hechler.patrick.pfs.interfaces;

import java.io.IOException;

public interface PatrFile extends PatrFileSysElement {
	
	/**
	 * fills the {@code bytes} from {@code bytesOff} to {@code bytesOff + length} with the content of this file from {@code offset} to {@code offset + length}.
	 * 
	 * @param bytes
	 *            the bytes to be filled with the content
	 * @param offset
	 *            the offset in the file
	 * @param bytesOff
	 *            the offset in the byte array {@code bytes}
	 * @param length
	 *            the count of bytes to be read/copied from this file to the byte array {@code bytes}
	 * @throws IllegalArgumentException
	 *             if {@code offset < 0} or {@code length < 0} or <code>offset + length > {@link #length()}</code> or {@code bytesOff + length > bytes.length}
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void getContent(byte[] bytes, long offset, int bytesOff, int length) throws IllegalArgumentException, IOException;
	
	/**
	 * removes the content from {@code offset} to {@code offset + length} from this file.
	 * 
	 * @param offset
	 *            the offset of the block to be removed
	 * @param length
	 *            the number of bytes to be removed
	 * @throws IllegalArgumentException
	 *             if {@code offset < 0} or {@code length < 0} or <code>offset + length > {@link #length()}</code>
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void removeContent(long offset, long length) throws IllegalArgumentException, IOException;
	
	/**
	 * overwrites the content from {@code offset} to {@code offset + length} with the values from {@code bytes} at the area from {@code bytesOff} to {@code bytesOff + length}
	 * 
	 * @param bytes
	 *            the byte array with the new content
	 * @param offset
	 *            the offset in the file of the new content
	 * @param bytesOff
	 *            the offset in the byte array {@code bytes} of the new content
	 * @param length
	 *            the number of bytes in the new content
	 * @throws IllegalArgumentException
	 *             if {@code offset < 0} or {@code length < 0} or <code>offset + length > {@link #length()}</code> or {@code bytesOff + length > bytes.length}
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void setContent(byte[] bytes, long offset, int bytesOff, int length) throws IllegalArgumentException, IOException;
	
	/**
	 * appends the given content in the {@code bytes} from {@code bytesOff} to {@code bytesOff + length} to this file
	 * 
	 * @param bytes
	 *            the byte array containing the bytes to be appended to this file
	 * @param bytesOff
	 *            the offset in the byte array of the content to be appended
	 * @param length
	 *            the number of bytes to be appended to this file
	 * @throws IllegalArgumentException
	 *             if {@code bytesOff < 0} or {@code length < 0} or {@code bytesOff + length > bytes.length}
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void appendContent(byte[] bytes, int bytesOff, int length) throws IllegalArgumentException, IOException;
	
	/**
	 * returns the length of this file in bytes
	 * 
	 * @return the length of this file in bytes
	 * @throws IOException
	 *             if an IO error occurs
	 */
	long length() throws IOException;
	
	/**
	 * deletes this file from the file system.<br>
	 * 
	 * if the file still has content, this operation will remove the content first.
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void delete() throws IOException;
	
	@Override
	default boolean isFile() throws IOException {
		return true;
	}
	
	@Override
	default boolean isFolder() throws IOException {
		return false;
	}
	
	@Override
	default PatrFile getFile() throws IllegalStateException, IOException {
		return this;
	}
	
	@Override
	default PatrFolder getFolder() throws IllegalStateException, IOException {
		throw new IllegalStateException("this is no folder!");
	}
	
}

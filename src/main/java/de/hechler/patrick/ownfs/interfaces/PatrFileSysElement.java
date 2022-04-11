package de.hechler.patrick.ownfs.interfaces;

import java.io.IOException;

public interface PatrFileSysElement {
	
	/**
	 * returns the parent folder of this element.<br>
	 * this method will throw an {@link IllegalStateException} if this element is the root element
	 * 
	 * @return the parent folder of this element
	 * @throws IllegalStateException
	 *             if this element is the root element
	 * @throws IOException
	 *             if an IO error occurs
	 */
	PatrFolder getParent() throws IllegalStateException, IOException;
	
	/**
	 * returns the folder representing this element<br>
	 * if this element is no folder an {@link IllegalStateException} will be thrown.
	 * 
	 * @return the folder representing this element
	 * @throws IllegalStateException
	 *             if this element is no folder
	 * @throws IOException
	 *             if an IO error occurs
	 * @see #isFolder()
	 */
	PatrFolder getFolder() throws IllegalStateException, IOException;
	
	/**
	 * returns the file representing this element<br>
	 * if this element is no file an {@link IllegalStateException} will be thrown.
	 * 
	 * @return the file representing this element
	 * @throws IllegalStateException
	 *             if this element is no file
	 * @throws IOException
	 *             if an IO error occurs
	 * @see #isFile()
	 */
	PatrFile getFile() throws IllegalStateException, IOException;
	
	/**
	 * returns <code>true</code> if this element represents a folder
	 * 
	 * @return <code>true</code> if this element represents a folder
	 * @throws IOException
	 *             if an IO error occurs
	 * @see #getFolder()
	 */
	boolean isFolder() throws IOException;
	
	/**
	 * returns <code>true</code> if this element represents a file
	 * 
	 * @return <code>true</code> if this element represents a file
	 * @throws IOException
	 *             if an IO error occurs
	 * @see #getFile()
	 */
	boolean isFile() throws IOException;
	
	/**
	 * returns the metadata of this element
	 * 
	 * @return the metadata of this element
	 * @throws IOException
	 *             if an IO error occurs
	 */
	byte[] getMetadata() throws IOException;
	
	/**
	 * sets the metadata of this element
	 * 
	 * @param data
	 *            the new metadata
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void setMetadata(byte[] data) throws IOException;
	
	/**
	 * returns <code>true</code> if this element is marked as executable
	 * 
	 * @return <code>true</code> if this element is marked as executable
	 * @throws IOException
	 *             if an IO error occurs
	 */
	boolean isExecutable() throws IOException;
	
	/**
	 * returns <code>true</code> if this element is marked as hidden
	 * 
	 * @return <code>true</code> if this element is marked as hidden
	 * @throws IOException
	 *             if an IO error occurs
	 */
	boolean isHidden() throws IOException;
	
	/**
	 * returns <code>true</code> if this element is marked as read only
	 * 
	 * @return <code>true</code> if this element is marked as read only
	 * @throws IOException
	 *             if an IO error occurs
	 */
	boolean isReadOnly() throws IOException;
	
}

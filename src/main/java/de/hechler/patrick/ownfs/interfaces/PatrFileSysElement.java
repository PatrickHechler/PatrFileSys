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
	
}

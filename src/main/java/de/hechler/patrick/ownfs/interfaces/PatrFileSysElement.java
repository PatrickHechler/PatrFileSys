package de.hechler.patrick.ownfs.interfaces;

import java.io.IOException;

import de.hechler.patrick.ownfs.utils.PatrFileSysConstants;

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
	
	/**
	 * returns the owner of this element
	 * 
	 * @return the owner of this element
	 * @throws IOException
	 *             if an IO error occurs
	 */
	int getOwner() throws IOException;
	
	/**
	 * sets the owner of this element
	 * 
	 * @param owner
	 *            the new owner of this element
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void setOwner(int owner) throws IOException;
	
	/**
	 * returns the time of the creation of this element
	 * 
	 * @return the time this element was created
	 * @throws IOException
	 *             if an IO error occurs
	 */
	long getCreateTime() throws IOException;
	
	
	/**
	 * returns the last time this element was modified
	 * 
	 * @return the last time this element was modified
	 * @throws IOException
	 *             if an IO error occurs
	 */
	long getLastModTime() throws IOException;
	
	/**
	 * returns the lock of this element
	 * 
	 * @return the lock of this element
	 * @throws IOException
	 *             if an IO error occurs
	 */
	long getLock() throws IOException;
	
	/**
	 * returns the time this element was locked
	 * 
	 * @return the time this element was locked
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws IllegalStateException
	 *             if this element is not locked
	 */
	long getLockTime() throws IOException, IllegalStateException;
	
	/**
	 * removes the lock from this element if the given lock is equal to the lock of this element of if the given lock is {@link PatrFileSysConstants#LOCK_NO_LOCK}
	 * 
	 * @param lock
	 *            the lock to remove
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws IllegalArgumentException
	 *             if the locks are not equal
	 */
	void removeLock(long lock) throws IOException, IllegalArgumentException;
	
	String getName() throws IOException;
	
}

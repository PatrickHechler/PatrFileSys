package de.hechler.patrick.pfs.interfaces;

import java.io.IOException;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;

public interface PatrLink extends PatrFileSysElement {
	
	/**
	 * returns the target element of this link
	 * 
	 * @return the target element of this link
	 * @throws IOException
	 *             if an IO error occurs
	 */
	PatrFileSysElement getTarget() throws IOException;
	
	/**
	 * returns the target file of this link
	 * <p>
	 * if the target element is no file an {@link IllegalStateException} will be thrown.
	 * 
	 * @return the target file
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws IllegalStateException
	 *             if the target of this link is no file
	 */
	@Override
	PatrFile getFile() throws IOException, IllegalStateException;
	
	/**
	 * returns the target folder of this link
	 * <p>
	 * if the target element is no folder an {@link IllegalStateException} will be thrown.
	 * 
	 * @return the target folder
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws IllegalStateException
	 *             if the target of this link is no file
	 */
	@Override
	PatrFolder getFolder() throws IOException, IllegalStateException;
	
	/**
	 * sets the target of this link
	 * 
	 * @param newTarget
	 *            the new target of this link
	 * @param lock
	 *            the lock of this link or {@link PatrFileSysConstants#LOCK_NO_LOCK} if there is no lock
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws IllegalArgumentException
	 *             if {@code newTarget} is invalid (for example it belongs to an different file system)
	 */
	void setTarget(PatrFileSysElement newTarget, long lock) throws IOException, IllegalArgumentException;
	
	/**
	 * deletes this element from the file system.
	 * 
	 * 
	 * @param myLock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @param myLock
	 *            the current of the parent element lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 */
	@Override
	void delete(long myLock, long parentLock) throws IOException, ElementLockedException;
	
}

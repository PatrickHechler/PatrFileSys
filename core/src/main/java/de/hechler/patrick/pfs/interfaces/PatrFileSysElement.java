package de.hechler.patrick.pfs.interfaces;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingBooleanSupplier;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingIntSupplier;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingLongSupplier;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingRunnable;
import de.hechler.patrick.pfs.interfaces.functional.ThrowingSupplier;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;

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
	 * changes the parent of this element.
	 * 
	 * @param newParent
	 *            the new parent of this element
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @throws IllegalStateException
	 *             if this element is the root folder
	 * @throws IllegalArgumentException
	 *             if the given argument can not be the parent of this element (for example the parent may be in an other file system)
	 * @throws NullPointerException
	 *             if {@code newParent} is <code>null</code>
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             when this element or one of the parents (old/new) is locked with a different lock
	 * @throws FileAlreadyExistsException
	 *             if the new parent has already a child with the same name
	 */
	void setParent(PatrFolder newParent, long myLock, long oldParentLock, long newParentLock)
		throws IllegalStateException, IllegalArgumentException, NullPointerException, IOException, ElementLockedException, FileAlreadyExistsException;
	
	/**
	 * changes the parent and the name of this element
	 * 
	 * @param newParent
	 *            the new parent folder of this element
	 * @param newName
	 *            the new name of this element
	 * @param myLock
	 *            the current lock of this element or {@link PatrFileSysConstants#NO_LOCK}
	 * @param oldParentLock
	 *            the current lock of the old parent folder or {@link PatrFileSysConstants#NO_LOCK}
	 * @param newParentLock
	 *            the current lock of the new parent folder or {@link PatrFileSysConstants#NO_LOCK}
	 * @throws IllegalStateException
	 *             if this element is the root folder
	 * @throws IllegalArgumentException
	 *             if {@code newParent} is from a different file system
	 * @throws NullPointerException
	 *             if {@code newParent} or {@code newName} is <code>null</code>
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             if this element, its parent or the (not) new parent is locked with a different lock
	 * @throws FileAlreadyExistsException
	 *             if the new parent already has a child element with the given name
	 */
	void move(PatrFolder newParent, String newName, long myLock, long oldParentLock, long newParentLock)
		throws IllegalStateException, IllegalArgumentException, NullPointerException, IOException, ElementLockedException, FileAlreadyExistsException;
	
	/**
	 * returns the folder representing this element
	 * <p>
	 * if this element is a link and the target represents a folder the target folder will be returned
	 * <p>
	 * if this element is no folder an {@link IllegalStateException} will be thrown.
	 * 
	 * @return the folder representing this element or the target folder of this link
	 * @throws IllegalStateException
	 *             if this element is no folder
	 * @throws IOException
	 *             if an IO error occurs
	 * @see #isFolder()
	 */
	PatrFolder getFolder() throws IllegalStateException, IOException;
	
	/**
	 * returns the file representing this element
	 * <p>
	 * if this element is a link and the target represents a file the target file will be returned
	 * <p>
	 * if this element is no file an {@link IllegalStateException} will be thrown.
	 * 
	 * @return the file representing this element or the target file of this link
	 * @throws IllegalStateException
	 *             if this element is no file
	 * @throws IOException
	 *             if an IO error occurs
	 * @see #isFile()
	 */
	PatrFile getFile() throws IllegalStateException, IOException;
	
	/**
	 * returns the file representing this element<br>
	 * if this element is no file an {@link IllegalStateException} will be thrown.
	 * 
	 * @return the file representing this element
	 * @throws IllegalStateException
	 *             if this element is no link
	 * @throws IOException
	 *             if an IO error occurs
	 * @see #isFile()
	 */
	PatrLink getLink() throws IllegalStateException, IOException;
	
	/**
	 * returns the id from this element
	 * 
	 * @return the id from this element
	 * @throws IOException
	 *             if an IOError occurs
	 */
	Object getID() throws IOException;
	
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
	 * returns <code>true</code> if this element represents a link
	 * 
	 * @return <code>true</code> if this element represents a link
	 * @throws IOException
	 *             if an IO error occurs
	 * @see #getLink()
	 */
	boolean isLink() throws IOException;
	
	/**
	 * returns <code>true</code> if this element is marked as executable
	 * 
	 * @return <code>true</code> if this element is marked as executable
	 * @throws IOException
	 *             if an IO error occurs
	 */
	boolean isExecutable() throws IOException;
	
	/**
	 * sets the executable flag of this element
	 * 
	 * @param isExecutale
	 *            <code>true</code> if this element should be marked as executable and <code>false</code> if not
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void setExecutable(boolean isExecutale, long lock) throws IOException, ElementLockedException;
	
	/**
	 * returns <code>true</code> if this element is marked as hidden
	 * 
	 * @return <code>true</code> if this element is marked as hidden
	 * @throws IOException
	 *             if an IO error occurs
	 */
	boolean isHidden() throws IOException;
	
	/**
	 * sets the hidden flag of this element
	 * 
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @param isHidden
	 *            <code>true</code> if this element should be marked as hidden and <code>false</code> if not
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void setHidden(boolean isHidden, long lock) throws IOException, ElementLockedException;
	
	/**
	 * returns <code>true</code> if this element is marked as read only
	 * 
	 * @return <code>true</code> if this element is marked as read only
	 * @throws IOException
	 *             if an IO error occurs
	 */
	boolean isReadOnly() throws IOException;
	
	/**
	 * sets the read-only flag of this element
	 * 
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @param isReadOnly
	 *            <code>true</code> if this element should be marked as read only and <code>false</code> if not
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void setReadOnly(boolean isReadOnly, long lock) throws IOException, ElementLockedException;
	
	/**
	 * returns the time of the creation of this element
	 * 
	 * @return the time this element was created
	 * @throws IOException
	 *             if an IO error occurs
	 */
	long getCreateTime() throws IOException;
	
	/**
	 * sets the create time of this element
	 * 
	 * @param createTime
	 *            the time when this element should be marked as created
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#NO_LOCK}
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 */
	void setCreateTime(long createTime, long lock) throws IOException, ElementLockedException;
	
	/**
	 * returns the last time this element was modified
	 * 
	 * @return the last time this element was modified
	 * @throws IOException
	 *             if an IO error occurs
	 */
	long getLastModTime() throws IOException;
	
	/**
	 * sets the last modify time of this element
	 * 
	 * @param lastModTime
	 *            the time when this element should be marked as last modified
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#NO_LOCK}
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 */
	void setLastModTime(long lastModTime, long lock) throws IOException, ElementLockedException;
	
	/**
	 * returns the last time this element or it's metadata was modified
	 * 
	 * @return the last time this element or it's metadata was modified
	 * @throws IOException
	 *             if an IO error occurs
	 */
	long getLastMetaModTime() throws IOException;
	
	/**
	 * sets the last meta modify time of this element
	 * 
	 * @param lastMetaModTime
	 *            the time when this element should be marked as last modified on meta data
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#NO_LOCK}
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 */
	void setLastMetaModTime(long lastMetaModTime, long lock) throws IOException, ElementLockedException;
	
	/**
	 * returns the data of the current lock <code>(current_lock & {@link PatrFileSysConstants#LOCK_DATA})</code>
	 * 
	 * @return <code>(current_lock & {@link PatrFileSysConstants#LOCK_DATA})</code>
	 * @throws IOException
	 *             if an IO error occurs
	 */
	long getLockData() throws IOException;
	
	/**
	 * returns the time this element was locked or {@link PatrFileSysConstants#NO_TIME} if the element has {@link PatrFileSysConstants#LOCK_LOCKED_LOCK} as lock
	 * 
	 * @return the time this element was locked or {@link PatrFileSysConstants#NO_TIME} if the element has {@link PatrFileSysConstants#LOCK_LOCKED_LOCK} as lock
	 * @throws IOException
	 *             if an IO error occurs
	 */
	long getLockTime() throws IOException;
	
	/**
	 * ensures, that this element can be accessed with the given lock.<br>
	 * if {@code lock} is {@link PatrFileSysConstants#NO_LOCK} it ensures that <code>(current_lock & forbiddenBits)</code> is {@code 0}.<br>
	 * if {@code readOnlyForbidden} is <code>true</code> and the {@link #isReadOnly()} flag is set an {@link ElementLockedException} will be thrown.
	 * 
	 * @param lock
	 *            the lock or {@link PatrFileSysConstants#NO_LOCK}
	 * @param forbiddenBits
	 *            the forbidden bits if {@code lock} is {@link PatrFileSysConstants#NO_LOCK}
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             if the access is denied
	 * @throws IllegalArgumentException
	 *             if the forbidden bits asks for non data bits (<code>{@link PatrFileSysConstants#LOCK_NO_DATA} & forbiddenBits</code> must be {@code 0})
	 */
	void ensureAccess(long lock, long forbiddenBits) throws IOException, ElementLockedException, IllegalArgumentException;
	
	/**
	 * removes the lock from this element if the given lock is equal to the lock of this element of if the given lock is {@link PatrFileSysConstants#NO_LOCK}
	 * <p>
	 * the always remove with {@link PatrFileSysConstants#NO_LOCK} function of this method should only be used with great care
	 * 
	 * @param lock
	 *            the lock to remove
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             if the locks are not equal
	 */
	void removeLock(long lock) throws IOException, ElementLockedException;
	
	/**
	 * locks this element.<br>
	 * this operation will throw an {@link IllegalStateException} if this element is already non-shared locked or if a non shared lock is requested.<br>
	 * this operation will also throw an {@link IllegalStateException} if this element is shared locked with different lock-data
	 * <p>
	 * if {@code lock} is {@link PatrFileSysConstants#NO_LOCK} an {@link IllegalArgumentException} will be thrown.
	 * <p>
	 * the lock data with wich this element should be locked. when this is no shared lock it contains also the user
	 * 
	 * @return the new lock of this element
	 * @param lock
	 *            the lock data with wich this element should be locked. when this is no shared lock it contains also the user
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 * @throws IllegalArgumentException
	 *             if {@code lock} is {@link PatrFileSysConstants#NO_LOCK}
	 */
	long lock(long lock) throws IOException, IllegalStateException, ElementLockedException;
	
	/**
	 * deletes this element from the file system.<br>
	 * this operation will fail, if this element is not empty, but a folder.
	 * 
	 * @param myLock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @param myLock
	 *            the current of the parent element lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 * @throws IllegalStateException
	 *             if this element can not be deleted currently
	 */
	void delete(long myLock, long parentLock) throws IOException, IllegalStateException, ElementLockedException;
	
	/**
	 * sets the name of this element
	 * 
	 * @param name
	 *            the new name
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 * @throws NullPointerException
	 *             if the new name is null
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws FileAlreadyExistsException
	 *             if the current parent has already a child with the given name
	 */
	void setName(String name, long lock) throws IOException, NullPointerException, ElementLockedException, FileAlreadyExistsException;
	
	/**
	 * returns the name of this element.<br>
	 * if this element is the root folder an empty string is returned
	 * 
	 * @return the name of this element
	 * @throws IOException
	 *             if an IO error occurs
	 */
	String getName() throws IOException;
	
	/**
	 * executes the given throwing runnable with to lock of this file system element. this may be the lock for the entire file system
	 * <p>
	 * the method will also ensure that the block of this element is loaded, so the access will be a bit faster.
	 * 
	 * @param <T>
	 *            the exception type which may be thrown
	 * @param exec
	 *            the runnable, which should be executed
	 * @throws T
	 *             when the given throwing runnable throws the given exception
	 */
	<T extends Throwable> void withLock(ThrowingRunnable <T> exec) throws T, IOException;
	
	/**
	 * executes the given throwing runnable with to lock of this file system element. this may be the lock for the entire file system.<br>
	 * in addition to {@link #withLock(ThrowingRunnable)} this method returns the value returned by the supplier
	 * <p>
	 * the method will also ensure that the block of this element is loaded, so the access will be a bit faster.
	 * 
	 * @param <T>
	 *            the exception type which may be thrown
	 * @param exec
	 *            the supplier, which should be executed
	 * @return the return value of the supplier
	 * @throws T
	 *             when the given throwing runnable throws the given exception
	 */
	<T extends Throwable, R> R withLock(ThrowingSupplier <T, R> exec) throws T, IOException;
	
	/**
	 * executes the given throwing runnable with to lock of this file system element. this may be the lock for the entire file system.<br>
	 * in addition to {@link #withLock(ThrowingRunnable)} this method returns the int-value returned by the int-supplier
	 * <p>
	 * the method will also ensure that the block of this element is loaded, so the access will be a bit faster.
	 * 
	 * @param <T>
	 *            the exception type which may be thrown
	 * @param exec
	 *            the supplier, which should be executed
	 * @return the int-return-value of the supplier
	 * @throws T
	 *             when the given throwing runnable throws the given exception
	 */
	<T extends Throwable> int withLockInt(ThrowingIntSupplier <T> exec) throws T, IOException;
	
	/**
	 * executes the given throwing runnable with to lock of this file system element. this may be the lock for the entire file system.<br>
	 * in addition to {@link #withLock(ThrowingRunnable)} this method returns the long-value returned by the long-supplier
	 * <p>
	 * the method will also ensure that the block of this element is loaded, so the access will be a bit faster.
	 * 
	 * @param <T>
	 *            the exception type which may be thrown
	 * @param exec
	 *            the supplier, which should be executed
	 * @return the long-return-value of the supplier
	 * @throws T
	 *             when the given throwing runnable throws the given exception
	 */
	<T extends Throwable> long withLockLong(ThrowingLongSupplier <T> exec) throws T, IOException;
	
	/**
	 * executes the given throwing runnable with to lock of this file system element. this may be the lock for the entire file system.<br>
	 * in addition to {@link #withLock(ThrowingRunnable)} this method returns the boolean-value returned by the boolean-supplier
	 * <p>
	 * the method will also ensure that the block of this element is loaded, so the access will be a bit faster.
	 * 
	 * @param <T>
	 *            the exception type which may be thrown
	 * @param exec
	 *            the supplier, which should be executed
	 * @return the boolean-return-value of the supplier
	 * @throws T
	 *             when the given throwing runnable throws the given exception
	 */
	<T extends Throwable> boolean withLockBoolean(ThrowingBooleanSupplier <T> exec) throws T, IOException;
	
	/**
	 * this method is equal to <code>
	 * <pre>
	 * if (obj instanceof {@link PatrFileSysElement}) {
	 *     return equal(({@link PatrFileSysElement}) obj);
	 * } else {
	 *     return false;
	 * }
	 * </pre>
	 * </code>
	 * 
	 * @see #equals(PatrFileSysElement)
	 */
	@Override
	boolean equals(Object obj);
	
	boolean equals(PatrFileSysElement other);
	
}

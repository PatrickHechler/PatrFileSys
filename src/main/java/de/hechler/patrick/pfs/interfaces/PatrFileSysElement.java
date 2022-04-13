package de.hechler.patrick.pfs.interfaces;

import java.io.IOException;

import de.hechler.patrick.pfs.exception.ElementLockedException;
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
	 */
	void setParent(PatrFolder newParent, long myLock, long oldParentLock, long newParentLock)
		throws IllegalStateException, IllegalArgumentException, NullPointerException, IOException, ElementLockedException;
	
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
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @param owner
	 *            the new owner of this element
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void setOwner(int owner, long lock) throws IOException, ElementLockedException;
	
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
	 * returns the last time this element or it's metadata was modified
	 * 
	 * @return the last time this element or it's metadata was modified
	 * @throws IOException
	 *             if an IO error occurs
	 */
	long getLastMetaModTime() throws IOException;
	
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
	 * if {@code lock} is {@link PatrFileSysConstants#LOCK_NO_LOCK} it ensures that <code>(current_lock & forbiddenBits)</code> is {@code 0}.
	 * 
	 * @param lock
	 *            the lock or {@link PatrFileSysConstants#LOCK_NO_LOCK}
	 * @param forbiddenBits
	 *            the forbidden bits if {@code lock} is {@link PatrFileSysConstants#LOCK_NO_LOCK}
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             if the access is denied
	 * @throws IllegalArgumentException
	 *             if the forbidden bits asks for non data bits (<code>{@link PatrFileSysConstants#LOCK_NO_DATA} & forbiddenBits</code> must be {@code 0})
	 */
	void ensureAccess(long lock, long forbiddenBits) throws IOException, ElementLockedException, IllegalArgumentException;
	
	/**
	 * removes the lock from this element if the given lock is equal to the lock of this element of if the given lock is {@link PatrFileSysConstants#LOCK_NO_LOCK}
	 * <p>
	 * the always remove with {@link PatrFileSysConstants#LOCK_NO_LOCK} function of this method should only be used with great care
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
	 * if {@code lock} is {@link PatrFileSysConstants#LOCK_NO_LOCK} an {@link IllegalArgumentException} will be thrown.
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
	 *             if {@code lock} is {@link PatrFileSysConstants#LOCK_NO_LOCK}
	 */
	long lock(long lock) throws IOException, IllegalStateException, ElementLockedException;
	
	/**
	 * sets the name of this element<br>
	 * if this element is the root element an {@link IllegalStateException} will be thrown
	 * 
	 * @param name
	 *            the new name
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 * @throws NullPointerException
	 *             if the new name is null
	 * @throws IllegalStateException
	 *             if this is the root element
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void setName(String name, long lock) throws IOException, NullPointerException, IllegalStateException, ElementLockedException;
	
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
	 * this method also checks if the thread already has the lock (see: {@link Thread#holdsLock(Object)})
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
	 * this method also checks if the thread already has the lock (see: {@link Thread#holdsLock(Object)})
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
	 * this method also checks if the thread already has the lock (see: {@link Thread#holdsLock(Object)})
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
	 * this method also checks if the thread already has the lock (see: {@link Thread#holdsLock(Object)})
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
	 * executes the given throwing runnable with to lock of this file system element. this may be the lock for the entire file system
	 * <p>
	 * this method also checks if the thread already has the lock (see: {@link Thread#holdsLock(Object)})
	 * 
	 * @param <T>
	 *            the exception type which may be thrown
	 * @param exec
	 *            the runnable, which should be executed
	 * @throws T
	 *             when the given throwing runnable throws the given exception
	 */
	<T extends Throwable> void simpleWithLock(ThrowingRunnable <T> exec) throws T;
	
	/**
	 * executes the given throwing runnable with to lock of this file system element. this may be the lock for the entire file system.<br>
	 * in addition to {@link #simpleWithLock(ThrowingSupplier)} this method returns the value returned by the supplier
	 * <p>
	 * this method also checks if the thread already has the lock (see: {@link Thread#holdsLock(Object)})
	 * 
	 * @param <T>
	 *            the exception type which may be thrown
	 * @param exec
	 *            the supplier, which should be executed
	 * @return the return value of the supplier
	 * @throws T
	 *             when the given throwing runnable throws the given exception
	 */
	<T extends Throwable, R> R simpleWithLock(ThrowingSupplier <T, R> exec) throws T;
	
	/**
	 * executes the given throwing runnable with to lock of this file system element. this may be the lock for the entire file system.<br>
	 * in addition to {@link #simpleWithLock(ThrowingSupplier)} this method returns the int-value returned by the int-supplier
	 * <p>
	 * this method also checks if the thread already has the lock (see: {@link Thread#holdsLock(Object)})
	 * 
	 * @param <T>
	 *            the exception type which may be thrown
	 * @param exec
	 *            the supplier, which should be executed
	 * @return the int-return-value of the supplier
	 * @throws T
	 *             when the given throwing runnable throws the given exception
	 */
	<T extends Throwable> int simpleWithLockInt(ThrowingIntSupplier <T> exec) throws T;
	
	/**
	 * executes the given throwing runnable with to lock of this file system element. this may be the lock for the entire file system.<br>
	 * in addition to {@link #simpleWithLock(ThrowingSupplier)} this method returns the long-value returned by the long-supplier
	 * <p>
	 * this method also checks if the thread already has the lock (see: {@link Thread#holdsLock(Object)})
	 * 
	 * @param <T>
	 *            the exception type which may be thrown
	 * @param exec
	 *            the supplier, which should be executed
	 * @return the long-return-value of the supplier
	 * @throws T
	 *             when the given throwing runnable throws the given exception
	 */
	<T extends Throwable> long simpleWithLockLong(ThrowingLongSupplier <T> exec) throws T;
	
}
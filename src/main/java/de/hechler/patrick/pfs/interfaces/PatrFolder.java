package de.hechler.patrick.pfs.interfaces;

import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.*;

import java.io.IOException;
import java.util.Iterator;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.objects.fs.PatrFolderIterator;
import de.hechler.patrick.pfs.utils.PatrFileSysConstants;

public interface PatrFolder extends Iterable <PatrFileSysElement>, PatrFileSysElement {
	
	/**
	 * adds a folder with the specified name to this folder
	 * 
	 * @param name
	 *            the name of the new folder
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @return the new created folder
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws NullPointerException
	 *             if {@code name} is <code>null</code>
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 */
	PatrFolder addFolder(String name, long lock) throws IOException, NullPointerException, ElementLockedException;
	
	/**
	 * adds an empty file with the specified name to this folder
	 * 
	 * @param name
	 *            the name of the new file
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @return the new created file
	 * @throws NullPointerException
	 *             if {@code name} is <code>null</code>
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws NullPointerException
	 *             if <code>name</code> is <code>null</code>
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 */
	PatrFile addFile(String name, long lock) throws IOException, NullPointerException, ElementLockedException;
	
	/**
	 * adds an link with the given target and the specified name to this folder
	 * 
	 * @param name
	 *            the name of the new link
	 * @param target
	 *            the target of the new link
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @return the new created link
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws NullPointerException
	 *             if <code>name</code> or <code>target</code> is <code>null</code>
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 */
	PatrLink addLink(String name, PatrFileSysElement target, long lock) throws IOException, NullPointerException, ElementLockedException;
	
	/**
	 * deletes this Folder.<br>
	 * 
	 * an IllegalStateException will be thrown when this folder is not empty or the root folder
	 * 
	 * @param myLock
	 *            the current lock of this element or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @param parentLock
	 *            the current lock of the parent folder of this element or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @throws IllegalStateException
	 *             if this folder is not empty or if this is the root folder
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 * @see #canDeleteWithContent()
	 */
	void delete(long myLock, long parentLock) throws IllegalStateException, IOException, ElementLockedException;
	
	/**
	 * returns <code>true</code> if this folder is the root folder of the file system.
	 * 
	 * @return <code>true</code> if this folder is the root folder of the file system.
	 * @throws IOException
	 *             if an IO error occurs
	 */
	boolean isRoot() throws IOException;
	
	/**
	 * returns the number of elements in this folder
	 * 
	 * @return the number of elements in this folder
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 * @throws IOException
	 *             if an IO error occurs
	 */
	int elementCount(long lock) throws ElementLockedException, IOException;
	
	/**
	 * returns the child from the given index
	 * 
	 * @param index
	 *            the index of the element
	 * @param lock
	 *            the current lock or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @return the given child
	 * @throws IndexOutOfBoundsException
	 *             if the index is smaller than {@code 0} or greater than {@link #elementCount(long)}
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock
	 * @throws IOException
	 *             if an IO error occurs
	 */
	PatrFileSysElement getElement(int index, long lock) throws ElementLockedException, IOException;
	
	@Override
	default Iterator <PatrFileSysElement> iterator() {
		return iterator(e -> PatrFileSysConstants.LOCK_NO_LOCK);
	}
	
	default Iterator <PatrFileSysElement> iterator(long lock) {
		return iterator(e -> e == this ? lock : PatrFileSysConstants.LOCK_NO_LOCK);
	}
	
	default Iterator <PatrFileSysElement> iterator(LockSupplier lock) {
		return new PatrFolderIterator(this, lock);
	}
	
	/**
	 * deletes this folder even if it has children elements (which can also have children).<br>
	 * this operation fails, when at least one children has a non delete lock.
	 * 
	 * @param myLock
	 *            the current lock of this element or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @param parentLock
	 *            the current lock of the parent folder of this element or {@link PatrFileSysConstants#LOCK_LOCKED_LOCK}
	 * @throws ElementLockedException
	 *             when this element is locked with a different lock, or if at least one of it's children is locked
	 * @throws IOException
	 *             if an IO error occurs
	 */
	default void deepDelete(long myLock, long parentLock) throws IOException, ElementLockedException {
		withLock(() -> {
			PatrFolder p = getParent();
			deepDelete(e -> this == e ? myLock : p.equals(e) ? parentLock : PatrFileSysConstants.LOCK_NO_LOCK);
		});
	}
	
	/**
	 * deletes this folder even if it has children elements (which can also have children).<br>
	 * 
	 * @throws ElementLockedException
	 *             when a is locked with a different lock
	 * @throws IOException
	 *             if an IO error occurs
	 */
	default void deepDelete(LockSupplier lockSupplieer) throws IOException, ElementLockedException {
		withLock(() -> {
			PatrFolder parent = getParent();
			long delLock = lockSupplieer.getLock(this);
			long parentLock = lockSupplieer.getLock(parent);
			ensureAccess(delLock, LOCK_NO_DELETE_ALLOWED_LOCK, true);
			parent.ensureAccess(parentLock, LOCK_NO_WRITE_ALLOWED_LOCK, true);
			int ec = elementCount(delLock);
			while (ec > 0) {
				PatrFileSysElement pfe = this.getElement(ec - 1, delLock);
				if (pfe.isFolder()) {
					pfe.getFolder().deepDelete(lockSupplieer);
				} else {
					pfe.delete(lockSupplieer.getLock(pfe), delLock);
				}
				ec -- ;
			}
			ec = elementCount(delLock);
			assert ec == 0;
			delete(delLock, parentLock);
		});
	}
	
	@Override
	default boolean isFile() throws IOException {
		return false;
	}
	
	@Override
	default boolean isFolder() throws IOException {
		return true;
	}
	
	@Override
	default PatrFile getFile() throws IllegalStateException, IOException {
		throw new IllegalStateException("this is no file!");
	}
	
	@Override
	default PatrFolder getFolder() throws IllegalStateException, IOException {
		return this;
	}
	
	@FunctionalInterface
	interface LockSupplier {
		
		long getLock(PatrFileSysElement element) throws IOException;
		
	}
	
}

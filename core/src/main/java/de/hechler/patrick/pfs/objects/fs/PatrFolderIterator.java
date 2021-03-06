package de.hechler.patrick.pfs.objects.fs;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.interfaces.PatrFolder.LockSupplier;


public class PatrFolderIterator implements Iterator <PatrFileSysElement> {
	
	private final PatrFolder   folder;
	private final LockSupplier lockSupplier;
	private int                length;
	private int                index;
	
	public PatrFolderIterator(PatrFolder folder, LockSupplier lock) {
		try {
			this.folder = folder;
			this.lockSupplier = lock;
			this.length = folder.elementCount(lock.getLock(folder));
			this.index = 0;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public boolean hasNext() {
		return index < length;
	}
	
	@Override
	public PatrFileSysElement next() {
		try {
			return folder.withLock(this::executeNext);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private PatrFileSysElement executeNext() throws ElementLockedException, IOException {
		if (folder.elementCount(lockSupplier.getLock(folder)) != length) {
			throw new ConcurrentModificationException("added/removed items");
		}
		if (index >= length) {
			throw new NoSuchElementException("there are no more children");
		}
		PatrFileSysElement element = folder.getElement(index, lockSupplier.getLock(folder));
		index ++ ;
		return element;
	}
	
	@Override
	public void remove() {
		try {
			folder.withLock(this::executeRemove);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void executeRemove() throws ElementLockedException, IOException {
		if (index <= 0) {
			throw new IllegalStateException("I havn't read any child to remove");
		}
		index -- ;
		PatrFileSysElement element;
		element = folder.getElement(index, lockSupplier.getLock(folder));
		element.delete(lockSupplier.getLock(element), lockSupplier.getLock(folder));
	}
	
}

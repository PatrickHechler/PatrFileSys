package de.hechler.patrick.pfs.objects;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFolder;


public class PatrFolderIterator implements Iterator <PatrFileSysElement> {
	
	private final PatrFolder folder;
	private final long       lock;
	private int              length;
	private int              index;
	
	public PatrFolderIterator(PatrFolder folder, long lock) {
		try {
			this.folder = folder;
			this.lock = lock;
			this.length = folder.elementCount(lock);
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
			if (folder.elementCount(lock) != length) {
				throw new ConcurrentModificationException("added/removed items");
			}
			if (index >= length) {
				throw new NoSuchElementException("there are no more children");
			}
			PatrFileSysElement element = folder.getElement(index, lock);
			index ++ ;
			return element;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void remove() {
		if (index <= 0) {
			throw new IllegalStateException("I havn't read any child to remove");
		}
		index -- ;
		PatrFileSysElement element;
		try {
			element = folder.getElement(index, lock);
			if (element.isFile()) {
				element.getFile().delete(lock);
			} else {
				element.getFolder().delete(lock);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}

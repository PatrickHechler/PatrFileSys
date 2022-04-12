package de.hechler.patrick.pfs.objects;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFolder;


public class PatrFolderIterator implements Iterator <PatrFileSysElement> {
	
	private final PatrFolder folder;
	private int              length;
	private int              index;
	
	public PatrFolderIterator(PatrFolder folder) {
		try {
			this.folder = folder;
			this.length = folder.elementCount();
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
			if (folder.elementCount() != length) {
				throw new ConcurrentModificationException("added/removed items");
			}
			if (index >= length) {
				throw new NoSuchElementException("there are no more children");
			}
			PatrFileSysElement element = folder.getElement(index);
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
			element = folder.getElement(index);
			if (element.isFile()) {
				element.getFile().delete();
			} else {
				element.getFolder().delete();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}

package de.hechler.patrick.pfs.folder;

import java.util.Iterator;
import java.util.NoSuchElementException;

import de.hechler.patrick.pfs.element.PFSElement;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;

public interface FolderIter extends Iterator <PFSElement> {
	
	@Override
	default PFSElement next() throws NoSuchElementException {
		try {
			return nextElement();
		} catch (PatrFileSysException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * returns the next {@link PFSElement} or <code>null</code> if there are no more elements
	 * 
	 * @return the next {@link PFSElement} or <code>null</code> if there are no more elements
	 * @throws PatrFileSysException
	 *             if an error occurs
	 */
	PFSElement getNext() throws PatrFileSysException;
	
	/**
	 * returns the next {@link PFSElement}
	 * 
	 * @return the next {@link PFSElement}
	 * @throws PatrFileSysException
	 *             if an error occurs
	 * @throws NoSuchElementException
	 *             if there are no more {@link PFSElement elements}
	 */
	default PFSElement nextElement() throws PatrFileSysException, NoSuchElementException {
		PFSElement val = getNext();
		if (val != null) {
			return val;
		}
		throw new NoSuchElementException("no more elements");
	}
	
	@Override
	default boolean hasNext() {
		try {
			return hasNextElement();
		} catch (PatrFileSysException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * returns <code>true</code> if there are more elements, which can be received by calling {@link #getNext()}, {@link #nextElement()} and {@link #next()} and <code>false</code> if not
	 * 
	 * @return <code>true</code> if there are more elements and <code>false</code> if not
	 * @throws PatrFileSysException
	 */
	boolean hasNextElement() throws PatrFileSysException;
	
}

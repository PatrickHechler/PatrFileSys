package de.hechler.patrick.pfs.folder;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import de.hechler.patrick.pfs.element.PFSElement;

public interface FolderIter extends Iterator <PFSElement> {
	
	@Override
	default PFSElement next() throws NoSuchElementException {
		try {
			return nextElement();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * returns the next {@link PFSElement} or <code>null</code> if there are no more elements
	 * 
	 * @return the next {@link PFSElement} or <code>null</code> if there are no more elements
	 * @throws IOException
	 *             if an error occurs
	 */
	PFSElement getNext() throws IOException;
	
	/**
	 * returns the next {@link PFSElement}
	 * 
	 * @return the next {@link PFSElement}
	 * @throws IOException
	 *             if an error occurs
	 * @throws NoSuchElementException
	 *             if there are no more {@link PFSElement elements}
	 */
	default PFSElement nextElement() throws IOException, NoSuchElementException {
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
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * returns <code>true</code> if there are more elements, which can be received by calling {@link #getNext()}, {@link #nextElement()} and {@link #next()} and <code>false</code> if not
	 * 
	 * @return <code>true</code> if there are more elements and <code>false</code> if not
	 * @throws IOException
	 */
	boolean hasNextElement() throws IOException;
	
}

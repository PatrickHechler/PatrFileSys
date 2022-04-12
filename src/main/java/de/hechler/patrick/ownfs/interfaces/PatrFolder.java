package de.hechler.patrick.ownfs.interfaces;

import java.io.IOException;
import java.util.Iterator;

import de.hechler.patrick.ownfs.objects.PatrFolderIterator;

public interface PatrFolder extends Iterable <PatrFileSysElement>, PatrFileSysElement {
	
	/**
	 * adds a folder with the specified name to this folder
	 * 
	 * @param name
	 *            the name of the new folder
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws NullPointerException
	 *             if {@code name} is <code>null</code>
	 */
	void addFolder(String name) throws IOException, NullPointerException;
	
	/**
	 * adds an empty file with the specified name to this folder
	 * 
	 * @param name
	 *            the name of the new file
	 * @throws IOException
	 *             if an IO error occurs
	 * @throws NullPointerException
	 *             if {@code name} is <code>null</code>
	 */
	void addFile(String name) throws IOException, NullPointerException;
	
	/**
	 * deletes this Folder.<br>
	 * 
	 * an IllegalStateException will be thrown when this folder is not empty and {@link #canDeleteWithContent()} returns <code>false</code>
	 * 
	 * @throws IllegalStateException
	 *             if this folder is not empty and {@link #canDeleteWithContent()} returns <code>false</code> or if this is the root folder
	 * @throws IOException
	 *             if an IO error occurs
	 * @see #canDeleteWithContent()
	 */
	void delete() throws IllegalStateException, IOException;
	
	/**
	 * returns <code>true</code> if this folder can be deleted when it is not empty.
	 * 
	 * @return <code>true</code> if this folder can be deleted when it is not empty
	 */
	boolean canDeleteWithContent();
	
	/**
	 * returns <code>true</code> if this folder is the root folder of the file system.
	 * 
	 * @return <code>true</code> if this folder is the root folder of the file system.
	 */
	boolean isRoot();
	
	/**
	 * returns the number of elements in this folder
	 * 
	 * @return the number of elements in this folder
	 */
	int elementCount();
	
	/**
	 * returns the child from the given index
	 * 
	 * @param index
	 *            the index of the element
	 * @return the given child
	 */
	PatrFileSysElement getElement(int index);
	
	@Override
	default Iterator <PatrFileSysElement> iterator() {
		return new PatrFolderIterator(this);
	}
	
	/**
	 * deletes this folder even if it has children elements (which can also have children)
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	default void deepDelete() throws IOException {
		if ( !canDeleteWithContent()) {
			int ec = elementCount();
			while (ec > 0) {
				PatrFileSysElement pfe = getElement(ec - 1);
				if (pfe.isFile()) {
					pfe.getFile().delete();
				} else {
					pfe.getFolder().deepDelete();
				}
				ec -- ;
			}
			ec = elementCount();
			assert ec == 0;
		}
		delete();
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
	
}

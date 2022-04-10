package de.hechler.patrick.ownfs.interfaces;

import java.io.IOException;

public interface PatrFolder extends Iterable <PatrFileSysElement>, PatrFileSysElement {
	
	/**
	 * adds a folder with the specified name to this folder
	 * 
	 * @param name
	 *            the name of the new folder
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void addFolder(String name) throws IOException;
	
	/**
	 * adds an empty file with the specified name to this folder
	 * 
	 * @param name
	 *            the name of the new file
	 * @throws IOException
	 *             if an IO error occurs
	 */
	void addFile(String name) throws IOException;
	
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
	
	static void deepDelete(PatrFolder del) throws IllegalStateException, IOException {
		if ( !del.canDeleteWithContent()) {
			for (PatrFileSysElement pfe : del) {
				if (pfe.isFile()) {
					pfe.getFile().delete();
				} else {
					deepDelete(pfe.getFolder());
				}
			}
		}
		del.delete();
	}
	
}

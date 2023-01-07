package de.hechler.patrick.zeugs.pfs.interfaces;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

/**
 * the {@link Folder} provides methods:
 * <ul>
 * <li>to get handles for the children.
 * <ul>
 * <li>for generic children (over the {@link #iter(boolean)} method)</li>
 * <li>for special children (over the {@link #childElement(String)},
 * {@link #childFile(String)}, {@link #childFolder(String)} and
 * {@link #childPipe(String)} methods).</li>
 * </ul>
 * <li>add children to the folder (with the {@link #createFile(String)},
 * {@link #createFolder(String)} and {@link #createPipe(String)} methods)</li>
 * <li>remove children #re
 * </ul>
 * 
 * @author pat
 */
public interface Folder extends FSElement, Iterable<FSElement> {

	/**
	 * returns an {@link FolderIter} which iterates over the children of this folder
	 * <p>
	 * if {@code showHidden} is <code>true</code> the iterator will return all child
	 * elements of this folder and <code>false</code> if the iterator should only
	 * the elements which are not marked with {@link FSElement#FLAG_HIDDEN}.
	 * 
	 * @param showHidden <code>true</code> if all elements should be returned by the
	 *                   returned {@link FolderIter} and <code>false</code> if only
	 *                   non hidden elements should be returned by the returned
	 *                   {@link FolderIter}
	 * @return an {@link FolderIter} which iterates over the children of this folder
	 * @throws IOException if an IO error occurs
	 */
	FolderIter iter(boolean showHidden) throws IOException;

	/**
	 * returns an {@link FolderIter} which iterates over all non
	 * {@link FSElement#FLAG_HIDDEN hidden} children of this Folder
	 * <p>
	 * this method works like {@link #iter(boolean)} with <code>false</code> as
	 * argument
	 * 
	 * @return an {@link FolderIter} which iterates over all non
	 *         {@link FSElement#FLAG_HIDDEN hidden} children of this Folder
	 */
	@Override
	default FolderIter iterator() {
		try {
			return iter(false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * returns the number of child elements this folder has
	 * 
	 * @return the number of child elements this folder has
	 * @throws IOException if an IO error occurs
	 */
	long childCount() throws IOException;

	/**
	 * returns the child element with the given name.<br>
	 * this method will fail if this folder does not has a child with the given name
	 * 
	 * @param name the name of the child
	 * @return the child element with the given name
	 * @throws IOException if an IO error occurs
	 */
	FSElement childElement(String name) throws IOException;

	/**
	 * returns the child element with the given name.<br>
	 * this method will fail
	 * <ul>
	 * <li>if this folder does not has a child with the given name</li>
	 * <li>if the child with the given name is no folder</li>
	 * </ul>
	 * 
	 * @param name the name of the child
	 * @return the child element with the given name
	 * @throws IOException if an IO error occurs
	 */
	Folder childFolder(String name) throws IOException;

	/**
	 * returns the child element with the given name.<br>
	 * this method will fail
	 * <ul>
	 * <li>if this folder does not has a child with the given name</li>
	 * <li>if the child with the given name is no file</li>
	 * </ul>
	 * 
	 * @param name the name of the child
	 * @return the child element with the given name
	 * @throws IOException if an IO error occurs
	 */
	File childFile(String name) throws IOException;

	/**
	 * returns the child element with the given name.<br>
	 * this method will fail
	 * <ul>
	 * <li>if this folder does not has a child with the given name</li>
	 * <li>if the child with the given name is no pipe</li>
	 * </ul>
	 * 
	 * @param name the name of the child
	 * @return the child element with the given name
	 * @throws IOException if an IO error occurs
	 */
	Pipe childPipe(String name) throws IOException;

	/**
	 * creates a new child folder with the given name and adds the new child to this
	 * folder
	 * <p>
	 * the child folder will be initially empty
	 * 
	 * @param name the name of the new child
	 * @return the newly created child
	 * @throws IOException if an IO error occurs
	 */
	Folder createFolder(String name) throws IOException;

	/**
	 * creates a new child file with the given name and adds the new child to this
	 * folder
	 * <p>
	 * the child file will be initially empty
	 * 
	 * @param name the name of the new child
	 * @return the newly created child
	 * @throws IOException if an IO error occurs
	 */
	File createFile(String name) throws IOException;

	/**
	 * creates a new child pipe with the given name and adds the new child to this
	 * folder
	 * <p>
	 * the child pipe will be initially empty
	 * 
	 * @param name the name of the new child
	 * @return the newly created child
	 * @throws IOException if an IO error occurs
	 */
	Pipe createPipe(String name) throws IOException;

	/**
	 * this interface describes an {@link Iterator} which returns {@link FSElement}
	 * objects
	 * <p>
	 * in addition to the {@link Iterator} methods {@link #next()}
	 * {@link #hasNext()} and {@link #remove()} this interface adds the three
	 * methods {@link #nextElement()}, {@link #hasNextElement()} and
	 * {@link #delete()} which do the same work, but are allowed to throw an
	 * {@link IOException}
	 * 
	 * @author pat
	 */
	static interface FolderIter extends Iterator<FSElement>, Closeable {

		/**
		 * returns the next element
		 * 
		 * @return the next element
		 * @throws IOException if an IO error occurs
		 */
		FSElement nextElement() throws IOException;

		/**
		 * returns <code>true</code> if there is a next element and <code>false</code>
		 * if not.
		 * 
		 * @return <code>true</code> if there is a next element and <code>false</code>
		 *         if not
		 * @throws IOException if an IO error occurs
		 */
		boolean hasNextElement() throws IOException;

		/**
		 * {@link FSElement#delete() deletes} the element which has been returned from
		 * the last {@link #nextElement()} or {@link #next()} call.
		 * 
		 * @throws IOException if an IO error occurs
		 */
		void delete() throws IOException;

		/**
		 * returns the next element
		 * 
		 * @return the next element
		 */
		@Override
		default FSElement next() {
			try {
				return nextElement();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * returns <code>true</code> if there is a next element and <code>false</code>
		 * if not.
		 * 
		 * @return <code>true</code> if there is a next element and <code>false</code>
		 *         if not
		 */
		@Override
		default boolean hasNext() {
			try {
				return hasNextElement();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * {@link FSElement#delete() deletes} the element which has been returned from
		 * the last {@link #nextElement()} or {@link #next()} call.
		 */
		@Override
		default void remove() {
			try {
				delete();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	}

}

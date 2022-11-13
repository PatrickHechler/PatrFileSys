package de.hechler.patrick.zeugs.pfs.interfaces;

import java.io.Closeable;
import java.io.IOException;

import de.hechler.patrick.zeugs.pfs.misc.ElementType;

/**
 * the {@link FSElement} interface provides methods to extract and modify basic
 * informations of a file system element, navigate to the parent folder and
 * change the elements position/name in the file system tree or remove the
 * element from it.
 * 
 * @author pat
 */
public interface FSElement extends Closeable {

	/**
	 * these bits are not allowed to be modified after a file system has been
	 * created.
	 * 
	 * @see #flag(int, int)
	 */
	static final int FLAG_UNMODIFIABLE = 0x000000FF;
	/**
	 * an element marked with this flag represent a folder
	 * 
	 * @see #flags()
	 */
	static final int FLAG_FOLDER = 0x00000001;
	/**
	 * an element marked with this flag represent a file
	 * 
	 * @see #flags()
	 */
	static final int FLAG_FILE = 0x00000002;
	/**
	 * an element marked with this flag represent a pipe
	 * 
	 * @see #flags()
	 */
	static final int FLAG_PIPE = 0x00000004;
	/**
	 * a file marked with this flag can be executed
	 * 
	 * @see #flags()
	 * @see #flag(int, int)
	 */
	static final int FLAG_EXECUTABLE = 0x00000100;
	/**
	 * an element marked with this flag will be hidden by default
	 * 
	 * @see #flags()
	 * @see #flag(int, int)
	 * @see Folder#iter(boolean)
	 * @see Folder#iterator()
	 */
	static final int FLAG_HIDDEN = 0x01000000;

	/**
	 * returns the parent folder of this {@link FSElement}.
	 * <p>
	 * if this {@link FSElement} is the root {@link Folder} the method will fail
	 * 
	 * @return the parent folder of this {@link FSElement}.
	 *         <p>
	 * @throws IOException
	 */
	Folder parent() throws IOException;

	/**
	 * returns the flags of this {@link FSElement}
	 * 
	 * @return the flags of this {@link FSElement}
	 * @throws IOException
	 */
	int flags() throws IOException;

	/**
	 * modifies the flags of this {@link FSElement}.
	 * <p>
	 * <ul>
	 * <li>{@code add} and {@code rem} are not allowed to contain common bits.
	 * {@code (add & rem)} has to be zero</li>
	 * <li>{@code add} and {@code rem} are not allowed to contain bits set in
	 * {@link #FLAG_UNMODIFIABLE}</li>
	 * </ul>
	 * 
	 * @param add the flags which should be added to this {@link FSElement}
	 * @param rem the flags which should be removed from this {@link FSElement}
	 * @throws IOException
	 */
	void flag(int add, int rem) throws IOException;

	/**
	 * returns the last modification time of this {@link FSElement}
	 * 
	 * @return the last modification time of this {@link FSElement}
	 * @throws IOException
	 */
	long lastModTime() throws IOException;

	/**
	 * sets the last modification time of this {@link FSElement}
	 * 
	 * @param time the new last modification time of this {@link FSElement}
	 * @throws IOException
	 */
	void lastModTime(long time) throws IOException;

	/**
	 * returns the create time of this {@link FSElement}
	 * 
	 * @return the create time of this {@link FSElement}
	 * @throws IOException
	 */
	long createTime() throws IOException;

	/**
	 * sets the create time of this {@link FSElement}
	 * 
	 * @param time the create time of this {@link FSElement}
	 * @throws IOException
	 */
	void createTime(long time) throws IOException;

	/**
	 * returns the name of this {@link FSElement}
	 * 
	 * @return the name of this {@link FSElement}
	 * @throws IOException
	 */
	String name() throws IOException;

	/**
	 * changes the name of this {@link FSElement}
	 * <p>
	 * this call is like {@link #move(Folder, String)} with {@link #parent()} as
	 * first parameter and {@code name} as second
	 * 
	 * @param name the new name of this {@link FSElement}
	 * @throws IOException
	 */
	void name(String name) throws IOException;

	/**
	 * moves this element to the new parent
	 * <p>
	 * this call is like {@link #move(Folder, String)} with {@code parent} as first
	 * parameter and {@link #name()} as second
	 * 
	 * @param parent the new parent of this {@link FSElement}
	 * @throws IOException
	 */
	void parent(Folder parent) throws IOException;

	/**
	 * moves this {@link FSElement} in the file system
	 * <p>
	 * this operation moves this {@link FSElement} to the folder {@code parent} and
	 * changes the {@link #name() name} of this {@link FSElement}
	 * 
	 * @param parent the new parent of this {@link FSElement}
	 * @param name   the new name of this {@link FSElement}
	 * @throws IOException
	 */
	void move(Folder parent, String name) throws IOException;

	/**
	 * deletes this {@link FSElement}.
	 * <p>
	 * this operation will fail if this {@link FSElement} is a folder, but not
	 * empty. <br>
	 * A folder does not need to implement the {@link Folder} interface. to check if
	 * a {@link FSElement} is a folder use the {@link #isFolder()} or
	 * {@link #flags()} method.
	 * <p>
	 * after this operation this {@link FSElement} should not be used
	 * 
	 * @throws IOException
	 */
	void delete() throws IOException;

	/**
	 * returns <code>true</code> if this {@link FSElement} represents a folder and
	 * <code>false</code> if not
	 * 
	 * @return <code>true</code> if this {@link FSElement} represents a folder and
	 *         <code>false</code> if not
	 * @throws IOException
	 * @see #getFolder()
	 */
	default boolean isFolder() throws IOException {
		return (flags() & FLAG_FOLDER) != 0;
	}

	/**
	 * returns <code>true</code> if this {@link FSElement} represents a file and
	 * <code>false</code> if not
	 * 
	 * @return <code>true</code> if this {@link FSElement} represents a file and
	 *         <code>false</code> if not
	 * @throws IOException
	 * @see {@link #getFile()}
	 */
	default boolean isFile() throws IOException {
		return (flags() & FLAG_FILE) != 0;
	}

	/**
	 * returns <code>true</code> if this {@link FSElement} represents a pipe and
	 * <code>false</code> if not
	 * 
	 * @return <code>true</code> if this {@link FSElement} represents a pipe and
	 *         <code>false</code> if not
	 * @throws IOException
	 * @see {@link #getPipe()}
	 */
	default boolean isPipe() throws IOException {
		return (flags() & FLAG_PIPE) != 0;
	}

	default ElementType type() throws IOException {
		int flags = flags();
		switch (flags & (FLAG_FOLDER | FLAG_FILE | FLAG_PIPE)) {
		case FLAG_FOLDER:
			return ElementType.folder;
		case FLAG_FILE:
			return ElementType.file;
		case FLAG_PIPE:
			return ElementType.pipe;
		default:
			throw new InternalError("unknown flags: 0x" + Integer.toHexString(flags));
		}
	}

	/**
	 * returns a {@link Folder} which represents the same file system element as
	 * this {@link FSElement}.
	 * <p>
	 * the returned {@link Folder} will be {@link #equals(Object) equal} with this
	 * {@link FSElement}
	 * <p>
	 * if this {@link FSElement} represents no folder (if {@link #isFolder()}
	 * returns <code>false</code>) this operation will fail
	 * 
	 * @return a {@link Folder} which represents the same file system element as
	 *         this {@link FSElement}.
	 * @throws IOException
	 * @see {@link #isFolder()}
	 */
	Folder getFolder() throws IOException;

	/**
	 * returns a {@link File} which represents the same file system element as this
	 * {@link FSElement}.
	 * <p>
	 * the returned {@link File} will be {@link #equals(Object) equal} with this
	 * {@link FSElement}
	 * <p>
	 * if this {@link FSElement} represents no file (if {@link #isFile()} returns
	 * <code>false</code>) this operation will fail
	 * 
	 * @return a {@link File} which represents the same file system element as this
	 *         {@link FSElement}.
	 * @throws IOException
	 * @see {@link #isFile()}
	 */
	File getFile() throws IOException;

	/**
	 * returns a {@link Pipe} which represents the same file system element as this
	 * {@link FSElement}.
	 * <p>
	 * the returned {@link Pipe} will be {@link #equals(Object) equal} with this
	 * {@link FSElement}
	 * <p>
	 * if this {@link FSElement} represents no folder (if {@link #isFolder()}
	 * returns <code>false</code>) this operation will fail
	 * 
	 * @return a {@link Pipe} which represents the same file system element as this
	 *         {@link FSElement}.
	 * @throws IOException
	 * @see {@link #isPipe()}
	 */
	Pipe getPipe() throws IOException;

	/**
	 * @see #equals(Object)
	 */
	@Override
	int hashCode();

	/**
	 * returns <code>true</code> if this {@link FSElement} represents the same
	 * {@link FSElement} as the given {@link Object}
	 * <p>
	 * this method will work like: <code><pre>if (obj == null) { 
	 *   return false; 
	 * } else if (obj instanceof FSElement e) {
	 *   return {@link #equals(FSElement) equals(e)}; 
	 * } else { 
	 *   return false; 
	 * }</pre></code>
	 * 
	 * @param obj the object which is potentially
	 * @return <code>true</code> if this {@link FSElement} represents the same
	 *         {@link FSElement} as the given {@link Object}
	 */
	boolean equals(Object obj);

	/**
	 * returns <code>true</code> if this {@link FSElement} represents the same
	 * {@link FSElement} as the given {@link FSElement} {@code e} and
	 * <code>false</code> if not
	 * 
	 * @param e the given {@link FSElement} which is potentially equal to this
	 *          {@link FSElement}
	 * @return <code>true</code> if this {@link FSElement} represents the same
	 *         {@link FSElement} as the given {@link FSElement} {@code e} and
	 *         <code>false</code> if not
	 * @throws IOException
	 */
	boolean equals(FSElement e) throws IOException;

}
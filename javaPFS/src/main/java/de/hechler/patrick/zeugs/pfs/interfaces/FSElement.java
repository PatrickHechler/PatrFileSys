// This file is part of the Patr File System Project
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
// Copyright (C) 2023 Patrick Hechler
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
package de.hechler.patrick.zeugs.pfs.interfaces;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;

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
	static final int FLAG_FOLDER       = 0x00000001;
	/**
	 * an element marked with this flag represent a file
	 * 
	 * @see #flags()
	 */
	static final int FLAG_FILE         = 0x00000002;
	/**
	 * an element marked with this flag represent a pipe
	 * 
	 * @see #flags()
	 */
	static final int FLAG_PIPE         = 0x00000004;
	/**
	 * an element marked with this flag represent a mount point
	 * 
	 * @see #flags()
	 */
	static final int FLAG_MOUNT         = 0x00000008;
	/**
	 * a file marked with this flag can be executed
	 * 
	 * @see #flags()
	 * @see #flag(int, int)
	 */
	static final int FLAG_EXECUTABLE   = 0x00000100;
	/**
	 * an element marked with this flag will be hidden by default
	 * 
	 * @see #flags()
	 * @see #flag(int, int)
	 * @see Folder#iter(boolean)
	 * @see Folder#iterator()
	 */
	static final int FLAG_HIDDEN       = 0x01000000;
	
	/**
	 * returns the parent folder of this {@link FSElement}.
	 * <p>
	 * if this {@link FSElement} is the root {@link Folder} the method will fail
	 * 
	 * @return the parent folder of this {@link FSElement}.
	 * 
	 * @throws IOException           if an IO error occurs
	 * @throws IllegalStateException if this element is the root
	 */
	Folder parent() throws IOException, IllegalStateException;
	
	/**
	 * returns the file system which is responsible for this element
	 * 
	 * @return the file system which is responsible for this element
	 * 
	 * @throws ClosedChannelException if this element handle was already closed
	 */
	FS fs() throws ClosedChannelException;
	
	/**
	 * returns the mount point on which this element resides
	 * 
	 * @return the mount point on which this element resides
	 * 
	 * @throws ClosedChannelException if this element handle was already closed
	 */
	Mount mountPoint() throws ClosedChannelException;
	
	/**
	 * returns the flags of this {@link FSElement}
	 * 
	 * @return the flags of this {@link FSElement}
	 * 
	 * @throws IOException if an IO error occurs
	 */
	int flags() throws IOException;
	
	/**
	 * modifies the flags of this {@link FSElement}.
	 * <ul>
	 * <li>{@code add} and {@code rem} are not allowed to contain common bits.
	 * {@code (add & rem)} has to be zero</li>
	 * <li>{@code add} and {@code rem} are not allowed to contain bits set in
	 * {@link #FLAG_UNMODIFIABLE}</li>
	 * </ul>
	 * 
	 * @param add the flags which should be added to this {@link FSElement}
	 * @param rem the flags which should be removed from this {@link FSElement}
	 * 
	 * @throws IOException if an IO error occurs
	 */
	void flag(int add, int rem) throws IOException;
	
	/**
	 * returns the last modification time of this {@link FSElement}
	 * 
	 * @return the last modification time of this {@link FSElement}
	 * 
	 * @throws IOException if an IO error occurs
	 */
	long lastModTime() throws IOException;
	
	/**
	 * sets the last modification time of this {@link FSElement}
	 * 
	 * @param time the new last modification time of this {@link FSElement}
	 * 
	 * @throws IOException if an IO error occurs
	 */
	void lastModTime(long time) throws IOException;
	
	/**
	 * returns the create time of this {@link FSElement}
	 * 
	 * @return the create time of this {@link FSElement}
	 * 
	 * @throws IOException if an IO error occurs
	 */
	long createTime() throws IOException;
	
	/**
	 * sets the create time of this {@link FSElement}
	 * 
	 * @param time the create time of this {@link FSElement}
	 * 
	 * @throws IOException if an IO error occurs
	 */
	void createTime(long time) throws IOException;
	
	/**
	 * returns the name of this {@link FSElement}
	 * 
	 * @return the name of this {@link FSElement}
	 * 
	 * @throws IOException if an IO error occurs
	 */
	String name() throws IOException;
	
	/**
	 * changes the name of this {@link FSElement}
	 * <p>
	 * this call is like {@link #move(Folder, String)} with {@link #parent()} as
	 * first parameter and {@code name} as second
	 * 
	 * @param name the new name of this {@link FSElement}
	 * 
	 * @throws IOException if an IO error occurs
	 */
	void name(String name) throws IOException;
	
	/**
	 * moves this element to the new parent
	 * <p>
	 * this call is like {@link #move(Folder, String)} with {@code parent} as first
	 * parameter and {@link #name()} as second
	 * 
	 * @param parent the new parent of this {@link FSElement}
	 * 
	 * @throws IOException if an IO error occurs
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
	 * 
	 * @throws IOException if an IO error occurs
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
	 * @throws IOException if an IO error occurs
	 */
	void delete() throws IOException;
	
	/**
	 * returns <code>true</code> if this {@link FSElement} represents a folder and
	 * <code>false</code> if not
	 * 
	 * @return <code>true</code> if this {@link FSElement} represents a folder and
	 *         <code>false</code> if not
	 * 		
	 * @throws IOException if an IO error occurs
	 * 		
	 * @see #getFolder()
	 */
	default boolean isFolder() throws IOException { return (flags() & FLAG_FOLDER) != 0; }
	
	/**
	 * returns <code>true</code> if this {@link FSElement} represents a file and
	 * <code>false</code> if not
	 * 
	 * @return <code>true</code> if this {@link FSElement} represents a file and
	 *         <code>false</code> if not
	 * 		
	 * @throws IOException if an IO error occurs
	 * 		
	 * @see #getFile()
	 */
	default boolean isFile() throws IOException { return (flags() & FLAG_FILE) != 0; }
	
	/**
	 * returns <code>true</code> if this {@link FSElement} represents a pipe and
	 * <code>false</code> if not
	 * 
	 * @return <code>true</code> if this {@link FSElement} represents a pipe and
	 *         <code>false</code> if not
	 * 		
	 * @throws IOException if an IO error occurs
	 * 		
	 * @see #getPipe()
	 */
	default boolean isPipe() throws IOException { return (flags() & FLAG_PIPE) != 0; }
	
	/**
	 * returns the {@link ElementType} of this {@link FSElement}
	 * 
	 * @return the {@link ElementType} of this {@link FSElement}
	 * 
	 * @throws IOException if an IO error occurs
	 */
	default ElementType type() throws IOException {
		int flags = flags();
		switch (flags & (FLAG_FOLDER | FLAG_FILE | FLAG_PIPE | FLAG_MOUNT)) {
		case FLAG_FOLDER:
			return ElementType.FOLDER;
		case FLAG_FILE:
			return ElementType.FILE;
		case FLAG_PIPE:
			return ElementType.PIPE;
		case FLAG_MOUNT:
			return ElementType.MOUNT;
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
	 * 		
	 * @throws IOException if an IO error occurs
	 * 		
	 * @see #isFolder()
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
	 * 
	 * @throws IOException if an IO error occurs
	 * 
	 * @see #isFile()
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
	 * 
	 * @throws IOException if an IO error occurs
	 * 
	 * @see #isPipe()
	 */
	Pipe getPipe() throws IOException;
	
	/**
	 * @see #equals(Object)
	 * @see #equals(FSElement)
	 */
	@Override
	int hashCode();
	
	/**
	 * returns <code>true</code> if this {@link FSElement} represents the same
	 * {@link FSElement} as the given {@link Object}
	 * <p>
	 * this method will work like:
	 * 
	 * <pre><code>if (obj == null) {
	 *   return false;
	 * } else if (obj instanceof FSElement e) {
	 *   return {@link #equals(FSElement) equals(e)};
	 * } else {
	 *   return false;
	 * }</code></pre>
	 * 
	 * @param obj the object which is potentially equal to this fs-element
	 * 
	 * @return <code>true</code> if this {@link FSElement} represents the same
	 *         {@link FSElement} as the given {@link Object}
	 */
	@Override
	boolean equals(Object obj);
	
	/**
	 * returns <code>true</code> if this {@link FSElement} represents the same
	 * {@link FSElement} as the given {@link FSElement} {@code e} and
	 * <code>false</code> if not
	 * 
	 * @param e the given {@link FSElement} which is potentially equal to this
	 *          {@link FSElement}
	 * 
	 * @return <code>true</code> if this {@link FSElement} represents the same
	 *         {@link FSElement} as the given {@link FSElement} {@code e} and
	 *         <code>false</code> if not
	 * 		
	 * @throws IOException if an IO error occurs
	 */
	boolean equals(FSElement e) throws IOException;
	
}

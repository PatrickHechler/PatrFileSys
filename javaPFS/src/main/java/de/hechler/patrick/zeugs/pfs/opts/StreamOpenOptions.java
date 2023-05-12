//This file is part of the Patr File System Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.
package de.hechler.patrick.zeugs.pfs.opts;

import de.hechler.patrick.zeugs.pfs.interfaces.FSElement;
import de.hechler.patrick.zeugs.pfs.misc.ElementType;

/**
 * the {@link StreamOpenOptions} provide options for opening streams:
 * 
 * @author pat
 * 
 * @param read       if the stream should be opened for read access
 * @param write      if the stream should be opened for write access
 * @param append     if the stream should be opened for append access (implicit <code>write</code>)
 * @param type       the {@link FSElement} type of the element for which the stream should be opened
 *                   ({@link ElementType#FOLDER} is forbidden, <code>null</code> is allowed)
 * @param createAlso if the element should be created if it doesn't exist already (needs a non <code>null</code>
 *                   <code>type</code>)
 * @param createOnly if the open operation should fail if the element already exists (implicit <code>createAlso</code>)
 */
public record StreamOpenOptions(boolean read, boolean write, boolean append, ElementType type, boolean createAlso,
		boolean createOnly) {
	
	/**
	 * creates a new {@link StreamOpenOptions} instance from the given parameters:
	 * <ul>
	 * <li><code>read</code>: if the stream should be opened for read access</li>
	 * <li><code>write</code>: if the stream should be opened for write access</li>
	 * <li><code>append</code>: if the stream should be opened for append access (implicit <code>write</code>)</li>
	 * <li><code>type</code> the {@link FSElement} type of the element for which the stream should be opened
	 * ({@link ElementType#FOLDER} is forbidden, <code>null</code> is allowed, if the value is <code>null</code>,
	 * <code>type</code> will be set when opening the {@link FSElement})</li>
	 * <li><code>createAlso</code>: if the element should be created if it doesn't exist already (needs a non
	 * <code>null</code> <code>type</code>)</li>
	 * <li><code>createOnly</code>: if the open operation should fail if the element already exists (implicit
	 * <code>createAlso</code>)</li>
	 * </ul>
	 * 
	 * @param read       if the stream should be opened for read access
	 * @param write      if the stream should be opened for write access
	 * @param append     if the stream should be opened for append access (implicit <code>write</code>)
	 * @param type       the {@link FSElement} type of the element for which the stream should be opened
	 *                   ({@link ElementType#FOLDER} is forbidden, <code>null</code> is allowed)
	 * @param createAlso if the element should be created if it doesn't exist already (needs a non <code>null</code>
	 *                   <code>type</code>)
	 * @param createOnly if the open operation should fail if the element already exists (implicit
	 *                   <code>createAlso</code>)
	 */
	public StreamOpenOptions(boolean read, boolean write, boolean append, ElementType type, boolean createAlso,
			boolean createOnly) {
		if (append) { write = true; }
		if (createOnly) { createAlso = true; }
		if (!read && !write) {
			throw new IllegalArgumentException(
					"can't open streams without read and without write access! spezify at least one of them.");
		}
		this.type = switch (type) {
		case ElementType e when e == ElementType.FILE || e == ElementType.PIPE -> type;
		case ElementType e when e == ElementType.FOLDER ->
			throw new IllegalArgumentException("can't open folder streams");
		case null -> {
			if (createAlso) {
				throw new IllegalArgumentException(
						"can't open a streams when createAlso or createOnly is set, but type is null");
			} else {
				yield null;
			}
		}
		case ElementType e -> throw new IllegalArgumentException("unknown element type: " + type.name());
		};
		this.read = read;
		this.write = write;
		this.append = append;
		this.createAlso = createAlso;
		this.createOnly = createOnly;
	}
	
	/**
	 * creates new {@link StreamOpenOptions} with the given parameters
	 * <ul>
	 * <li>{@link #append()} will be set to <code>false</code></li>
	 * <li>{@link #type()} will be set to <code>null</code></li>
	 * <li>{@link #createAlso()} will be set to <code>false</code></li>
	 * <li>{@link #createOnly()} will be set to <code>false</code></li>
	 * </ul>
	 * 
	 * @param read  if the stream should be opened for read access
	 * @param write if the stream should be opened for write access
	 */
	public StreamOpenOptions(boolean read, boolean write) { this(read, write, false, null, false, false); }
	
	/**
	 * creates new {@link StreamOpenOptions} with the given parameters
	 * <ul>
	 * <li>{@link #type()} will be set to <code>null</code></li>
	 * <li>{@link #createAlso()} will be set to <code>false</code></li>
	 * <li>{@link #createOnly()} will be set to <code>false</code></li>
	 * </ul>
	 * 
	 * @param read   if the stream should be opened for read access
	 * @param write  if the stream should be opened for write access
	 * @param append if the stream should be opened for append access (implicit <code>write</code>)
	 */
	public StreamOpenOptions(boolean read, boolean write, boolean append) {
		this(read, write, append, null, false, false);
	}
	
	/**
	 * creates new {@link StreamOpenOptions} with the given parameters
	 * <ul>
	 * <li>{@link #createAlso()} will be set to <code>false</code></li>
	 * <li>{@link #createOnly()} will be set to <code>false</code></li>
	 * </ul>
	 * 
	 * @param read   if the stream should be opened for read access
	 * @param write  if the stream should be opened for write access
	 * @param append if the stream should be opened for append access (implicit <code>write</code>)
	 * @param type   the type of the element on which the stream should be opened
	 */
	public StreamOpenOptions(boolean read, boolean write, boolean append, ElementType type) {
		this(read, write, append, type, true, false);
	}
	
	/**
	 * returns <code>true</code> if the stream can perform seek operations and <code>false</code> if not
	 * 
	 * @return <code>true</code> if the stream can perform seek operations and <code>false</code> if not
	 */
	@SuppressWarnings({"preview", "unused"})
	public boolean seekable() {
		try {
			return switch (type) {
			case FILE -> true;
			case PIPE -> false;
			case null -> throw new IllegalStateException("type is not set");
			default -> throw new AssertionError("unknown type: " + type.name());
			};
		} catch (NullPointerException npe) { // bug in eclipse compiler
			throw new IllegalStateException("type is not set");
		}
	}
	
	/**
	 * ensures that the {@link StreamOpenOptions} can be opened for the given <code>type</code><br>
	 * that is always, when {@link #type()} is <code>null</code> or if <code>type == {@link #type()}</code> is
	 * <code>true</code> <br>
	 * if this {@link StreamOpenOptions} can not be used to open streams for the given <code>type</code> the operation
	 * will fail
	 * 
	 * @param type the {@link ElementType} for which the stream should be opened
	 * 
	 * @return a {@link StreamOpenOptions}, which are the same to this, but with {@link #type()} set to <code>type</code>
	 *         (or <code>this</code> if {@link #type()} is already set)
	 * 		
	 * @throws IllegalArgumentException if this {@link StreamOpenOptions} can not be used to open streams for the given
	 *                                  <code>type</code>
	 */
	public StreamOpenOptions ensureType(ElementType type) throws IllegalArgumentException {
		return switch (type) {
		case null -> throw new NullPointerException("can't ensure to be of type null");
		case ElementType e when e == this.type -> this;
		case ElementType e when null == this.type ->
			new StreamOpenOptions(read, write, append, e, createAlso, createOnly);
		case ElementType e -> throw new IllegalArgumentException("this is a " + type + ", but type is set to " + this.type);
		};
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("Stream[");
		boolean first = true;
		if (type != null) {
			first = checkFirst(b, first);
			b.append(type);
		}
		if (read) {
			first = checkFirst(b, first);
			b.append("read");
		}
		if (append) {
			first = checkFirst(b, first);
			b.append("append");
		} else if (write) {
			first = checkFirst(b, first);
			b.append("write");
		}
		if (createOnly) {
			first = checkFirst(b, first);
			b.append("createOnly");
		} else if (createAlso) {
			first = checkFirst(b, first);
			b.append("createAlso");
		}
		return b.append(']').toString();
	}
	
	private static boolean checkFirst(StringBuilder b, boolean first) {
		if (!first) {
			b.append(", ");
		}
		return false;
	}
	
}

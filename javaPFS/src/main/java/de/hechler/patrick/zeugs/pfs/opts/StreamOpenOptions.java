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
 * @param append     if the stream should be opened for append access (implicit
 *                   <code>write</code>)
 * @param type       the {@link FSElement} type of the element for which the
 *                   stream should be opened ({@link ElementType#folder} is
 *                   forbidden, <code>null</code> is allowed)
 * @param createAlso if the element should be created if it doesn't exist
 *                   already (needs a non <code>null</code> <code>type</code>)
 * @param createOnly if the open operation should fail if the element already
 *                   exists (implicit <code>createAlso</code>)
 */
public record StreamOpenOptions(boolean read, boolean write, boolean append, ElementType type, boolean createAlso, boolean createOnly) {
	
	/**
	 * creates a new {@link StreamOpenOptions} instance from the given parameters:
	 * <ul>
	 * <li><code>read</code>: if the stream should be opened for read access</li>
	 * <li><code>write</code>: if the stream should be opened for write access</li>
	 * <li><code>append</code>: if the stream should be opened for append access
	 * (implicit <code>write</code>)</li>
	 * <li><code>type</code> the {@link FSElement} type of the element for which the
	 * stream should be opened ({@link ElementType#folder} is
	 * forbidden, <code>null</code> is allowed, if the value is <code>null</code>,
	 * <code>type</code> will be set when opening the {@link FSElement})</li>
	 * <li><code>createAlso</code>: if the element should be created if it doesn't
	 * exist already (needs a non <code>null</code> <code>type</code>)</li>
	 * <li><code>createOnly</code>: if the open operation should fail if the element
	 * already exists (implicit <code>createAlso</code>)</li>
	 * </ul>
	 * 
	 * @param read       if the stream should be opened for read access
	 * @param write      if the stream should be opened for write access
	 * @param append     if the stream should be opened for append access (implicit
	 *                   <code>write</code>)
	 * @param type       the {@link FSElement} type of the element for which the
	 *                   stream should be opened ({@link ElementType#folder} is
	 *                   forbidden, <code>null</code> is allowed)
	 * @param createAlso if the element should be created if it doesn't exist
	 *                   already (needs a non <code>null</code> <code>type</code>)
	 * @param createOnly if the open operation should fail if the element already
	 *                   exists (implicit <code>createAlso</code>)
	 */
	public StreamOpenOptions(boolean read, boolean write, boolean append, ElementType type, boolean createAlso, boolean createOnly) {
		if (append) {
			write = true;
		}
		if (createOnly) {
			createAlso = true;
		}
		if (!read && !write) {
			throw new IllegalArgumentException("can't open streams without read and without write access! spezify at least one of them.");
		}
		switch (type) {
		case file, pipe -> this.type = type;
		case folder -> throw new IllegalArgumentException("can't open folder streams");
		case null -> {
			if (createAlso) {
				throw new IllegalArgumentException("can't open a streams when createAlso or createOnly is set, but type is null");
			} else {
				this.type = null;
			}
		}
		default -> throw new IllegalArgumentException("unknown element type: " + type.name());
		}
		this.read       = read;
		this.write      = write;
		this.append     = append;
		this.createAlso = createAlso;
		this.createOnly = createOnly;
	}
	
	/**
	 * creates new {@link StreamOpenOptions} with the given parameters
	 * <ul>
	 * <li>{@link #append} will be set to <code>false</code></li>
	 * <li>{@link #type} will be set to <code>null</code></li>
	 * <li>{@link #createAlso} will be set to <code>false</code></li>
	 * <li>{@link #createOnly} will be set to <code>false</code></li>
	 * </ul>
	 * 
	 * @param read  if the stream should be opened for read access
	 * @param write if the stream should be opened for write access
	 */
	public StreamOpenOptions(boolean read, boolean write) {
		this(read, write, false, null, false, false);
	}
	
	/**
	 * creates new {@link StreamOpenOptions} with the given parameters
	 * <ul>
	 * <li>{@link #type} will be set to <code>null</code></li>
	 * <li>{@link #createAlso} will be set to <code>false</code></li>
	 * <li>{@link #createOnly} will be set to <code>false</code></li>
	 * </ul>
	 * 
	 * @param read   if the stream should be opened for read access
	 * @param write  if the stream should be opened for write access
	 * @param append if the stream should be opened for append access (implicit
	 *               <code>write</code>)
	 */
	public StreamOpenOptions(boolean read, boolean write, boolean append) {
		this(read, write, append, null, false, false);
	}
	
	/**
	 * creates new {@link StreamOpenOptions} with the given parameters
	 * <ul>
	 * <li>{@link #createAlso} will be set to <code>false</code></li>
	 * <li>{@link #createOnly} will be set to <code>false</code></li>
	 * </ul>
	 * 
	 * @param read   if the stream should be opened for read access
	 * @param write  if the stream should be opened for write access
	 * @param append if the stream should be opened for append access (implicit
	 *               <code>write</code>)
	 * @param type   the type of the element on which the stream should be opened
	 */
	public StreamOpenOptions(boolean read, boolean write, boolean append, ElementType type) {
		this(read, write, append, type, true, false);
	}
	
	/**
	 * returns <code>true</code> if the stream can perform seek operations and
	 * <code>false</code> if not
	 * 
	 * @return <code>true</code> if the stream can perform seek operations and
	 *         <code>false</code> if not
	 */
	public boolean seekable() {
		return switch (type) {
		case file -> true;
		case pipe -> false;
		case null -> throw new IllegalStateException("type is not set");
		default -> throw new AssertionError("unknown type: " + type.name());
		};
	}
	
	/**
	 * ensures that the {@link StreamOpenOptions} can be opened for the given
	 * <code>type</code><br>
	 * that is always, when {@link #type} is <code>null</code> or if
	 * <code>type == {@link #type}</code> is <code>true</code>
	 * <br>
	 * if this {@link StreamOpenOptions} can not be used to open streams for the
	 * given <code>type</code> the operation will fail
	 * 
	 * @param type the {@link ElementType} for which the stream should be opened
	 * 
	 * @return a {@link StreamOpenOptions}, which are the same to this, but with
	 *         {@link #type} set to <code>type</code> (or <code>this</code> if
	 *         {@link #type} is already set)
	 * 		
	 * @throws IllegalArgumentException if this {@link StreamOpenOptions} can not be
	 *                                  used to open streams for the given
	 *                                  <code>type</code>
	 */
	public StreamOpenOptions ensureType(ElementType type) throws IllegalArgumentException {
		if (type == null) { throw new NullPointerException("type is null"); }
		if (type == this.type) { return this; }
		if (this.type != null) { throw new IllegalArgumentException("this is a " + type + ", but type is set to " + this.type); }
		return new StreamOpenOptions(read, write, append, type, createAlso, createOnly);
	}
	
}

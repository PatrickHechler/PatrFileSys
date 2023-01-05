package de.hechler.patrick.zeugs.pfs.opts;

import de.hechler.patrick.zeugs.pfs.interfaces.Stream;
import de.hechler.patrick.zeugs.pfs.misc.ElementType;

/**
 * the {@link StreamOpenOptions} provide options for opening streams:
 * <ul>
 * <li>{@link #read()} if the stream should be opened with read access</li>
 * <li>{@link #write()} if the stream should be opened with write access</li>
 * <li>{@link #append()} if the streams position should be set to the end of the
 * file before every write operation. This option implicitly set the
 * {@link #write()} option</li>
 * <li>{@link #type()} the type of the element</li>
 * <ul>
 * <li>{@link ElementType#folder} is forbidden</li>
 * <li><code>null</code> stands for accept all types for which a {@link Stream}
 * can be opened</li>
 * <ul>
 * <li>note that <code>null</code> is forbidden when {@link #createAlso} is
 * set</li>
 * </ul>
 * </ul>
 * <li>{@link #createAlso} if the element should be created if the element does
 * not exist already</li>
 * <li>{@link #createOnly()} like {@link #createAlso()}, but fail if the element
 * exist already. When {@link #createOnly()} is set {@link #createAlso()} will
 * be set implicitly</li>
 * </ul>
 * 
 * @author pat
 */
public record StreamOpenOptions(boolean read, boolean write, boolean append, ElementType type, boolean createAlso, boolean createOnly) {
	
	public StreamOpenOptions(boolean read, boolean write, boolean append, ElementType type, boolean createAlso, boolean createOnly) {
		write      = write || append;
		createAlso = createAlso || createOnly;
		this.read  = read;
		this.write = write;
		if (!read && !write) {
			throw new IllegalArgumentException("can't open streams without read and without write access! spezify at least one of them.");
		}
		this.append = append;
		switch (type) {
		case folder:
			throw new IllegalArgumentException("can't open folder streams");
		default:
			throw new IllegalArgumentException("unknown element type: " + type.name());
		case null:
			if (createAlso) { throw new IllegalArgumentException("can't open a streams when createAlso or createOnly is set, but type is null"); }
		case file:
		case pipe:
			this.type = type;
		}
		this.createAlso = createAlso;
		this.createOnly = createOnly;
	}
	
	public StreamOpenOptions(boolean read, boolean write) {
		this(read, write, false, null, false, false);
	}
	
	public StreamOpenOptions(boolean read, boolean write, boolean append) {
		this(read, write, append, null, false, false);
	}
	
	public StreamOpenOptions(boolean read, boolean write, boolean append, ElementType type) {
		this(read, write, append, type, true, false);
	}
	
	public boolean seekable() {
		switch (type) {
		case file:
			return true;
		case pipe:
			return false;
		case null:
			throw new IllegalStateException("type is not set");
		default:
			throw new AssertionError("unknown type: " + type.name());
		}
	}
	
	public StreamOpenOptions ensureType(ElementType type) {
		if (type == null) { throw new NullPointerException("type is null"); }
		if (type == this.type) { return this; }
		if (this.type != null) { throw new IllegalArgumentException("this is a " + type + ", but type is set to " + this.type); }
		return new StreamOpenOptions(read, write, append, type, createAlso, createOnly);
	}
	
}

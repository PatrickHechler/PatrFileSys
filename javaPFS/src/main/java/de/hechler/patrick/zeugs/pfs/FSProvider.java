package de.hechler.patrick.zeugs.pfs;

import java.io.IOException;
import java.util.Collection;

import de.hechler.patrick.zeugs.pfs.impl.PatrFSProvider;
import de.hechler.patrick.zeugs.pfs.interfaces.FS;
import de.hechler.patrick.zeugs.pfs.interfaces.FSOptions;
import de.hechler.patrick.zeugs.pfs.opts.PatrFSOptions;

/**
 * the {@link FSProvider} provides methods to load simple file systems with the
 * interface {@link FS}
 * 
 * @author pat
 */
public abstract class FSProvider {

	/**
	 * the {@link #name} of the {@link PatrFSProvider}
	 */
	public static final String PATR_FS_NAME = "patr-fs";

	/**
	 * the name of the Provider
	 * 
	 * @see #name()
	 */
	private final String name;
	/**
	 * the maximum number of loaded file systems at a time
	 * 
	 * @see #maxLoadedFSCount()
	 */
	private final int maxFSCount;

	/**
	 * creates a new file system provider with the given name and given maximum file
	 * system load count
	 * 
	 * @param name       the name of the provider ({@link #name})
	 * @param maxFSCount the maximum number of loaded file systems at a time
	 *                   ({@link #maxFSCount})
	 */
	public FSProvider(String name, int maxFSCount) {
		this.name = name;
		this.maxFSCount = maxFSCount;
	}

	/**
	 * returns the name of this {@link FSProvider}
	 * 
	 * @return the name of this {@link FSProvider}
	 * @see #name
	 */
	public final String name() {
		return name;
	}

	/**
	 * returns the maximum number of file systems which can be loaded at a time by
	 * this file system provider
	 * 
	 * @return the maximum number of file systems which can be loaded at a time by
	 *         this file system provider
	 */
	public final int maxLoadedFSCount() {
		return maxFSCount;
	}

	/**
	 * loads a new file system with the given load options
	 * <p>
	 * the implementation may require the {@link FSOptions} {@code opts} to be a
	 * special class.<br>
	 * for example the {@value #PATR_FS_NAME} requires the {@code opts} to be
	 * {@link PatrFSOptions}.
	 * 
	 * @param opts the options for the file system
	 * @return the new loaded file system
	 * @throws IOException
	 */
	public abstract FS loadFS(FSOptions opts) throws IOException;

	/**
	 * returns a {@link Collection} which contains all file systems which has been
	 * loaded by this provider and where the {@link FS#close()} method has not yet
	 * been called
	 * <p>
	 * it is up to the implementation if the returned {@link Collection} is
	 * modifiable and if the returned {@link Collection} changes when
	 * {@link #loadedFS()} and {@link FS#close()} are called.
	 * 
	 * @return a {@link Collection} which contains all file systems which has been
	 *         loaded by this provider and where the {@link FS#close()} method has
	 *         not yet been called
	 */
	public abstract Collection<? extends FS> loadedFS();

}

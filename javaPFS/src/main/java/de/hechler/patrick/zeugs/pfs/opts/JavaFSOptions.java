package de.hechler.patrick.zeugs.pfs.opts;

import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;

import de.hechler.patrick.zeugs.pfs.FSProvider;
import de.hechler.patrick.zeugs.pfs.interfaces.FSOptions;

/**
 * the {@link FSOptions} which need to be used, when creating an File System for
 * the java wrapping provider
 * <p>
 * it is save to use a Path, which is not from the default
 * {@link FileSystemProvider}
 * 
 * @author pat
 * 
 * @param root                    the root path of the file system
 * @param allowReuseAlreadyLoaded if an already loaded FileSystem should be
 *                                used if the path is already open or if the
 *                                load operation should fail
 * 								
 * @see FSProvider#JAVA_FS_PROVIDER_NAME
 */
public record JavaFSOptions(Path root, boolean allowReuseAlreadyLoaded) implements FSOptions {
	
	/**
	 * creates new {@link JavaFSOptions} with the given options
	 * 
	 * @param root                    the root path of the file system
	 * @param allowReuseAlreadyLoaded if an already loaded FileSystem should be
	 *                                used if the path is already open or if the
	 *                                load operation should fail
	 */
	public JavaFSOptions(Path root, boolean allowReuseAlreadyLoaded) {
		this.root                    = root.normalize().toAbsolutePath();
		this.allowReuseAlreadyLoaded = allowReuseAlreadyLoaded;
	}
	
	/**
	 * creates new {@link JavaFSOptions} with the given path and
	 * {@link #allowReuseAlreadyLoaded} set to <code>true</code>
	 * 
	 * @param root the root {@link Path} of the file system
	 */
	public JavaFSOptions(Path root) {
		this(root, true);
	}
	
}

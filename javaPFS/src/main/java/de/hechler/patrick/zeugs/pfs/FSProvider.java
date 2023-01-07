package de.hechler.patrick.zeugs.pfs;

import java.io.IOException;
import java.nio.file.spi.FileSystemProvider;
import java.security.NoSuchProviderException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

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
	 * the {@link #name} of the {@link FSProvider}, which implements the
	 * patr-file-system
	 * <p>
	 * <b>WARNING:</b>
	 * this provider is only ensured to work on LINUX.<br>
	 * make sure the binaries are
	 */
	public static final String PATR_FS_PROVIDER_NAME = "patr-fs";
	/**
	 * the {@link #name} of the {@link FSProvider}, which delegates to the default
	 * {@link FileSystemProvider}
	 */
	public static final String JAVA_FS_PROVIDER_NAME = "java-fs";
	
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
	private final int    maxFSCount;
	
	/**
	 * creates a new file system provider with the given name and given maximum file
	 * system load count
	 * 
	 * @param name       the name of the provider ({@link #name})
	 * @param maxFSCount the maximum number of loaded file systems at a time
	 *                   ({@link #maxFSCount})
	 */
	protected FSProvider(String name, int maxFSCount) {
		this.name       = name;
		this.maxFSCount = maxFSCount;
	}
	
	/**
	 * returns the name of this {@link FSProvider}
	 * 
	 * @return the name of this {@link FSProvider}
	 * 
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
	 * for example the {@value #PATR_FS_PROVIDER_NAME} requires the {@code opts} to
	 * be
	 * {@link PatrFSOptions}.
	 * 
	 * @param opts the options for the file system
	 * 
	 * @return the new loaded file system
	 * 
	 * @throws IOException if an IO error occurs
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
	
	@Override
	public String toString() {
		return "FSProvider [name=" + name + "]";
	}
	
	/**
	 * this map saves all installed {@link FSProvider file-system-providers} mapped
	 * with their {@link #name}
	 */
	private static final Map<String, FSProvider> provs = loadProfs();
	
	/**
	 * load the installed providers
	 * 
	 * @return a map containing all installed {@link FSProvider
	 *         file-system-providers} mapped with their {@link #name}
	 */
	private static Map<String, FSProvider> loadProfs() {
		Map<String, FSProvider>   result  = new HashMap<>();
		ServiceLoader<FSProvider> service = ServiceLoader.load(FSProvider.class);
		for (FSProvider fsp : service) {
			FSProvider old = result.put(fsp.name, fsp);
			if (old != null) {
				throw new AssertionError("multiple FSProviders with the same name: (name='" + fsp.name + "') '" + old + "' and '" + fsp + "'");
			}
		}
		return Collections.unmodifiableMap(result);
	}
	
	/**
	 * returns the {@link FSProvider} with the given {@link #name}
	 * 
	 * @param name the {@link #name} of the provider
	 * 
	 * @return the {@link FSProvider} with the given {@link #name}
	 * 
	 * @throws NoSuchProviderException if there is no such provider
	 */
	public static FSProvider ofName(String name) throws NoSuchProviderException {
		FSProvider res = provs.get(name);
		if (res == null) { throw new NoSuchProviderException("no provider with name '" + name + "' found"); }
		return res;
	}
	
	/**
	 * returns a {@link Map} containing the file system providers mapped with the
	 * {@link #name()}
	 * 
	 * @return a {@link Map} containing the file system providers mapped with the
	 *         {@link #name()}
	 */
	public static Map<String, FSProvider> providerMap() {
		return provs;
	}
	
	/**
	 * returns a {@link Collection} containing the file system providers
	 * 
	 * @return a {@link Collection} containing the file system providers
	 */
	public static Collection<FSProvider> providers() {
		return provs.values();
	}
	
}

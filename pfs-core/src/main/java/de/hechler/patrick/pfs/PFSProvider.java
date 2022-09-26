package de.hechler.patrick.pfs;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.fs.NativePFSProvider;
import de.hechler.patrick.pfs.fs.PFS;

public abstract class PFSProvider {
	
	private static PFSProvider               defaultProv;
	private static Map <String, PFSProvider> providers;
	
	private final String identifier;
	
	public PFSProvider(String identifier) {
		if (identifier == null) {
			throw new NullPointerException("identifier is null");
		}
		this.identifier = identifier;
	}
	
	/**
	 * returns a new {@link PFS} for an existing PatrFileSystem.
	 * 
	 * the block manager of the returned {@link PFS} will store it's data on the given file
	 * 
	 * @param pfsFile
	 *            the file which stores the {@link PFS}
	 * @return the newly loaded {@link PFS} from the file
	 * @throws PatrFileSysException
	 */
	public static PFS load(String pfsFile) throws PatrFileSysException {
		if (defaultProv == null) {
			load();
		}
		return defaultProv.loadPFS(pfsFile);
	}
	
	/**
	 * creates a new {@link PFS}
	 * 
	 * @param pfsFile
	 * @param blockCount
	 * @param blockSize
	 * @return
	 * @throws PatrFileSysException
	 */
	public static PFS create(String pfsFile, long blockCount, int blockSize) throws PatrFileSysException {
		if (defaultProv == null) {
			load();
		}
		return defaultProv.createPFS(pfsFile, blockCount, blockSize);
	}
	
	/**
	 * returns the default provider
	 * 
	 * @return the default provider
	 */
	public static PFSProvider defaultProvider() {
		if (defaultProv == null) {
			load();
		}
		return defaultProv;
	}
	
	/**
	 * returns an unmodifiable map which contains all providers with their {@link #identifier} as keys
	 * 
	 * @return an unmodifiable map with all providers
	 */
	public static Map <String, PFSProvider> allProviders() {
		if (defaultProv == null) {
			load();
		}
		return providers;
	}
	
	private static synchronized void load() {
		if (defaultProv != null) {
			return;
		}
		java.nio.file.spi.FileSystemProvider.installedProviders();
		ServiceLoader <PFSProvider> loader = ServiceLoader.load(PFSProvider.class, PFSProvider.class.getClassLoader());
		final Map <String, PFSProvider> provs = new HashMap <>();
		loader.forEach(p -> {
			PFSProvider old = provs.put(p.identifier, p);
			if (old != null) {
				throw new InternalError("multiple providers use the same identifier: '" + p.identifier + "'");
			}
		});
		String defPI = System.getProperty("pfs.provider", NativePFSProvider.IDENTIFIER);
		PFSProvider defP = provs.get(defPI);
		if (defP == null) {
			if (provs.isEmpty()) {
				throw new InternalError("no PFSProviders found! (not even the embedded ones)");
			}
			defP = provs.values().iterator().next();
		}
		defaultProv = defP;
		providers = Collections.unmodifiableMap(provs);
	}
	
	public abstract PFS loadPFS(String pfsFile) throws PatrFileSysException;
	
	public abstract PFS createPFS(String pfsFile, long blockCount, int blockSize) throws PatrFileSysException;
	
	public final String identifier() {
		return identifier;
	}
	
}

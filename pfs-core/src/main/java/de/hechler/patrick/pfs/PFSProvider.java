package de.hechler.patrick.pfs;


import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import de.hechler.patrick.pfs.bm.BlockManager;
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
	 * @throws IOException
	 */
	public static PFS load(String pfsFile) throws IOException {
		if (defaultProv == null) {
			load();
		}
		return defaultProv.loadPFS(pfsFile);
	}
	
	/**
	 * creates a new {@link PFS} this method will also format the new {@link PFS}
	 * 
	 * @param pfsFile
	 *            the underlying file
	 * @param blockCount
	 *            the number of blocks available for the {@link PFS}
	 * @param blockSize
	 *            the sieze of each block
	 * @return
	 * @throws IOException
	 */
	public static PFS create(String pfsFile, long blockCount, int blockSize) throws IOException {
		if (defaultProv == null) {
			load();
		}
		return defaultProv.createPFS(pfsFile, blockCount, blockSize);
	}
	
	/**
	 * returns a new {@link PFS} for an existing PatrFileSystem.
	 * 
	 * the block manager of the returned {@link PFS} will store it's data on the given {@link BlockManager}
	 * 
	 * @param bm
	 *            the underlying {@link BlockManager}
	 * @return the newly loaded {@link PFS} from the file
	 * @throws IOException
	 */
	public static PFS load(BlockManager bm) throws IOException {
		if (defaultProv == null) {
			load();
		}
		return defaultProv.loadPFS(bm);
	}
	
	/**
	 * creates a new {@link PFS} this method will also format the new {@link PFS}
	 * <p>
	 * the block-size will be read from the {@link BlockManager#blockSize()}
	 * 
	 * @param bm
	 *            the underlying {@link BlockManager}
	 * @param blockCount
	 *            the number of blocks which will be available for the {@link PFS}
	 * @return the newly created {@link PFS}
	 * @throws IOException
	 *             if an error occurs
	 */
	public static PFS create(BlockManager bm, long blockCount) throws IOException {
		if (defaultProv == null) {
			load();
		}
		return defaultProv.createPFS(bm, blockCount);
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
	
	public abstract PFS loadPFS(String pfsFile) throws IOException;
	
	public abstract PFS createPFS(String pfsFile, long blockCount, int blockSize) throws IOException;
	
	public abstract PFS loadPFS(BlockManager bm) throws IOException;
	
	public abstract PFS createPFS(BlockManager bm, long blockCount) throws IOException;
	
	public final String identifier() {
		return identifier;
	}
	
}

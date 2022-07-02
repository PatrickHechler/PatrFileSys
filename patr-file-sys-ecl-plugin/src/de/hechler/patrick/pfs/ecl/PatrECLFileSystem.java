package de.hechler.patrick.pfs.ecl;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.IPath;

public class PatrECLFileSystem extends FileSystem {
	
	public static final char   PATH_SEPARATOR         = IPath.SEPARATOR;
	public static final char   PFS_PATH_SEPERATOR     = IPath.DEVICE_SEPARATOR;
	public static final String PATH_SEPARATOR_STR     = "" + IPath.SEPARATOR;
	public static final String PFS_PATH_SEPERATOR_STR = "" + IPath.DEVICE_SEPARATOR;
	
	@Override
	public IFileStore getStore(URI uri) {
		String path = uri.getPath();
		String[] split = path.split(PFS_PATH_SEPERATOR_STR, 2);
		if (split.length < 2) {
			throw new IllegalArgumentException("illegal URI path: '" + path + "'");
		}
		return new PatrFileStore(split[0], split[1].split(PATH_SEPARATOR_STR));
	}
	
	@Override
	public IFileStore getStore(IPath path) {
		try {
			return getStore(new URI(getScheme(), null, path.toString(), null));
		} catch (URISyntaxException e) {
			return EFS.getNullFileSystem().getStore(path);
		}
	}
	
	@Override
	public boolean canDelete() {
		return canWrite();
	}
	
	@Override
	public boolean canWrite() {
		return true;
	}
	
	@Override
	public boolean isCaseSensitive() {
		return true;
	}
	
}

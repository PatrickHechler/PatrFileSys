package patrick.hechler.de.pfs.ecl;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.IPath;

public class PatrECLFileSystem extends FileSystem {
	
	@Override
	public IFileStore getStore(URI uri) {
		String path = uri.getPath();
		String[] split = path.split(":://", 2);
		if (split.length < 2) {
			throw new IllegalArgumentException("illegal URI path: '" + path + "'");
		}
		return new PatrFileStore(split[0], split[1].split("/"));
	}
	
	@Override
	public IFileStore getStore(IPath path) {
		try {
			return getStore(new URI(getScheme(), path.toString(), null));
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

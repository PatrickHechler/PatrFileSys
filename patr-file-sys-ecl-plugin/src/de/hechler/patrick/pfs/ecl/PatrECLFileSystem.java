package de.hechler.patrick.pfs.ecl;

import static de.hechler.patrick.pfs.ecl.PatrFileStore.checkCanceled;
import static de.hechler.patrick.pfs.ecl.PatrFileStore.isCanceled;
import static de.hechler.patrick.pfs.ecl.PatrFileStore.throwCoreExep;
import static de.hechler.patrick.pfs.ecl.PatrFileStore.throwCoreUnknownOpts;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_LOCK;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.filesystem.IFileTree;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.filesystem.provider.FileTree;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.objects.jfs.PFSFileSystemProviderImpl;

public class PatrECLFileSystem extends FileSystem {
	
	public static final char DEVICE_SEPARATOR = IPath.DEVICE_SEPARATOR;
	public static final char PATH_SEPARATOR   = IPath.SEPARATOR;
	
	@Override
	public PatrFileStore getStore(URI uri) {
		String path = uri.getPath();
		if (path == null) {
			throw new IllegalArgumentException("path='" + path + "'");
		}
		int li = path.lastIndexOf(DEVICE_SEPARATOR);
		return new PatrFileStore(URI.create(path.substring(0, li)), path.substring(li + 1).split(PATH_SEPARATOR + ""));
	}
	
	@Override
	public IFileStore getStore(IPath path) {
		try {
			String str = path.toString();
			Path p;
			if (str.charAt(0) == PATH_SEPARATOR) {
				p = Paths.get("/");
			} else {
				p = Paths.get(".");
			}
			int i = 0;
			while (Files.exists(p)) {
				p = p.resolve(path.segment(i));
				i ++ ;
			}
			p = p.getParent();
			Path other = Paths.get(str);
			other = other.subpath(i, other.getNameCount());
			String pfsPath = p.toAbsolutePath().toString();
			String inPfsPathStr = other.toString();
			if (inPfsPathStr.indexOf(DEVICE_SEPARATOR) != -1) {
				throw new IllegalArgumentException("inPFSPath contains '" + DEVICE_SEPARATOR + "'");
			}
			return getStore(new URI("pfs", str, str, 0, (pfsPath.charAt(0) == '/' ? "file:" + pfsPath : "file:/" + pfsPath) + DEVICE_SEPARATOR + inPfsPathStr, str, null));
		} catch (URISyntaxException e) {
			return EFS.getNullFileSystem().getStore(path);
		}
	}
	
	@Override
	public IFileTree fetchFileTree(final IFileStore root, final IProgressMonitor monitor) throws CoreException {
		if ( ! (root instanceof PatrFileStore)) {
			return null;
		}
		try {
			IFileTree result = ((PatrFileStore) root).pfs().getRoot().withLock(() -> {
				PatrFileSysElement e = ((PatrFileStore) root).element();
				List <IFileInfo> infos = new ArrayList <>();
				List <IFileStore> stores = new ArrayList <>();
				read(monitor, e, root, ((PatrFileStore) root).pfs.toString(), infos, stores);
				assert infos.size() == stores.size();
				final Map <IFileStore, IFileInfo> map = new HashMap <>(infos.size());
				for (int i = 0; i < infos.size(); i ++ ) {
					map.put(stores.get(i), infos.get(i));
				}
				final IFileStore[] s = stores.toArray(new IFileStore[stores.size()]);
				final IFileInfo[] i = infos.toArray(new IFileInfo[infos.size()]);
				return new FileTree(root) {
					
					@Override
					public IFileInfo getFileInfo(IFileStore arg0) {
						return map.get(arg0);
					}
					
					@Override
					public IFileStore[] getChildStores(IFileStore arg0) {
						return s;
					}
					
					@Override
					public IFileInfo[] getChildInfos(IFileStore arg0) {
						return i;
					}
					
				};
			});
			checkCanceled(monitor, getClass());
			return result;
		} catch (IOException e) {
			throw throwCoreExep(e, getClass());
		}
	}
	
	public static abstract class EmptyArrays extends FileStore {
		
		private static final String[]     STRING     = FileStore.EMPTY_STRING_ARRAY;
		private static final IFileStore[] FILE_STORE = new IFileStore[0];
		private static final IFileInfo[]  FILE_INFO  = FileStore.EMPTY_FILE_INFO_ARRAY;
		
	}
	
	private void read(IProgressMonitor mon, PatrFileSysElement e, IFileStore parent, String pfs, List <IFileInfo> i, List <IFileStore> s) throws IOException {
		if (isCanceled(mon)) return;
		CachFileStore store = new CachFileStore();
		FileInfo info = new FileInfo(e.getName());
		i.add(info);
		s.add(store);
		if (isCanceled(mon)) return;
		info.setError(EFS.NONE);
		info.setExists(true);
		info.setLastModified(e.getLastModTime());
		if (isCanceled(mon)) return;
		info.setAttribute(EFS.ATTRIBUTE_EXECUTABLE, e.isExecutable());
		if (isCanceled(mon)) return;
		info.setAttribute(EFS.ATTRIBUTE_HIDDEN, e.isHidden());
		if (isCanceled(mon)) return;
		info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, e.isReadOnly());
		if (isCanceled(mon)) return;
		String[] cn = EmptyArrays.STRING;
		IFileInfo[] cis = EmptyArrays.FILE_INFO;
		IFileStore[] cs = EmptyArrays.FILE_STORE;
		try {
			if (e.isFolder()) {
				info.setDirectory(true);
				PatrFolder f = e.getFolder();
				int len = f.elementCount(NO_LOCK);
				for (int ci = 0; ci < len; ci ++ ) {
					if (isCanceled(mon)) return;
					PatrFileSysElement c = f.getElement(ci, NO_LOCK);
					read(mon, c, store, pfs, i, s);
				}
			} else {
				info.setDirectory(false);
				PatrFile f = e.getFile();
				info.setLength(f.length(NO_LOCK));
			}
		} catch (ElementLockedException ignore) {
			info.setError(EFS.ERROR_AUTH_FAILED);
		}
		if (isCanceled(mon)) return;
		URI uri;
		try {
			uri = new URI("pfs", null, null, 0, pfs + DEVICE_SEPARATOR + PFSFileSystemProviderImpl.buildName(e), null, null);
		} catch (URISyntaxException e1) {
			throw new RuntimeException(e1);
		}
		store.info = info;
		store.parent = parent;
		store.cn = cn;
		store.cis = cis;
		store.cs = cs;
		store.uri = uri;
	}
	
	private class CachFileStore extends FileStore {
		
		private FileInfo     info;
		private IFileStore   parent;
		private String[]     cn;
		private IFileInfo[]  cis;
		private IFileStore[] cs;
		private URI          uri;
		
		@Override
		public String[] childNames(int arg0, IProgressMonitor arg1) throws CoreException {
			checkOpts(arg0, EFS.NONE);
			return cn.clone();
		}
		
		@Override
		public IFileInfo fetchInfo(int arg0, IProgressMonitor arg1) throws CoreException {
			checkOpts(arg0, EFS.NONE);
			return (IFileInfo) info.clone();
		}
		
		@Override
		public IFileInfo[] childInfos(int options, IProgressMonitor monitor) throws CoreException {
			checkOpts(options, EFS.NONE);
			return cis.clone();
		}
		
		@Override
		public IFileStore[] childStores(int options, IProgressMonitor monitor) throws CoreException {
			checkOpts(options, EFS.NONE);
			return cs.clone();
		}
		
		@Override
		public IFileStore getChild(String arg0) {
			for (int i = 0; i < cn.length; i ++ ) {
				if (cn[i].equals(arg0)) {
					return cs[i];
				}
			}
			CachFileStore c = new CachFileStore();
			c.info = new FileInfo(arg0);
			c.info.setExists(false);
			c.parent = this;
			c.cn = EmptyArrays.STRING;
			c.cis = EmptyArrays.FILE_INFO;
			c.cs = EmptyArrays.FILE_STORE;
			try {
				c.uri = new URI("pfs", null, null, 0, uri.getPath() + PATH_SEPARATOR + arg0, null, null);
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
			return c;
		}
		
		@Override
		public String getName() {
			return info.getName();
		}
		
		@Override
		public IFileStore getParent() {
			return parent;
		}
		
		@Override
		public URI toURI() {
			return uri;
		}
		
		@Override
		public IFileInfo fetchInfo() {
			return info;
		}
		
		@Override
		public void copy(IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
			getStore(uri).copy(destination, options, monitor);
		}
		
		@Override
		public void move(IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
			if (destination instanceof CachFileStore) {
				destination = getStore( ((CachFileStore) destination).uri);
			}
			getStore(uri).move(destination, options, monitor);
		}
		
		@Override
		public void delete(int options, IProgressMonitor monitor) throws CoreException {
			getStore(uri).delete(options, monitor);
		}
		
		@Override
		public InputStream openInputStream(int arg0, IProgressMonitor arg1) throws CoreException {
			return getStore(uri).openInputStream(arg0, arg1);
		}
		
		@Override
		public boolean isParentOf(IFileStore other) {
			if (other instanceof CachFileStore) {
				return ((CachFileStore) other).parent.equals(this);
			}
			return getStore(uri).isParentOf(other);
		}
		
		@Override
		public IFileSystem getFileSystem() {
			return PatrECLFileSystem.this;
		}
		
		@Override
		public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
			getStore(uri).mkdir(options, monitor);
			return this;
		}
		
		private void checkOpts(int arg0, int i) throws CoreException {
			if ( (arg0 & i) != arg0) {
				throw throwCoreUnknownOpts(arg0, getClass());
			}
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

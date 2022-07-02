package de.hechler.patrick.pfs.ecl;

import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_LOCK;
import static de.hechler.patrick.pfs.ecl.PatrECLFileSystem.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.interfaces.BlockAccessor;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.objects.ba.SeekablePathBlockAccessor;
import de.hechler.patrick.pfs.objects.fs.PatrFileSysImpl;

public class PatrFileStore extends FileStore {
	
	private final String   pfs;
	private final String[] path;
	
	public PatrFileStore(String pfs, String[] path) {
		this.pfs = pfs;
		this.path = path;
	}
	
	@Override
	public String[] childNames(int opts, IProgressMonitor mon) throws CoreException {
		if (opts != EFS.NONE) {
			throw throwCoreUnknownOpts(opts);
		}
		try {
			PatrFileSysElement e = element();
			String[] result = e.withLock(() -> {
				if ( !e.isFolder()) {
					return new String[0];
				}
				PatrFolder f = e.getFolder();
				String[] res = new String[f.elementCount(NO_LOCK)];
				for (int i = 0; i < res.length; i ++ ) {
					if (isCanceled(mon)) {
						return null;
					}
					res[i] = f.getElement(i, NO_LOCK).getName();
				}
				return res;
			});
			if (result == null) {
				throw throwCoreCancel();
			}
			return result;
		} catch (IOException e) {
			throw throwCoreExep(e);
		}
	}
	
	@Override
	public IFileInfo fetchInfo(int opts, IProgressMonitor mon) throws CoreException {
		PatrFileSysElement e;
		FileInfo info = new FileInfo(getName());
		try {
			e = element();
		} catch (NoSuchElementException | NotDirectoryException | IllegalStateException e1) {
			info.setExists(false);
			return info;
		} catch (IOException e1) {
			throw throwCoreExep(e1);
		}
		info.setExists(true);
		try {
			checkCanceled(mon);
			e.withLock(() -> {
				if (isCanceled(mon)) return;
				if (e.isFile()) {
					info.setDirectory(false);
					if (isCanceled(mon)) return;
					try {
						info.setLength(e.getFile().length(NO_LOCK));
					} catch (ElementLockedException e1) {
						info.setLength(EFS.NONE);
					}
				} else {
					info.setDirectory(true);
					info.setLength(EFS.NONE);
				}
				if (isCanceled(mon)) return;
				info.setLastModified(e.getLastModTime());
				if (isCanceled(mon)) return;
				info.setAttribute(EFS.ATTRIBUTE_EXECUTABLE, e.isExecutable());
				if (isCanceled(mon)) return;
				info.setAttribute(EFS.ATTRIBUTE_HIDDEN, e.isHidden());
				if (isCanceled(mon)) return;
				info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, e.isReadOnly());
				return;
			});
			checkCanceled(mon);
			info.setAttribute(EFS.ATTRIBUTE_SYMLINK, false);
			info.setError(IFileInfo.NONE);
		} catch (IOException e1) {
			throw throwCoreExep(e1);
		}
		return null;
	}
	
	@Override
	public IFileStore getChild(String name) {
		String[] p = Arrays.copyOf(path, path.length + 1);
		p[path.length] = name;
		return new PatrFileStore(pfs, p);
	}
	
	@Override
	public String getName() {
		return path[path.length - 1];
	}
	
	@Override
	public IFileStore getParent() {
		if (path.length == 0) {
			return null;
		}
		return new PatrFileStore(pfs, Arrays.copyOf(path, path.length - 1));
	}
	
	@Override
	public InputStream openInputStream(int opts, IProgressMonitor mon) throws CoreException {
		if (opts != EFS.NONE) {
			throw throwCoreUnknownOpts(opts);
		}
		try {
			PatrFileSysElement e = element();
			return e.withLock(() -> {
				return e.getFile().openInput(NO_LOCK);
			});
		} catch (IOException | IllegalStateException e) {
			throw throwCoreExep(e);
		}
	}
	
	@Override
	public URI toURI() {
		try {
			return new URI("pfs", null, pfs + PFS_PATH_SEPERATOR + path, null);
		} catch (URISyntaxException e) {
			throw new AssertionError(e);
		}
	}
	
	@Override
	public IFileInfo[] childInfos(int options, IProgressMonitor monitor) throws CoreException {
		try {
			return element().withLock(() -> super.childInfos(options, monitor));
		} catch (IOException e) {
			throw throwCoreExep(e);
		}
	}
	
	@Override
	public void copy(IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
		try {
			element().withLock(() -> super.copy(destination, options, monitor));
		} catch (IOException e) {
			throw throwCoreExep(e);
		}
	}
	
	@Override
	public IFileStore mkdir(final int options, final IProgressMonitor monitor) throws CoreException {
		if ( (options & EFS.SHALLOW) != options) {
			throw throwCoreExep("onknown options: " + options + " [0x" + Integer.toHexString(options) + "]");
		}
		try {
			PatrFileSystem patrFS = pfs();
			PatrFolder root = patrFS.getRoot();
			if (root.withLockBoolean(() -> {
				PatrFolder parent = root;
				for (int i = 0; i < path.length; i ++ ) {
					if (isCanceled(monitor)) return false;
					try {
						parent = parent.getElement(path[i], NO_LOCK).getFolder();
					} catch (IllegalStateException | NoSuchFileException e) {
						if (options == EFS.SHALLOW) {
							if (i + 1 < path.length) {
								return true;
							}
						}
						parent = parent.addFolder(path[i], NO_LOCK);
					}
				}
				return false;
			})) {
				throw throwCoreExep("SWALLOW set, but not only the parent does not exist (or is a file)! (" + this + ')');
			}
			checkCanceled(monitor);;
			return this;
		} catch (IOException e) {
			throw throwCoreExep(e);
		}
	}
	
	@Override
	public void move(IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
		try {
			element().withLock(() -> super.move(destination, options, monitor));
		} catch (IOException e) {
			throw throwCoreExep(e);
		}
	}
	
	@Override
	public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
		if ( (options & EFS.APPEND) != options) {
			throw throwCoreUnknownOpts(options);
		}
		try {
			PatrFileSystem patrFS = pfs();
			PatrFolder parent = parent(patrFS.getRoot());
			PatrFile file;
			try {
				file = parent.getElement(path[path.length - 1], NO_LOCK).getFile();
			} catch (NoSuchFileException e) {
				file = parent.addFile(path[path.length - 1], NO_LOCK);
			}
			return file.openOutput(options == EFS.APPEND, NO_LOCK);
		} catch (IOException | IllegalStateException e) {
			throw throwCoreExep(e);
		}
	}
	
	@Override
	public void putInfo(IFileInfo info, int options, IProgressMonitor monitor) throws CoreException {
		try {
			PatrFileSysElement e = element();
			e.withLock(() -> {
				if ( (options & EFS.SET_ATTRIBUTES) != 0) {
					if (isCanceled(monitor)) return;
					e.setExecutable(info.getAttribute(EFS.ATTRIBUTE_EXECUTABLE), NO_LOCK);
					if (isCanceled(monitor)) return;
					e.setHidden(info.getAttribute(EFS.ATTRIBUTE_HIDDEN), NO_LOCK);
					if (isCanceled(monitor)) return;
					e.setReadOnly(info.getAttribute(EFS.ATTRIBUTE_READ_ONLY), NO_LOCK);
				}
				if ( (options & EFS.SET_LAST_MODIFIED) != 0) {
					if (isCanceled(monitor)) return;
					e.setLastModTime(info.getLastModified(), NO_LOCK);
				}
			});
		} catch (IOException e) {
			throw throwCoreExep(e);
		}
		checkCanceled(monitor);
	}
	
	public PatrFileSystem pfs() throws IOException {
		Path p = Paths.get(pfs);
		BlockAccessor ba = SeekablePathBlockAccessor.create(p, -1, false);
		PatrFileSystem patrFS = new PatrFileSysImpl(ba);
		return patrFS;
	}
	
	public PatrFileSysElement element() throws IOException {
		PatrFileSystem patrFS = pfs();
		PatrFolder root = patrFS.getRoot();
		return root.withLock(() -> {
			PatrFolder parent = parent(root);
			return parent.getElement(path[path.length - 1], NO_LOCK);
		});
	}
	
	private PatrFolder parent(PatrFolder root) throws IOException, ElementLockedException, NoSuchFileException {
		PatrFolder parent = root;
		for (int i = 0; i < path.length - 1; i ++ ) {
			parent = parent.getElement(path[i], NO_LOCK).getFolder();
		}
		return parent;
	}
	
	private static boolean isCanceled(IProgressMonitor mon) {
		return mon != null && mon.isCanceled();
	}
	
	private static void checkCanceled(IProgressMonitor mon) throws CoreException {
		if (isCanceled(mon)) {
			throw throwCoreCancel();
		}
	}
	
	private CoreException throwCoreUnknownOpts(int opts) throws CoreException {
		return throwCoreExep("unknown options: " + opts + " [0x" + Integer.toHexString(opts).toUpperCase() + "]");
	}
	
	private static CoreException throwCoreCancel() throws CoreException {
		throw new CoreException(new Status(IStatus.CANCEL, PatrFileStore.class, "canceled"));
	}
	
	private static CoreException throwCoreExep(Throwable t) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, PatrFileStore.class, t.toString(), t));
	}
	
	private static CoreException throwCoreExep(String msg) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, PatrFileStore.class, msg));
	}
	
}


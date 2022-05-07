package de.hechler.patrick.pfs.objects.jfs;

import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.LOCK_NO_LOCK;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.objects.jfs.PFSPathImpl.Name;


public class PFSDirectoryStreamImpl implements DirectoryStream <Path> {
	
	private final PFSFileSystemImpl     fileSys;
	private final long                  lock;
	private final PatrFolder            folder;
	private final Filter <? super Path> filter;
	private final Name[]                path;
	private final PFSPathImpl           root;
	
	public PFSDirectoryStreamImpl(PFSFileSystemImpl fileSys, long lock, PatrFolder folder, Filter <? super Path> filter, Name[] path) {
		this.fileSys = fileSys;
		this.lock = lock;
		this.folder = folder;
		this.filter = filter;
		this.path = path;
		this.root = new PFSPathImpl(fileSys);
	}
	
	@Override
	public void close() throws IOException {
		if (lock != LOCK_NO_LOCK) {
			folder.removeLock(lock);
		}
	}
	
	@Override
	public Iterator <Path> iterator() {
		return new PFSDirectoryStreamImplIterator();
	}
	
	private class PFSDirectoryStreamImplIterator implements Iterator <Path> {
		
		private int  lastindex = -1;
		private int  index     = 0;
		private Path n;
		
		@Override
		public boolean hasNext() {
			if (n != null) {
				return true;
			}
			try {
				n = folder.withLock(this::executeNext);
				return true;
			} catch (NoSuchElementException e) {
				return false;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public Path next() throws NoSuchElementException {
			if (n != null) {
				Path p = n;
				n = null;
				return p;
			}
			try {
				Path result = folder.withLock(this::executeNext);
				lastindex = index;
				return result;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		private Path executeNext() throws NoSuchElementException, IOException {
			try {
				PFSPathImpl result;
				do {
					PatrFileSysElement e = folder.getElement(index, lock);
					Name[] names = Arrays.copyOf(path, path.length + 1);
					names[path.length] = Name.create(e.getName());
					result = new PFSPathImpl(fileSys, root, names);
					index ++ ;
				} while ( !filter.accept(result));
				return result;
			} catch (IndexOutOfBoundsException e) {
				throw new NoSuchElementException(e.getMessage());
			}
		}
		
		@Override
		public void remove() {
			try {
				PatrFileSysElement child = folder.getElement(lastindex, lock);
				if (child.isFile()) {
					child.getFile().delete(LOCK_NO_LOCK, lock);
				} else {
					child.getFolder().delete(LOCK_NO_LOCK, lock);
				}
				lastindex = -1;
				index -- ;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
	}
	
}

package de.hechler.patrick.zeugs.pfs.impl.java;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

import de.hechler.patrick.zeugs.pfs.interfaces.FSElement;
import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Folder;
import de.hechler.patrick.zeugs.pfs.interfaces.Pipe;

public class JavaFolder extends JavaFSElement implements Folder {
	
	public JavaFolder(Path root, Path path) {
		super(root, path);
		
	}
	
	@Override
	public FolderIter iter(boolean showHidden) throws IOException {
		ensureOpen();
		DirectoryStream<Path> stream;
		if (showHidden) {
			stream = Files.newDirectoryStream(root.resolve(path));
		} else {
			stream = Files.newDirectoryStream(root.resolve(path), p -> {
				Path fn = p.getFileName();
				if (fn == null) { return true; }
				String str = fn.toString();
				if (str.isEmpty()) { return true; }
				return str.charAt(0) != '.';
			});
		}
		return new FolderIter() {
			
			private volatile boolean iterClosed;
			
			private Iterator<Path> iter;
			private JavaFSElement  last = null;
			
			
			private void ensureIterOpen() throws ClosedChannelException {
				if (iterClosed) { throw new ClosedChannelException(); }
			}
			
			@Override
			public void close() throws IOException {
				stream.close();
				last       = null;
				iterClosed = true;
			}
			
			@Override
			public FSElement nextElement() throws IOException {
				ensureIterOpen();
				Path          n = iter.next();
				JavaFSElement e = new JavaFSElement(root, n.relativize(root));
				last = e;
				return e;
			}
			
			@Override
			public boolean hasNextElement() throws IOException {
				ensureIterOpen();
				return iter.hasNext();
			}
			
			@Override
			public void delete() throws IOException {
				ensureIterOpen();
				JavaFSElement l = last;
				last = null;
				if (l == null) { throw new NoSuchElementException(); }
				l.delete();
			}
			
		};
	}
	
	@Override
	public long childCount() throws IOException {
		long cnt = 0L;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(root.resolve(path))) {
			for (@SuppressWarnings("unused")
			Path p : stream) {
				cnt++;
			}
		}
		return cnt;
	}
	
	@Override
	public FSElement childElement(String name) throws IOException {
		checkValidName(name);
		Path f = full.resolve(name);
		Path p = checkExists(f);
		return new JavaFSElement(root, p);
	}
	
	@Override
	public Folder childFolder(String name) throws IOException {
		checkValidName(name);
		Path f = full.resolve(name);
		Path p = checkExists(f);
		if (!Files.isDirectory(f)) { throw new NotDirectoryException(p.toString()); }
		return new JavaFolder(root, p);
	}
	
	@Override
	public File childFile(String name) throws IOException {
		checkValidName(name);
		Path f = full.resolve(name);
		Path p = checkExists(f);
		if (!Files.isRegularFile(f)) { throw new NotDirectoryException(p.toString()); }
		return new JavaFile(root, p);
	}
	
	@Override
	public Pipe childPipe(String name) throws IOException {
		checkValidName(name);
		Path p = path.resolve(name);
		throw new NoSuchFileException(p.toString(), path.toString(), "pipes are not supported");
	}
	
	@Override
	public Folder createFolder(String name) throws IOException {
		checkValidName(name);
		Path f = full.resolve(name);
		Files.createDirectory(f);
		return new JavaFolder(root, root.relativize(f));
	}
	
	@Override
	public File createFile(String name) throws IOException {
		checkValidName(name);
		Path f = full.resolve(name);
		Files.createFile(f);
		return new JavaFile(root, root.relativize(f));
	}
	
	@Override
	public Pipe createPipe(String name) throws IOException {
		throw new UnsupportedOperationException("pipes are not supported");
	}
	
	
	private Path checkExists(Path f) throws NoSuchFileException {
		Path res = root.relativize(f);
		if (!Files.exists(f)) { throw new NoSuchFileException(res.toString(), path.toString(), "no child element"); }
		return res;
	}
	
	private static void checkValidName(String name) {
		if (name.indexOf('/') != -1) { throw new IllegalArgumentException("name contains path seperator"); }
	}
	
}

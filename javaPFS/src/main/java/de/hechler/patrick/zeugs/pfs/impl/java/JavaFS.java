package de.hechler.patrick.zeugs.pfs.impl.java;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.hechler.patrick.zeugs.pfs.interfaces.FS;
import de.hechler.patrick.zeugs.pfs.interfaces.FSElement;
import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Folder;
import de.hechler.patrick.zeugs.pfs.interfaces.Pipe;
import de.hechler.patrick.zeugs.pfs.interfaces.Stream;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

public class JavaFS implements FS {
	
	private final JavaFSProvider prov;
	final Path                   root;
	
	private volatile JavaFolder cwd;
	private volatile boolean    closed;
	
	public JavaFS(JavaFSProvider prov, Path root) {
		this.prov = prov;
		this.root = root;
		this.cwd  = new JavaFolder(this, Paths.get(".").normalize());
	}
	
	
	public void ensureOpen() throws ClosedChannelException {
		if (closed) { throw new ClosedChannelException(); }
	}
	
	@Override
	public long blockCount() throws IOException {
		ensureOpen();
		long blocks = 0L;
		for (FileStore store : this.root.getFileSystem().getFileStores()) {
			blocks += store.getTotalSpace() / store.getBlockSize();
		}
		return blocks;
	}
	
	@Override
	public int blockSize() throws IOException {
		ensureOpen();
		long min = Long.MAX_VALUE;
		for (FileStore store : this.root.getFileSystem().getFileStores()) {
			long size = store.getBlockSize();
			if (size > min) {
				min = size;
			}
		}
		return min > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) min;
	}
	
	@Override
	public FSElement element(String path) throws IOException {
		ensureOpen();
		if (path.isEmpty()) { throw new IllegalArgumentException("path is empty"); }
		Path p;
		if (path.charAt(0) == '/') {
			if (path.length() == 1) {
				p = root;
			} else {
				p = root.resolve('.' + path);
			}
		} else {
			p = cwd.f().resolve(path);
		}
		p = p.toRealPath();
		if (!p.startsWith(root)) throw new IllegalArgumentException("the root folder has no parent");
		Path rel = root.relativize(p);
		if (Files.isDirectory(p)) {
			return new JavaFolder(this, rel);
		} else {
			return new JavaFile(this, rel);
		}
	}
	
	@Override
	public Folder folder(String path) throws IOException {
		return element(path).getFolder();
	}
	
	@Override
	public File file(String path) throws IOException {
		return element(path).getFile();
	}
	
	@Override
	public Pipe pipe(String path) throws IOException {
		throw new UnsupportedOperationException("pipe");
	}
	
	@Override
	public Stream stream(String path, StreamOpenOptions opts) throws IOException {
		return file(path).open(opts);
	}
	
	@Override
	public Folder cwd() throws IOException {
		ensureOpen();
		return new JavaFolder(this, cwd.p());
	}
	
	@Override
	public void cwd(Folder f) throws IOException {
		ensureOpen();
		if (!(f instanceof JavaFolder jf) || jf.fs != this) {
			throw new IllegalArgumentException("the folder does not belong to this file system (folder: " + f + ")");
		}
		cwd = jf;
	}
	
	@Override
	public void close() throws IOException {
		if (closed) { return; }
		closed = true;
		prov.unload(this);
	}
	
}

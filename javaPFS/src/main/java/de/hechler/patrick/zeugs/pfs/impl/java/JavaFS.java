package de.hechler.patrick.zeugs.pfs.impl.java;

import java.io.IOException;
import java.nio.file.FileStore;
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
	
	final Path root;
	
	private volatile JavaFolder cwd;
	
	public JavaFS(Path root) {
		this.root = root;
		this.cwd  = new JavaFolder(root, Paths.get(".").normalize());
	}
	
	@Override
	public long blockCount() throws IOException {
		long blocks = 0L;
		for (FileStore store : this.root.getFileSystem().getFileStores()) {
			blocks += store.getTotalSpace() / store.getBlockSize();
		}
		return blocks;
	}
	
	@Override
	public int blockSize() throws IOException {
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
		// TODO Auto-generated method stub
		return null;
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
		return element(path).getPipe();
	}
	
	@Override
	public Stream stream(String path, StreamOpenOptions opts) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Folder cwd() throws IOException {
		return cwd;
	}
	
	@Override
	public void cwd(Folder f) throws IOException {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
	}
	
}

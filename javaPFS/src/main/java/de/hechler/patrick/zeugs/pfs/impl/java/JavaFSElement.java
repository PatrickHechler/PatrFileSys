package de.hechler.patrick.zeugs.pfs.impl.java;

import java.io.IOError;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Path;

import de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSElement;
import de.hechler.patrick.zeugs.pfs.interfaces.FSElement;
import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Folder;
import de.hechler.patrick.zeugs.pfs.interfaces.Pipe;

public class JavaFSElement implements FSElement {
	
	protected final Path       root;
	protected final Path       path;
	protected final Path       full;
	protected volatile boolean closed;
	
	public JavaFSElement(Path root, Path path) {
		this.root = root;
		this.path = path;
		this.full = root.resolve(path);
	}
	
	protected void ensureOpen() throws ClosedChannelException {
		if (closed) { throw new ClosedChannelException(); }
	}
	
	@Override
	public void close() throws IOException {
		closed = true;
	}
	
	@Override
	public Folder parent() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int flags() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public void flag(int add, int rem) throws IOException {
		// TODO Auto-generated method stub
	}
	
	@Override
	public long lastModTime() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public void lastModTime(long time) throws IOException {
		// TODO Auto-generated method stub
	}
	
	@Override
	public long createTime() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public void createTime(long time) throws IOException {
		// TODO Auto-generated method stub
	}
	
	@Override
	public String name() throws IOException {
		return path.getFileName().toString();
	}
	
	@Override
	public void name(String name) throws IOException {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void parent(Folder parent) throws IOException {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void move(Folder parent, String name) throws IOException {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void delete() throws IOException {
		// TODO Auto-generated method stub
	}
	
	@Override
	public Folder getFolder() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public File getFile() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Pipe getPipe() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean equals(FSElement e) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PatrFSElement element) {
			try {
				return equals(element);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		try {
			return name().hashCode();
		} catch (IOException e) {
			throw new IOError(e);
		}
	}
	
}

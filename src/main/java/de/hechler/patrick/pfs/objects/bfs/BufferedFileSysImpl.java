package de.hechler.patrick.pfs.objects.bfs;

import java.io.IOException;

import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.interfaces.PatrFolder;

public class BufferedFileSysImpl implements PatrFileSystem {
	
	private final PatrFileSystem fs;
	private final PatrFolder     root;
	private long                 totalSpace;
	private long                 blockCount;
	private int                  blockSize;
	private boolean              changed;
	private long                 freeSpace;
	private long                 usedSpace;
	
	public BufferedFileSysImpl(PatrFileSystem fs) throws IOException {
		this.fs = fs;
		PatrFolder fsroot = fs.getRoot();
		PatrFileSysElementBuffer buffer = new PatrFileSysElementBuffer(this, fsroot);
		this.root = new BufferedFolderImpl(buffer);
	}
	
	
	public void change() {
		changed = true;
	}
	
	@Override
	public void close() throws IOException {
		fs.close();
	}
	
	@Override
	public PatrFolder getRoot() throws IOException {
		return root;
	}
	
	@Override
	public PatrFileSysElement fromID(Object id) throws IOException, IllegalArgumentException, NullPointerException {
		PatrFileSysElement e = fs.fromID(id);
		PatrFileSysElementBuffer buffer = new PatrFileSysElementBuffer(this, e);
		return new BufferedFileSysElementImpl(buffer);
	}
	
	@Override
	public void format() throws IOException {
		totalSpace = 0L;
		fs.format();
	}
	
	@Override
	public long totalSpace() throws IOException {
		if (totalSpace == 0L) {
			totalSpace = fs.totalSpace();
		}
		return totalSpace;
	}
	
	@Override
	public long freeSpace() throws IOException {
		if (freeSpace == 0L || changed) {
			freeSpace = fs.freeSpace();
		}
		return freeSpace;
	}
	
	@Override
	public long usedSpace() throws IOException {
		if (usedSpace == 0L || changed) {
			usedSpace = fs.usedSpace();
		}
		return usedSpace;
	}
	
	@Override
	public int blockSize() throws IOException {
		if (blockSize == 0) {
			blockSize = fs.blockSize();
		}
		return blockSize;
	}
	
	@Override
	public long blockCount() throws IOException {
		if (blockCount == 0L) {
			blockCount = fs.blockCount();
		}
		return blockCount;
	}
	
}

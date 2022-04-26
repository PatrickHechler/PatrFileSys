package de.hechler.patrick.pfs.objects.bfs;

import java.io.IOException;

import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.interfaces.PatrFolder;

public class BufferedFileSysImpl implements PatrFileSystem {
	
	private final PatrFileSystem fs;
	private final PatrFolder     root;
	private boolean              bigChanged;
	private long                 totalSpace;
	private long                 blockCount;
	private int                  blockSize;
	private boolean              simpleChanged;
	private long                 freeSpace;
	private long                 usedSpace;
	
	public BufferedFileSysImpl(PatrFileSystem fs) throws IOException {
		this.fs = fs;
		PatrFolder fsroot = fs.getRoot();
		PatrFileSysElementBuffer buffer = new PatrFileSysElementBuffer(this, fsroot);
		this.root = new BufferedFolderImpl(buffer);
	}
	
	
	public void changeSize() {
		simpleChanged = true;
	}
	
	public void changeTotalSize() {
		bigChanged = true;
	}
	
	public void changeBlockData() {
		bigChanged = true;
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
		fs.format();
		bigChanged = true;
		simpleChanged = true;
	}
	
	@Override
	public long totalSpace() throws IOException {
		if (totalSpace == 0L || bigChanged) {
			totalSpace = fs.totalSpace();
		}
		return totalSpace;
	}
	
	@Override
	public long freeSpace() throws IOException {
		if (freeSpace == 0L || simpleChanged) {
			freeSpace = fs.freeSpace();
		}
		return freeSpace;
	}
	
	@Override
	public long usedSpace() throws IOException {
		if (usedSpace == 0L || simpleChanged) {
			usedSpace = fs.usedSpace();
		}
		return usedSpace;
	}
	
	@Override
	public int blockSize() throws IOException {
		if (blockSize == 0 || bigChanged) {
			blockSize = fs.blockSize();
		}
		return blockSize;
	}
	
	@Override
	public long blockCount() throws IOException {
		if (blockCount == 0L || bigChanged) {
			blockCount = fs.blockCount();
		}
		return blockCount;
	}
	
}

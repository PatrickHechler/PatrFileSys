// This file is part of the Patr File System Project
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
// Copyright (C) 2023 Patrick Hechler
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
package de.hechler.patrick.zeugs.pfs.impl.java;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import de.hechler.patrick.zeugs.pfs.interfaces.FS;
import de.hechler.patrick.zeugs.pfs.interfaces.Folder;
import de.hechler.patrick.zeugs.pfs.interfaces.Mount;
import de.hechler.patrick.zeugs.pfs.interfaces.Pipe;
import de.hechler.patrick.zeugs.pfs.interfaces.Stream;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

@SuppressWarnings("javadoc")
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
		if (this.closed) { throw new ClosedChannelException(); }
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
	public JavaFSElement element(String path) throws IOException {
		ensureOpen();
		if (path.isEmpty()) { throw new IllegalArgumentException("path is empty"); }
		Path p;
		if (path.charAt(0) == '/') {
			if (path.length() == 1) {
				p = this.root;
			} else {
				p = this.root.resolve('.' + path);
			}
		} else {
			p = this.cwd.f().resolve(path);
		}
		p = p.toRealPath();
		if (!p.startsWith(this.root)) throw new IllegalArgumentException("the root folder has no parent");
		Path rel = this.root.relativize(p);
		if (Files.isDirectory(p)) {
			return new JavaFolder(this, rel);
		}
		return new JavaFile(this, rel);
	}
	
	@Override
	public JavaFolder folder(String path) throws IOException {
		return element(path).getFolder();
	}
	
	@Override
	public JavaFile file(String path) throws IOException {
		return element(path).getFile();
	}
	
	@Override
	public Pipe pipe(@SuppressWarnings("unused") String path) throws IOException {
		throw new UnsupportedOperationException("pipe");
	}
	
	@Override
	public Mount mount(@SuppressWarnings("unused") String path) throws IOException {
		throw new UnsupportedOperationException("pipe");
	}
	
	@Override
	public Stream stream(String path, StreamOpenOptions opts) throws IOException {
		if (opts.createAlso()) {
			FileSystem fs = this.root.getFileSystem();
			Path       p  = fs.getPath(path);
			if (p.isAbsolute()) {
				Path r = fs.getPath(fs.getSeparator());
				p = r.relativize(p);
				p = this.root.resolve(p);
			} else {
				p = this.cwd.f().resolve(p);
			}
			if (!Files.exists(p)) {
				Path pp = p.getParent();
				if (!p.startsWith(pp)) {
					throw new NoSuchFileException(pp.toString(), p.getFileName().toString(), "the element does not start with its parent");
				}
				if (!Files.exists(pp)) {
					throw new NoSuchFileException(pp.toString(), p.getFileName().toString(), "the parent folder does not exist");
				}
				try (JavaFolder parent = folder(this.root.resolve(pp).toString())) {
					try (JavaFile file = parent.createFile(p.getFileName().toString())) {
						return file.open0(opts, false);
					}
				}
			}
		}
		return file(path).open(opts);
	}
	
	private static class JavaMount extends JavaFolder implements Mount {
		
		public JavaMount(JavaFS fs, Path path) {
			super(fs, path);
		}
		
		@Override
		public long blockCount() throws IOException {
			return this.fs.blockCount();
		}
		
		@Override
		public int blockSize() throws IOException {
			return this.fs.blockSize();
		}
		
		@Override
		public UUID uuid() throws IOException {
			return null;
		}
		
		@Override
		public void uuid(@SuppressWarnings("unused") UUID uuid) throws IOException {
			throw new UnsupportedOperationException("UUID");
		}
		
		@Override
		public String fsName() throws IOException {
			return "";
		}
		
		@Override
		public boolean readOnly() throws IOException {
			return false;
		}
		
		@Override
		public MountType mountType() throws IOException {
			return MountType.REL_FS_BACKED_FILE_SYSTEM;
		}
		
	}
	
	@Override
	public Mount root() throws IOException {
		return new JavaMount(this, this.root);
	}
	
	@Override
	public Folder cwd() throws IOException {
		ensureOpen();
		return new JavaFolder(this, this.cwd.p());
	}
	
	@Override
	public void cwd(Folder f) throws IOException {
		ensureOpen();
		if (!(f instanceof JavaFolder jf) || jf.fs != this) {
			throw new IllegalArgumentException("the folder does not belong to this file system (folder: " + f + ")");
		}
		this.cwd = jf;
	}
	
	@Override
	public void close() throws IOException {
		if (this.closed) { return; }
		this.closed = true;
		this.prov.unload(this);
	}

}

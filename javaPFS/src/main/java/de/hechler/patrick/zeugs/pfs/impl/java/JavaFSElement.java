//This file is part of the Patr File System Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.
package de.hechler.patrick.zeugs.pfs.impl.java;

import java.io.IOError;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.TimeUnit;

import de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSElement;
import de.hechler.patrick.zeugs.pfs.interfaces.FS;
import de.hechler.patrick.zeugs.pfs.interfaces.FSElement;
import de.hechler.patrick.zeugs.pfs.interfaces.Folder;
import de.hechler.patrick.zeugs.pfs.interfaces.Pipe;

@SuppressWarnings("javadoc")
public class JavaFSElement implements FSElement {
	
	protected final JavaFS     fs;
	private volatile Path      path;
	private volatile Path      full;
	protected volatile boolean closed;
	
	public JavaFSElement(JavaFS fs, Path path) {
		this.fs   = fs;
		this.path = path;
		this.full = fs.root.resolve(path);
	}
	
	protected void ensureOpen() throws ClosedChannelException {
		if (this.closed) { throw new ClosedChannelException(); }
		this.fs.ensureOpen();
	}
	
	@Override
	public Folder parent() throws IOException {
		ensureOpen();
		Path p = p().getParent();
		if (p == null) { throw new IllegalStateException("the root has no parent"); }
		return new JavaFolder(this.fs, p);
	}
	
	@Override
	public FS fs() throws ClosedChannelException {
		ensureOpen();
		return this.fs;
	}
	
	@Override
	public int flags() throws IOException {
		ensureOpen();
		Path f = f();
		int  flags;
		if (Files.isDirectory(f)) {
			flags = FLAG_FOLDER;
		} else {
			flags = FLAG_FILE;
		}
		if (Files.isHidden(f)) {
			flags |= FLAG_HIDDEN;
		}
		if (Files.isExecutable(f)) {
			flags |= FLAG_EXECUTABLE;
		}
		return flags;
	}
	
	@Override
	public void flag(int add, int rem) throws IOException {
		ensureOpen();
		if (((add | rem) & FLAG_UNMODIFIABLE) != 0) { throw new IllegalArgumentException("I won't modify unmodifiable flags"); }
		if ((add & rem) != 0) { throw new IllegalArgumentException("I won't add and remove the flag"); }
		if (((add | rem) & ~(FLAG_HIDDEN | FLAG_EXECUTABLE)) != 0) { throw new UnsupportedOperationException("modify custom flags"); }
		Path f = f();
		modHiddenFlag(add, rem, f);
		modExecutableFlag(add, rem, f);
	}
	
	private static void modHiddenFlag(int add, int rem, Path f) throws IOException {
		if (Files.isHidden(f)) {
			if ((rem & FLAG_HIDDEN) != 0) { throw new UnsupportedOperationException("modify the hidden flag"); }
		} else if ((add & FLAG_HIDDEN) != 0) { throw new UnsupportedOperationException("modify the hidden flag"); }
	}
	
	private static void modExecutableFlag(int add, int rem, Path f) throws IOException {
		if ((add & FLAG_EXECUTABLE) != 0) {
			if (Files.isDirectory(f)) throw new UnsupportedOperationException("modify the executable flag on folders");
			String old = PosixFilePermissions.toString(Files.getPosixFilePermissions(f));
			char[] arr = old.toCharArray();
			arr[2] = 'x';
			arr[5] = 'x';
			arr[8] = 'x';
			Files.setPosixFilePermissions(f, PosixFilePermissions.fromString(String.valueOf(arr)));
		} else if ((rem & FLAG_EXECUTABLE) != 0) {
			if (Files.isDirectory(f)) throw new UnsupportedOperationException("modify the executable flag on folders");
			String old = PosixFilePermissions.toString(Files.getPosixFilePermissions(f));
			char[] arr = old.toCharArray();
			arr[2] = '-';
			arr[5] = '-';
			arr[8] = '-';
			Files.setPosixFilePermissions(f, PosixFilePermissions.fromString(String.valueOf(arr)));
		}
	}
	
	@Override
	public long lastModTime() throws IOException {
		ensureOpen();
		return Files.readAttributes(f(), BasicFileAttributes.class).lastModifiedTime().to(TimeUnit.SECONDS);
	}
	
	@Override
	public void lastModTime(long time) throws IOException {
		ensureOpen();
		Files.getFileAttributeView(p(), BasicFileAttributeView.class).setTimes(FileTime.from(time, TimeUnit.SECONDS), null, null);
	}
	
	@Override
	public long createTime() throws IOException {
		ensureOpen();
		return Files.readAttributes(f(), BasicFileAttributes.class).creationTime().to(TimeUnit.SECONDS);
	}
	
	@Override
	public void createTime(long time) throws IOException {
		ensureOpen();
		Files.getFileAttributeView(p(), BasicFileAttributeView.class).setTimes(null, null, FileTime.from(time, TimeUnit.SECONDS));
	}
	
	@Override
	public String name() throws IOException {
		ensureOpen();
		return p().getFileName().toString();
	}
	
	@Override
	public void name(String name) throws IOException {
		ensureOpen();
		if (name.indexOf('/') != -1) { throw new IllegalArgumentException("name contains path seperator"); }
		synchronized (this) {
			Path p = this.path.resolveSibling(name);
			Path f = this.fs.root.resolve(p);
			Files.move(this.full, f);
			this.path = p;
			this.full = f;
		}
	}
	
	@Override
	public void parent(Folder parent) throws IOException {
		ensureOpen();
		if (!(parent instanceof JavaFolder jf) || jf.fs != this.fs) {
			throw new IllegalArgumentException("the target folder does not belong to the same file system");
		}
		synchronized (this) {
			Path target  = jf.f();
			Path oldFull = this.full;
			if (target.startsWith(oldFull)) { throw new IllegalArgumentException("I won't move a folder to it's own child"); }
			Path newFull = target.resolve(oldFull.getFileName());
			Files.move(oldFull, newFull);
			this.path = this.fs.root.relativize(newFull);
			this.full = newFull;
		}
	}
	
	@Override
	public void move(Folder parent, String name) throws IOException {
		ensureOpen();
		if (!(parent instanceof JavaFolder jf) || jf.fs != this.fs) {
			throw new IllegalArgumentException("the target folder does not belong to the same file system");
		}
		if (name.indexOf('/') != -1) { throw new IllegalArgumentException("name contains path seperator"); }
		synchronized (this) {
			Path target  = jf.f();
			Path oldFull = this.full;
			if (target.startsWith(oldFull)) { throw new IllegalArgumentException("I won't move a folder to it's own child"); }
			Path newFull = target.resolve(name);
			Files.move(oldFull, newFull);
			this.path = this.fs.root.relativize(newFull);
			this.full = newFull;
		}
	}
	
	@Override
	public void delete() throws IOException {
		ensureOpen();
		Files.delete(f());
	}
	
	@Override
	public JavaFolder getFolder() throws IOException {
		ensureOpen();
		if (Files.isDirectory(f())) {
			if (this instanceof JavaFolder f) {
				return f;
			}
			return new JavaFolder(this.fs, p());
		}
		throw new IllegalStateException("this is no folder");
	}
	
	@Override
	public JavaFile getFile() throws IOException {
		if (!Files.isDirectory(f())) {
			if (this instanceof JavaFile f) {
				return f;
			}
			return new JavaFile(this.fs, p());
		}
		throw new IllegalStateException("this is no folder");
	}
	
	@Override
	public Pipe getPipe() throws IOException {
		throw new UnsupportedOperationException("pipe");
	}
	
	protected synchronized Path p() { return this.path; }
	
	protected synchronized Path f() { return this.full; }
	
	@Override
	public void close() throws IOException {
		this.closed = true;
	}
	
	@Override
	public boolean equals(FSElement e) throws IOException {
		if (e == null) return false;
		if (e == this) return true;
		if (!(e instanceof JavaFSElement je)) {
			return false;
		} else if (je.fs != this.fs) {
			return false;
		} else {
			return je.path.equals(p());
		}
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


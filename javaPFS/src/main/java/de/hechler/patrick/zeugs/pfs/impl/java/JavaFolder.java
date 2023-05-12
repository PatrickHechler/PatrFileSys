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
	
	public JavaFolder(JavaFS fs, Path path) {
		super(fs, path);
		
	}
	
	@Override
	public FolderIter iter(boolean showHidden) throws IOException {
		ensureOpen();
		DirectoryStream<Path> stream;
		if (showHidden) {
			stream = Files.newDirectoryStream(fs.root.resolve(p()));
		} else {
			stream = Files.newDirectoryStream(fs.root.resolve(p()), p -> !Files.isHidden(p));
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
				JavaFSElement e = new JavaFSElement(fs, n.relativize(fs.root));
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
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(fs.root.resolve(p()))) {
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
		Path f = f().resolve(name);
		Path p = checkExists(f);
		return new JavaFSElement(fs, p);
	}
	
	@Override
	public Folder childFolder(String name) throws IOException {
		checkValidName(name);
		Path f = f().resolve(name);
		Path p = checkExists(f);
		if (!Files.isDirectory(f)) { throw new NotDirectoryException(p.toString()); }
		return new JavaFolder(fs, p);
	}
	
	@Override
	public File childFile(String name) throws IOException {
		checkValidName(name);
		Path f = f().resolve(name);
		Path p = checkExists(f);
		if (!Files.isRegularFile(f)) { throw new NotDirectoryException(p.toString()); }
		return new JavaFile(fs, p);
	}
	
	@Override
	public Pipe childPipe(String name) throws IOException {
		checkValidName(name);
		Path p = p().resolve(name);
		throw new NoSuchFileException(p.toString(), p().toString(), "pipes are not supported");
	}
	
	@Override
	public Folder createFolder(String name) throws IOException {
		checkValidName(name);
		Path f = f().resolve(name);
		Files.createDirectory(f);
		return new JavaFolder(fs, fs.root.relativize(f));
	}
	
	@Override
	public File createFile(String name) throws IOException {
		checkValidName(name);
		Path f = f().resolve(name);
		Files.createFile(f);
		return new JavaFile(fs, fs.root.relativize(f));
	}
	
	@Override
	public Pipe createPipe(String name) throws IOException {
		throw new UnsupportedOperationException("pipes are not supported");
	}
	
	
	private Path checkExists(Path f) throws NoSuchFileException {
		Path res = fs.root.relativize(f);
		if (!Files.exists(f)) { throw new NoSuchFileException(res.toString(), p().toString(), "no child element"); }
		return res;
	}
	
	private static void checkValidName(String name) {
		if (name.indexOf('/') != -1) { throw new IllegalArgumentException("name contains path seperator"); }
	}
	
}

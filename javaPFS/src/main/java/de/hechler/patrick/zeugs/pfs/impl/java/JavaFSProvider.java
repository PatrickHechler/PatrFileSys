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
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import de.hechler.patrick.zeugs.pfs.FSProvider;
import de.hechler.patrick.zeugs.pfs.interfaces.FS;
import de.hechler.patrick.zeugs.pfs.interfaces.FSOptions;
import de.hechler.patrick.zeugs.pfs.opts.JavaFSOptions;

/**
 * this class implements the File System interfaces by delegating to an
 * implementation of the nio api of the java library
 * <br>
 * the {@link #name()} of this provider is
 * {@value FSProvider#JAVA_FS_PROVIDER_NAME}
 * 
 * @author pat
 * 
 * @see FSProvider#JAVA_FS_PROVIDER_NAME
 */
public class JavaFSProvider extends FSProvider {
	
	private final BiFunction<Path, JavaFS, JavaFS> notAllowOldMapFunc = (p, fs) -> {
		if (fs == null) {
			throw new FileSystemAlreadyExistsException(p.toString());
		} else {
			return new JavaFS(this, p);
		}
	};
	
	private final Function<Path, JavaFS> allowOldMapFunc = p -> new JavaFS(this, p);
	
	private final Map<Path, JavaFS> loaded = new HashMap<>();
	
	public JavaFSProvider() {
		super(FSProvider.JAVA_FS_PROVIDER_NAME, Integer.MAX_VALUE);
	}
	
	@Override
	public FS loadFS(FSOptions opts) throws IOException {
		return switch (opts) {
		case JavaFSOptions o when o.allowReuseAlreadyLoaded() -> loaded.computeIfAbsent(o.root(), allowOldMapFunc);
		case JavaFSOptions o when !o.allowReuseAlreadyLoaded() -> loaded.compute(o.root(), notAllowOldMapFunc);
		default -> throw new IllegalArgumentException("option type is not supported: " + opts.getClass());
		};
	}
	
	@Override
	public Collection<? extends FS> loadedFS() {
		return this.loaded.values();
	}
	
	void unload(JavaFS fs) {
		JavaFS old = loaded.remove(fs.root);
		if (old != fs) {
			loaded.put(fs.root, old);
			throw new AssertionError();
		}
	}
	
}

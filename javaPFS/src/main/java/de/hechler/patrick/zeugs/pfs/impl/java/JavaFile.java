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
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Stream;
import de.hechler.patrick.zeugs.pfs.misc.ElementType;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;


public class JavaFile extends JavaFSElement implements File {
	
	public JavaFile(JavaFS fs, Path path) {
		super(fs, path);
	}
	
	@Override
	public long length() throws IOException {
		return Files.size(f());
	}
	
	@Override
	public void truncate(long length) throws IOException {
		try (SeekableByteChannel channel = Files.newByteChannel(f(), StandardOpenOption.WRITE)) {
			channel.truncate(length);
		}
	}
	
	@Override
	public Stream open(StreamOpenOptions options) throws IOException {
		return open0(options, true);
	}
	
	@SuppressWarnings("preview")
	public Stream open0(StreamOpenOptions options, boolean addCreate) throws IOException {
		options = options.ensureType(ElementType.FILE);
		List<OpenOption> opts = new ArrayList<>();
		if (options.append()) {
			opts.add(StandardOpenOption.APPEND);
		} else if (options.write()) {
			opts.add(StandardOpenOption.WRITE);
		}
		if (options.read()) {
			opts.add(StandardOpenOption.READ);
		}
		if (addCreate) {
			if (options.createOnly()) {
				opts.add(StandardOpenOption.CREATE_NEW);
			} else if (options.createAlso()) {
				opts.add(StandardOpenOption.CREATE);
			}
		}
		if (options.truncate()) {
			opts.add(StandardOpenOption.TRUNCATE_EXISTING);
		}
		SeekableByteChannel channel = Files.newByteChannel(f(), opts.toArray(new OpenOption[opts.size()]));
		return switch (options) {
		case StreamOpenOptions o when o.write() && o.read() -> new ChannelReadWriteStream(channel, options, f());
		case StreamOpenOptions o when o.write() && !o.read() -> new ChannelWriteStream(channel, options, f());
		case StreamOpenOptions o when !o.write() && o.read() -> new ChannelReadStream(channel, options, f());
		default -> throw new IllegalArgumentException("I won't open a stream without read and without write access!");
		};
	}
	
}

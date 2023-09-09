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
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import de.hechler.patrick.zeugs.pfs.interfaces.ReadStream;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

public record ChannelReadStream(SeekableByteChannel channel, StreamOpenOptions opts, Path path) implements ReadStream {
	
	@Override
	public StreamOpenOptions options() {
		return this.opts;
	}
	
	@Override
	public void seek(long pos) throws IOException {
		this.channel.position(pos);
	}
	
	@Override
	public long seekAdd(long add) throws IOException {
		long oldPos = this.channel.position();
		long newPos = oldPos + add;
		this.channel.position(newPos);
		return newPos;
	}
	
	@Override
	public long seekEOF() throws IOException {
		long newPos = Files.size(this.path);
		this.channel.position(newPos);
		return newPos;
	}
	
	@Override
	public int read(ByteBuffer data) throws IOException {
		int read = this.channel.read(data);
		return read == -1 ? 0 : read;
	}
	
	@Override
	public int read(byte[] data) throws IOException {
		return read(ByteBuffer.wrap(data));
	}
	
	@Override
	public int read(byte[] data, int off, int len) throws IOException {
		return read(ByteBuffer.wrap(data, off, len));
	}
	
	@Override
	public long read(MemorySegment seg) throws IOException {
		if (seg.byteSize() <= Integer.MAX_VALUE - 8) {
			return read(seg.asByteBuffer());
		}
		for (long off = 0L; off < seg.byteSize(); off += Integer.MAX_VALUE - 8) {
			int read = read(seg.asSlice(off, (int) Math.min(Integer.MAX_VALUE - 8, seg.byteSize() - off)).asByteBuffer());
			if (read != Integer.MAX_VALUE - 8) { return off + read; }
		}
		return seg.byteSize();
	}
	
	@Override
	public void close() throws IOException {
		this.channel.close();
	}
	
}

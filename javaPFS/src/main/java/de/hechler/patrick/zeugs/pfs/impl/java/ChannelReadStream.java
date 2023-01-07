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
		return opts;
	}
	
	@Override
	public void seek(long pos) throws IOException {
		channel.position(pos);
	}
	
	@Override
	public long seekAdd(long add) throws IOException {
		long oldPos = channel.position();
		long newPos = oldPos + add;
		channel.position(newPos);
		return newPos;
	}
	
	@Override
	public long seekEOF() throws IOException {
		long newPos = Files.size(path);
		channel.position(newPos);
		return newPos;
	}
	
	@Override
	public int read(ByteBuffer data) throws IOException {
		int read = channel.read(data);
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
		if (seg.byteSize() <= Integer.MAX_VALUE) {
			return read(seg.asByteBuffer());
		} else {
			for (long off = 0L; off < seg.byteSize(); off += Integer.MAX_VALUE) {
				int read = read(seg.asSlice(off, (int) Math.min(Integer.MAX_VALUE, seg.byteSize() - off)).asByteBuffer());
				if (read != Integer.MAX_VALUE) { return off + read; }
			}
			return seg.byteSize();
		}
	}
	
	@Override
	public void close() throws IOException {
		channel.close();
	}
	
}

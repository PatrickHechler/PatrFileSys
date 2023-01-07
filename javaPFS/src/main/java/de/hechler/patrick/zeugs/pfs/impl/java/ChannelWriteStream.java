package de.hechler.patrick.zeugs.pfs.impl.java;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import de.hechler.patrick.zeugs.pfs.interfaces.WriteStream;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

public record ChannelWriteStream(SeekableByteChannel channel, StreamOpenOptions opts, Path path) implements WriteStream {
	
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
	public int write(ByteBuffer data) throws IOException {
		return channel.write(data);
	}
	
	@Override
	public int write(byte[] data) throws IOException {
		return write(ByteBuffer.wrap(data));
	}
	
	@Override
	public int write(byte[] data, int off, int len) throws IOException {
		return write(ByteBuffer.wrap(data, off, len));
	}
	
	@Override
	public long write(MemorySegment seg) throws IOException {
		if (seg.byteSize() <= Integer.MAX_VALUE) {
			return write(seg.asByteBuffer());
		} else {
			for (long off = 0L; off < seg.byteSize(); off += Integer.MAX_VALUE) {
				int wrote = write(seg.asSlice(off, (int) Math.min(Integer.MAX_VALUE, seg.byteSize() - off)).asByteBuffer());
				if (wrote != Integer.MAX_VALUE) { return off + wrote; }
			}
			return seg.byteSize();
		}
	}
	
	@Override
	public void close() throws IOException {
		channel.close();
	}
	
}

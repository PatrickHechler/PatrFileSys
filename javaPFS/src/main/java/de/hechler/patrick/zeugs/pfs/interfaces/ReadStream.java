package de.hechler.patrick.zeugs.pfs.interfaces;

import java.nio.ByteBuffer;

import de.hechler.patrick.zeugs.pfs.records.StreamOptions;
import jdk.incubator.foreign.MemorySegment;

/**
 * a {@link ReadStream} is a stream with the {@link StreamOptions#read()} set to
 * <code>true</code>
 * <p>
 * a {@link ReadStream} provides methods to read data from the {@link Stream}
 * with different types (byte[], {@link ByteBuffer} and {@link MemorySegment})
 * 
 * @author pat
 */
public interface ReadStream extends Stream {

	/**
	 * read the next data.length bytes of the {@link Stream} and copy them to the
	 * array
	 * 
	 * @param data the buffer which should be filled with the Stream data
	 * @return the number of bytes actually read
	 */
	default int read(byte[] data) {
		// if read(byte[], int, int) is overwritten
		return read(data, 0, data.length);
	}

	/**
	 * read the next len bytes of the {@link Stream} and copy them to the array with
	 * the offset off
	 * 
	 * @param data the buffer which should be filled with the Stream data
	 * @param off  the offset of the data inside the array
	 * @param len  the number of bytes which should be read
	 * @return the number of bytes actually read
	 */
	default int read(byte[] data, int off, int len) {
		MemorySegment seg = MemorySegment.ofArray(data);
		return (int) read(seg.asSlice((long) off, (long) len));
	}

	/**
	 * read the next {@link ByteBuffer#remaining()} bytes to the {@link ByteBuffer}
	 * data
	 * 
	 * @param data the {@link ByteBuffer} which should be filled with the read data
	 * @return the number of bytes which has been read
	 */
	default int read(ByteBuffer data) {
		MemorySegment seg = MemorySegment.ofByteBuffer(data);
		return (int) read(seg);
	}

	/**
	 * read the next {@link MemorySegment#byteSize()} bytes to the
	 * {@link MemorySegment} {@code seg}
	 * 
	 * @param seg the {@link MemorySegment} which should be filled with the read
	 *            data
	 * @return the number of bytes which has been read
	 */
	long read(MemorySegment seg);

}

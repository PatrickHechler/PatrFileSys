package de.hechler.patrick.zeugs.pfs.interfaces;

import java.nio.ByteBuffer;

import de.hechler.patrick.zeugs.pfs.records.StreamOptions;
import jdk.incubator.foreign.MemorySegment;

/**
 * a {@link WriteStream} is a stream with the {@link StreamOptions#write()} set
 * to <code>true</code>
 * <p>
 * a {@link WriteStream} provides methods to write data to the {@link Stream}
 * with different types (byte[], {@link ByteBuffer} and {@link MemorySegment})
 * 
 * @author pat
 */
public interface WriteStream extends Stream {

	/**
	 * write the next data.length bytes from the array to the {@link Stream}
	 * 
	 * @param data the buffer which holds the data for the {@link Stream}
	 */
	default void write(byte[] data) {
		// if write(byte[], int, int) is overwritten
		write(data, 0, data.length);
	}

	/**
	 * write the next len bytes of the {@link Stream} and copy them to the array
	 * with the offset off
	 * 
	 * @param data the buffer which holds the data for the {@link Stream}
	 * @param off  the offset of the data inside the array
	 * @param len  the number of bytes to write
	 */
	default void write(byte[] data, int off, int len) {
		MemorySegment seg = MemorySegment.ofArray(data);
		write(seg.asSlice((long) off, (long) len));
	}

	/**
	 * write the next {@link ByteBuffer#remaining()} bytes from the
	 * {@link ByteBuffer} data to the {@link Stream}
	 * 
	 * @param data the {@link ByteBuffer} which holds the data for the
	 *             {@link Stream}
	 */
	default void write(ByteBuffer data) {
		MemorySegment seg = MemorySegment.ofByteBuffer(data);
		write(seg);
	}

	/**
	 * write the next {@link MemorySegment#byteSize()} bytes from the
	 * {@link MemorySegment} {@code seg} to the {@link Stream}
	 * 
	 * @param seg the {@link MemorySegment} which holds the data for the
	 *            {@link Stream}
	 */
	void write(MemorySegment seg);

}

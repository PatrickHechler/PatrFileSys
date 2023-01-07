package de.hechler.patrick.zeugs.pfs.interfaces;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;

import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

/**
 * a {@link WriteStream} is a stream with the {@link StreamOpenOptions#write()}
 * set to <code>true</code>
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
	 * 
	 * @return the number of bytes actually written, may be less than the array
	 *         length, because of an error
	 * 		
	 * @throws IOException if an IO error occurs
	 */
	default int write(byte[] data) throws IOException {
		// if write(byte[], int, int) is overwritten
		return write(data, 0, data.length);
	}
	
	/**
	 * write the next len bytes of the {@link Stream} and copy them to the array
	 * with the offset off
	 * 
	 * @param data the buffer which holds the data for the {@link Stream}
	 * @param off  the offset of the data inside the array
	 * @param len  the number of bytes to write
	 * 
	 * @return the number of bytes actually written, may be less than the given len,
	 *         because of an error
	 * 		
	 * @throws IOException if an IO error occurs
	 */
	default int write(byte[] data, int off, int len) throws IOException {
		try (MemorySession mem = MemorySession.openConfined()) {
			MemorySegment seg = mem.allocate(len);
			MemorySegment.copy(data, off, seg, ValueLayout.JAVA_BYTE, 0, len);
			return (int) write(seg);
		}
	}
	
	/**
	 * write the next {@link ByteBuffer#remaining()} bytes from the
	 * {@link ByteBuffer} data to the {@link Stream}
	 * 
	 * @param data the {@link ByteBuffer} which holds the data for the
	 *             {@link Stream}
	 * 
	 * @return the number of bytes actually written, may be less than the buffers
	 *         size, because of an error
	 * 		
	 * @throws IOException if an IO error occurs
	 */
	default int write(ByteBuffer data) throws IOException {
		MemorySegment seg = MemorySegment.ofBuffer(data);
		return (int) write(seg);
	}
	
	/**
	 * write the next {@link MemorySegment#byteSize()} bytes from the
	 * {@link MemorySegment} {@code seg} to the {@link Stream}
	 * 
	 * @param seg the {@link MemorySegment} which holds the data for the
	 *            {@link Stream}
	 * 
	 * @return the number of bytes actually written, may be less than the segments
	 *         size, because of an error
	 * 		
	 * @throws IOException if an IO error occurs
	 */
	long write(MemorySegment seg) throws IOException;
	
}

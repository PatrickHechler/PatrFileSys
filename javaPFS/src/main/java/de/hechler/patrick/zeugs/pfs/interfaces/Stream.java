package de.hechler.patrick.zeugs.pfs.interfaces;

import java.io.Closeable;
import java.io.IOException;

import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

/**
 * {@link Stream} can be used to transfer data
 * 
 * @author pat
 */
public interface Stream extends Closeable {

	/**
	 * returns the {@link StreamOpenOptions} of this {@link Stream}
	 * 
	 * @return the {@link StreamOpenOptions} of this {@link Stream}
	 */
	StreamOpenOptions options();

	/**
	 * changes the position of the stream to {@code pos}<br>
	 * if the stream is not {@link StreamOpenOptions#seekable()} this operation will
	 * fail
	 * <p>
	 * {@code pos} does not need to be inside of the file
	 * 
	 * @param pos the new position of the stream
	 * @throws IOException
	 */
	void seek(long pos) throws IOException;

	/**
	 * changes the position of the stream to the sum of the current position and
	 * {@code pos}<br>
	 * if the stream is not {@link StreamOpenOptions#seekable()} this operation will
	 * fail
	 * <p>
	 * the new position does not need to be inside of the file
	 * 
	 * @param pos the new position of the stream
	 * @throws IOException
	 */
	long seekAdd(long add) throws IOException;

	/**
	 * returns the current position of the stream <br>
	 * if the stream is not {@link StreamOpenOptions#seekable()} this operation will
	 * fail
	 * 
	 * @return the current position of the stream
	 * @throws IOException
	 */
	default long position() throws IOException {
		return seekAdd(0L);
	}

	/**
	 * changes the current position of the stream to the end of the file <br>
	 * if the stream is not {@link StreamOpenOptions#seekable()} this operation will
	 * fail
	 * 
	 * @return the new position of the stream
	 * @throws IOException
	 */
	long seekEOF() throws IOException;

}

package de.hechler.patrick.zeugs.pfs.interfaces;

import java.io.IOException;

import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

/**
 * the {@link Pipe} interface provides a bunch of methods:
 * <ul>
 * <li>to get ({@link #length()} the pipes length</li>
 * <li>the content of the pipe can be read with {@link #openRead()}</li>
 * <li>to write to the pipe use {@link #openWrite()}</li>
 * <li>to read and write to the pipe use {@link #openReadWrite()}</li>
 * <li>also {@link #open(StreamOpenOptions)} can be used to open a stream with the
 * given options</li>
 * </ul>
 * 
 * @author pat
 */
public interface Pipe extends FSElement {

	/**
	 * returns the current length of the pipe
	 * 
	 * @return the current length of the pipe
	 * @throws IOException if an IO error occurs
	 */
	long length() throws IOException;

	/**
	 * opens a {@link ReadStream} for this pipe
	 * <p>
	 * this call is like {@link #open(StreamOpenOptions)} with a {@link StreamOpenOptions}
	 * set to {@link StreamOpenOptions#read()}, but not {@link StreamOpenOptions#write()}
	 * <p>
	 * the stream will not be {@link StreamOpenOptions#seekable()}
	 * 
	 * @return the opened {@link ReadStream}
	 * @throws IOException if an IO error occurs
	 */
	default ReadStream openRead() throws IOException {
		return (ReadStream) open(new StreamOpenOptions(true, false));
	}

	/**
	 * opens a {@link WriteStream} for this pipe
	 * <p>
	 * this call is like {@link #open(StreamOpenOptions)} with a {@link StreamOpenOptions}
	 * set to {@link StreamOpenOptions#write()}, but not {@link StreamOpenOptions#read()}
	 * <p>
	 * the stream will not be {@link StreamOpenOptions#seekable()}
	 * 
	 * @return the opened {@link WriteStream}
	 * @throws IOException if an IO error occurs
	 */
	default WriteStream openWrite() throws IOException {
		return (WriteStream) open(new StreamOpenOptions(false, true));
	}

	/**
	 * opens a {@link Stream} for this pipe
	 * <p>
	 * this call is like {@link #open(StreamOpenOptions)} with a {@link StreamOpenOptions}
	 * set to {@link StreamOpenOptions#read()} and {@link StreamOpenOptions#write()}
	 * <p>
	 * the stream will not be {@link StreamOpenOptions#seekable()}
	 * 
	 * @return the opened {@link Stream}
	 * @throws IOException if an IO error occurs
	 */
	default ReadWriteStream openReadWrite() throws IOException {
		return (ReadWriteStream) open(new StreamOpenOptions(true, true));
	}

	/**
	 * opens a new {@link Stream} for this pipe
	 * <p>
	 * the {@link Stream} will never be {@link StreamOpenOptions#seekable()} (even if
	 * the option is set).<br>
	 * when the {@link StreamOpenOptions#write()} option is set the
	 * {@link StreamOpenOptions#append()} will also be set in the {@link Stream}
	 * 
	 * @param options the options for the stream
	 * @return the opened {@link Stream}
	 * @throws IOException if an IO error occurs
	 */
	Stream open(StreamOpenOptions options) throws IOException;

}

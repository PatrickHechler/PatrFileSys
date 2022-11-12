package de.hechler.patrick.zeugs.pfs.interfaces;

import java.io.IOException;

import de.hechler.patrick.zeugs.pfs.records.StreamOptions;

/**
 * the {@link Pipe} interface provides a bunch of methods:
 * <ul>
 * <li>to get ({@link #length()} the pipes length</li>
 * <li>to set ({@link #truncate()} the pipes length</li>
 * <li>the content of the pipe can be read with {@link #openRead()}</li>
 * <li>to write to the pipe use {@link #openWrite()} and
 * {@link #openAppend()}</li>
 * <li>to read and write to the pipe use {@link #openReadWrite()}</li>
 * <li>also {@link #open(StreamOptions)} can be used to open a stream with the
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
	 * @throws IOException
	 */
	long length() throws IOException;

	/**
	 * opens a {@link ReadStream} for this pipe
	 * <p>
	 * this call is like {@link #open(StreamOptions)} with a {@link StreamOptions}
	 * set to {@link StreamOptions#read()}, but not {@link StreamOptions#write()}
	 * <p>
	 * the stream will not be {@link StreamOptions#seekable()}
	 * 
	 * @return the opened {@link ReadStream}
	 * @throws IOException
	 */
	default ReadStream openRead() throws IOException {
		return (ReadStream) open(new StreamOptions(true, false));
	}

	/**
	 * opens a {@link WriteStream} for this pipe
	 * <p>
	 * this call is like {@link #open(StreamOptions)} with a {@link StreamOptions}
	 * set to {@link StreamOptions#write()}, but not {@link StreamOptions#read()}
	 * <p>
	 * the stream will not be {@link StreamOptions#seekable()}
	 * 
	 * @return the opened {@link WriteStream}
	 * @throws IOException
	 */
	default WriteStream openWrite() throws IOException {
		return (WriteStream) open(new StreamOptions(false, true));
	}

	/**
	 * opens a {@link Stream} for this pipe
	 * <p>
	 * this call is like {@link #open(StreamOptions)} with a {@link StreamOptions}
	 * set to {@link StreamOptions#read()} and {@link StreamOptions#write()}
	 * <p>
	 * the stream will not be {@link StreamOptions#seekable()}
	 * 
	 * @return the opened {@link Stream}
	 * @throws IOException
	 */
	default Stream openReadWrite() throws IOException {
		return open(new StreamOptions(true, true));
	}

	/**
	 * opens a new {@link Stream} for this pipe
	 * <p>
	 * the {@link Stream} will never be {@link StreamOptions#seekable()} (even if
	 * the option is set).<br>
	 * when the {@link StreamOptions#write()} option is set the
	 * {@link StreamOptions#append()} will also be set in the {@link Stream}
	 * 
	 * @param options the options for the stream
	 * @return the opened {@link Stream}
	 * @throws IOException
	 */
	Stream open(StreamOptions options) throws IOException;

}

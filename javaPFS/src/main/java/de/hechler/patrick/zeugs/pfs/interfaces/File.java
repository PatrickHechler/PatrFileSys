package de.hechler.patrick.zeugs.pfs.interfaces;

import java.io.IOException;

import de.hechler.patrick.zeugs.pfs.records.StreamOptions;

/**
 * the {@link File} interface provides a bunch of methods:
 * <ul>
 * <li>to get ({@link #length()} the files length</li>
 * <li>to set ({@link #truncate()} the files length</li>
 * <li>the content of the file can be read with {@link #openRead()}</li>
 * <li>to write to the file use {@link #openWrite()} and
 * {@link #openAppend()}</li>
 * <li>to read and write to the file use {@link #openReadWrite()}</li>
 * <li>also {@link #open(StreamOptions)} can be used to open a stream with the
 * given options</li>
 * </ul>
 * 
 * @author pat
 */
public interface File extends FSElement {

	/**
	 * returns the current length of the file
	 * 
	 * @return the current length of the file
	 * @throws IOException
	 */
	long length() throws IOException;

	/**
	 * changes the length of the file. <br>
	 * all content after length will be removed from the file. <br>
	 * there will be zeros added to the file until the files length is
	 * {@code length}
	 * 
	 * @param length the new length of the file
	 * @throws IOException
	 */
	void truncate(long length) throws IOException;

	/**
	 * opens a {@link ReadStream} for this file
	 * <p>
	 * this call is like {@link #open(StreamOptions)} with a {@link StreamOptions}
	 * set to {@link StreamOptions#read()}, but not {@link StreamOptions#write()}
	 * <p>
	 * the stream will be {@link StreamOptions#seekable()}
	 * 
	 * @return the opened {@link ReadStream}
	 * @throws IOException
	 */
	default ReadStream openRead() throws IOException {
		return (ReadStream) open(new StreamOptions(true, false));
	}

	/**
	 * opens a {@link WriteStream} for this file
	 * <p>
	 * this call is like {@link #open(StreamOptions)} with a {@link StreamOptions}
	 * set to {@link StreamOptions#write()}, but not {@link StreamOptions#read()}
	 * <p>
	 * the stream will be {@link StreamOptions#seekable()}
	 * 
	 * @return the opened {@link WriteStream}
	 * @throws IOException
	 */
	default WriteStream openWrite() throws IOException {
		return (WriteStream) open(new StreamOptions(false, true));
	}

	/**
	 * opens a {@link ReadStream} for this file
	 * <p>
	 * this call is like {@link #open(StreamOptions)} with a {@link StreamOptions}
	 * set to {@link StreamOptions#write()}, but not {@link StreamOptions#read()}
	 * <p>
	 * the stream will not be {@link StreamOptions#seekable()}
	 * 
	 * @return the opened {@link WriteStream}
	 * @throws IOException
	 */
	default WriteStream openAppend() throws IOException {
		return (WriteStream) open(new StreamOptions(false, true, true));
	}

	/**
	 * opens a {@link Stream} for this file
	 * <p>
	 * this call is like {@link #open(StreamOptions)} with a {@link StreamOptions}
	 * set to {@link StreamOptions#read()} and {@link StreamOptions#write()}
	 * <p>
	 * the stream will be {@link StreamOptions#seekable()}
	 * 
	 * @return the opened {@link Stream}
	 * @throws IOException
	 */
	default Stream openReadWrite() throws IOException {
		return open(new StreamOptions(true, true));
	}

	/**
	 * opens a new {@link Stream} for this file
	 * <p>
	 * the {@link StreamOptions#seekable()} option will be ignored.
	 * 
	 * @param options the options for the stream
	 * @return the opened {@link Stream}
	 * @throws IOException
	 */
	Stream open(StreamOptions options) throws IOException;

}

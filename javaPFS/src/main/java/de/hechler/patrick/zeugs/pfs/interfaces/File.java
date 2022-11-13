package de.hechler.patrick.zeugs.pfs.interfaces;

import java.io.IOException;

import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

/**
 * the {@link File} interface provides a bunch of methods:
 * <ul>
 * <li>to get ({@link #length()} the files length</li>
 * <li>to set ({@link #truncate()} the files length</li>
 * <li>the content of the file can be read with {@link #openRead()}</li>
 * <li>to write to the file use {@link #openWrite()} and
 * {@link #openAppend()}</li>
 * <li>to read and write to the file use {@link #openReadWrite()}</li>
 * <li>also {@link #open(StreamOpenOptions)} can be used to open a stream with the
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
	 * this call is like {@link #open(StreamOpenOptions)} with a {@link StreamOpenOptions}
	 * set to {@link StreamOpenOptions#read()}, but not {@link StreamOpenOptions#write()}
	 * <p>
	 * the stream will be {@link StreamOpenOptions#seekable()}
	 * 
	 * @return the opened {@link ReadStream}
	 * @throws IOException
	 */
	default ReadStream openRead() throws IOException {
		return (ReadStream) open(new StreamOpenOptions(true, false));
	}

	/**
	 * opens a {@link WriteStream} for this file
	 * <p>
	 * this call is like {@link #open(StreamOpenOptions)} with a {@link StreamOpenOptions}
	 * set to {@link StreamOpenOptions#write()}, but not {@link StreamOpenOptions#read()}
	 * <p>
	 * the stream will be {@link StreamOpenOptions#seekable()}
	 * 
	 * @return the opened {@link WriteStream}
	 * @throws IOException
	 */
	default WriteStream openWrite() throws IOException {
		return (WriteStream) open(new StreamOpenOptions(false, true));
	}

	/**
	 * opens a {@link ReadStream} for this file
	 * <p>
	 * this call is like {@link #open(StreamOpenOptions)} with a {@link StreamOpenOptions}
	 * set to {@link StreamOpenOptions#write()}, but not {@link StreamOpenOptions#read()}
	 * <p>
	 * the stream will not be {@link StreamOpenOptions#seekable()}
	 * 
	 * @return the opened {@link WriteStream}
	 * @throws IOException
	 */
	default WriteStream openAppend() throws IOException {
		return (WriteStream) open(new StreamOpenOptions(false, true, true));
	}

	/**
	 * opens a {@link Stream} for this file
	 * <p>
	 * this call is like {@link #open(StreamOpenOptions)} with a {@link StreamOpenOptions}
	 * set to {@link StreamOpenOptions#read()} and {@link StreamOpenOptions#write()}
	 * <p>
	 * the stream will be {@link StreamOpenOptions#seekable()}
	 * 
	 * @return the opened {@link Stream}
	 * @throws IOException
	 */
	default Stream openReadWrite() throws IOException {
		return open(new StreamOpenOptions(true, true));
	}

	/**
	 * opens a new {@link Stream} for this file
	 * <p>
	 * the {@link StreamOpenOptions#seekable()} option will be ignored.
	 * 
	 * @param options the options for the stream
	 * @return the opened {@link Stream}
	 * @throws IOException
	 */
	Stream open(StreamOpenOptions options) throws IOException;

}

package de.hechler.patrick.zeugs.pfs.impl;

import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.INT;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.LINKER;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.LONG;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.SO_APPEND;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.SO_READ;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFS.SO_WRITE;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFSProvider.loaded;
import static de.hechler.patrick.zeugs.pfs.impl.PatrFSProvider.thrw;

import java.io.IOException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;

import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Stream;
import de.hechler.patrick.zeugs.pfs.misc.ElementType;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

public class PatrFile extends PatrFSElement implements File {

	private final MethodHandle pfs_pipe_length;
	private final MethodHandle pfs_open_stream;
	private final MethodHandle pfs_file_truncate;

	public PatrFile(int handle) {
		super(handle);
		this.pfs_pipe_length = LINKER
				.downcallHandle(loaded.lockup.lookup("pfs_pipe_length").orElseThrow(), FunctionDescriptor.of(LONG, INT))
				.bindTo(handle);
		this.pfs_file_truncate = LINKER.downcallHandle(loaded.lockup.lookup("pfs_file_truncate").orElseThrow(),
				FunctionDescriptor.of(INT, INT, LONG)).bindTo(handle);
		this.pfs_open_stream = LINKER.downcallHandle(loaded.lockup.lookup("pfs_open_stream").orElseThrow(),
				FunctionDescriptor.of(INT, INT, INT)).bindTo(handle);
	}

	@Override
	public long length() throws IOException {
		try {
			long res = (long) pfs_pipe_length.invoke();
			if (res == -1) {
				throw thrw(loaded.lockup, "get file length");
			}
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public void truncate(long length) throws IOException {
		try {
			if (0 == (int) pfs_file_truncate.invoke(length)) {
				throw thrw(loaded.lockup, "get file length");
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

	@Override
	public Stream open(StreamOpenOptions options) throws IOException {
		int o = 0;
		if (options.createOnly()) {
			throw new IllegalStateException("create only is not possible when using open for an existing file!");
		}
		if (options.read()) {
			o |= SO_READ;
		}
		if (options.append()) {
			o |= SO_APPEND;
		} else if (options.write()) {
			o |= SO_WRITE;
		}
		switch (options.type()) {
		case null:
			options = options.setType(ElementType.file);
		case file:
			break;
		default:
			throw new IllegalArgumentException("this is a file, but type is set to " + options.type());
		}
		try {
			int res = (int) pfs_open_stream.invoke(o);
			if (res == -1) {
				throw thrw(loaded.lockup, "open stream");
			}
			if (options.read()) {
				if (options.write()) {
					return new PatrReadWriteStream(res, options);
				} else {
					return new PatrReadStream(res, options);
				}
			} else {
				return new PatrWriteStream(res, options);
			}
		} catch (Throwable e) {
			throw thrw(e);
		}
	}

}

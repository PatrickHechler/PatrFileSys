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
	
	private static final MethodHandle PFS_PIPE_LENGTH;
	private static final MethodHandle PFS_OPEN_STREAM;
	private static final MethodHandle PFS_FILE_TRUNCATE;
	
	static {
		PFS_PIPE_LENGTH   = LINKER.downcallHandle(loaded.lockup.lookup("pfs_pipe_length").orElseThrow(), FunctionDescriptor.of(LONG, INT));
		PFS_FILE_TRUNCATE = LINKER.downcallHandle(loaded.lockup.lookup("pfs_file_truncate").orElseThrow(), FunctionDescriptor.of(INT, INT, LONG));
		PFS_OPEN_STREAM   = LINKER.downcallHandle(loaded.lockup.lookup("pfs_open_stream").orElseThrow(), FunctionDescriptor.of(INT, INT, INT));
	}
	
	public PatrFile(int handle) {
		super(handle);
	}
	
	@Override
	public long length() throws IOException {
		try {
			long res = (long) PFS_PIPE_LENGTH.invoke(this.handle);
			if (res == -1) { throw thrw(loaded.lockup, "get file length"); }
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void truncate(long length) throws IOException {
		try {
			if (0 == (int) PFS_FILE_TRUNCATE.invoke(this.handle, length)) { throw thrw(loaded.lockup, "get file length"); }
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Stream open(StreamOpenOptions options) throws IOException {
		int o = 0;
		if (options.createOnly()) { throw new IllegalStateException("create only is not possible when using open for an existing file!"); }
		if (options.read()) {
			o |= SO_READ;
		}
		if (options.append()) {
			o |= SO_APPEND;
		} else if (options.write()) {
			o |= SO_WRITE;
		}
		options = options.ensureType(ElementType.file);
		try {
			int res = (int) PFS_OPEN_STREAM.invoke(this.handle, o);
			if (res == -1) { throw thrw(loaded.lockup, "open stream"); }
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

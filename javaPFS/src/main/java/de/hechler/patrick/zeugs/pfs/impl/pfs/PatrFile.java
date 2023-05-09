package de.hechler.patrick.zeugs.pfs.impl.pfs;

import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.INT;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.LINKER;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.LOCKUP;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.LONG;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.SO_APPEND;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.SO_READ;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFS.SO_WRITE;
import static de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider.thrw;

import java.io.IOException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;

import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Stream;
import de.hechler.patrick.zeugs.pfs.misc.ElementType;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

public class PatrFile extends PatrFSElement implements File {
	
	private static final MethodHandle PFS_FILE_LENGTH;
	private static final MethodHandle PFS_OPEN_STREAM;
	private static final MethodHandle PFS_FILE_TRUNCATE;
	
	static {
		PFS_FILE_LENGTH   = LINKER.downcallHandle(LOCKUP.find("pfs_file_length").orElseThrow(), FunctionDescriptor.of(LONG, INT));
		PFS_FILE_TRUNCATE = LINKER.downcallHandle(LOCKUP.find("pfs_file_truncate").orElseThrow(), FunctionDescriptor.of(INT, INT, LONG));
		PFS_OPEN_STREAM   = LINKER.downcallHandle(LOCKUP.find("pfs_open_stream").orElseThrow(), FunctionDescriptor.of(INT, INT, INT));
	}
	
	public PatrFile(int handle) {
		super(handle);
	}
	
	@Override
	public long length() throws IOException {
		ensureOpen();
		try {
			long res = (long) PFS_FILE_LENGTH.invoke(this.handle);
			if (res == -1) { throw thrw(PFSErrorCause.GET_FILE_LEN, null); }
			return res;
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public void truncate(long length) throws IOException {
		ensureOpen();
		try {
			if (0 == (int) PFS_FILE_TRUNCATE.invoke(this.handle, length)) { throw thrw(PFSErrorCause.GET_FILE_LEN, null); }
		} catch (Throwable e) {
			throw thrw(e);
		}
	}
	
	@Override
	public Stream open(StreamOpenOptions options) throws IOException {
		ensureOpen();
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
		options = options.ensureType(ElementType.FILE);
		try {
			int res = (int) PFS_OPEN_STREAM.invoke(this.handle, o);
			if (res == -1) { throw thrw(PFSErrorCause.OPEN_STREAM, null); }
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

package de.hechler.patrick.zeugs.pfs.impl;

import de.hechler.patrick.zeugs.pfs.interfaces.ReadStream;
import de.hechler.patrick.zeugs.pfs.interfaces.Stream;
import de.hechler.patrick.zeugs.pfs.interfaces.WriteStream;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

public class PatrReadWriteStream extends PatrStream implements Stream, ReadStream, WriteStream {

	public PatrReadWriteStream(int handle, StreamOpenOptions opts) {
		super(handle, opts);
	}

}
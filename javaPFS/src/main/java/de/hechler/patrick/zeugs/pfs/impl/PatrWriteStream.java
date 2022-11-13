package de.hechler.patrick.zeugs.pfs.impl;

import de.hechler.patrick.zeugs.pfs.interfaces.Stream;
import de.hechler.patrick.zeugs.pfs.interfaces.WriteStream;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

public class PatrWriteStream extends PatrStream implements Stream, WriteStream {

	public PatrWriteStream(int handle, StreamOpenOptions opts) {
		super(handle, opts);
	}

}

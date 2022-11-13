package de.hechler.patrick.zeugs.pfs.impl;

import de.hechler.patrick.zeugs.pfs.interfaces.ReadStream;
import de.hechler.patrick.zeugs.pfs.interfaces.Stream;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

public class PatrReadStream extends PatrStream implements Stream, ReadStream {

	public PatrReadStream(int handle, StreamOpenOptions opts) {
		super(handle, opts);
	}

}

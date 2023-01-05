package de.hechler.patrick.zeugs.pfs.impl;

import de.hechler.patrick.zeugs.pfs.interfaces.ReadStream;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

public final class PatrReadStream extends PatrStream implements ReadStream {

	public PatrReadStream(int handle, StreamOpenOptions opts) {
		super(handle, opts);
	}

}

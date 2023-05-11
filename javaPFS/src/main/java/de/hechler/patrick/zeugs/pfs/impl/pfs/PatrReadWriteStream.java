package de.hechler.patrick.zeugs.pfs.impl.pfs;

import de.hechler.patrick.zeugs.pfs.interfaces.ReadWriteStream;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

public final class PatrReadWriteStream extends PatrStream implements ReadWriteStream {

	public PatrReadWriteStream(int handle, StreamOpenOptions opts) {
		super(handle, opts);
	}

}

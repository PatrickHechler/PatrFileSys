package de.hechler.patrick.zeugs.pfs.impl.pfs;

import de.hechler.patrick.zeugs.pfs.interfaces.WriteStream;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

public final class PatrWriteStream extends PatrStream implements WriteStream {

	public PatrWriteStream(int handle, StreamOpenOptions opts) {
		super(handle, opts);
	}

}

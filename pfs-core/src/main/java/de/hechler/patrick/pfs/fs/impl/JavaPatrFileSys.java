package de.hechler.patrick.pfs.fs.impl;

import static de.hechler.patrick.pfs.other.PatrFileSysConstants.B0.*;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.EMPTY_SIZE;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry;

import java.nio.ByteBuffer;

import de.hechler.patrick.pfs.bm.BlockManager;
import de.hechler.patrick.pfs.exceptions.PFSErr;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.folder.impl.JavaPatrFileSysFolder;
import de.hechler.patrick.pfs.fs.PFS;
import de.hechler.patrick.pfs.other.Place;

public class JavaPatrFileSys implements PFS {
	
	public final BlockManager          bm;
	public final JavaPatrFileSysFolder root;
	
	private JavaPatrFileSys(BlockManager bm) {
		this.bm = bm;
		this.root = new JavaPatrFileSysFolder(this, null, new Place( -1L, -1), null, null);
	}
	
	@Override
	public void format(long blockCount) throws PatrFileSysException {
		if (bm.blockSize() < (SIZE + EMPTY_SIZE + (Entry.SIZE * 2) + 30)) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ILLEGAL_ARG, "block size is too small");
		}
		
	}
	
	@Override
	public PFSFolder root() throws PatrFileSysException {
		ByteBuffer b0 = bm.get(0L);
		try {
			root.element.block = b0.getLong(OFF_ROOT_BLOCK);
			root.element.pos = b0.getInt(OFF_ROOT_POS);
			return root;
		} finally {
			bm.unget(0L);
		}
	}
	
	@Override
	public long blockCount() throws PatrFileSysException {
		ByteBuffer b0 = bm.get(0L);
		try {
			return b0.getLong(OFF_BLOCK_COUNT);
		} finally {
			bm.unget(0L);
		}
	}
	
	@Override
	public int blockSize() throws PatrFileSysException {
		ByteBuffer b0 = bm.get(0L);
		try {
			return b0.getInt(OFF_BLOCK_SIZE);
		} finally {
			bm.unget(0L);
		}
	}
	
	@Override
	public void close() throws PatrFileSysException {
		bm.close();
	}
	
}

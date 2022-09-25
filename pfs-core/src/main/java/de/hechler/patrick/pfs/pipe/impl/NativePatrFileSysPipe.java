package de.hechler.patrick.pfs.pipe.impl;

import java.lang.ref.Reference;

import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.folder.impl.NativePatrFileSysFolder;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSys;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSysElement;
import de.hechler.patrick.pfs.pipe.PFSPipe;
import jdk.incubator.foreign.MemorySegment;


public class NativePatrFileSysPipe extends NativePatrFileSysElement implements PFSPipe {

	public NativePatrFileSysPipe(NativePatrFileSys pfs, MemorySegment eh, Reference <NativePatrFileSysFolder> parent) {
		super(pfs, eh, parent);
	}

	@Override
	public void read(byte[] data, int length) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void append(byte[] data, int length) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long length() throws PatrFileSysException {
		// TODO Auto-generated method stub
		return 0;
	}
}

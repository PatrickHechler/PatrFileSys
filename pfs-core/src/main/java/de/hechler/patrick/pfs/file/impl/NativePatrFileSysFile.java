package de.hechler.patrick.pfs.file.impl;

import java.lang.ref.Reference;

import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.file.PFSFile;
import de.hechler.patrick.pfs.folder.impl.NativePatrFileSysFolder;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSys;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSysElement;
import jdk.incubator.foreign.MemorySegment;


public class NativePatrFileSysFile extends NativePatrFileSysElement implements PFSFile {

	public NativePatrFileSysFile(NativePatrFileSys pfs, MemorySegment eh, Reference <NativePatrFileSysFolder> parent) {
		super(pfs, eh, parent);
	}

	@Override
	public void read(long position, byte[] data, int length) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void overwrite(long position, byte[] data, int length) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void append(byte[] data, int length) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void truncate(long newLength) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long length() throws PatrFileSysException {
		// TODO Auto-generated method stub
		return 0;
	}

}

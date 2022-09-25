package de.hechler.patrick.pfs.folder.impl;

import java.lang.ref.Reference;

import de.hechler.patrick.pfs.element.PFSElement;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.file.PFSFile;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSys;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.ThrowableIter;
import de.hechler.patrick.pfs.pipe.PFSPipe;
import jdk.incubator.foreign.MemorySegment;


public class NativePatrFileSysFolder extends NativePatrFileSysElement implements PFSFolder {
	
//	private static final long PFS_FI_SIZE = 40L;
	
	public NativePatrFileSysFolder(NativePatrFileSys pfs, MemorySegment root, Reference <NativePatrFileSysFolder> parent) {
		super(pfs, root, parent);
	}

	@Override
	public ThrowableIter <PatrFileSysException, PFSElement> iterator(boolean showHidden) throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long childCount() throws PatrFileSysException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public PFSElement element(String childName) throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PFSFolder folder(String childName) throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PFSFile file(String childName) throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PFSPipe pipe(String childName) throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PFSFolder addFolder(String newChildName) throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PFSFile addFile(String newChildName) throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PFSPipe addPipe(String newChildName) throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}
	
}

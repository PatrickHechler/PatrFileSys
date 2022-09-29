package de.hechler.patrick.pfs.folder.impl;

import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.*;

import java.lang.ref.Reference;
import java.nio.ByteBuffer;

import de.hechler.patrick.pfs.element.PFSElement;
import de.hechler.patrick.pfs.element.impl.JavaPatrFileSysElement;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.file.PFSFile;
import de.hechler.patrick.pfs.folder.FolderIter;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.fs.impl.JavaPatrFileSys;
import de.hechler.patrick.pfs.other.Place;
import de.hechler.patrick.pfs.pipe.PFSPipe;

@SuppressWarnings("exports")
public class JavaPatrFileSysFolder extends JavaPatrFileSysElement implements PFSFolder {

	public JavaPatrFileSysFolder(JavaPatrFileSys pfs, JavaPatrFileSysFolder parentRef,
		Place element, Place parent, Place entry, int directParentPos) {
		super(pfs, parentRef, element, parent, entry, directParentPos);
	}

	@Override
	public FolderIter iterator(boolean showHidden) throws PatrFileSysException {
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

	@Override
	public void delete() throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}
}

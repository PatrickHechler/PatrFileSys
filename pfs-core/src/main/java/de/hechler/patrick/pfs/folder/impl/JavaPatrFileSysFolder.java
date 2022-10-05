package de.hechler.patrick.pfs.folder.impl;

import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.OFF_DIRECT_CHILD_COUNT;

import java.io.IOException;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import de.hechler.patrick.pfs.element.PFSElement;
import de.hechler.patrick.pfs.element.impl.JavaPatrFileSysElement;
import de.hechler.patrick.pfs.file.PFSFile;
import de.hechler.patrick.pfs.folder.FolderIter;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.fs.impl.JavaPatrFileSys;
import de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry.Flags;
import de.hechler.patrick.pfs.other.Place;
import de.hechler.patrick.pfs.pipe.PFSPipe;

@SuppressWarnings("exports")
public class JavaPatrFileSysFolder extends JavaPatrFileSysElement implements PFSFolder {
	
	public Map <Object, Reference <JavaPatrFileSysElement>> childs = new HashMap <>();

	public JavaPatrFileSysFolder(JavaPatrFileSys pfs, JavaPatrFileSysFolder parentRef,
		Place element, Place entry, int directParentPos) {
		super(pfs, parentRef, element, entry, directParentPos);
	}
	
	@Override
	public FolderIter iterator(boolean showHidden) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public long childCount() throws IOException {
		ByteBuffer f = pfs.bm.get(element.block);
		try {
			return f.getInt(element.pos + OFF_DIRECT_CHILD_COUNT);
		} finally {
			pfs.bm.unget(element.block);
		}
	}
	
	public PFSElement element(String childName, int flags) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public PFSElement element(String childName) throws IOException {
		return element(childName, -1);
	}
	
	@Override
	public PFSFolder folder(String childName) throws IOException {
		return (PFSFolder) element(childName, Flags.FOLDER);
	}
	
	@Override
	public PFSFile file(String childName) throws IOException {
		return (PFSFile) element(childName, Flags.FILE);
	}
	
	@Override
	public PFSPipe pipe(String childName) throws IOException {
		return (PFSPipe) element(childName, Flags.PIPE);
	}
	
	@Override
	public PFSFolder addFolder(String newChildName) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public PFSFile addFile(String newChildName) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public PFSPipe addPipe(String newChildName) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void delete() throws IOException {
		// TODO Auto-generated method stub
		
	}
	
}

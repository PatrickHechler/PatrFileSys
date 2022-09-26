package de.hechler.patrick.pfs.folder;

import de.hechler.patrick.pfs.element.PFSElement;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.file.PFSFile;
import de.hechler.patrick.pfs.pipe.PFSPipe;

public interface PFSFolder extends PFSElement {
	
	FolderIter iterator(boolean showHidden) throws PatrFileSysException;
	
	long childCount() throws PatrFileSysException;
	
	PFSElement element(String childName) throws PatrFileSysException;
	
	PFSFolder folder(String childName) throws PatrFileSysException;
	
	PFSFile file(String childName) throws PatrFileSysException;
	
	PFSPipe pipe(String childName) throws PatrFileSysException;
	
	PFSFolder addFolder(String newChildName) throws PatrFileSysException;
	
	PFSFile addFile(String newChildName) throws PatrFileSysException;
	
	PFSPipe addPipe(String newChildName) throws PatrFileSysException;
	
}

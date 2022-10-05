package de.hechler.patrick.pfs.folder;

import java.io.IOException;

import de.hechler.patrick.pfs.element.PFSElement;
import de.hechler.patrick.pfs.file.PFSFile;
import de.hechler.patrick.pfs.pipe.PFSPipe;

public interface PFSFolder extends PFSElement {
	
	FolderIter iterator(boolean showHidden) throws IOException;
	
	long childCount() throws IOException;
	
	PFSElement element(String childName) throws IOException;
	
	PFSFolder folder(String childName) throws IOException;
	
	PFSFile file(String childName) throws IOException;
	
	PFSPipe pipe(String childName) throws IOException;
	
	PFSFolder addFolder(String newChildName) throws IOException;
	
	PFSFile addFile(String newChildName) throws IOException;
	
	PFSPipe addPipe(String newChildName) throws IOException;
	
}

package de.hechler.patrick.zeugs.pfs.impl.java;

import java.io.IOException;
import java.nio.file.Path;

import de.hechler.patrick.zeugs.pfs.interfaces.FSElement;
import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Folder;
import de.hechler.patrick.zeugs.pfs.interfaces.Pipe;

public class JavaFolder extends JavaFSElement implements Folder {
	
	public JavaFolder(Path root, Path path) {
		super(root, path);
		
	}

	@Override
	public FolderIter iter(boolean showHidden) throws IOException { // TODO Auto-generated method stub
	return null; }

	@Override
	public long childCount() throws IOException { // TODO Auto-generated method stub
	return 0; }

	@Override
	public FSElement childElement(String name) throws IOException { // TODO Auto-generated method stub
	return null; }

	@Override
	public Folder childFolder(String name) throws IOException { // TODO Auto-generated method stub
	return null; }

	@Override
	public File childFile(String name) throws IOException { // TODO Auto-generated method stub
	return null; }

	@Override
	public Pipe childPipe(String name) throws IOException { // TODO Auto-generated method stub
	return null; }

	@Override
	public Folder createFolder(String name) throws IOException { // TODO Auto-generated method stub
	return null; }

	@Override
	public File createFile(String name) throws IOException { // TODO Auto-generated method stub
	return null; }

	@Override
	public Pipe createPipe(String name) throws IOException { // TODO Auto-generated method stub
	return null; }
	
}

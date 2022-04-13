package de.hechler.patrick.pfs.objects.java;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;

import de.hechler.patrick.pfs.interfaces.PatrFolder;


public class PFSDirectoryStreamImpl implements DirectoryStream <Path> {
	
	public PFSDirectoryStreamImpl(long lock, PatrFolder folder, Filter <? super Path> filter) {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public Iterator <Path> iterator() {
		// TODO Auto-generated method stub
		return null;
	}
	
}

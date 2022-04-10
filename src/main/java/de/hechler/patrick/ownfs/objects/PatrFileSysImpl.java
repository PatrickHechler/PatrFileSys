package de.hechler.patrick.ownfs.objects;

import java.io.IOException;
import java.util.Iterator;

import de.hechler.patrick.ownfs.interfaces.BlockAccessor;
import de.hechler.patrick.ownfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.ownfs.interfaces.PatrFileSystem;
import de.hechler.patrick.ownfs.interfaces.PatrFolder;


public class PatrFileSysImpl implements PatrFileSystem, PatrFolder {
	
	private final BlockAccessor ba;
	
	public PatrFileSysImpl(BlockAccessor ba) {
		this.ba = ba;
	}
	
	@Override
	public PatrFolder getRoot() {
		return this;
	}
	
	@Override
	public void close() throws IOException {
		ba.close();
	}
	
	@Override
	public Iterator <PatrFileSysElement> iterator() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public PatrFolder getParent() throws IllegalStateException, IOException {
		throw new IllegalStateException("this is the root element!");
	}
	
	@Override
	public void addFolder(String name) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void addFile(String name) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void delete() throws IllegalStateException, IOException {
		throw new IllegalStateException("this is the root element!");
	}
	
	@Override
	public boolean canDeleteWithContent() {
		return false;
	}
	
	@Override
	public boolean isRoot() {
		return true;
	}
	
}

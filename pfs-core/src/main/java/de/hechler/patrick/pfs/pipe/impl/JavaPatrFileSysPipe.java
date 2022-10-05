package de.hechler.patrick.pfs.pipe.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

import de.hechler.patrick.pfs.element.impl.JavaPatrFileSysElement;
import de.hechler.patrick.pfs.folder.impl.JavaPatrFileSysFolder;
import de.hechler.patrick.pfs.fs.impl.JavaPatrFileSys;
import de.hechler.patrick.pfs.other.Place;
import de.hechler.patrick.pfs.pipe.PFSPipe;

@SuppressWarnings("exports")
public class JavaPatrFileSysPipe extends JavaPatrFileSysElement implements PFSPipe {
	
	public JavaPatrFileSysPipe(JavaPatrFileSys pfs, JavaPatrFileSysFolder parentRef,
		Place element, Place entry, int directParentPos) {
		super(pfs, parentRef, element, entry, directParentPos);
	}
	
	@Override
	public ByteBuffer read(int length) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void read(ByteBuffer data, int length) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void append(ByteBuffer data, int length) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public long length() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void delete() throws IOException {
		// TODO Auto-generated method stub
		
	}
	
}

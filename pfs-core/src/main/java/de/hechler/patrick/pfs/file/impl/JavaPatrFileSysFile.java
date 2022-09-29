package de.hechler.patrick.pfs.file.impl;

import java.lang.ref.Reference;
import java.nio.ByteBuffer;

import de.hechler.patrick.pfs.element.impl.JavaPatrFileSysElement;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.file.PFSFile;
import de.hechler.patrick.pfs.folder.impl.JavaPatrFileSysFolder;
import de.hechler.patrick.pfs.fs.impl.JavaPatrFileSys;
import de.hechler.patrick.pfs.other.Place;

@SuppressWarnings("exports")
public class JavaPatrFileSysFile extends JavaPatrFileSysElement implements PFSFile {

	public JavaPatrFileSysFile(JavaPatrFileSys pfs, JavaPatrFileSysFolder parentRef,
		Place element, Place parent, Place entry, int directParentPos) {
		super(pfs, parentRef, element, parent, entry, directParentPos);
	}

	@Override
	public ByteBuffer read(long position, int length) throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void read(long position, ByteBuffer data, int length) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void overwrite(long position, ByteBuffer data, int length) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void append(ByteBuffer data, int length) throws PatrFileSysException {
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

	@Override
	public void delete() throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}

}

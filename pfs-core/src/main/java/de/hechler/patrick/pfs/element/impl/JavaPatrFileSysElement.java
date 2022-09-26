package de.hechler.patrick.pfs.element.impl;

import static de.hechler.patrick.pfs.fs.impl.PatrFileSysConstants.Element.*;
import static de.hechler.patrick.pfs.fs.impl.PatrFileSysConstants.Element.Folder.Entry.*;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;

import de.hechler.patrick.pfs.element.PFSElement;
import de.hechler.patrick.pfs.exceptions.PFSErr;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.file.PFSFile;
import de.hechler.patrick.pfs.file.impl.JavaPatrFileSysFile;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.folder.impl.JavaPatrFileSysFolder;
import de.hechler.patrick.pfs.fs.impl.JavaPatrFileSys;
import de.hechler.patrick.pfs.pipe.PFSPipe;
import de.hechler.patrick.pfs.pipe.impl.JavaPatrFileSysPipe;

public abstract class JavaPatrFileSysElement implements PFSElement {
	
	public static final Reference <JavaPatrFileSysFolder> EMPTY_REF = new PhantomReference <
		JavaPatrFileSysFolder>(null, null);
	
	public final JavaPatrFileSys             pfs;
	public Reference <JavaPatrFileSysFolder> parentRef;
	
	public Place element;
	public Place parent;
	public Place entry;
	
	public JavaPatrFileSysElement(JavaPatrFileSys pfs, Reference <JavaPatrFileSysFolder> parentRef,
		Place element, Place parent, Place entry) {
		this.pfs = pfs;
		this.parentRef = parentRef;
		this.element = element;
		this.parent = parent;
		this.entry = entry;
	}
	
	@Override
	public int flags() throws PatrFileSysException {
		if (entry == null) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ROOT_FOLDER, "root folder has no flags");
		}
		ByteBuffer data = pfs.bm.get(entry.block);
		try {
			return data.getInt(entry.pos + OFF_FLAGS);
		} finally {
			pfs.bm.unget(entry.block);
		}
	}
	
	@Override
	public void modifyFlags(int addFlags, int remFlags) throws PatrFileSysException {
		if (entry == null) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ROOT_FOLDER, "root folder has no flags");
		}
		if ( (addFlags & remFlags) != 0) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ILLEGAL_ARG,
				"(addFlags & remFlags) != 0 (add=0x" + Integer.toHexString(addFlags) + ", rem=0x"
					+ Integer.toHexString(remFlags) + ")");
		}
		if ( ( (addFlags | remFlags) & UNMODIFIABLE_FLAGS) != 0) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ILLEGAL_ARG,
				"((addFlags | remFlags) & ESSENTIAL_FLAGS) != 0 (add=0x" + Integer.toHexString(
					addFlags) + ", rem=0x" + Integer.toHexString(remFlags) + ")");
		}
		ByteBuffer data = pfs.bm.get(entry.block);
		try {
			int flags = data.getInt(entry.pos + OFF_FLAGS);
			flags = (flags & ~remFlags) | addFlags;
			data.putInt(entry.pos + OFF_FLAGS, flags);
		} finally {
			pfs.bm.set(entry.block);
		}
	}
	
	@Override
	public String name() throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void name(String newName) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public long createTime() throws PatrFileSysException {
		if (entry == null) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ROOT_FOLDER,
				"root folder has no create time");
		}
		ByteBuffer data = pfs.bm.get(entry.block);
		try {
			return data.getLong(entry.pos + OFF_CREATE_TIME);
		} finally {
			pfs.bm.unget(entry.block);
		}
	}
	
	@Override
	public void createTime(long ct) throws PatrFileSysException {
		if (entry == null) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ROOT_FOLDER,
				"root folder has no create time");
		}
		ByteBuffer data = pfs.bm.get(entry.block);
		try {
			data.putLong(entry.pos + OFF_CREATE_TIME, ct);
		} finally {
			pfs.bm.set(entry.block);
		}
	}
	
	@Override
	public long lastModTime() throws PatrFileSysException {
		ByteBuffer data = pfs.bm.get(element.block);
		try {
			return data.getLong(entry.pos + OFF_LAST_MODIFY_TIME);
		} finally {
			pfs.bm.unget(element.block);
		}
	}
	
	@Override
	public void lastModTime(long lmt) throws PatrFileSysException {
		ByteBuffer data = pfs.bm.get(element.block);
		try {
			data.putLong(entry.pos + OFF_CREATE_TIME, lmt);
		} finally {
			pfs.bm.set(element.block);
		}
	}
	
	@Override
	public void delete() throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public PFSFolder parent() throws PatrFileSysException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void parent(PFSFolder newParent) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void move(PFSFolder newParent, String newName) throws PatrFileSysException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public PFSFolder toFolder() throws IllegalStateException {
		throw new IllegalStateException("I am no folder");
	}
	
	@Override
	public PFSFile toFile() throws IllegalStateException {
		throw new IllegalStateException("I am no file");
	}
	
	@Override
	public PFSPipe toPipe() throws IllegalStateException {
		throw new IllegalStateException("I am no pipe");
	}
	
	@Override
	public boolean isFolder() {
		return this instanceof JavaPatrFileSysFolder;
	}
	
	@Override
	public boolean isFile() {
		return this instanceof JavaPatrFileSysFile;
	}
	
	@Override
	public boolean isPipe() {
		return this instanceof JavaPatrFileSysPipe;
	}
	
	@Override
	public boolean isRoot() {
		return this == pfs.root;
	}
	
}

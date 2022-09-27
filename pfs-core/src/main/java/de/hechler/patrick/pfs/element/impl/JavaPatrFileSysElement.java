package de.hechler.patrick.pfs.element.impl;

import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.OFF_LAST_MODIFY_TIME;
import de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import de.hechler.patrick.pfs.element.PFSElement;
import de.hechler.patrick.pfs.exceptions.PFSErr;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.file.PFSFile;
import de.hechler.patrick.pfs.file.impl.JavaPatrFileSysFile;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.folder.impl.JavaPatrFileSysFolder;
import de.hechler.patrick.pfs.fs.impl.JavaPatrFileSys;
import de.hechler.patrick.pfs.other.Place;
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
	public int   directParentPos;
	
	public JavaPatrFileSysElement(JavaPatrFileSys pfs, Reference <JavaPatrFileSysFolder> parentRef,
		Place element, Place parent, Place entry, int directParentPos) {
		this.pfs = pfs;
		this.parentRef = parentRef;
		this.element = element;
		this.parent = parent;
		this.entry = entry;
		this.directParentPos = directParentPos;
	}
	
	@Override
	public int flags() throws PatrFileSysException {
		if (entry == null) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ROOT_FOLDER, "root folder has no flags");
		}
		ByteBuffer data = pfs.bm.get(entry.block);
		try {
			return data.getInt(entry.pos + Entry.OFF_FLAGS);
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
		if ( ( (addFlags | remFlags) & Entry.Flags.UNMODIFIABLE) != 0) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ILLEGAL_ARG,
				"((addFlags | remFlags) & ESSENTIAL_FLAGS) != 0 (add=0x" + Integer.toHexString(
					addFlags) + ", rem=0x" + Integer.toHexString(remFlags) + ")");
		}
		ByteBuffer data = pfs.bm.get(entry.block);
		try {
			int flags = data.getInt(entry.pos + Entry.OFF_FLAGS);
			flags = (flags & ~remFlags) | addFlags;
			data.putInt(entry.pos + Entry.OFF_FLAGS, flags);
		} finally {
			pfs.bm.set(entry.block);
		}
	}
	
	@Override
	public String name() throws PatrFileSysException {
		if (parent == null) {
			return "";
		}
		ByteBuffer data = pfs.bm.get(entry.block);
		try {
			int namePos = data.getInt(entry.pos + Entry.OFF_NAME_POS);
			int nameLen = sizeInBlockTable(data, namePos);
			byte[] buffer = new byte[nameLen];
			data.get(namePos, buffer);
			return new String(buffer, StandardCharsets.UTF_8);
		} finally {
			pfs.bm.unget(entry.block);
		}
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
			return data.getLong(entry.pos + Entry.OFF_CREATE_TIME);
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
			data.putLong(entry.pos + Entry.OFF_CREATE_TIME, ct);
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
			data.putLong(entry.pos + Entry.OFF_CREATE_TIME, lmt);
		} finally {
			pfs.bm.set(element.block);
		}
	}
	
	@Override
	public abstract void delete() throws PatrFileSysException;
	
	public void deleteFolderEntry(Place place, int folderPos) throws PatrFileSysException {
		ByteBuffer data = pfs.bm.get(place.block);
		try {
			
		} finally {
			pfs.bm.set(place.block);
		}
	}
	
	@Override
	public PFSFolder parent() throws PatrFileSysException {
		if (parent == null) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ROOT_FOLDER, "root folder has no parent");
		}
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
	
	public int resizeInBlockTable(ByteBuffer data, int pos, int newLen, boolean copy)
		throws PatrFileSysException {
		
	}
	
	public void shrinkInBlockTable(long block, int pos, int newLen) throws PatrFileSysException {
		ByteBuffer data = pfs.bm.get(block);
		try {
			int tablePos = findInBlockTable(data, pos);
			data.putInt(tablePos, pos + newLen);
		} finally {
			pfs.bm.set(block);
		}
	}
	
	public boolean removeFromBlockTable(long block, int pos, boolean freeBlock)
		throws PatrFileSysException {
		ByteBuffer data = pfs.bm.get(block);
		try {
			int tablePos = findInBlockTable(data, pos);
			int newTableEnd = data.getInt(data.capacity() - 4) + 8;
			if (newTableEnd == data.capacity() - 4 && freeBlock) {
				pfs.freeBlock(block);
				return true;
			}
			for (int off = newTableEnd; off <= tablePos; off += 8) {
				data.putInt(off + 8, data.getInt(off));
				data.putInt(off + 12, data.getInt(off + 4));
			}
			data.putInt(data.capacity() - 4, newTableEnd);
		} finally {
			pfs.bm.set(block);
		}
		return false;
	}
	
	public static int sizeInBlockTable(ByteBuffer data, int pos) {
		int tablePos = findInBlockTable(data, pos);
		return data.getInt(tablePos + 4) - data.getInt(tablePos);
	}
	
	private static int findInBlockTable(ByteBuffer data, int pos) throws AssertionError {
		int max = data.capacity() - 4;
		int min = data.getInt(max);
		max -= 8;
		while (min <= max) {
			int mid = min + ( (max - min) >>> 1) & ~3;
			int val = data.getInt(mid);
			if (val > pos) {
				min = mid + 4;
			} else if (val < pos) {
				max = mid - 4;
			} else {
				min |= 4;
				assert pos == data.getInt(mid);
				return min;
			}
		}
		throw new AssertionError("min > max");
	}
	
}

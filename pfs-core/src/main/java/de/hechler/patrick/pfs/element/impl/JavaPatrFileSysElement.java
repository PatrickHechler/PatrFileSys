package de.hechler.patrick.pfs.element.impl;

import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.OFF_LAST_MODIFY_TIME;

import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

import de.hechler.patrick.pfs.element.PFSElement;
import de.hechler.patrick.pfs.exceptions.PFSErr;
import de.hechler.patrick.pfs.file.PFSFile;
import de.hechler.patrick.pfs.file.impl.JavaPatrFileSysFile;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.folder.impl.JavaPatrFileSysFolder;
import de.hechler.patrick.pfs.fs.impl.JavaPatrFileSys;
import de.hechler.patrick.pfs.other.PatrFileSysConstants.B0;
import de.hechler.patrick.pfs.other.PatrFileSysConstants.BlockFlags;
import de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder;
import de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry;
import de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry.Flags;
import de.hechler.patrick.pfs.other.Place;
import de.hechler.patrick.pfs.pipe.PFSPipe;
import de.hechler.patrick.pfs.pipe.impl.JavaPatrFileSysPipe;

public abstract class JavaPatrFileSysElement implements PFSElement {
	
	public static final Reference <JavaPatrFileSysFolder> EMPTY_REF = new PhantomReference <
		JavaPatrFileSysFolder>(null, null);
	
	public final JavaPatrFileSys pfs;
	/**
	 * need strong reference to parent, otherwise an illegal state may occur:
	 * <ol>
	 * <li>folder A : any folder</li>
	 * <li>folder B : child of A</li>
	 * <li>element C : child of B</li>
	 * <li>B gets Garbage-Collected</li>
	 * <li>folder D : parent of C</li>
	 * <li>folder E : corresponding child of A (same folder as D)</li>
	 * <li>if D/E changes it's position only one will be updated and the other will be
	 * corrupted</li>
	 * </ol>
	 */
	public JavaPatrFileSysFolder parentRef;
	
	public final Place element;
	public final Place entry;
	public int         directParentPos;
	
	public JavaPatrFileSysElement(JavaPatrFileSys pfs, JavaPatrFileSysFolder parentRef,
		Place element, Place entry, int directParentPos) {
		this.pfs = pfs;
		this.parentRef = parentRef;
		this.element = element;
		this.entry = entry;
		this.directParentPos = directParentPos;
	}
	
	@Override
	public int flags() throws IOException {
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
	public void modifyFlags(int addFlags, int remFlags) throws IOException {
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
	public String name() throws IOException {
		if (parentRef == null) {
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
	public void name(String newName) throws IOException {
		if (entry == null) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ROOT_FOLDER, "the root folder has no name!");
		}
		byte[] name = newName.getBytes(StandardCharsets.UTF_8);
		ByteBuffer data = pfs.bm.get(entry.block);
		try {
			try {
				int oldNamePos = data.getInt(entry.pos + Entry.OFF_NAME_POS);
				int newNamePos = resizeInBlockTable(entry.block, oldNamePos, name.length, false);
				data.put(newNamePos, name);
				data.putInt(entry.pos + Entry.OFF_NAME_POS, newNamePos);
			} catch (IOException e) {
				if ( ! (e instanceof PFSErr)) {
					throw e;
				}
				if ( ((PFSErr) e).pfs_errno() != PFSErr.PFS_ERR_OUT_OF_SPACE) {
					throw e;
				}
				// TODO block change / helper folder
				throw e;
			}
		} finally {
			pfs.bm.set(entry.block);
		}
	}
	
	@Override
	public long createTime() throws IOException {
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
	public void createTime(long ct) throws IOException {
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
	public long lastModTime() throws IOException {
		ByteBuffer data = pfs.bm.get(element.block);
		try {
			return data.getLong(entry.pos + OFF_LAST_MODIFY_TIME);
		} finally {
			pfs.bm.unget(element.block);
		}
	}
	
	@Override
	public void lastModTime(long lmt) throws IOException {
		ByteBuffer data = pfs.bm.get(element.block);
		try {
			data.putLong(entry.pos + Entry.OFF_CREATE_TIME, lmt);
		} finally {
			pfs.bm.set(element.block);
		}
	}
	
	@Override
	public abstract void delete() throws IOException;
	
	public void deleteFolderEntry(Place place, int folderPos) throws IOException {
		final JavaPatrFileSysFolder me = (JavaPatrFileSysFolder) this;
		ByteBuffer data = pfs.bm.get(place.block);
		try {
			int newSize = Folder.EMPTY_SIZE + ( (data.get(folderPos + Folder.OFF_DIRECT_CHILD_COUNT)
				- 1) * Entry.SIZE);
			shrinkInBlockTable(place.block, folderPos, newSize);
			data.put(place.pos, data, place.pos + Entry.SIZE, folderPos + newSize - Entry.SIZE
				- place.pos);
			for (Iterator <Map.Entry <Object, Reference <JavaPatrFileSysElement>>> iter = me.childs
				.entrySet().iterator(); iter.hasNext();) {
				Map.Entry <?, Reference <JavaPatrFileSysElement>> entry = iter.next();
				JavaPatrFileSysElement val = entry.getValue().get();
				if (val == null) {
					try {
						iter.remove();
					} catch (UnsupportedOperationException ignore) {}
					continue;
				}
				if (val.entry.pos > place.pos) {
					val.entry.pos -= Entry.SIZE;
				} // entry is not used in hashCode/equals, so this is fine
			}
			for (int end = folderPos + newSize; place.pos < end; place.pos += Entry.SIZE) {
				int flags = data.getInt(place.pos + Entry.OFF_FLAGS);
				if ( (flags & Flags.FOLDER) != 0) {
					long cb = data.getLong(place.pos + Entry.OFF_CHILD_BLOCK);
					ByteBuffer child = pfs.bm.get(cb);
					try {
						int cp = data.getInt(place.pos + Entry.OFF_CHILD_POS);
						child.putInt(cp + Folder.OFF_ENTRY_POS, place.pos);
					} finally {
						pfs.bm.set(cb);
					}
				}
			}
		} finally {
			pfs.bm.set(place.block);
		}
	}
	
	@Override
	public PFSFolder parent() throws IOException {
		if (parentRef == null) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ROOT_FOLDER, "root folder has no parent");
		}
		return parentRef;
	}
	
	@Override
	public void parent(PFSFolder newParent) throws IOException {
		if (parentRef == null) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ROOT_FOLDER, "root folder has no parent");
		}
		if ( ! (newParent instanceof JavaPatrFileSysFolder)) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ILLEGAL_ARG,
				"parent is not from this file system");
		}
		JavaPatrFileSysFolder np = (JavaPatrFileSysFolder) newParent;
		if ( !pfs.equals(np.pfs)) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ILLEGAL_ARG,
				"parent is not from this file system");
		}
		for (JavaPatrFileSysFolder check = np; check != null; check = check.parentRef) {
			if (check == this) {
				throw PFSErr.createAndThrow(PFSErr.PFS_ERR_PARENT_IS_CHILD,
					"newParent is a ((in)direct) child from me");
			}
		}
		// TODO Auto-generated method stub
	}
	
	@Override
	public void move(PFSFolder newParent, String newName) throws IOException {
		if (parentRef == null) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ROOT_FOLDER, "root folder has no parent");
		}
		if ( ! (newParent instanceof JavaPatrFileSysFolder)) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ILLEGAL_ARG,
				"parent is not from this file system");
		}
		JavaPatrFileSysFolder np = (JavaPatrFileSysFolder) newParent;
		if ( !pfs.equals(np.pfs)) {
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_ILLEGAL_ARG,
				"parent is not from this file system");
		}
		for (JavaPatrFileSysFolder check = np; check != null; check = check.parentRef) {
			if (check == this) {
				throw PFSErr.createAndThrow(PFSErr.PFS_ERR_PARENT_IS_CHILD,
					"newParent is a ((in)direct) child from me");
			}
		}
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
	
	public void moveFolderToNewBlock(Place place) throws IOException {
		long oldBlock = place.block;
		ByteBuffer oldData = pfs.bm.get(oldBlock);
		try {
			long eblock = oldData.getLong(place.pos + Folder.OFF_ENTRY_BLOCK);
			Place myEntryPlace;
			boolean isRealParent;
			if (eblock == -1L) {
				myEntryPlace = new Place(0L, B0.OFF_ROOT_BLOCK);
				isRealParent = true;
			} else {
				myEntryPlace = new Place(eblock, oldData.getInt(place.pos + Folder.OFF_ENTRY_POS)
					+ Entry.OFF_CHILD_BLOCK);
				isRealParent = myEntryPlace.block == oldData.getLong(place.pos
					+ Folder.OFF_PARENT_BLOCK);
			}
			long newBlock = pfs.allocateBlock(BlockFlags.USED | BlockFlags.ENTRIES);
			try {
				ByteBuffer newData = pfs.bm.get(newBlock);
				try {
					int mySize = Folder.EMPTY_SIZE + oldData.getInt(place.pos
						+ Folder.OFF_DIRECT_CHILD_COUNT) * Entry.SIZE;
					initBlock(newBlock, mySize);
					newData.put(0, oldData, place.pos, mySize);
					for (int off = Folder.EMPTY_SIZE; off < mySize; off += Entry.SIZE) {
						int flags = newData.getInt(off + Entry.OFF_FLAGS);
						if ( (flags & Flags.FOLDER) != 0) {
							long cfblock = newData.getLong(off + Entry.OFF_CHILD_BLOCK);
							ByteBuffer cfdata = pfs.bm.get(cfblock);
							try {
								int cfpos = newData.getInt(off + Entry.OFF_CHILD_POS);
								cfdata.putLong(cfpos + Folder.OFF_ENTRY_BLOCK, newBlock);
								cfdata.putInt(cfpos + Folder.OFF_ENTRY_POS, off);
								if (isRealParent) {
									cfdata.putLong(cfpos + Folder.OFF_PARENT_BLOCK, newBlock);
									cfdata.putLong(cfpos + Folder.OFF_PARENT_POS, 0);
								}
							} finally {
								pfs.bm.set(cfblock);
							}
						}
					}
				} finally {
					pfs.bm.set(newBlock);
				}
			} catch (Throwable t) {
				pfs.freeBlock(newBlock);
				throw t;
			}
			place.block = newBlock;
			place.pos = 0;
		} finally {
			pfs.bm.set(oldBlock);
		}
	}
	
	private void initBlock(long block, int mySize) throws IOException {
		ByteBuffer data = pfs.bm.get(block);
		try {
			if (data.capacity() - mySize < 12) {
				throw PFSErr.createAndThrow(PFSErr.PFS_ERR_OUT_OF_SPACE, null);
			}
			data.putInt(data.capacity() - 12, 0);
			data.putInt(data.capacity() - 8, mySize);
			data.putInt(data.capacity() - 4, data.capacity() - 12);
		} finally {
			pfs.bm.set(block);
		}
	}
	
	public int allocateInBlockTable(long block, int length, int tablePos, boolean copy)
		throws IOException {
		ByteBuffer data = pfs.bm.get(block);
		try {
			int tableEnd = data.capacity() - 4;
			int tableStart = data.getInt(tableEnd);
			if (tablePos == -1 && tableStart != tableEnd) {
				int tableMaxGrow = tableStart - data.get(tableEnd - 4);
				if (tableMaxGrow < 8) {
					throw PFSErr.createAndThrow(PFSErr.PFS_ERR_OUT_OF_SPACE,
						"the table from this block can not grow!");
				}
			}
			for (int table = tableStart; table < tableEnd; table += 8) {
				int prev = table == tableStart ? 0 : data.getInt(table - 4);
				int current = data.getInt(table);
				int free = current - prev;
				if (free < length) {
					continue;
				}
				int newPos = prev + ( (free - length) >> 1);
				if (copy) {
					int oldPos = data.getInt(tablePos);
					int oldLen = oldPos - data.get(tablePos - 4);
					data.put(newPos, data, oldPos, oldLen);
				}
				if (tablePos == -1) {
					data.putInt(data.capacity() - 4, tableStart - 8);
					tablePos = tableStart;
				}
				if (tablePos > table) {
					for (; tablePos > table; tablePos -= 8) {
						data.putInt(tablePos, data.getInt(tablePos - 8));
						data.putInt(tablePos + 4, data.getInt(tablePos - 4));
					}
				} else {
					for (; tablePos < table - 8; tablePos += 8) {
						data.putInt(tablePos, data.getInt(tablePos + 8));
						data.putInt(tablePos + 4, data.getInt(tablePos + 12));
					}
				}
			}
			throw PFSErr.createAndThrow(PFSErr.PFS_ERR_OUT_OF_SPACE, "this block is to full");
		} finally {
			pfs.bm.set(block);
		}
	}
	
	public int resizeInBlockTable(long block, int pos, int newLen, boolean copy)
		throws IOException {
		ByteBuffer data = pfs.bm.get(block);
		try {
			int tablePos = findInBlockTable(data, pos);
			int next = data.getInt(tablePos + 8);
			if (next <= pos + newLen) {
				data.putInt(tablePos + 4, pos + newLen);
				return pos;
			}
			int tableStart = data.getInt(data.capacity() - 4);
			int prev = tableStart == tablePos ? 0 : data.getInt(tablePos - 4);
			if (next - prev >= newLen) {
				int newStart = prev + (next - prev - newLen) >> 1;
				int rns = newStart & 7;
				if (rns >= prev) {
					newStart = rns;
				}
				if (copy) {
					int oldLen = data.getInt(tablePos + 4) - pos;
					data.put(newStart, data, pos, oldLen);
				}
				data.putInt(tablePos, newStart);
				data.putInt(tablePos + 4, newStart + newLen);
				return newStart;
			}
			return allocateInBlockTable(block, newLen, tablePos, copy);
		} finally {
			pfs.bm.set(block);
		}
	}
	
	public void shrinkInBlockTable(long block, int pos, int newLen) throws IOException {
		ByteBuffer data = pfs.bm.get(block);
		try {
			int tablePos = findInBlockTable(data, pos);
			data.putInt(tablePos + 4, pos + newLen);
		} finally {
			pfs.bm.set(block);
		}
	}
	
	public boolean removeFromBlockTable(long block, int pos, boolean freeBlock) throws IOException {
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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + element.hashCode();
		result = prime * result + pfs.hashCode();
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		JavaPatrFileSysElement other = (JavaPatrFileSysElement) obj;
		if ( !element.equals(other.element)) return false;
		if ( !pfs.equals(other.pfs)) return false;
		return true;
	}
	
}

package de.hechler.patrick.ownfs.objects;

import static de.hechler.patrick.zeugs.NumberConvert.byteArrToInt;
import static de.hechler.patrick.zeugs.NumberConvert.byteArrToLong;
import static de.hechler.patrick.zeugs.NumberConvert.intToByteArr;
import static de.hechler.patrick.zeugs.NumberConvert.longToByteArr;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import de.hechler.patrick.ownfs.interfaces.BlockAccessor;
import de.hechler.patrick.zeugs.objects.LongLongOpenImpl;


public class PatrFileSys {
	
	private static final int FIRST_BLOCK_BLOCK_SIZE_OFFSET              = 0;
	private static final int FIRST_BLOCK_ROOT_FOLDER_OFFSET_PNTR_OFFSET = FIRST_BLOCK_BLOCK_SIZE_OFFSET + 4;
	private static final int FIRST_BLOCK_ROOT_FOLDER_BLOCK_PNTR_OFFSET  = FIRST_BLOCK_ROOT_FOLDER_OFFSET_PNTR_OFFSET + 4;
	private static final int FIRST_BLOCK_START_END_OFFSET               = FIRST_BLOCK_ROOT_FOLDER_BLOCK_PNTR_OFFSET + 8;
	
	private static final int SECOND_BLOCK_LAST_ENTRY_OFFSET  = 0;
	private static final int SECOND_BLOCK_BLOCK_COUNT_OFFSET = SECOND_BLOCK_LAST_ENTRY_OFFSET + 4;
	private static final int SECOND_BLOCK_TABLE_OFFSET       = SECOND_BLOCK_BLOCK_COUNT_OFFSET + 8;
	
	
	
	private final BlockAccessor ba;
	private final int           BLOCK_INTERN_TABLE_TABLE_ALLOC_ENTRY_OFFSET;
	
	
	
	/**
	 * creates a new {@link PatrFileSys} instance for an existing Patr-File-Sys.<br>
	 * 
	 * to create a complete new Patr-File-System use {@link #format(BlockAccessor, long)}.
	 * 
	 * @param ba
	 *            the {@link BlockAccessor} of an existing Patr-File-Sys.
	 */
	public PatrFileSys(BlockAccessor ba) {
		this.ba = ba;
		this.BLOCK_INTERN_TABLE_TABLE_ALLOC_ENTRY_OFFSET = ba.blockSize() - 8;
	}
	
	/**
	 * Formats the blocks of the {@link BlockAccessor} with the Patr-File-Sys.
	 * 
	 * @param bl
	 *            the {@link BlockAccessor}
	 * @param blockCnt
	 *            the number of blocks
	 * @throws IOException
	 *             if the {@link BlockAccessor} throws an {@link IOException}.
	 */
	public static void format(BlockAccessor bl, long blockCnt) throws IOException {
		assert blockCnt >= 2L;
		byte[] b = bl.loadBlock(0L);
		try {
			final int blocklen = bl.blockSize();
			assert b.length == blocklen;
			
			initBlock(b, FIRST_BLOCK_START_END_OFFSET, PatrFolder.FOLDER_EMPTY_SIZE);
			
			// // block-intern memory management (of the first block, because it is now formatted)
			// // allocate memory for memory list
			// intToByteArr(blocklen, b, blocklen - 4);
			// intToByteArr(blocklen - 24, b, blocklen - 8);
			// // allocate memory for the root folder
			// intToByteArr(FIRST_BLOCK_START_END_OFFSET + PatrFolder.FOLDER_EMPTY_SIZE, b, blocklen - 12);
			// intToByteArr(FIRST_BLOCK_START_END_OFFSET, b, blocklen - 16);
			// // allocate memory for the start block
			// intToByteArr(FIRST_BLOCK_START_END_OFFSET, b, blocklen - 20);
			// intToByteArr(0, b, blocklen - 24);
			
			// save size of the blocks (may bee needed when the BlockAccesor is loaded)
			intToByteArr(blocklen, b, FIRST_BLOCK_BLOCK_SIZE_OFFSET);
			// save position of root folder (block=0, offset=end of start block)
			longToByteArr(0L, b, FIRST_BLOCK_ROOT_FOLDER_BLOCK_PNTR_OFFSET);
			intToByteArr(FIRST_BLOCK_START_END_OFFSET, b, FIRST_BLOCK_ROOT_FOLDER_OFFSET_PNTR_OFFSET);
			// number of elements in the rootFolder
			intToByteArr(0, b, FIRST_BLOCK_START_END_OFFSET + PatrFolder.FOLDER_COUNT_ELEMENTS_OFFSET);
			// last mod is now
			longToByteArr(System.currentTimeMillis(), b, FIRST_BLOCK_START_END_OFFSET + PatrFolder.FOLDER_LAST_MOD_OFFSET);
			// -1L, -1 for no parent (this is the root folder)
			longToByteArr( -1L, b, FIRST_BLOCK_START_END_OFFSET + PatrFolder.FOLDER_PARENT_BLOCK_OFFSET);
			intToByteArr( -1, b, FIRST_BLOCK_START_END_OFFSET + PatrFolder.FOLDER_PARENT_POS_OFFSET);
		} finally {
			bl.saveBlock(b, 0L);
		}
		b = bl.loadBlock(1L);
		try {
			intToByteArr(SECOND_BLOCK_TABLE_OFFSET, b, SECOND_BLOCK_LAST_ENTRY_OFFSET);
			longToByteArr(blockCnt, b, SECOND_BLOCK_BLOCK_COUNT_OFFSET);
			longToByteArr(0L, b, SECOND_BLOCK_TABLE_OFFSET);
			longToByteArr(2L, b, SECOND_BLOCK_TABLE_OFFSET + 8);
		} finally {
			bl.saveBlock(b, 1L/* pointer to the last element */);
		}
	}
	
	
	public long blockCount() throws IOException {
		byte[] bl = ba.loadBlock(1L);
		return byteArrToLong(bl, SECOND_BLOCK_BLOCK_COUNT_OFFSET);
	}
	
	public BlockAccessor getBlockAccessor() {
		return ba;
	}
	
	public PatrFolder rootFolder() throws ClosedChannelException, IOException {
		final long block;
		final int offset;
		byte[] b0 = ba.loadBlock(0L);
		try {
			block = byteArrToLong(b0, FIRST_BLOCK_ROOT_FOLDER_BLOCK_PNTR_OFFSET);
			offset = byteArrToInt(b0, FIRST_BLOCK_ROOT_FOLDER_OFFSET_PNTR_OFFSET);
		} finally {
			ba.unloadBlock(0L);
		}
		return new PatrFolder(block, offset);
	}
	
	@Override
	public int hashCode() {
		return ba.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		PatrFileSys other = (PatrFileSys) obj;
		return ba.equals(other.ba);
	}
	
	public class PatrFolder {
		
		private static final int FOLDER_COUNT_ELEMENTS_OFFSET = 0;
		private static final int FOLDER_PARENT_BLOCK_OFFSET   = FOLDER_COUNT_ELEMENTS_OFFSET + 4;
		private static final int FOLDER_PARENT_POS_OFFSET     = FOLDER_PARENT_BLOCK_OFFSET + 8;
		private static final int FOLDER_LAST_MOD_OFFSET       = FOLDER_PARENT_POS_OFFSET + 4;
		private static final int FOLDER_ELEMENTS_OFFSET       = FOLDER_LAST_MOD_OFFSET + 8;
		private static final int FOLDER_EMPTY_SIZE            = FOLDER_ELEMENTS_OFFSET;
		
		private long    block;
		private int     offset;
		private boolean autoSetLM;
		
		
		
		private PatrFolder(long block, int offset) {
			this.block = block;
			this.offset = offset;
			this.autoSetLM = true;
		}
		
		
		
		public boolean isRoot() throws IOException {
			byte[] bl = ba.loadBlock(block);
			boolean b;
			try {
				final long pblock = byteArrToLong(bl, offset + FOLDER_PARENT_BLOCK_OFFSET);
				final int poffset = byteArrToInt(bl, offset + FOLDER_PARENT_POS_OFFSET);
				if (pblock == -1L) {
					assert poffset == -1;
					b = true;
				} else {
					b = false;
				}
			} finally {
				ba.unloadBlock(block);
			}
			return b;
		}
		
		public PatrFolder getParentFolder() throws IOException {
			byte[] bl = ba.loadBlock(block);
			PatrFolder pf;
			try {
				final long pblock = byteArrToLong(bl, offset + FOLDER_PARENT_BLOCK_OFFSET);
				final int poffset = byteArrToInt(bl, offset + FOLDER_PARENT_POS_OFFSET);
				if (pblock == -1L) {
					assert poffset == -1;
					return null;
				}
				pf = new PatrFolder(pblock, poffset);
			} finally {
				ba.unloadBlock(block);
			}
			return pf;
		}
		
		public void setAutoSetLAstMod(boolean autoSetLM) {
			this.autoSetLM = autoSetLM;
		}
		
		public boolean isAutoSetLastMod() {
			return autoSetLM;
		}
		
		public void setLastModified(long lm) throws IOException {
			byte[] bl = ba.loadBlock(block);
			longToByteArr(lm, bl, offset + FOLDER_LAST_MOD_OFFSET);
			ba.saveBlock(bl, block);
		}
		
		public long getLastModified() throws IOException {
			byte[] bl = ba.loadBlock(block);
			long lm = byteArrToLong(bl, offset + FOLDER_LAST_MOD_OFFSET);
			ba.unloadBlock(block);
			return lm;
		}
		
		public FolderElement addElement(final String name, final boolean file) throws IOException, OutOfMemoryError {
			final long oldblock = block;
			final int oldoffset = offset;
			byte[] obl = ba.loadBlock(oldblock);
			FolderElement nfe;
			try {
				if (name.indexOf('\0') != -1) {
					throw new IllegalArgumentException("names are not allowed to contain the '\\0' character, because it is used to mark the end of the name!");
				}
				// aktuelle Größe berechnen
				final int oldElementCount = byteArrToInt(obl, offset + FOLDER_COUNT_ELEMENTS_OFFSET);
				final int oldSize = FOLDER_EMPTY_SIZE + FolderElement.ELEMENT_SIZE * oldElementCount;
				final int newSize = oldSize + FolderElement.ELEMENT_SIZE;
				final int newoffset;
				final long newblock;
				byte[] nbl;
				byte[] nameBytes = name.getBytes(StandardCharsets.UTF_16LE);
				final int nameSize = nameBytes.length + 2/* + '\0'.len */;
				final int namePNTR;
				byte[] table = ba.loadBlock(1L);
				try {
					{
						long _newBlock;
						int _newOff = oldoffset;
						int _namePNTR;
						try {
							_newOff = resize(obl, newSize, oldoffset);
							_namePNTR = resize(obl, nameSize, -1);
							_newBlock = block;
							nbl = obl;
						} catch (OutOfMemoryError e) {
							resize(obl, 0, _newOff);
							LongLongOpenImpl[] ll = addBlocksToTable(table, 1);
							assert ll.length == 1;
							_newBlock = ll[0].first;
							assert (ll[0].second - _newBlock) == 1;
							nbl = ba.loadBlock(_newBlock);
							try {
								initBlock(nbl, newSize);
								_newOff = 0;
								// intToByteArr(blocklen - 16, nbl, BLOCK_INTERN_TABLE_TABLE_ALLOC_ENTRY_OFFSET);
								// intToByteArr(BLOCK_INTERN_TABLE_TABLE_ALLOC_ENTRY_OFFSET, nbl, blocklen - 16);
								// intToByteArr(BLOCK_INTERN_TABLE_TABLE_ALLOC_ENTRY_OFFSET + 4, nbl, blocklen - 12);
								// intToByteArr(blocklen - 16, nbl, blocklen - 8);
								// intToByteArr(blocklen, nbl, blocklen - 4);
								// _newOff = resize(nbl, newSize, -1);
								_namePNTR = resize(nbl, nameSize, -1);
							} catch (Throwable t) {// only in case of an exception normally it is later done
								ba.unloadBlock(_newBlock);
								throw t;
							}
						}
						newblock = _newBlock;
						newoffset = _newOff;
						namePNTR = _namePNTR;
					}
					try {
						
						if (newoffset != oldoffset || newblock != oldblock) {
							final long pblock = byteArrToLong(obl, oldoffset + FOLDER_PARENT_BLOCK_OFFSET);
							final int poffset = byteArrToInt(obl, oldoffset + FOLDER_PARENT_POS_OFFSET);
							System.arraycopy(obl, oldoffset, nbl, newoffset, oldSize);
							if (pblock != -1L) {
								assert poffset != -1;
								byte[] pbl;
								if (pblock != block) {
									pbl = ba.loadBlock(pblock);
								} else {
									pbl = obl;
								}
								try {
									final int pElementCnt = byteArrToInt(pbl, poffset + FOLDER_COUNT_ELEMENTS_OFFSET);
									for (int i = poffset + FOLDER_ELEMENTS_OFFSET, cnt = 0;; i += FolderElement.ELEMENT_SIZE, cnt ++ ) {
										assert cnt < pElementCnt;
										final long sblock = byteArrToLong(pbl, i + FolderElement.ELEMENT_BLOCK_OFFSET);
										final int soffset = byteArrToInt(pbl, i + FolderElement.ELEMENT_POS_OFFSET);
										if (sblock == oldblock && soffset == oldoffset) {
											longToByteArr(newblock, pbl, i + FolderElement.ELEMENT_BLOCK_OFFSET);
											intToByteArr(newoffset, pbl, i + FolderElement.ELEMENT_POS_OFFSET);
											break;
										}
									}
								} finally {
									if (pblock != block) {
										ba.saveBlock(pbl, pblock);
									}
								}
							} else {
								byte[] blockZero;
								if (oldblock == 0L) {
									blockZero = obl;
								} else {
									blockZero = ba.loadBlock(0L);
								}
								try {
									longToByteArr(newblock, blockZero, FIRST_BLOCK_ROOT_FOLDER_BLOCK_PNTR_OFFSET);
									intToByteArr(newoffset, blockZero, FIRST_BLOCK_ROOT_FOLDER_OFFSET_PNTR_OFFSET);
								} finally {
									if (oldblock != 0L) {
										ba.saveBlock(blockZero, 0L);
									}
								}
							}
						}
						offset = newoffset;
						block = newblock;
						
						System.arraycopy(nameBytes, 0, nbl, namePNTR, nameBytes.length);
						nbl[namePNTR + nameBytes.length] = nbl[namePNTR + nameBytes.length + 1] = 0;// set '\0'
						
						intToByteArr(oldElementCount + 1, nbl, newoffset + FOLDER_COUNT_ELEMENTS_OFFSET);
						
						// intToByteArr(file ? FolderElement.ELEMENT_FLAG_FILE : 0, nbl, nfeoff + FolderElement.ELEMENT_FLAGS_OFFSET);
						// intToByteArr(namePNTR, bl, nfeoff + FolderElement.ELEMENT_NAME_PNTR_OFFSET);
						
						// try {
						// nfeoffset = resize(nbl, newSize, -1);
						// nfe = new FolderElement(newblock, nfeoffset);
						// } catch (OutOfMemoryError oome) {
						// LongLongOpenImpl[] ll = addBlocksToTable(table, 2L);
						// assert ll.length <= 2;
						// final long nefBlock = ll[0].first;
						// final long nfeDataTableBlock = ll.length > 1 ? ll[1].second : (nefBlock + 1);
						// assert (ll[0].second - ll[0].second + (ll.length > 1 ? (ll[1].second - ll[1].first) : 0)) == 2;// total amount of allocated blocks
						// byte[] blockBytes = ba.loadBlock(nefBlock);
						// try {
						// intToByteArr(blocklen - 16, blockBytes, BLOCK_INTERN_TABLE_START_PNTR_OFFSET);
						// intToByteArr(BLOCK_INTERN_TABLE_START_PNTR_OFFSET, blockBytes, blocklen - 16);
						// intToByteArr(BLOCK_INTERN_TABLE_START_PNTR_OFFSET + 4, blockBytes, blocklen - 12);
						// intToByteArr(blocklen - 16, blockBytes, blocklen - 8);
						// intToByteArr(blocklen, blockBytes, blocklen - 4);
						//
						// // resize already done
						final int nfeoffset = newoffset + oldSize;
						nfe = new FolderElement(newblock, nfeoffset);
						final int childOffset;
						final long childBlock;
						if (file) {
							long fileDataTableBlock;
							long fileBlock = newblock;
							int fileOffset;
							byte[] fileBlockBytes = nbl;
							try {
								fileOffset = resize(fileBlockBytes, PatrFile.FILE_EMPTY_SIZE, -1);
								LongLongOpenImpl[] added = addBlocksToTable(table, 1L);
								assert added.length == 1;
								fileDataTableBlock = added[0].first;
								assert (added[0].second - fileDataTableBlock) == 1L;
							} catch (OutOfMemoryError e) {
								LongLongOpenImpl[] added = addBlocksToTable(table, 2L);
								assert added.length <= 2;
								fileBlock = added[0].first;
								if (added.length > 1) {
									fileDataTableBlock = added[1].first;
									assert (added[0].second - fileBlock) == 1L;
									assert (added[1].second - fileDataTableBlock) == 1L;
								} else {
									assert (added[0].second - fileBlock) == 2L;
									fileDataTableBlock = fileBlock + 1;
								}
								fileBlockBytes = ba.loadBlock(fileBlock);
								try {
									initBlock(fileBlockBytes, PatrFile.FILE_EMPTY_SIZE);
									fileOffset = 0;
								} catch (Throwable t) {
									ba.unloadBlock(fileBlock);
									throw t;
								}
							}
							try {
								longToByteArr(System.currentTimeMillis(), fileBlockBytes, fileOffset + PatrFile.FILE_LAST_MOD_OFFSET);
								longToByteArr(fileDataTableBlock, fileBlockBytes, fileOffset + PatrFile.FILE_DATA_TABLE_BLOCK_OFFSET);
								intToByteArr(0, fileBlockBytes, fileOffset + PatrFile.FILE_DATA_TABLE_END_PNTR_OFFSET);
								longToByteArr(0L, fileBlockBytes, fileOffset + PatrFile.FILE_SIZE_OFFSET);
								longToByteArr(PatrFile.NO_LOCK, fileBlockBytes, fileOffset + PatrFile.FILE_LOCK_OFFSET);
								childBlock = fileBlock;
								childOffset = fileOffset;
							} finally {
								if (fileBlock != newblock) {
									ba.saveBlock(fileBlockBytes, fileBlock);
								}
							}
						} else {
							int folderoffset;
							long folderblock = newblock;
							byte[] folderBlockBytes = nbl;
							try {
								try {
									folderoffset = resize(nbl, PatrFolder.FOLDER_EMPTY_SIZE, -1);
								} catch (OutOfMemoryError e) {
									LongLongOpenImpl[] added = addBlocksToTable(table, 1L);
									assert added.length == 1;
									folderblock = added[0].first;
									assert (added[0].second - folderblock) == 1L;
									folderoffset = resize(folderBlockBytes, PatrFile.FILE_EMPTY_SIZE, -1);
								}
								longToByteArr(System.currentTimeMillis(), folderBlockBytes, folderoffset + PatrFolder.FOLDER_LAST_MOD_OFFSET);
								intToByteArr(0, folderBlockBytes, folderoffset + PatrFolder.FOLDER_COUNT_ELEMENTS_OFFSET);
								longToByteArr(newblock, folderBlockBytes, folderoffset + PatrFolder.FOLDER_PARENT_BLOCK_OFFSET);
								intToByteArr(newoffset, folderBlockBytes, folderoffset + PatrFolder.FOLDER_PARENT_POS_OFFSET);
								childBlock = folderblock;
								childOffset = folderoffset;
							} finally {
								if (folderblock != newblock) {
									ba.saveBlock(folderBlockBytes, folderblock);
								}
							}
						}
						intToByteArr(childOffset, nbl, nfe.offset + FolderElement.ELEMENT_POS_OFFSET);
						longToByteArr(childBlock, nbl, nfe.offset + FolderElement.ELEMENT_BLOCK_OFFSET);
						final int flags = (file ? FolderElement.ELEMENT_FLAG_FILE : 0);
						intToByteArr(flags, nbl, nfe.offset + FolderElement.ELEMENT_FLAGS_OFFSET);
						intToByteArr(namePNTR, nbl, nfe.offset + FolderElement.ELEMENT_NAME_PNTR_OFFSET);
						// } finally {
						// ba.saveBlock(blockBytes, nefBlock);
						// }
						// }
						if (autoSetLM) {
							longToByteArr(System.currentTimeMillis(), nbl, offset + FOLDER_LAST_MOD_OFFSET);
						}
					} finally {
						if (oldblock != newblock) {
							ba.saveBlock(nbl, newblock);
						}
					}
				} finally {
					ba.saveBlock(table, 1L);
				}
			} finally {
				ba.saveBlock(obl, oldblock);
			}
			return nfe;
		}
		
		public void removeElement(FolderElement fe) throws IOException {
			byte[] bl = ba.loadBlock(block);
			try {
				final int oldChildCnt = byteArrToInt(bl, this.offset + FOLDER_COUNT_ELEMENTS_OFFSET);
				final int oldChildStart = byteArrToInt(bl, this.offset + FOLDER_ELEMENTS_OFFSET);
				if (fe.block != this.block || this.offset > fe.offset || fe.offset < oldChildCnt * FolderElement.ELEMENT_SIZE + offset + oldChildStart) {
					throw new IllegalArgumentException("I can only remove my Elements!");
				}
				int flags = byteArrToInt(bl, fe.offset + FolderElement.ELEMENT_FLAGS_OFFSET);
				boolean isFile = (FolderElement.ELEMENT_FLAG_FILE & flags) != 0;
				final long childBlock = byteArrToLong(bl, fe.offset + FolderElement.ELEMENT_BLOCK_OFFSET);
				final int childOffset = byteArrToInt(bl, fe.offset + FolderElement.ELEMENT_POS_OFFSET);
				final int blocklen = bl.length;
				byte[] cbl;
				if (childBlock != this.block) {
					cbl = ba.loadBlock(childBlock);
				} else {
					cbl = bl;
				}
				try {
					byte[] table = ba.loadBlock(1L);
					try {
						if (isFile) {
							// free the content of the file
							final int tableStart = byteArrToInt(cbl, childOffset + PatrFile.FILE_DATA_TABLE_BLOCK_OFFSET);
							final int tableEnd = byteArrToInt(cbl, childOffset + PatrFile.FILE_DATA_TABLE_END_PNTR_OFFSET);
							for (int i = tableStart; i < tableEnd; i += 16) {
								final long start = byteArrToLong(cbl, i);
								final long end = byteArrToLong(cbl, i + 8);
								removeBlocksFromTable(table, start, end);
							}
						} else {
							// check if there the folder is empty
							int childChhildCount = byteArrToInt(cbl, childOffset + FOLDER_COUNT_ELEMENTS_OFFSET);
							if (childChhildCount != 0) {
								throw new IllegalStateException("you need to remove first all elements of the folder before removing the folder!");
							}
						}
						// free from block intern table
						resize(cbl, 0, childOffset);
						final int oldChildTableStart = byteArrToInt(cbl, BLOCK_INTERN_TABLE_TABLE_ALLOC_ENTRY_OFFSET);
						int childTableStart = oldChildTableStart;
						for (int i = childTableStart; i < blocklen; i += 8) {
							final int start = byteArrToInt(table, i);
							final int end = byteArrToInt(table, i + 4);
							if (start == end) {
								int ip8 = i + 8;
								System.arraycopy(cbl, childTableStart, cbl, ip8, ip8 - childTableStart);
								childTableStart += 8;
							}
						}
						if (childTableStart == blocklen) {
							removeBlocksFromTable(table, childBlock, childBlock + 1);
						} else if (childTableStart != oldChildTableStart) {
							intToByteArr(childTableStart, cbl, BLOCK_INTERN_TABLE_TABLE_ALLOC_ENTRY_OFFSET);
						}
						if (autoSetLM) {
							longToByteArr(System.currentTimeMillis(), bl, offset + FOLDER_LAST_MOD_OFFSET);
						}
					} finally {
						ba.saveBlock(table, 1L);
					}
				} finally {
					if (childBlock != this.block) {
						ba.saveBlock(cbl, block);
					}
				}
			} finally {
				ba.saveBlock(bl, block);
			}
		}
		
		public void forEachElement(Consumer <FolderElement> c) throws IOException {
			byte[] bl = ba.loadBlock(block);
			final int end = (byteArrToInt(bl, offset + FOLDER_COUNT_ELEMENTS_OFFSET) * FolderElement.ELEMENT_SIZE) + offset + FOLDER_ELEMENTS_OFFSET;
			for (int off = offset + FOLDER_ELEMENTS_OFFSET; off < end; off += FolderElement.ELEMENT_SIZE) {
				c.accept(new FolderElement(block, off));
			}
			ba.unloadBlock(block);
		}
		
		public int elementCount() throws IOException {
			byte[] bl = ba.loadBlock(block);
			int cnt = byteArrToInt(bl, offset + FOLDER_COUNT_ELEMENTS_OFFSET);
			ba.unloadBlock(block);
			return cnt;
		}
		
	}
	
	public class PatrFile {
		
		public static final long NO_LOCK = -1L;
		
		private static final int FILE_SIZE_OFFSET                = 0;
		private static final int FILE_LOCK_OFFSET                = FILE_SIZE_OFFSET + 8;
		private static final int FILE_LAST_MOD_OFFSET            = FILE_LOCK_OFFSET + 8;
		private static final int FILE_DATA_TABLE_END_PNTR_OFFSET = FILE_LAST_MOD_OFFSET + 8;
		private static final int FILE_DATA_TABLE_BLOCK_OFFSET    = FILE_DATA_TABLE_END_PNTR_OFFSET + 4;
		private static final int FILE_EMPTY_SIZE                 = FILE_DATA_TABLE_BLOCK_OFFSET + 8;
		
		private final long block;
		private final int  offset;
		private boolean    autoSetLM;
		
		
		
		private PatrFile(final long block, final int offset) {
			this.block = block;
			this.offset = offset;
			this.autoSetLM = true;
		}
		
		
		
		public void removeLoock() throws IOException {
			byte[] bl = ba.loadBlock(block);
			int lockpos = offset + FILE_LOCK_OFFSET;
			longToByteArr(NO_LOCK, bl, lockpos);
			ba.saveBlock(bl, block);
		}
		
		public void setLoock(final long lock, final boolean suppressCheck) throws IOException {
			byte[] bl = ba.loadBlock(block);
			try {
				int lockpos = offset + FILE_LOCK_OFFSET;
				if ( !suppressCheck) {
					final long oldlock = byteArrToLong(bl, lockpos);
					if (oldlock != NO_LOCK) {
						throw new IllegalStateException("lock already set: lock=" + oldlock + " notNewLock=" + lock);
					}
				}
				longToByteArr(lock, bl, lockpos);
			} finally {
				ba.saveBlock(bl, block);
			}
		}
		
		public long getLoock() throws IOException {
			byte[] bl = ba.loadBlock(block);
			final long lock = byteArrToLong(bl, offset + FILE_LOCK_OFFSET);
			ba.saveBlock(bl, block);
			return lock;
		}
		
		public boolean isLoocked() throws IOException {
			byte[] bl = ba.loadBlock(block);
			final long lock = byteArrToLong(bl, offset + FILE_LOCK_OFFSET);
			ba.saveBlock(bl, block);
			return NO_LOCK != lock;
		}
		
		public void setAutoSetLAstMod(boolean autoSetLM) {
			this.autoSetLM = autoSetLM;
		}
		
		public boolean isAutoSetLastMod() {
			return autoSetLM;
		}
		
		public void setLastModified(long lm) throws IOException {
			byte[] bl = ba.loadBlock(block);
			longToByteArr(lm, bl, offset + FILE_LAST_MOD_OFFSET);
			ba.saveBlock(bl, block);
		}
		
		public long getLastModified() throws IOException {
			byte[] bl = ba.loadBlock(block);
			long lm = byteArrToLong(bl, offset + FILE_LAST_MOD_OFFSET);
			ba.unloadBlock(block);
			return lm;
		}
		
		public long size() throws IOException {
			byte[] bl = ba.loadBlock(block);
			long size = byteArrToLong(bl, offset + FILE_SIZE_OFFSET);
			ba.unloadBlock(block);
			return size;
		}
		
		public void read(byte[] fill, final int fillOff, final long off, final int len) throws IOException {
			byte[] bl = ba.loadBlock(block);
			try {
				final long size = byteArrToLong(bl, offset + FILE_SIZE_OFFSET);
				if (fillOff < 0 || off < 0 || len < 0 || fill.length < fillOff + len || off + len > size) {
					throw new IllegalArgumentException("fillOff=" + fillOff + " off=" + off + " len=" + len + " mySize=" + size + " fill.length=" + fill.length);
				}
				int read = 0;
				boolean first = true;
				int blocklen = bl.length;
				final long blocktable = byteArrToLong(bl, offset + FILE_DATA_TABLE_BLOCK_OFFSET);
				final long blocktableEnd = byteArrToLong(bl, offset + FILE_DATA_TABLE_END_PNTR_OFFSET);
				final byte[] blocktableBytes;
				if (blocktable != block) {
					blocktableBytes = ba.loadBlock(blocktable);
				} else {
					blocktableBytes = bl;
				}
				try {
					final int myMod = (int) (off % blocklen);
					int posInTable = 0;
					for (long skip = off / blocklen; skip > 0; posInTable += 16) {
						assert posInTable <= blocktableEnd;
						final long start = byteArrToLong(blocktableBytes, posInTable);
						final long end = byteArrToLong(blocktableBytes, posInTable + 8);
						final long length = end - start;
						long newSkip = skip - length;
						if (newSkip < 0) {
							long block = end + /* newSkip is negative */newSkip;
							newSkip = block - start;
							byte[] blockBytes = ba.loadBlock(block);
							try {
								first = false;
								final int copylen = blocklen - myMod;
								System.arraycopy(blockBytes, myMod, fill, fillOff + read, copylen);
								read += copylen;
							} finally {
								ba.unloadBlock(block);
							}
							for (block ++ ; block < end && read < len; block ++ ) {
								blockBytes = ba.loadBlock(block);
								try {
									final int copylen;
									if (read + blocklen > len) {
										copylen = len - read;
									} else {
										copylen = blocklen;
									}
									System.arraycopy(blockBytes, 0, fill, fillOff + read, copylen);
									read += copylen;
								} finally {
									ba.unloadBlock(block);
								}
							}
						}
						skip = newSkip;
					}
					for (; read < len; posInTable += 16) {
						final long start = byteArrToLong(blocktableBytes, posInTable);
						final long end = byteArrToLong(blocktableBytes, posInTable + 8);
						assert end - start > 0;
						for (long block = start; block < end && read < len; block ++ ) {
							byte[] blockBytes = ba.loadBlock(block);
							try {
								int copylen = blocklen;
								if (first) {
									copylen -= myMod;
								}
								if (read + blocklen > len) {
									copylen = len - read;
								}
								System.arraycopy(blockBytes, first ? myMod : 0, fill, fillOff + read, copylen);
								first = false;
								read += copylen;
							} finally {
								ba.unloadBlock(block);
							}
						}
					}
					assert read == len;
					assert posInTable <= blocktableEnd;
				} finally {
					if (blocktable != block) {
						ba.unloadBlock(blocktable);
					}
				}
			} finally {
				ba.unloadBlock(block);
			}
		}
		
		public void overwrite(final byte[] data, final int dataOff, final long off, final int len) throws IOException {
			byte[] bl = ba.loadBlock(block);
			try {
				final long size = byteArrToLong(bl, offset + FILE_SIZE_OFFSET);
				if (dataOff < 0 || off < 0 || len < 0 || data.length < dataOff + len || off + len > size) {
					throw new IllegalArgumentException("dataOff=" + dataOff + " off=" + off + " len=" + len + " mySize=" + size + " data.length=" + data.length);
				}
				if (len != 0 && autoSetLM) {
					longToByteArr(System.currentTimeMillis(), bl, offset + FILE_LAST_MOD_OFFSET);
				}
				int wrote = 0;
				int blocklen = bl.length;
				final long blocktable = byteArrToLong(bl, offset + FILE_DATA_TABLE_BLOCK_OFFSET);
				final int blocktableEnd = byteArrToInt(bl, offset + FILE_DATA_TABLE_END_PNTR_OFFSET);
				final byte[] blocktableBytes = ba.loadBlock(blocktable);
				try {
					final int myMod = (int) (off % blocklen);
					boolean first = true;
					int i = 0;
					for (long skip = off / blocklen; skip > 0; i += 16) {
						assert i <= blocktableEnd;
						final long start = byteArrToLong(blocktableBytes, i);
						final long end = byteArrToLong(blocktableBytes, i + 8);
						final long length = end - start;
						long newSkip = skip - length;
						if (newSkip < 0) {
							long block = end + /* newSkip is negative */newSkip;
							newSkip = block - start;
							byte[] blockBytes = ba.loadBlock(block);
							try {
								final int copylen = blocklen - myMod;
								first = false;
								System.arraycopy(data, dataOff + wrote, blockBytes, myMod, copylen);
								wrote += copylen;
							} finally {
								ba.saveBlock(blockBytes, block);
							}
							for (block ++ ; block < end && wrote < len; block ++ ) {
								blockBytes = ba.loadBlock(block);
								try {
									final int copylen;
									if (wrote + blocklen > len) {
										copylen = len - wrote;
									} else {
										copylen = blocklen;
									}
									System.arraycopy(data, dataOff + wrote, blockBytes, 0, copylen);
									wrote += copylen;
								} finally {
									ba.saveBlock(blockBytes, block);
								}
							}
						}
						skip = newSkip;
					}
					for (; wrote < len; i += 16) {
						final long start = byteArrToLong(blocktableBytes, i);
						final long end = byteArrToLong(blocktableBytes, i + 8);
						for (long block = start; block < end && wrote < len; block ++ ) {
							byte[] blockBytes = ba.loadBlock(block);
							try {
								int copylen = blocklen;
								if (first) {
									copylen -= myMod;
								}
								if (wrote + copylen > len) {
									copylen = len - wrote;
								}
								System.arraycopy(data, dataOff + wrote, blockBytes, first ? myMod : 0, copylen);
								wrote += copylen;
								first = false;
							} finally {
								ba.saveBlock(blockBytes, block);
							}
						}
					}
					assert wrote == len;
					assert i <= blocktableEnd;
				} finally {
					ba.unloadBlock(blocktable);
				}
			} finally {
				ba.saveBlock(bl, block);
			}
		}
		
		public void append(final byte[] data, final int dataOff, final int len) throws IOException {
			if (dataOff < 0 || len < 0 || data.length < dataOff + len) {
				throw new IllegalArgumentException("dataOff=" + dataOff + " len=" + len + " data.length=" + data.length);
			}
			byte[] bl = ba.loadBlock(block);
			try {
				final long oldSize = byteArrToLong(bl, offset + FILE_SIZE_OFFSET);
				final long newSize = oldSize + len;
				final int oldTableEnd = byteArrToInt(bl, offset + FILE_DATA_TABLE_END_PNTR_OFFSET);
				final long addBlockCnt;
				final int blocklen = bl.length;
				final int oldMod = (int) (oldSize % (long) blocklen);
				{
					long add = oldMod + newSize;
					long div = add / blocklen;
					if (add % blocklen != 0) {
						addBlockCnt = div + 1;
					} else {
						addBlockCnt = div;
					}
				}
				int appended = 0;
				final long dataTableBlock = byteArrToLong(bl, offset + FILE_DATA_TABLE_BLOCK_OFFSET);
				byte[] dataTableBytes = ba.loadBlock(dataTableBlock);
				try {
					boolean first = true;
					if (oldMod != 0) {
						final long oldLastBlock = byteArrToLong(bl, oldTableEnd + 8);
						byte[] oldlastbl = ba.loadBlock(blocklen);
						try {
							int copyLen = (int) (blocklen - oldMod);
							first = false;
							int newAppended = appended + copyLen;
							if (newAppended > len) {
								copyLen = len - appended;
								newAppended = appended + copyLen;
							}
							System.arraycopy(data, dataOff, oldlastbl, oldMod, copyLen);
							appended = newAppended;
						} finally {
							ba.saveBlock(oldlastbl, oldLastBlock);
						}
					}
					byte[] table = ba.loadBlock(1L);
					LongLongOpenImpl[] newBlocks;
					try {
						newBlocks = addBlocksToTable(table, addBlockCnt);
					} finally {
						ba.saveBlock(table, 1L);
					}
					int endPNTR = oldTableEnd;
					for (int i = 0; i < newBlocks.length; i ++ , endPNTR += 16) {
						final long start = newBlocks[i].first;
						final long end = newBlocks[i].second;
						longToByteArr(start, dataTableBytes, endPNTR);
						longToByteArr(end, dataTableBytes, endPNTR + 8);
						for (long block = start; block < end; block ++ ) {
							byte[] blockBytes = ba.loadBlock(block);
							try {
								int copyLen = (int) blocklen;
								if (first) {
									copyLen -= oldMod;
									first = false;
								}
								int newAppended = appended + copyLen;
								if (newAppended > len) {
									copyLen = len - appended;
									newAppended = appended + copyLen;
								}
								System.arraycopy(data, dataOff + appended, blockBytes, 0, copyLen);
								appended = newAppended;
							} finally {
								// free blocks in finally, so it is ensured, that all in here used blocks are free after the method ended
								ba.saveBlock(blockBytes, block);
							}
						}
					}
					assert appended == len : "appended=" + appended + " len=" + len + " oldsize=" + oldSize + " newsize=" + newSize + " oldMod=" + oldMod + " addBlockCnt=" + addBlockCnt
						+ " newBlocks.len=" + newBlocks.length + " newBlocks=" + Arrays.deepToString(newBlocks);
					intToByteArr(endPNTR, bl, offset + FILE_DATA_TABLE_END_PNTR_OFFSET);
					longToByteArr(newSize, bl, offset + FILE_SIZE_OFFSET);
				} finally {
					ba.saveBlock(dataTableBytes, dataTableBlock);
				}
			} finally {
				ba.saveBlock(bl, block);
			}
		}
		
		public void remove(long rem) throws IOException {
			byte[] bl = ba.loadBlock(block);
			try {
				final long oldsize = byteArrToLong(bl, offset + FILE_SIZE_OFFSET);
				final long newsize = oldsize - rem;
				if (newsize < 0) {
					throw new IllegalArgumentException("can not remove more bytes than I have! mysize=" + oldsize + " remove=" + rem + " notNewSize=" + newsize);
				}
				longToByteArr(newsize, bl, offset + FILE_SIZE_OFFSET);
				final int blocksize = bl.length;
				final long oldBlocksCnt = oldsize / blocksize;
				final long newBlocksCnt = newsize / blocksize;
				if (oldBlocksCnt == newBlocksCnt) {
					return;// don't need to free any blocks
				}
				final int oldTableEnd = byteArrToInt(bl, offset + FILE_DATA_TABLE_END_PNTR_OFFSET);
				final long tableBlock = byteArrToLong(bl, offset + FILE_DATA_TABLE_BLOCK_OFFSET);
				final long remBlockCnt = oldBlocksCnt - newBlocksCnt;
				long removedBlocks = 0;
				byte[] table = ba.loadBlock(1L);
				try {
					byte[] myTable = ba.loadBlock(tableBlock);
					try {
						for (int off = oldTableEnd - 16; removedBlocks < remBlockCnt; off -= 16) {
							final long start = byteArrToLong(myTable, off);
							final long end = byteArrToLong(myTable, off + 8);
							final long dif = end - start;
							long newRemovedBlocks = removedBlocks + dif;
							if (newRemovedBlocks > remBlockCnt) {
								final long newDif = remBlockCnt - removedBlocks;
								final long newEnd = start + newDif;
								longToByteArr(newEnd, bl, off + 8);
								newRemovedBlocks = removedBlocks + newDif;
								removeBlocksFromTable(table, newEnd, end);
							} else {
								removeBlocksFromTable(table, start, end);
							}
							removedBlocks = newRemovedBlocks;
						}
						assert remBlockCnt == removedBlocks;
					} finally {
						ba.saveBlock(myTable, tableBlock);
					}
				} finally {
					ba.saveBlock(table, 1L);
				}
			} finally {
				ba.saveBlock(bl, block);
			}
		}
	}
	
	public class FolderElement {
		
		public static final int ELEMENT_FLAG_FILE       = 0x00000001;
		public static final int ELEMENT_FLAG_HIDDEN     = 0x00000002;
		public static final int ELEMENT_FLAG_PASSWORD   = 0x00000004;
		public static final int ELEMENT_FLAG_EXECUTABLE = 0x00000008;
		
		private static final int ELEMENT_FLAGS_OFFSET     = 0;
		private static final int ELEMENT_NAME_PNTR_OFFSET = ELEMENT_FLAGS_OFFSET + 4;
		private static final int ELEMENT_BLOCK_OFFSET     = ELEMENT_NAME_PNTR_OFFSET + 4;
		private static final int ELEMENT_POS_OFFSET       = ELEMENT_BLOCK_OFFSET + 8;
		private static final int ELEMENT_SIZE             = ELEMENT_POS_OFFSET + 4;
		
		private long block;
		private int  offset;
		
		
		
		private FolderElement(long block, int offset) {
			this.block = block;
			this.offset = offset;
		}
		
		
		
		public void setName(String name) throws IOException {
			if (name.indexOf('\0') != -1) {
				throw new IllegalArgumentException("name can not contain a '\\0' caracter (this is the ending character)");
			}
			byte[] bl = ba.loadBlock(block);
			try {
				byte[] bytes = name.getBytes(StandardCharsets.UTF_16LE);
				int namePNTR = byteArrToInt(bl, offset + ELEMENT_NAME_PNTR_OFFSET);
				final int byteslength = bytes.length;
				final int oldPNTR = namePNTR;
				namePNTR = resize(bl, byteslength + 2/* '\0' ending in UTF-16 */, namePNTR);
				if (oldPNTR != namePNTR) {
					intToByteArr(namePNTR, bl, offset + ELEMENT_NAME_PNTR_OFFSET);
				}
				System.arraycopy(bytes, 0, bl, namePNTR, byteslength);
				bl[namePNTR + byteslength] = bl[namePNTR + byteslength + 1] = 0;// '\0' ending
			} finally {
				ba.saveBlock(bl, block);
			}
		}
		
		public String getName() throws IOException {
			byte[] bl = ba.loadBlock(block);
			String name;
			try {
				int namePNTR = byteArrToInt(bl, offset + ELEMENT_NAME_PNTR_OFFSET);
				int len;
				for (len = 0; bl[namePNTR + len] != 0 || bl[namePNTR + len + 1] != 0; len += 2) {
					// int b1 = 0xFF & bl[namePNTR + len];
					// int b2 = 0xFF & bl[namePNTR + len + 1];
					// System.out.printf("len=%d, bl[i]=%d, bl[i+1]=%d, char=%c\n", len, b1, b2, (char) (b1 | (b2 << 8)));
				}
				name = new String(bl, namePNTR, len, StandardCharsets.UTF_16LE);
			} finally {
				ba.unloadBlock(block);
			}
			return name;
		}
		
		public int getFlags() throws IOException {
			byte[] bl = ba.loadBlock(block);
			int flags;
			try {
				flags = byteArrToInt(bl, offset + ELEMENT_FLAGS_OFFSET);
			} finally {
				ba.unloadBlock(block);
			}
			return flags;
		}
		
		public void setFlags(int newflags) throws IOException {
			byte[] bl = ba.loadBlock(block);
			try {
				int off = offset + ELEMENT_FLAGS_OFFSET;
				int oldflags = byteArrToInt(bl, off);
				if ( ( (oldflags ^ newflags) & ELEMENT_FLAG_FILE) != 0) {
					throw new AssertionError("change of file/folder flag is not allowed: myflags=0x" + Integer.toHexString(oldflags) + " notNewFlags=0x" + Integer.toHexString(newflags));
				}
				intToByteArr(newflags, bl, off);
			} finally {
				ba.saveBlock(bl, block);
			}
		}
		
		public PatrFile getFile() throws IOException {
			byte[] bl = ba.loadBlock(block);
			PatrFile f;
			try {
				int flags = byteArrToInt(bl, offset + ELEMENT_FLAGS_OFFSET);
				if ( (flags & ELEMENT_FLAG_FILE) == 0) {
					throw new InternalError("getFile() called, but I have a Folder!");
				}
				long block = byteArrToLong(bl, offset + ELEMENT_BLOCK_OFFSET);
				int off = byteArrToInt(bl, offset + ELEMENT_POS_OFFSET);
				f = new PatrFile(block, off);
			} finally {
				ba.unloadBlock(block);
			}
			return f;
		}
		
		public PatrFolder getFolder() throws IOException {
			byte[] bl = ba.loadBlock(block);
			PatrFolder f;
			try {
				int flags = byteArrToInt(bl, offset + ELEMENT_FLAGS_OFFSET);
				if ( (flags & ELEMENT_FLAG_FILE) != 0) {
					throw new InternalError("getFolder() called, but I have a File!");
				}
				long block = byteArrToLong(bl, offset + ELEMENT_BLOCK_OFFSET);
				int off = byteArrToInt(bl, offset + ELEMENT_POS_OFFSET);
				f = new PatrFolder(block, off);
			} finally {
				ba.unloadBlock(block);
			}
			return f;
		}
		
	}
	
	/**
	 * the only thing that changes is the block-intern memory table, so the memory block has to be copied afterwards is necessary.<br>
	 * 
	 * if {@value newSize} is zero the memory block will be deallocated and removed from the table and {@code -1} will be returned.
	 * 
	 * @param block
	 *            the memory block
	 * @param bytes
	 *            the new value of the pointer
	 * @param PNTR
	 *            the old pointer or -1 if there is no old pointer
	 * @return the new pointer (or {@code -1} if the pointer has been removed from the table (if {@code newSize} is {@code 0}))
	 * @throws OutOfMemoryError
	 *             if there is not enough memory in the block left
	 */
	private int resize(byte[] block, final int newSize, int PNTR) throws OutOfMemoryError {
		final int lastTableEntry = BLOCK_INTERN_TABLE_TABLE_ALLOC_ENTRY_OFFSET;
		final int firstTableEntry = byteArrToInt(block, lastTableEntry);
		int posInTabletable = PNTR == -1 ? -1 : tableSearch(block, PNTR, firstTableEntry);
		final int oldmemory = PNTR == -1 ? 0 : byteArrToInt(block, posInTabletable + 4) - byteArrToInt(block, posInTabletable);
		
		if (PNTR == -1) {
			if (newSize > 0) {
				if (byteArrToInt(block, lastTableEntry - 4) >= firstTableEntry - 8) {
					throw new OutOfMemoryError("not enugh memory for the table");
				}
			} else {
				throw new IllegalArgumentException("can't create a new Pointer without a size greather zero! newSize=" + newSize + " PNTR=" + PNTR + " (-1 stands for: 'make a new one')");
			}
		}
		
		int dif = oldmemory - newSize;
		if (dif > 0) {
			if (newSize == 0) {
				// remove entry and return -1
				PNTR = -1;
				System.arraycopy(block, firstTableEntry + 8, block, firstTableEntry, posInTabletable - firstTableEntry);
			} else {
				intToByteArr(PNTR + newSize, block, posInTabletable + 4);
			}
		} else if (dif < 0) {
			int free, next, prev;
			boolean done = false;
			if (oldmemory == 0) {
				free = 0;
				next = prev = -1;
			} else {
				next = byteArrToInt(block, posInTabletable + 8);
				prev = byteArrToInt(block, posInTabletable - 4);
				free = next - prev;
				if (free >= newSize) {
					done = true;
					if (next - PNTR >= free) {
						final int endPNTR = PNTR + newSize;
						assert PNTR >= prev;
						assert endPNTR <= next;
						intToByteArr(endPNTR, block, posInTabletable + 4);
					} else {
						final int halfSize = newSize >> 1;
						final int startPNTR = ( ( (next - prev) >> 1) + prev) - halfSize;
						final int endPNTR = startPNTR + newSize;
						assert startPNTR >= prev;
						assert endPNTR <= next;
						PNTR = startPNTR;
						intToByteArr(startPNTR, block, posInTabletable);
						intToByteArr(endPNTR, block, posInTabletable + 4);
					}
				}
			}
			if ( !done) {
				FIND_MEM:
				if (free < newSize) {
					final int oldPNTR = PNTR;
					final int oldposInTabletable = posInTabletable;
					for (int i = firstTableEntry + 4; i <= lastTableEntry - 4; i += 8) {
						prev = byteArrToInt(block, i);
						next = byteArrToInt(block, i + 4);
						free = next - prev;
						if (free >= newSize) {
							if (oldPNTR != -1) {
								int posInTabletabledif = oldPNTR - PNTR;
								if (posInTabletabledif > 0) {
									System.arraycopy(block, oldposInTabletable + 8, block, oldposInTabletable, posInTabletabledif);
								} else {
									System.arraycopy(block, posInTabletable, block, posInTabletable + 8, -posInTabletabledif);
								}
							} else {
								System.arraycopy(block, firstTableEntry, block, firstTableEntry - 8, i - firstTableEntry + 4);
							}
							posInTabletable = i - 4;
							break FIND_MEM;
						}
					}
					throw new OutOfMemoryError("could not find enugh memory in the needed block!");
				}
				int mid = (next + prev) / 2;
				PNTR = mid - (newSize / 2);
				assert PNTR > 0;
				assert PNTR >= prev : "PNTR=" + PNTR + " prev=" + prev + " next=" + next + " newSize=" + newSize + " free=" + (next - prev) + " mid=" + mid;
				assert (PNTR + newSize) <= next : "PNTR=" + PNTR + " prev=" + prev + " next=" + next + " newSize=" + newSize + " free=" + (next - prev) + " mid=" + mid;
				intToByteArr(PNTR, block, posInTabletable);
				intToByteArr(PNTR + newSize, block, posInTabletable + 4);
				if (oldmemory == 0) {
					intToByteArr(firstTableEntry - 8, block, lastTableEntry);
				}
			}
		} // else /*if (dif == 0)*/ {} //don't need to do anything if nothing has changed
		return PNTR;
	}
	
	/**
	 * @see Arrays#binarySearch(int[], int)
	 */
	private static int tableSearch(final byte[] bl, final int PNTR, final int start) {
		int low = start;
		int high = bl.length - 8;
		
		final int mod = high % 8;
		assert low % 8 == mod : "high=" + high + " low=" + low + " mod=" + mod + " lowMod=" + (low % 8);
		while (low <= high) {
			int mid = (low + high) >> 1;
			
			// {
			// final int midMod = mid % 8;
			// if (midMod != mod) {
			// mid += mod - midMod;
			// }
			// }
			mid = ( ( ~0x00000007) & mid) | mod;
			
			int midNum = byteArrToInt(bl, mid);
			if (midNum < PNTR) {
				low = mid + 8;
			} else if (midNum > PNTR) {
				high = mid - 8;
			} else {
				return mid;
			}
			// //(signed bytes, so this whould need '0xFF &' before the compares)
			// if (bl[mid] < bytes[0] || (bl[mid] == bytes[0] && (bl[mid + 1] < bytes[1] || (bl[mid + 1] == bytes[1]
			// && (bl[mid + 2] < bytes[2] || (bl[mid + 2] == bytes[2]
			// && (bl[mid + 3] < bytes[3]))))))) {
			// low = mid + 8;// a complete entry has 4 startPNTR bytes and 4 endPNTR bytes
			// } else if (bl[mid] > bytes[0] || (bl[mid] == bytes[0] && (bl[mid + 1] > bytes[1] || (bl[mid + 1] == bytes[1]
			// && (bl[mid + 2] > bytes[2] || (bl[mid + 2] == bytes[2] && (bl[mid + 3] > bytes[3]))))))) {
			// high = mid - 8;
			// } else {
			// return mid; // PNTR found
			// }
		}
		System.err.println("error in table search:");
		final String msg = "could not find PNTR in the table! PNTR=" + PNTR + " high=" + high + " low=" + low + " start=" + start;
		System.err.println(msg);
		for (int i = 0; i < bl.length; i ++ ) {
			String byteHexNum = Integer.toHexString(0xFF & (int) bl[i]);
			String iHexNum = Integer.toHexString(i);
			System.err.print("block[" + iHexNum + "]=0x" + ( (byteHexNum.length() == 1) ? "0" : "") + byteHexNum);
			if ( (i & 3) == 0) {
				int int32 = byteArrToInt(bl, i);
				System.err.println("   int32=" + int32 + " == 0x" + Integer.toHexString(int32));
			} else {
				System.err.println();
			}
		}
		throw new InternalError(msg);
	}
	
	private static LongLongOpenImpl[] addBlocksToTable(byte[] table, long need) {
		List <LongLongOpenImpl> lls = new ArrayList <>();
		final int tableEndPNTR = byteArrToInt(table, SECOND_BLOCK_LAST_ENTRY_OFFSET);
		final long blockCnt = byteArrToLong(table, SECOND_BLOCK_BLOCK_COUNT_OFFSET);
		for (int i = SECOND_BLOCK_TABLE_OFFSET + 8;; i += 16) {
			final boolean stop = i > tableEndPNTR;
			final long prev = byteArrToLong(table, i);
			final long next;
			if (stop) {
				next = blockCnt;
			} else {
				next = byteArrToLong(table, i + 8);
			}
			final long free = next - prev;
			final long use;
			final long myStart;
			final long myEnd;
			if (free >= need) {
				use = need;
				myStart = prev;
				myEnd = prev + use;
				longToByteArr(myEnd, table, i);
			} else {
				use = free;
				myStart = prev;
				myEnd = next;
				i -= 16;
				System.arraycopy(table, i + 8, table, i - 8, tableEndPNTR - i);
			}
			lls.add(new LongLongOpenImpl(myStart, myEnd));
			need -= use;
			if (need <= 0) {
				assert need == 0;
				break;
			}
			if (stop) {
				throw new OutOfMemoryError("could not allocate enugh memory-blocks");
			}
		}
		return lls.toArray(new LongLongOpenImpl[lls.size()]);
	}
	
	private static void removeBlocksFromTable(byte[] table, final long from, final long to) {
		final int tableEnd = byteArrToInt(table, SECOND_BLOCK_LAST_ENTRY_OFFSET);
		int pos = blockTableSearch(table, from, tableEnd);
		final long oldStart = byteArrToLong(table, pos);
		final long oldEnd = byteArrToLong(table, pos + 8);
		if (oldStart == from) {
			if (oldEnd == to) {
				intToByteArr(tableEnd - 16, table, SECOND_BLOCK_LAST_ENTRY_OFFSET);
				System.arraycopy(table, pos + 16, table, pos, tableEnd - pos - 16);
			} else {
				longToByteArr(to, table, pos);
			}
		} else {
			if (oldEnd == to) {
				longToByteArr(from, table, pos + 8);
			} else {
				intToByteArr(tableEnd + 16, table, SECOND_BLOCK_LAST_ENTRY_OFFSET);
				System.arraycopy(table, pos, table, pos + 16, tableEnd - pos - 16);
				// longToByteArr(oldStart, table, pos);//not needed (untouched by array-copy)
				longToByteArr(from, table, pos + 8);
				longToByteArr(to, table, pos + 16);
				// longToByteArr(oldEnd, table, pos + 20);//not needed (coped by array-copy)
			}
		}
	}
	
	/**
	 * @see Arrays#binarySearch(int[], int)
	 */
	private static int blockTableSearch(byte[] table, long block, final int end) {
		int low = SECOND_BLOCK_TABLE_OFFSET;
		int high = end;
		
		byte[] bytes = longToByteArr(block);
		int mod = high % 16;
		assert low % 16 == mod;
		while (low <= high) {
			int mid = (low + high) >> 1;
			
			{
				int midMod = mid % 16;
				if (midMod != mod) {
					mid += mod - midMod;
				}
			}
			
			//@formatter:off
			if (table[mid] < bytes[0] || (table[mid] == bytes[0] && (table[mid + 1] < bytes[1] || (table[mid + 1] == bytes[1]
			&& (table[mid + 2] < bytes[2] || (table[mid + 2] == bytes[2] && (table[mid + 3] < bytes[3])
			|| (table[mid + 4] < bytes[4] || (table[mid + 4] == bytes[4] && (table[mid + 5] < bytes[5] || (table[mid + 5] == bytes[5]
			&& (table[mid + 6] < bytes[6] || (table[mid + 6] == bytes[6] && (table[mid + 7] < bytes[7]) ) ) ) ) ) ) ) ) ) ) ) ) {
				low = mid + 16;
			} else if (table[mid] > bytes[0] || (table[mid] == bytes[0] && (table[mid + 1] < bytes[1] || (table[mid + 1] == bytes[1]
			&& (table[mid + 2] > bytes[2] || (table[mid + 2] == bytes[2] && (table[mid + 3] > bytes[3])
			|| (table[mid + 4] > bytes[4] || (table[mid + 4] == bytes[4] && (table[mid + 5] > bytes[5] || (table[mid + 5] == bytes[5]
			&& (table[mid + 6] > bytes[6] || (table[mid + 6] == bytes[6] && (table[mid + 7] > bytes[7]) ) ) ) ) ) ) ) ) ) ) ) ) {
				high = mid - 16;
			} else {
				return mid; // PNTR found
			}
			//@formatter:on
		}
		// high < low
		long highValue = byteArrToLong(table, high);
		if (highValue < block) {
			long highValue2 = byteArrToLong(table, high + 8);
			if (highValue2 > block) {
				return high;
			}
		}
		long lowValue = byteArrToLong(table, low);
		if (lowValue < block) {
			long lowValue2 = byteArrToLong(table, low + 8);
			if (lowValue2 > block) {
				if (low > end) {
					throw new AssertionError("not in bounds");
				}
				return low;
			}
		}
		System.err.println("[ERROR]: error in table search:");
		final String msg = "could not find block in the table! PNTR=" + block + " high=" + high + " low=" + low + " end=" + end;
		System.err.println("[ERROR]: " + msg);
		System.err.println("[ERROR]: table/block:");
		for (int i = 0; i < table.length; i ++ ) {
			String hs = Integer.toHexString(0xFF & (int) table[i]);
			System.err.print("[ERROR]: table/block[" + i + "]=" + (hs.length() == 1 ? ("0" + hs) : hs));
			if ( (i & 3) == 0) {
				int int32 = byteArrToInt(table, i);
				System.err.println("   int32=" + int32 + " == 0x" + Integer.toHexString(int32));
			} else {
				System.err.println();
			}
		}
		throw new InternalError(msg);
	}
	
	private static void initBlock(byte[] block, int... firstEntrys) throws OutOfMemoryError {
		final int blocklen = block.length;
		intToByteArr(blocklen, block, blocklen - 4);
		final int firstEntry = blocklen - (8 * (1 + firstEntrys.length));
		intToByteArr(firstEntry, block, blocklen - 8);
		for (int off = firstEntry, i = 0, pos = 0; i < firstEntrys.length; i ++ , off += 4/* eigentlich + 8 */) {
			intToByteArr(pos, block, off);
			pos += firstEntrys[i];
			off += 4;
			intToByteArr(pos, block, off);
		}
	}
	
}

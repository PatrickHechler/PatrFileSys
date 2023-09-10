package de.hechler.patrick.zeugs.pfs.interfaces;

import java.io.IOException;
import java.util.UUID;

public interface Mount extends Folder {
	
	/**
	 * returns the maximum block count of the file system
	 * 
	 * @return the maximum block count of the file system
	 * 
	 * @throws IOException if an IO error occurs
	 */
	long blockCount() throws IOException;
	
	/**
	 * returns the block size of the file system
	 * 
	 * @return the block size of the file system
	 * 
	 * @throws IOException if an IO error occurs
	 */
	int blockSize() throws IOException;
	
	/**
	 * returns the {@link UUID} of the file system
	 * 
	 * @return the {@link UUID} of the file system
	 * 
	 * @throws IOException if an IO error occurs
	 */
	UUID uuid() throws IOException;
	
	/**
	 * sets the {@link UUID} of the file system
	 * 
	 * @param uuid the new {@link UUID} of the file system
	 * 
	 * @throws IOException if an IO error occurs
	 */
	void uuid(UUID uuid) throws IOException;
	
	/**
	 * returns the name of this mount points file system
	 * 
	 * @return the name of this mount points file system
	 * 
	 * @throws IOException if an IO error occurs
	 */
	String fsName() throws IOException;
	
	/**
	 * returns <code>true</code> if no write operations are allowed and <code>false</code> if write access is permitted
	 * 
	 * @return <code>true</code> if no write operations are allowed and <code>false</code> if write access is permitted
	 * 
	 * @throws IOException if an IO error occurs
	 */
	boolean readOnly() throws IOException;
	
	/**
	 * returns the {@link MountType type} of this mount point
	 * 
	 * @return the {@link MountType type} of this mount point
	 * 
	 * @throws IOException if an IO error occurs
	 */
	MountType mountType() throws IOException;
	
	/**
	 * there are three mount types:
	 * <ul>
	 * <li>intern mounts
	 * <ul>
	 * <li>an invisible intern file is backing the data of the file system</li>
	 * </ul>
	 * </li>
	 * <li>temporary mounts
	 * <ul>
	 * <li>the file system is only stored in memory and created every time the mount point is loaded</li>
	 * </ul>
	 * </li>
	 * <li>extern mounts
	 * <ul>
	 * <li>an extern file from the Linux file system is backing the data of the file system</li>
	 * </ul>
	 * </li>
	 * <li>root file system
	 * <ul>
	 * <li>it is unknown how the root file system is saved (or if it is permanently saved)</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * 
	 * @author Patrick Hechler
	 */
	enum MountType {
		
		/**
		 * an invisible intern file is backing the data of the file system
		 */
		INTERN_FILE_SYSTEM,
		/**
		 * the file system is only stored in memory and created every time the mount point is loaded
		 */
		TEMP_FILE_SYSTEM,
		/**
		 * an extern file from the Linux file system is backing the data of the file system
		 */
		REL_FS_BACKED_FILE_SYSTEM,
		/**
		 * it is unknown how the root file system is saved (or if it is permanently saved)
		 */
		ROOT_FILE_SYSTEM,
		
	}
	
}

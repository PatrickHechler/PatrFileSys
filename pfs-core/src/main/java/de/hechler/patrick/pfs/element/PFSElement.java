package de.hechler.patrick.pfs.element;

import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry.FLAGS_FILE;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry.FLAGS_FILE_ENCRYPTED;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry.FLAGS_FILE_EXECUTABLE;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry.FLAGS_FOLDER;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry.FLAGS_HIDDEN;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry.FLAGS_PIPE;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry.UNMODIFIABLE_FLAGS;

import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.file.PFSFile;
import de.hechler.patrick.pfs.folder.PFSFolder;
import de.hechler.patrick.pfs.pipe.PFSPipe;

public interface PFSElement {
	
	/**
	 * get the flags of the {@link PFSElement}
	 * <p>
	 * note that the root folder has no flags
	 * 
	 * @return the flags of the {@link PFSElement}
	 * @throws PatrFileSysException
	 *             if an error occurs
	 */
	int flags() throws PatrFileSysException;
	
	/**
	 * modify the flags of the {@link PFSElement}
	 * <p>
	 * note that {@code addFlags} and {@code remFlags} are not allowed to contain common bits
	 * (<code>addFlags & remFlags</code> must be {@code 0})<br>
	 * also {@code addFlags} and {@code remFlags} are not allowed to contain
	 * {@link #PFS_UNMODIFIABLE_FLAGS unmodifiable} bits
	 * (<code>(addFlags | remFlags) & {@link #PFS_UNMODIFIABLE_FLAGS}</code> must be {@code 0})<br>
	 * <p>
	 * note that the root folder has no flags
	 * 
	 * @param addFlags
	 *            the flags to add to this {@link PFSElement}
	 * @param remFlags
	 *            the flags to remove from this {@link PFSElement}
	 * @throws PatrFileSysException
	 *             if an error occurs
	 */
	void modifyFlags(int addFlags, int remFlags) throws PatrFileSysException;
	
	/**
	 * returns the name of this element
	 * <p>
	 * note that the root folder has no name
	 * 
	 * @return the name of this element
	 * @throws PatrFileSysException
	 *             if an error occurs
	 */
	String name() throws PatrFileSysException;
	
	/**
	 * changes the name of the element
	 * <p>
	 * note that the root folder has no name
	 * 
	 * @param newName
	 *            the new name of this element
	 * @throws PatrFileSysException
	 *             if an error occurs
	 */
	void name(String newName) throws PatrFileSysException;
	
	/**
	 * returns the create time of this element
	 * <p>
	 * note that the root folder has no create time
	 * <p>
	 * note that times are saved in seconds since the epoch
	 * 
	 * @return the create time of this element
	 * @throws PatrFileSysException
	 *             if an error occurs
	 */
	long createTime() throws PatrFileSysException;
	
	/**
	 * sets the create time of this element
	 * <p>
	 * note that the root folder has no create time
	 * <p>
	 * note that times are saved in seconds since the epoch
	 * 
	 * @param ct
	 *            the new create time of this element
	 * @throws PatrFileSysException
	 *             if an error occurs
	 */
	void createTime(long ct) throws PatrFileSysException;
	
	/**
	 * returns the last modify time of this element
	 * <p>
	 * note that times are saved in seconds since the epoch
	 * 
	 * @return the last modify time of this element
	 * @throws PatrFileSysException
	 *             if an error occurs
	 */
	long lastModTime() throws PatrFileSysException;
	
	/**
	 * sets the last modify time of this element
	 * <p>
	 * note that times are saved in seconds since the epoch
	 * 
	 * @param lmt
	 *            the new last modify time of this element
	 * @throws PatrFileSysException
	 *             if an error occurs
	 */
	void lastModTime(long lmt) throws PatrFileSysException;
	
	/**
	 * deletes the element
	 * <p>
	 * note that if this element is a {@link #isFolder() folder} it needs to be empty
	 * ({@link PFSFolder#childCount()} has to be {@code 0})
	 * <p>
	 * note that the root folder can not be deleted
	 * 
	 * @throws PatrFileSysException
	 *             if an error occurs
	 */
	void delete() throws PatrFileSysException;
	
	/**
	 * returns the parent element of this element
	 * @return
	 * @throws PatrFileSysException
	 */
	PFSFolder parent() throws PatrFileSysException;
	
	void parent(PFSFolder newParent) throws PatrFileSysException;
	
	void move(PFSFolder newParent, String newName) throws PatrFileSysException;
	
	PFSFolder toFolder() throws IllegalStateException;
	
	PFSFile toFile() throws IllegalStateException;
	
	PFSPipe toPipe() throws IllegalStateException;
	
	boolean isFolder();
	
	boolean isFile();
	
	boolean isPipe();
	
	boolean isRoot();
	
	static int PFS_UNMODIFIABLE_FLAGS    = UNMODIFIABLE_FLAGS;
	static int PFS_FLAGS_FOLDER          = FLAGS_FOLDER;
	static int PFS_FLAGS_FILE            = FLAGS_FILE;
	static int PFS_FLAGS_PIPE            = FLAGS_PIPE;
	static int PFS_FLAGS_FILE_EXECUTABLE = FLAGS_FILE_EXECUTABLE;
	static int PFS_FLAGS_FILE_ENCRYPTED  = FLAGS_FILE_ENCRYPTED;
	static int PFS_FLAGS_HIDDEN          = FLAGS_HIDDEN;
	
}

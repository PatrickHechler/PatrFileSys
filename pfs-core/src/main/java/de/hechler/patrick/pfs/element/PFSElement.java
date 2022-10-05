package de.hechler.patrick.pfs.element;

import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry.Flags.FILE;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry.Flags.FILE_ENCRYPTED;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry.Flags.FILE_EXECUTABLE;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry.Flags.FOLDER;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry.Flags.HIDDEN;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry.Flags.PIPE;
import static de.hechler.patrick.pfs.other.PatrFileSysConstants.Element.Folder.Entry.Flags.UNMODIFIABLE;

import java.io.IOException;

import de.hechler.patrick.pfs.exceptions.PatrFileSysIOException;
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
	 * @throws PatrFileSysIOException,
	 *             PatrFileSysClosedChannelException if an error occurs
	 */
	int flags() throws IOException;
	
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
	 * @throws PatrFileSysIOException,
	 *             PatrFileSysClosedChannelException if an error occurs
	 */
	void modifyFlags(int addFlags, int remFlags) throws IOException;
	
	/**
	 * returns the name of this element
	 * <p>
	 * note that the root folder has no name
	 * 
	 * @return the name of this element
	 * @throws PatrFileSysIOException,
	 *             PatrFileSysClosedChannelException if an error occurs
	 */
	String name() throws IOException;
	
	/**
	 * changes the name of the element
	 * <p>
	 * note that the root folder has no name
	 * 
	 * @param newName
	 *            the new name of this element
	 * @throws PatrFileSysIOException,
	 *             PatrFileSysClosedChannelException if an error occurs
	 */
	void name(String newName) throws IOException;
	
	/**
	 * returns the create time of this element
	 * <p>
	 * note that the root folder has no create time
	 * <p>
	 * note that times are saved in seconds since the epoch
	 * 
	 * @return the create time of this element
	 * @throws PatrFileSysIOException,
	 *             PatrFileSysClosedChannelException if an error occurs
	 */
	long createTime() throws IOException;
	
	/**
	 * sets the create time of this element
	 * <p>
	 * note that the root folder has no create time
	 * <p>
	 * note that times are saved in seconds since the epoch
	 * 
	 * @param ct
	 *            the new create time of this element
	 * @throws PatrFileSysIOException,
	 *             PatrFileSysClosedChannelException if an error occurs
	 */
	void createTime(long ct) throws IOException;
	
	/**
	 * returns the last modify time of this element
	 * <p>
	 * note that times are saved in seconds since the epoch
	 * 
	 * @return the last modify time of this element
	 * @throws PatrFileSysIOException,
	 *             PatrFileSysClosedChannelException if an error occurs
	 */
	long lastModTime() throws IOException;
	
	/**
	 * sets the last modify time of this element
	 * <p>
	 * note that times are saved in seconds since the epoch
	 * 
	 * @param lmt
	 *            the new last modify time of this element
	 * @throws PatrFileSysIOException,
	 *             PatrFileSysClosedChannelException if an error occurs
	 */
	void lastModTime(long lmt) throws IOException;
	
	/**
	 * deletes the element
	 * <p>
	 * note that if this element is a {@link #isFolder() folder} it needs to be empty
	 * ({@link PFSFolder#childCount()} has to be {@code 0})
	 * <p>
	 * note that the root folder can not be deleted
	 * 
	 * @throws PatrFileSysIOException,
	 *             PatrFileSysClosedChannelException if an error occurs
	 */
	void delete() throws IOException;
	
	/**
	 * returns the parent element of this element
	 * 
	 * @return
	 * @throws PatrFileSysIOException,
	 *             PatrFileSysClosedChannelException
	 */
	PFSFolder parent() throws IOException;
	
	void parent(PFSFolder newParent) throws IOException;
	
	void move(PFSFolder newParent, String newName) throws IOException;
	
	PFSFolder toFolder() throws IllegalStateException;
	
	PFSFile toFile() throws IllegalStateException;
	
	PFSPipe toPipe() throws IllegalStateException;
	
	boolean isFolder();
	
	boolean isFile();
	
	boolean isPipe();
	
	boolean isRoot();
	
	static int PFS_UNMODIFIABLE_FLAGS    = UNMODIFIABLE;
	static int PFS_FLAGS_FOLDER          = FOLDER;
	static int PFS_FLAGS_FILE            = FILE;
	static int PFS_FLAGS_PIPE            = PIPE;
	static int PFS_FLAGS_FILE_EXECUTABLE = FILE_EXECUTABLE;
	static int PFS_FLAGS_FILE_ENCRYPTED  = FILE_ENCRYPTED;
	static int PFS_FLAGS_HIDDEN          = HIDDEN;
	
}

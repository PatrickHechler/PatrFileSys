package de.hechler.patrick.zeugs.pfs.misc;

import de.hechler.patrick.zeugs.pfs.interfaces.FSElement;
import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Folder;
import de.hechler.patrick.zeugs.pfs.interfaces.Pipe;

/**
 * this enumeration enumerates, the different types an {@link FSElement} can
 * have
 * 
 * @author pat
 * 
 * @see FSElement
 */
public enum ElementType {
	
	/**
	 * folder elements
	 * 
	 * @see Folder
	 */
	folder,
	/**
	 * file elements
	 * 
	 * @see File
	 */
	file,
	/**
	 * pipe elements
	 * 
	 * @see Pipe
	 */
	pipe
	
}

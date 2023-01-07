package de.hechler.patrick.zeugs.pfs.interfaces;

import de.hechler.patrick.zeugs.pfs.FSProvider;

/**
 * this interface is used to mark File System Open Options.
 * <p>
 * since it is very implementation specific, how to open/create a file system
 * this interface provides no common functions.<br>
 * each {@link FSProvider} should document, which implementations of this
 * interface are supported
 * 
 * @author pat
 * 
 * @see FSProvider#loadFS(FSOptions)
 */
public interface FSOptions {
	
}

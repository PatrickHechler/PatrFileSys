package de.hechler.patrick.zeugs.pfs.opts;

import de.hechler.patrick.zeugs.pfs.interfaces.FS;
import de.hechler.patrick.zeugs.pfs.interfaces.FSOptions;

/**
 * the {@link PatrRamFSOpts} provides options to create a new patr file system
 * which will be stored in the computers ram. after {@link FS#close()} is called
 * of the ram file system all stored data will be lost
 * 
 * @param blockCount the number of blocks, which can be used by the file system
 * @param blockSize  the size of the blocks, which can be used by the file
 *                   system
 * 
 * @author pat
 */
public record PatrRamFSOpts(long blockCount, int blockSize) implements FSOptions {
	
}

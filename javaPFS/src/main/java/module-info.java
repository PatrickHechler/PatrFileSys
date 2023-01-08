import de.hechler.patrick.zeugs.pfs.FSProvider;

/**
 * this module provides the {@link FSProvider} class and two implementations for
 * it.
 * <ul>
 * <li>the {@link FSProvider}, which wraps a nio File System</li>
 * <li>the {@link FSProvider}, which uses the linux only native api for the
 * PatrFileSystem</li>
 * </ul>
 * 
 * @author pat
 *
 * @provides FSProvider with a wrapper impl around the linux only native PatrFS
 *           impl and a wrapper around a nio File System
 * 			
 * @uses FSProvider as part of the project
 */
module de.hechler.patrick.zeugs.pfs {
	
	requires java.logging;
	
	exports de.hechler.patrick.zeugs.pfs;
	exports de.hechler.patrick.zeugs.pfs.interfaces;
	exports de.hechler.patrick.zeugs.pfs.misc;
	exports de.hechler.patrick.zeugs.pfs.opts;
	
	exports de.hechler.patrick.zeugs.pfs.impl.java to de.hechler.patrick.zeugs.check;
	exports de.hechler.patrick.zeugs.pfs.impl.pfs to de.hechler.patrick.zeugs.check;
	
	opens de.hechler.patrick.zeugs.pfs to de.hechler.patrick.zeugs.check;
	opens de.hechler.patrick.zeugs.pfs.interfaces to de.hechler.patrick.zeugs.check;
	opens de.hechler.patrick.zeugs.pfs.misc to de.hechler.patrick.zeugs.check;
	opens de.hechler.patrick.zeugs.pfs.opts to de.hechler.patrick.zeugs.check;
	opens de.hechler.patrick.zeugs.pfs.impl.pfs to de.hechler.patrick.zeugs.check;
	opens de.hechler.patrick.zeugs.pfs.impl.java to de.hechler.patrick.zeugs.check;
	
	uses FSProvider;
	// my implementations of the FSProvider are done internally
	
}

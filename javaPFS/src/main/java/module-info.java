import de.hechler.patrick.zeugs.pfs.FSProvider;
import de.hechler.patrick.zeugs.pfs.impl.java.JavaFSProvider;
import de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider;

/**
 * this module provides the {@link FSProvider} class and two implementations for
 * it.
 * <ul>
 * <li>the {@link JavaFSProvider}, which wraps a nio File System</li>
 * <li>the {@link PatrFSProvider}, which uses the linux native api for the
 * PatrFileSystem</li>
 * </ul>
 * 
 * @author pat
 *
 * @provides FSProvider with a wrapper impl around the linux native PatrFS impl
 *           and a wrapper around a nio File System
 * 			
 * @uses FSProvider as part of the project
 */
module de.hechler.patrick.zeugs.pfs {
	
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
	
	provides FSProvider with PatrFSProvider, JavaFSProvider;
	
}

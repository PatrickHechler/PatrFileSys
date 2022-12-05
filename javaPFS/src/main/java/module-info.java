//import java.nio.file.spi.FileSystemProvider;

import de.hechler.patrick.zeugs.pfs.FSProvider;
import de.hechler.patrick.zeugs.pfs.impl.PatrFSProvider;
//import de.hechler.patrick.zeugs.pfs.java.PatrFileSystemProvider;

module de.hechler.patrick.zeugs.pfs {

	exports de.hechler.patrick.zeugs.pfs;
	exports de.hechler.patrick.zeugs.pfs.interfaces;
	exports de.hechler.patrick.zeugs.pfs.misc;
	exports de.hechler.patrick.zeugs.pfs.opts;
//	exports de.hechler.patrick.zeugs.pfs.java;

	exports de.hechler.patrick.zeugs.pfs.java.impl to de.hechler.patrick.zeugs.check;
	exports de.hechler.patrick.zeugs.pfs.impl to de.hechler.patrick.zeugs.check;

	opens de.hechler.patrick.zeugs.pfs to de.hechler.patrick.zeugs.check;
	opens de.hechler.patrick.zeugs.pfs.interfaces to de.hechler.patrick.zeugs.check;
	opens de.hechler.patrick.zeugs.pfs.misc to de.hechler.patrick.zeugs.check;
	opens de.hechler.patrick.zeugs.pfs.opts to de.hechler.patrick.zeugs.check;
	opens de.hechler.patrick.zeugs.pfs.java to de.hechler.patrick.zeugs.check;
	opens de.hechler.patrick.zeugs.pfs.java.impl to de.hechler.patrick.zeugs.check;
	opens de.hechler.patrick.zeugs.pfs.impl to de.hechler.patrick.zeugs.check;
	
	uses FSProvider;

//	provides FileSystemProvider with PatrFileSystemProvider;
	provides FSProvider with PatrFSProvider;
}

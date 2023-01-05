import de.hechler.patrick.zeugs.pfs.FSProvider;
import de.hechler.patrick.zeugs.pfs.impl.PatrFSProvider;

module de.hechler.patrick.zeugs.pfs {

	exports de.hechler.patrick.zeugs.pfs;
	exports de.hechler.patrick.zeugs.pfs.interfaces;
	exports de.hechler.patrick.zeugs.pfs.misc;
	exports de.hechler.patrick.zeugs.pfs.opts;
	// for now java package is only visible for testing
	exports de.hechler.patrick.zeugs.pfs.java to de.hechler.patrick.zeugs.check;

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

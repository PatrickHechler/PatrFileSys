import de.hechler.patrick.zeugs.pfs.FSProvider;
import de.hechler.patrick.zeugs.pfs.impl.java.JavaFSProvider;
import de.hechler.patrick.zeugs.pfs.impl.pfs.PatrFSProvider;

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

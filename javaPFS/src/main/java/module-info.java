import java.nio.file.spi.FileSystemProvider;

import de.hechler.patrick.zeugs.pfs.PFSProvider;

module de.hechler.patrick.zeugs.pfs {

	requires jdk.incubator.foreign;
	
	exports de.hechler.patrick.zeugs.pfs;

	provides FileSystemProvider with PFSProvider;

	opens de.hechler.patrick.zeugs.pfs to de.hechler.patrick.zeugs.check;
}

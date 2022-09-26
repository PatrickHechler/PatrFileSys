module de.hechler.patrick.pfs {
	
	uses de.hechler.patrick.pfs.PFSProvider;
	
	provides de.hechler.patrick.pfs.PFSProvider with de.hechler.patrick.pfs.fs.NativePFSProvider;
	
	requires static jdk.incubator.foreign;
	
	exports de.hechler.patrick.pfs;
	exports de.hechler.patrick.pfs.exceptions;
	exports de.hechler.patrick.pfs.bm;
	exports de.hechler.patrick.pfs.bm.impl;
	exports de.hechler.patrick.pfs.element;
	exports de.hechler.patrick.pfs.file;
	exports de.hechler.patrick.pfs.folder;
	exports de.hechler.patrick.pfs.fs;
	exports de.hechler.patrick.pfs.pipe;
	
	exports de.hechler.patrick.pfs.file.impl to de.hechler.patrick.zeugs.check;
	exports de.hechler.patrick.pfs.folder.impl to de.hechler.patrick.zeugs.check;
	exports de.hechler.patrick.pfs.fs.impl to de.hechler.patrick.zeugs.check;
	exports de.hechler.patrick.pfs.pipe.impl to de.hechler.patrick.zeugs.check;
	
	opens de.hechler.patrick.pfs.file.impl to de.hechler.patrick.zeugs.check;
	opens de.hechler.patrick.pfs.folder.impl to de.hechler.patrick.zeugs.check;
	opens de.hechler.patrick.pfs.fs.impl to de.hechler.patrick.zeugs.check;
	opens de.hechler.patrick.pfs.pipe.impl to de.hechler.patrick.zeugs.check;
	
}

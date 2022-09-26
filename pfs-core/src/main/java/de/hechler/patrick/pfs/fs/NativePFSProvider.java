package de.hechler.patrick.pfs.fs;

import de.hechler.patrick.pfs.PFSProvider;
import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.fs.impl.NativePatrFileSys;

public class NativePFSProvider extends PFSProvider {

	public static final String IDENTIFIER = "native";
	
	public NativePFSProvider() {
		super(IDENTIFIER);
	}
	
	@Override
	public PFS loadPFS(String pfsFile) throws PatrFileSysException {
		return NativePatrFileSys.load(pfsFile);
	}
	
	@Override
	public PFS createPFS(String pfsFile, long blockCount, int blockSize) throws PatrFileSysException {
		return NativePatrFileSys.create(pfsFile, blockCount, blockSize);
	}
	
}

package de.hechler.patrick.pfs.exceptions;

import java.nio.file.FileAlreadyExistsException;

public class PatrFileSysFileAlreadyExistsException extends FileAlreadyExistsException implements PFSErr {
	
	/** UID */
	private static final long serialVersionUID = 251986072537732834L;
	
	public final int pfs_errno;
	
	public PatrFileSysFileAlreadyExistsException(int pfs_errno) {
		this(pfs_errno, null);
	}
	
	public PatrFileSysFileAlreadyExistsException(int pfs_errno, String msg) {
		super(msg == null ? PFSErr.tos(pfs_errno) : (msg + ": " + PFSErr.tos(pfs_errno)));
		this.pfs_errno = pfs_errno;
	}
	
	@Override
	public int pfs_errno() {
		return pfs_errno;
	}

}

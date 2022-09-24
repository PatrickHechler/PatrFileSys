package de.hechler.patrick.exceptions;

import java.io.IOException;

public class PatrFileSysException extends IOException implements PFSErr {
	
	/** UID */
	private static final long serialVersionUID = 251986072537732834L;
	
	public final int pfs_errno;
	
	public PatrFileSysException(int pfs_errno) {
		this(pfs_errno, null);
	}
	
	public PatrFileSysException(int pfs_errno, String msg) {
		super(msg == null ? PFSErr.tos(pfs_errno) : (msg + ": " + PFSErr.tos(pfs_errno)));
		this.pfs_errno = pfs_errno;
	}
	
	@Override
	public int pfs_errno() {
		return pfs_errno;
	}
	
}

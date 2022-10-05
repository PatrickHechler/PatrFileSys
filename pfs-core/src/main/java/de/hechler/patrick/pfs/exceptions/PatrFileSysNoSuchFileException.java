package de.hechler.patrick.pfs.exceptions;

import java.nio.channels.ClosedChannelException;

public class PatrFileSysNoSuchFileException extends ClosedChannelException implements PFSErr {
	
	/** UID */
	private static final long serialVersionUID = 251986072537732834L;
	
	public final int pfs_errno;
	
	public PatrFileSysNoSuchFileException(int pfs_errno) {
		this(pfs_errno, null);
	}
	
	public PatrFileSysNoSuchFileException(int pfs_errno, String msg) {
		super();
//		super(msg == null ? PFSErr.tos(pfs_errno) : (msg + ": " + PFSErr.tos(pfs_errno)));
		this.pfs_errno = pfs_errno;
	}
	
	@Override
	public int pfs_errno() {
		return pfs_errno;
	}

}

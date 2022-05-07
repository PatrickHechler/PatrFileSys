package de.hechler.patrick.pfs.exception;

import java.nio.file.AccessDeniedException;

public class ElementLockedException extends AccessDeniedException {
	
	/** UID */
	private static final long serialVersionUID = 4394717476317744164L;
	
	public ElementLockedException(String file, String other, String reason) {
		super(file, other, reason);
	}
	
	public ElementLockedException(String file, String reason) {
		super(file, null, reason);
	}
	
}

package de.hechler.patrick.pfs.exception;


public class ElementReadOnlyException extends ElementLockedException {
	
	/** UID */
	private static final long serialVersionUID = 3970906607016770072L;
	
	public ElementReadOnlyException(String file, String other, String reason) {
		super(file, other, reason);
	}
	
	public ElementReadOnlyException(String file, String reason) {
		super(file, reason);
	}
	
}

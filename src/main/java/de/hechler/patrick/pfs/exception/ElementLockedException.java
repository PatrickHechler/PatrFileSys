package de.hechler.patrick.pfs.exception;

import java.io.IOException;

public class ElementLockedException extends IOException {
	
	/** UID */
	private static final long serialVersionUID = 4394717476317744164L;
	
	public ElementLockedException() {
		super("the element is locked!");
	}
	
	public ElementLockedException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ElementLockedException(String message) {
		super(message);
	}
	
	public ElementLockedException(Throwable cause) {
		super(cause);
	}
	
}

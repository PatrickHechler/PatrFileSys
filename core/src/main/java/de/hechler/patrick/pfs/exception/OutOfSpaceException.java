package de.hechler.patrick.pfs.exception;

import java.io.IOException;

/**
 * thrown to indicate that there is not enough space.
 * 
 * @author Patrick
 *
 */
public class OutOfSpaceException extends IOException {

	/** UID */
	private static final long serialVersionUID = -1685910883667750230L;

	public OutOfSpaceException(String message, Throwable cause) {
		super(message, cause);
	}

	public OutOfSpaceException(String message) {
		super(message);
	}

	public OutOfSpaceException(Throwable cause) {
		super(cause);
	}

}

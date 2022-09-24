package de.hechler.patrick.exceptions;


public class PatrFileSysException extends Exception {
	
	/** if pfs_errno is not set/no error occurred */
	public static final int PFS_ERRNO_NONE                  = 0;
	/** if an operation failed because of an unknown/unspecified error */
	public static final int PFS_ERRNO_UNKNOWN_ERROR         = 1;
	/** if the iterator has no next element */
	public static final int PFS_ERRNO_NO_MORE_ELEMNETS      = 2;
	/** if an IO operation failed because the element is not of the correct type (file expected, but folder or reverse) */
	public static final int PFS_ERRNO_ELEMENT_WRONG_TYPE    = 3;
	/** if an IO operation failed because the element does not exist */
	public static final int PFS_ERRNO_ELEMENT_NOT_EXIST     = 4;
	/** if an IO operation failed because the element already existed */
	public static final int PFS_ERRNO_ELEMENT_ALREADY_EXIST = 5;
	/** if an IO operation failed because there was not enough space in the file system */
	public static final int PFS_ERRNO_OUT_OF_SPACE          = 6;
	/** if an unspecified IO error occurred */
	public static final int PFS_ERRNO_IO_ERR                = 7;
	/** if there was at least one invalid argument */
	public static final int PFS_ERRNO_ILLEGAL_ARG           = 8;
	/** if an IO operation failed because there was not enough space in the file system */
	public static final int PFS_ERRNO_OUT_OF_MEMORY         = 9;
	/** if an IO operation failed because the root folder has some restrictions */
	public static final int PFS_ERRNO_ROOT_FOLDER           = 10;
	/** if an folder can not be moved because the new child (maybe a deep/indirect child) is a child of the folder */
	public static final int PFS_ERRNO_PARENT_IS_CHILD       = 11;
	
	/** UID */
	private static final long serialVersionUID = 251986072537732834L;
	
	public final int errno;
	
	public PatrFileSysException(int errno) {
		this(tos(errno), errno);
	}
	
	public PatrFileSysException(String msg, int errno) {
		super(msg == null ? tos(errno) : (msg + ": " + tos(errno)));
		this.errno = errno;
	}
	
	public static String tos(int errno) {
		switch (errno) {
		case PFS_ERRNO_NONE:
			return "no error/success";
		case PFS_ERRNO_UNKNOWN_ERROR:
			return "some unknown/unspecified error";
		case PFS_ERRNO_NO_MORE_ELEMNETS:
			return "no more elements";
		case PFS_ERRNO_ELEMENT_WRONG_TYPE:
			return "wrong type";
		case PFS_ERRNO_ELEMENT_NOT_EXIST:
			return "element does not exist";
		case PFS_ERRNO_ELEMENT_ALREADY_EXIST:
			return "element exists already";
		case PFS_ERRNO_OUT_OF_SPACE:
			return "out of space";
		case PFS_ERRNO_IO_ERR:
			return "some IO error";
		case PFS_ERRNO_ILLEGAL_ARG:
			return "Illegal argument(s)";
		case PFS_ERRNO_OUT_OF_MEMORY:
			return "out of ram";
		case PFS_ERRNO_ROOT_FOLDER:
			return "root folder restrictions";
		case PFS_ERRNO_PARENT_IS_CHILD:
			return "new parent folder is a child folder";
		default:
			return "unknown errno value: <0x" + Integer.toHexString(errno) + ">";
		}
	}
	
	
}

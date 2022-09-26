package de.hechler.patrick.pfs.exceptions;

import static de.hechler.patrick.pfs.fs.impl.NativePatrFileSysDefines.Errno.*;

public interface PFSErr {
	
	int pfs_errno();
	
	static String tos(int pfs_errno) {
		switch (pfs_errno) {
		case NONE:
			return "no error/success";
		case UNKNOWN_ERROR:
			return "some unknown/unspecified error";
		case NO_MORE_ELEMNETS:
			return "no more elements";
		case ELEMENT_WRONG_TYPE:
			return "wrong type";
		case ELEMENT_NOT_EXIST:
			return "element does not exist";
		case ELEMENT_ALREADY_EXIST:
			return "element exists already";
		case OUT_OF_SPACE:
			return "out of space";
		case IO_ERR:
			return "some IO error";
		case ILLEGAL_ARG:
			return "Illegal argument(s)";
		case OUT_OF_MEMORY:
			return "out of ram";
		case ROOT_FOLDER:
			return "root folder restrictions";
		case PARENT_IS_CHILD:
			return "new parent folder is a child folder";
		case PFS_ERR_CLOSED:
			return "file system is closed";
		default:
			return "unknown errno value: <0x" + Integer.toHexString(pfs_errno) + ">";
		}
	}
	
	static Exception create(int pfs_errno, String msg) {
		switch (pfs_errno) {
		default:
			return new PatrFileSysException(pfs_errno, msg);
		}
	}
	
	/** if pfs_errno is not set/no error occurred */
	static final int PFS_ERR_NONE                  = NONE;
	/** if an operation failed because of an unknown/unspecified error */
	static final int PFS_ERR_UNKNOWN_ERROR         = UNKNOWN_ERROR;
	/** if the iterator has no next element */
	static final int PFS_ERR_NO_MORE_ELEMNETS      = NO_MORE_ELEMNETS;
	/**
	 * if an IO operation failed because the element is not of the correct type (file expected, but folder or reverse)
	 */
	static final int PFS_ERR_ELEMENT_WRONG_TYPE    = ELEMENT_WRONG_TYPE;
	/** if an IO operation failed because the element does not exist */
	static final int PFS_ERR_ELEMENT_NOT_EXIST     = ELEMENT_NOT_EXIST;
	/** if an IO operation failed because the element already existed */
	static final int PFS_ERR_ELEMENT_ALREADY_EXIST = ELEMENT_ALREADY_EXIST;
	/**
	 * if an IO operation failed because there was not enough space in the file system
	 */
	static final int PFS_ERR_OUT_OF_SPACE          = OUT_OF_SPACE;
	/** if an unspecified IO error occurred */
	static final int PFS_ERR_IO_ERR                = IO_ERR;
	/** if there was at least one invalid argument */
	static final int PFS_ERR_ILLEGAL_ARG           = ILLEGAL_ARG;
	/**
	 * if an IO operation failed because there was not enough space in the file system
	 */
	static final int PFS_ERR_OUT_OF_MEMORY         = OUT_OF_MEMORY;
	/**
	 * if an IO operation failed because the root folder has some restrictions
	 */
	static final int PFS_ERR_ROOT_FOLDER           = ROOT_FOLDER;
	/**
	 * if an folder can not be moved because the new child (maybe a deep/indirect child) is a child of the folder
	 */
	static final int PFS_ERR_PARENT_IS_CHILD       = PARENT_IS_CHILD;
	/**
	 * if the file system is already closed
	 */
	static final int PFS_ERR_CLOSED                = PARENT_IS_CHILD + 1;
	
}

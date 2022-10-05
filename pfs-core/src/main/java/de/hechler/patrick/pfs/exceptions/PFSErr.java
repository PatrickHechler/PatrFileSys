package de.hechler.patrick.pfs.exceptions;

import java.io.IOException;

public interface PFSErr {
	
	/**
	 * if pfs_errno is not set/no error occurred
	 */
	static final int PFS_ERR_NONE                  = 0;
	/**
	 * if an operation failed because of an unknown/unspecified error
	 */
	static final int PFS_ERR_UNKNOWN_ERROR         = 1;
	/**
	 * if the iterator has no next element
	 */
	static final int PFS_ERR_NO_MORE_ELEMNETS      = 2;
	/**
	 * if an IO operation failed because the element is not of the correct type (file expected, but
	 * folder or reverse)
	 */
	static final int PFS_ERR_ELEMENT_WRONG_TYPE    = 3;
	/**
	 * if an IO operation failed because the element does not exist
	 */
	static final int PFS_ERR_ELEMENT_NOT_EXIST     = 4;
	/**
	 * if an IO operation failed because the element already existed
	 */
	static final int PFS_ERR_ELEMENT_ALREADY_EXIST = 5;
	/**
	 * if an IO operation failed because there was not enough space in the file system
	 */
	static final int PFS_ERR_OUT_OF_SPACE          = 6;
	/**
	 * if an unspecified IO error occurred
	 */
	static final int PFS_ERR_IO_ERR                = 7;
	/**
	 * if there was at least one invalid argument
	 */
	static final int PFS_ERR_ILLEGAL_ARG           = 8;
	/**
	 * if an IO operation failed because there was not enough space in the file system
	 */
	static final int PFS_ERR_OUT_OF_MEMORY         = 9;
	/**
	 * if an IO operation failed because the root folder has some restrictions
	 */
	static final int PFS_ERR_ROOT_FOLDER           = 10;
	/**
	 * if an folder can not be moved because the new child (maybe a deep/indirect child) is a child
	 * of the folder
	 */
	static final int PFS_ERR_PARENT_IS_CHILD       = 11;
	/**
	 * if the operation failed because the object (for example the file system) is already closed
	 */
	static final int PFS_ERR_CLOSED                = 12;
	
	int pfs_errno();
	
	static String tos(int pfs_errno) {
		switch (pfs_errno) {
		case PFS_ERR_NONE:
			return "no error/success";
		case PFS_ERR_UNKNOWN_ERROR:
			return "some unknown/unspecified error";
		case PFS_ERR_NO_MORE_ELEMNETS:
			return "no more elements";
		case PFS_ERR_ELEMENT_WRONG_TYPE:
			return "wrong type";
		case PFS_ERR_ELEMENT_NOT_EXIST:
			return "element does not exist";
		case PFS_ERR_ELEMENT_ALREADY_EXIST:
			return "element exists already";
		case PFS_ERR_OUT_OF_SPACE:
			return "out of space";
		case PFS_ERR_IO_ERR:
			return "some IO error";
		case PFS_ERR_ILLEGAL_ARG:
			return "Illegal argument(s)";
		case PFS_ERR_OUT_OF_MEMORY:
			return "out of ram";
		case PFS_ERR_ROOT_FOLDER:
			return "root folder restrictions";
		case PFS_ERR_PARENT_IS_CHILD:
			return "new parent folder is a child folder";
		case PFS_ERR_CLOSED:
			return "file system is closed";
		default:
			return "unknown errno value: <0x" + Integer.toHexString(pfs_errno) + ">";
		}
	}
	
	static PatrFileSysIOException createAndThrow(int pfs_errno, String msg) throws IOException, RuntimeException {
		switch (pfs_errno) {
		case PFS_ERR_NONE:
		case PFS_ERR_UNKNOWN_ERROR:
		default:
			throw new PatrFileSysIOException(pfs_errno, msg);
		case PFS_ERR_NO_MORE_ELEMNETS:
			throw new PatrFileSysNoSuchElementException(pfs_errno, msg);
		case PFS_ERR_ELEMENT_WRONG_TYPE:
		case PFS_ERR_ELEMENT_NOT_EXIST:
			throw new PatrFileSysNoSuchFileException(pfs_errno, msg);
		case PFS_ERR_ELEMENT_ALREADY_EXIST:
			throw new PatrFileSysFileAlreadyExistsException(pfs_errno, msg);
		case PFS_ERR_OUT_OF_SPACE:
		case PFS_ERR_IO_ERR:
		case PFS_ERR_ILLEGAL_ARG:
		case PFS_ERR_OUT_OF_MEMORY:
		case PFS_ERR_ROOT_FOLDER:
		case PFS_ERR_PARENT_IS_CHILD:
		case PFS_ERR_CLOSED:
			throw new PatrFileSysIOException(pfs_errno, msg);
		}
	}
	
}

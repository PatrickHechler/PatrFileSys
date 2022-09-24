package de.hechler.patrick.exceptions;



public interface PFSErr {
	
	/** if pfs_errno is not set/no error occurred */
	int PFS_ERRNO_NONE                  = 0;
	/** if an operation failed because of an unknown/unspecified error */
	int PFS_ERRNO_UNKNOWN_ERROR         = 1;
	/** if the iterator has no next element */
	int PFS_ERRNO_NO_MORE_ELEMNETS      = 2;
	/** if an IO operation failed because the element is not of the correct type (file expected, but folder or reverse) */
	int PFS_ERRNO_ELEMENT_WRONG_TYPE    = 3;
	/** if an IO operation failed because the element does not exist */
	int PFS_ERRNO_ELEMENT_NOT_EXIST     = 4;
	/** if an IO operation failed because the element already existed */
	int PFS_ERRNO_ELEMENT_ALREADY_EXIST = 5;
	/** if an IO operation failed because there was not enough space in the file system */
	int PFS_ERRNO_OUT_OF_SPACE          = 6;
	/** if an unspecified IO error occurred */
	int PFS_ERRNO_IO_ERR                = 7;
	/** if there was at least one invalid argument */
	int PFS_ERRNO_ILLEGAL_ARG           = 8;
	/** if an IO operation failed because there was not enough space in the file system */
	int PFS_ERRNO_OUT_OF_MEMORY         = 9;
	/** if an IO operation failed because the root folder has some restrictions */
	int PFS_ERRNO_ROOT_FOLDER           = 10;
	/** if an folder can not be moved because the new child (maybe a deep/indirect child) is a child of the folder */
	int PFS_ERRNO_PARENT_IS_CHILD       = 11;
	
	int pfs_errno();
	
	static String tos(int pfs_errno) {
		switch (pfs_errno) {
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
			return "unknown errno value: <0x" + Integer.toHexString(pfs_errno) + ">";
		}
	}

}

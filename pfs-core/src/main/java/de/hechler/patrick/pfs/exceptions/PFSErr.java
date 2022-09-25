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
	
}

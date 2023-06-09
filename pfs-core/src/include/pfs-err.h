//This file is part of the Patr File System Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.
/*
 * pfs-err.h
 *
 *  Created on: Jul 10, 2022
 *      Author: pat
 */

#ifndef PFS_ERR_H_
#define PFS_ERR_H_

#include "patr-file-sys.h"

#ifndef I_AM_CORE_PFS
extern ui32 pfs_err_val;
extern ui32 *pfs_err_loc;
#else
ui32 pfs_err_val;
ui32 *pfs_err_loc = &pfs_err_val;
#endif

enum pfs_errno {
	// GENERATED-CODE-START
	// this code-block is automatic generated, do not modify
	PFS_ERRNO_NONE                          = 0, /* indicates no error */
	PFS_ERRNO_UNKNOWN_ERROR                 = 1, /* indicates an unknown error */
	PFS_ERRNO_NO_MORE_ELEMENTS              = 2, /* indicates that there are no more params */
	PFS_ERRNO_ELEMENT_WRONG_TYPE            = 3, /* indicates that the element has not the wanted/allowed type */
	PFS_ERRNO_ELEMENT_NOT_EXIST             = 4, /* indicates that the element does not exist */
	PFS_ERRNO_ELEMENT_ALREADY_EXIST         = 5, /* indicates that the element already exists */
	PFS_ERRNO_OUT_OF_SPACE                  = 6, /* indicates that there is not enough space on the device */
	PFS_ERRNO_IO_ERR                        = 7, /* indicates an IO error */
	PFS_ERRNO_ILLEGAL_ARG                   = 8, /* indicates an illegal argument */
	PFS_ERRNO_ILLEGAL_SUPER_BLOCK           = 9, /* indicates that some magic value is invalid */
	PFS_ERRNO_OUT_OF_MEMORY                 = 10, /* indicates that the system is out of memory */
	PFS_ERRNO_ROOT_FOLDER                   = 11, /* indicates that the root folder does not support this operation */
	PFS_ERRNO_PARENT_IS_CHILD               = 12, /* indicates that the parent can't be made to it's own child */
	PFS_ERRNO_ELEMENT_USED                  = 13, /* indicates the element is still used somewhere else */
	PFS_ERRNO_OUT_OF_RANGE                  = 14, /* indicates that some value was outside of the allowed range */
	PFS_ERRNO_FOLDER_NOT_EMPTY              = 15, /* indicates that the operation failed, because only empty folders can be deleted */
	PFS_ERRNO_ELEMENT_DELETED               = 16, /* indicates that the operation failed, because the element was deleted */
	
	// here is the end of the automatic generated code-block
	// GENERATED-CODE-END
};

extern const char* pfs_error();

extern void pfs_perror(const char *msg);

#endif /* PFS_ERR_H_ */

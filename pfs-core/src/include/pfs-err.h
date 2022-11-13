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
extern
#endif
ui32 pfs_errno;

enum PFS_ERRNO {
    PFS_ERRNO_NONE                  = 0,  /* if pfs_errno is not set/no error occurred */
    PFS_ERRNO_UNKNOWN_ERROR         = 1,  /* if an operation failed because of an unknown/unspecified error */
    PFS_ERRNO_NO_MORE_ELEMNETS      = 2,  /* if the iterator has no next element */
    PFS_ERRNO_ELEMENT_WRONG_TYPE    = 3,  /* if an IO operation failed because the element is not of the correct type (file expected, but folder or reverse) */
    PFS_ERRNO_ELEMENT_NOT_EXIST     = 4,  /* if an IO operation failed because the element does not exist */
    PFS_ERRNO_ELEMENT_ALREADY_EXIST = 5,  /* if an IO operation failed because the element already existed */
    PFS_ERRNO_OUT_OF_SPACE          = 6,  /* if an IO operation failed because there was not enough space in the file system */
    PFS_ERRNO_IO_ERR                = 7,  /* if an unspecified IO error occurred */
    PFS_ERRNO_ILLEGAL_ARG           = 8,  /* if there was at least one invalid argument */
    PFS_ERRNO_ILLEGAL_MAGIC         = 9,  /* if there was an invalid magic value */
    PFS_ERRNO_OUT_OF_MEMORY         = 10, /* if an IO operation failed because there was not enough memory available */
    PFS_ERRNO_ROOT_FOLDER           = 11, /* if an IO operation failed because the root folder has some restrictions */
    PFS_ERRNO_PARENT_IS_CHILD       = 12, /* if an folder can not be moved because the new child (maybe a deep/indirect child) is a child of the folder */
};

extern const char* pfs_error();

extern void pfs_perror(const char *msg);

#endif /* PFS_ERR_H_ */

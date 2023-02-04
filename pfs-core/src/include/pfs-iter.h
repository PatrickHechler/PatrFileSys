/*
 * pfs-iter.h
 *
 *  Created on: Oct 15, 2022
 *      Author: pat
 */

#ifndef SRC_INCLUDE_PFS_ITER_H_
#define SRC_INCLUDE_PFS_ITER_H_

#include "patr-file-sys.h"

/*
 * closes the given folder iterator handle
 *
 * the given folder iterator handle ID may be reused by the system
 * when a new folder iterator handle is opened
 */
extern int pfs_iter_close(int ih);

/*
 * opens a new element handle for the next child element
 *
 * when there is no next child element -1 is returned and (*pfs_err_loc) is set to PFS_ERRNO_NO_MORE_ELEMENTS
 *
 * returns on success the child element handle and on error -1
 */
extern int pfs_iter_next(int ih);

#endif /* SRC_INCLUDE_PFS_ITER_H_ */

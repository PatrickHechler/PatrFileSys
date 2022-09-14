/*
 * pfs-err.h
 *
 *  Created on: Jul 10, 2022
 *      Author: pat
 */

#ifndef PFS_ERR_H_
#define PFS_ERR_H_

#include "patr-file-sys.h"

#ifndef I_AM_PFS
extern
#endif
ui64 pfs_errno;

#define PFS_ERRNO_NONE                  0x0000000000000000UL /* if no error occurred */
#define PFS_ERRNO_NO_MORE_ELEMNETS      0x0020000000000000UL /* if the iterator has no next element */
#define PFS_ERRNO_ELEMENT_WRONG_TYPE    0x0040000000000000UL /* if an IO operation failed because the element is not of the correct type (file expected, but folder or reverse) */
#define PFS_ERRNO_ELEMENT_NOT_EXIST     0x0080000000000000UL /* if an IO operation failed because the element does not exist */
#define PFS_ERRNO_ELEMENT_ALREADY_EXIST 0x0100000000000000UL /* if an IO operation failed because the element already existed */
#define PFS_ERRNO_OUT_OF_SPACE          0x0200000000000000UL /* if an IO operation failed because there was not enough space in the file system */
#define PFS_ERRNO_READ_ONLY             0x0400000000000000UL /* if an IO operation was denied because of read-only */
#define PFS_ERRNO_ELEMENT_LOCKED        0x0800000000000000UL /* if an IO operation was denied because of lock */
#define PFS_ERRNO_IO_ERR                0x1000000000000000UL /* if an unspecified IO error occurred */
#define PFS_ERRNO_ILLEGAL_ARG           0x2000000000000000UL /* if there was at least one invalid argument */
#define PFS_ERRNO_OUT_OF_MEMORY         0x4000000000000000UL /* if an IO operation failed because there was not enough space in the file system */
#define PFS_ERRNO_UNKNOWN_ERROR         0x8000000000000000UL /* if an operation failed because there was not enough space in the file system */

#endif /* PFS_ERR_H_ */

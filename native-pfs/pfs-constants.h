/*
 * pfs-constants.h
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#ifndef PFS_CONSTANTS_H_
#define PFS_CONSTANTS_H_

#include "pfs-err.h"

#ifndef PFS_INTERN_H_
struct pfs_place {
	i64 a;
	i64 b;
};
static_assert(offsetof(struct pfs_place, a)
== 0);
static_assert(offsetof(struct pfs_place, b)
== 8);
static_assert(sizeof(struct pfs_place) == 16);
#endif
typedef struct pfs_place element;

#define PFS_NO_TIME -1L
#define PFS_NO_LOCK 0L

#define PFS_MAGIC_START 0xF17565393C422698

// these flags are reserved for intern use
#define	PFS_FLAGS_RESERVED               0x0000FFFFU
#define	PFS_FLAGS_FOLDER                 0x00000001U
#define	PFS_FLAGS_FILE                   0x00000002U
#define	PFS_FLAGS_HELPER_FOLDER          0x00000004U
#define	PFS_FLAGS_FOLDER_SORTED          0x00000100U
#define	PFS_FLAGS_FOLDER_HASH            0x00000200U
#define	PFS_FLAGS_FILE_ENCRYPTED         0x00010000U
#define	PFS_FLAGS_HIDDEN         0x0000010000000000UL

#endif /* PFS_CONSTANTS_H_ */

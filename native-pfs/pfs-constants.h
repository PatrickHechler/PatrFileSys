/*
 * pfs-constants.h
 *
 *  Created on: Sep 3, 2022
 *      Author: pat
 */

#ifndef PFS_CONSTANTS_H_
#define PFS_CONSTANTS_H_

#include "patr-file-sys.h"
#include "pfs-err.h"

#define PFS_NO_TIME -1L
#define PFS_NO_LOCK 0L

#define PFS_MAGIC_START 0xF17565393C422698

enum pfs_flag {
	PFS_FLAGS_FOLDER         = 0x00000001,
	PFS_FLAGS_FILE           = 0x00000002,
	PFS_FLAGS_FOLDER_SORTED  = 0x00000100,
	PFS_FLAGS_FOLDER_HASH    = 0x00000200,
	PFS_FLAGS_FILE_ENCRYPTED = 0x00008000,
	PFS_FLAGS_HIDDEN         = 0x00010000,
};

#endif /* PFS_CONSTANTS_H_ */
